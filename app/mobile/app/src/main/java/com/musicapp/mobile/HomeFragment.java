package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.api.SongResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView recentRecyclerView;
    private ApiService apiService;
    private SongAdapter adapter;
    private RecentSongAdapter recentAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Layout mới dùng 2 recycler riêng cho recent/trending; dùng trending để tránh crash/compile error.
        recyclerView = view.findViewById(R.id.recyclerTrendingSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        recentRecyclerView = view.findViewById(R.id.recyclerRecentSongs);
        if (recentRecyclerView != null) {
            recentRecyclerView.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
            );
            recentRecyclerView.setNestedScrollingEnabled(false);
            recentRecyclerView.setHasFixedSize(true);
        }
        
        apiService = RetrofitClient.getApiService(getContext());
        adapter = new SongAdapter(getContext(), song -> {
            AudioPlayer.play(getContext(), song.getFileUrl(), song.getTitle());
            saveHistory(song);
        });
        recyclerView.setAdapter(adapter);

        if (recentRecyclerView != null) {
            recentAdapter = new RecentSongAdapter(getContext(), song -> {
                AudioPlayer.play(getContext(), song.getFileUrl(), song.getTitle());
                saveHistory(song);
            });
            recentRecyclerView.setAdapter(recentAdapter);
        }

        // Set greeting name theo user đăng nhập (fallback từ fullName -> username)
        setUserGreeting(view);
        
        loadTrendingSongs();
        loadRecentSongs();
        loadFavorites();
        bindStaticActions(view);
        
        return view;
    }

    private void setUserGreeting(View view) {
        try {
            String username = SessionManager.getUsername(getContext());
            android.content.SharedPreferences sp =
                    requireActivity().getSharedPreferences("MusicApp", android.content.Context.MODE_PRIVATE);
            String fullName = sp.getString("fullName", "");

            String displayName = (fullName != null && !fullName.trim().isEmpty())
                    ? fullName.trim()
                    : (username != null ? username.trim() : "");

            android.widget.TextView tvGreetingName = view.findViewById(R.id.tvGreetingName);
            android.widget.TextView tvUserName = view.findViewById(R.id.tvUserName);

            if (tvGreetingName != null && !displayName.isEmpty()) {
                tvGreetingName.setText(displayName + "!");
            }
            if (tvUserName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            }
        } catch (Exception ignored) {}
    }

    private void loadTrendingSongs() {
        Call<SongResponse> call = apiService.getAllSongs();
        call.enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SongResponse songResponse = response.body();
                    if (songResponse.isSuccess() && songResponse.getData() != null) {
                        List<SongResponse.Song> all = songResponse.getData();
                        if (all.isEmpty()) return;

                        // Sort by playCount desc (nếu null thì xem như 0)
                        List<SongResponse.Song> sorted = new ArrayList<>(all);
                        sorted.sort((a, b) -> {
                            int playA = a.getPlayCount() != null ? a.getPlayCount() : 0;
                            int playB = b.getPlayCount() != null ? b.getPlayCount() : 0;
                            return Integer.compare(playB, playA);
                        });

                        int limit = Math.min(10, sorted.size());
                        adapter.setSongs(sorted.subList(0, limit));
                    } else {
                        String message = songResponse.getMessage();
                        Toast.makeText(getContext(), message != null ? message : "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Lỗi tải danh sách bài hát";
                    if (response.code() == 401) {
                        errorMsg = "Chưa đăng nhập";
                    } else if (response.code() == 403) {
                        errorMsg = "Không có quyền truy cập";
                    } else if (response.code() >= 500) {
                        errorMsg = "Lỗi server";
                    }
                    Toast.makeText(getContext(), errorMsg + " (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentSongs() {
        if (recentAdapter == null) return;

        Long userId = SessionManager.getUserId(getContext());
        String username = SessionManager.getUsername(getContext());
        if (userId == null || username == null) return;

        apiService.getHistory(userId, username).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                SongResponse songResponse = response.body();
                if (!songResponse.isSuccess() || songResponse.getData() == null) return;

                List<SongResponse.Song> data = songResponse.getData();
                if (data.isEmpty()) return;

                // Lấy vài bài gần đây để hiển thị ngang (UI cũ đang chỉ có 2 card)
                int limit = Math.min(8, data.size());
                recentAdapter.setSongs(data.subList(0, limit));
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) { }
        });
    }

    private void loadFavorites() {
        // favorites cần login; nếu chưa login thì skip
        apiService.getFavorites(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext())).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Set<Long> ids = new HashSet<>();
                    for (SongResponse.Song s : response.body().getData()) {
                        if (s.getId() != null) ids.add(s.getId());
                    }
                    adapter.setFavoriteSongIds(ids);
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) { }
        });
    }

    private void saveHistory(SongResponse.Song song) {
        if (song == null || song.getId() == null) return;
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("songId", song.getId());
        Long userId = SessionManager.getUserId(getContext());
        if (userId != null) body.put("userId", userId);
        String username = SessionManager.getUsername(getContext());
        if (username != null) body.put("username", username);
        apiService.addHistory(body).enqueue(new retrofit2.Callback<org.json.JSONObject>() {
            @Override public void onResponse(retrofit2.Call<org.json.JSONObject> call, retrofit2.Response<org.json.JSONObject> response) { }
            @Override public void onFailure(retrofit2.Call<org.json.JSONObject> call, Throwable t) { }
        });
    }

    private void bindStaticActions(View view) {
        View btnEmotion = view.findViewById(R.id.btnCaptureEmotion);
        if (btnEmotion != null) {
            btnEmotion.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), EmotionActivity.class)));
        }
        View moodHappy = view.findViewById(R.id.cardMoodHappy);
        View moodSad = view.findViewById(R.id.cardMoodSad);
        View moodChill = view.findViewById(R.id.cardMoodChill);
        View moodFocus = view.findViewById(R.id.cardMoodFocus);
        if (moodHappy != null) moodHappy.setOnClickListener(v -> loadByMood("Vui"));
        if (moodSad != null) moodSad.setOnClickListener(v -> loadByMood("Buồn"));
        if (moodChill != null) moodChill.setOnClickListener(v -> loadByMood("Thư giãn"));
        if (moodFocus != null) moodFocus.setOnClickListener(v -> loadByMood("Bình thường"));
    }

    private void loadByMood(String mood) {
        apiService.getSongsByMood(mood).enqueue(new Callback<SongResponse>() {
            @Override public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    adapter.setSongs(response.body().getData());
                }
            }
            @Override public void onFailure(Call<SongResponse> call, Throwable t) { }
        });
    }
}
