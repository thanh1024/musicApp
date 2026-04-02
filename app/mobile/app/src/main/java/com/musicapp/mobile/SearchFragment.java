package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.api.SongResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private EditText editTextSearch;
    private ImageButton buttonSearch;
    private ApiService apiService;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private final java.util.List<String> popularGenres = new java.util.ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        editTextSearch = view.findViewById(R.id.editTextSearch);
        // UI mới không có nút "Search" riêng; dùng nút mic như một action button tạm thời.
        buttonSearch = view.findViewById(R.id.btnVoiceSearch);
        ImageButton buttonBack = view.findViewById(R.id.btnBackSearch);
        recyclerView = view.findViewById(R.id.recyclerSearchResults);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new SongAdapter(getContext(), song -> {
                AudioPlayer.play(getContext(), song.getFileUrl(), song.getTitle());
                // click -> lưu history
                if (song != null && song.getId() != null) {
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
            });
            recyclerView.setAdapter(adapter);
        }
        
        apiService = RetrofitClient.getApiService(getContext());
        
        if (buttonSearch != null) {
            buttonSearch.setOnClickListener(v -> {
                String keyword = editTextSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    searchSongs(keyword);
                }
            });
        }
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        requireActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) nav.setSelectedItemId(R.id.nav_home);
            });
        }

        bindQuickFilters(view);
        loadPopularGenres(view);
        
        return view;
    }

    private void bindQuickFilters(View view) {
        int[] artistChips = new int[]{R.id.chipRecentArtist1, R.id.chipRecentArtist2, R.id.chipRecentArtist3};
        for (int id : artistChips) {
            View chip = view.findViewById(id);
            if (chip instanceof android.widget.TextView) {
                chip.setOnClickListener(v -> {
                    String keyword = ((android.widget.TextView) v).getText().toString();
                    editTextSearch.setText(keyword);
                    searchSongs(keyword);
                });
            }
        }

        View chipSad = view.findViewById(R.id.chipGenreSad);
        if (chipSad != null) chipSad.setOnClickListener(v -> loadByGenre("Ballad"));

        // Genre cards are populated dynamically from DB in loadPopularGenres()
    }

    private void loadPopularGenres(View view) {
        View[] cards = new View[]{
                view.findViewById(R.id.cardGenreNhacTre),
                view.findViewById(R.id.cardGenreBolero),
                view.findViewById(R.id.cardGenreRap),
                view.findViewById(R.id.cardGenreEdm),
                view.findViewById(R.id.cardGenreIndie),
                view.findViewById(R.id.cardGenreLofi)
        };

        apiService.getGenres().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONArray arr = obj.optJSONArray("data");
                    popularGenres.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject g = arr.optJSONObject(i);
                            if (g == null) continue;
                            String name = g.optString("name", "").trim();
                            if (!name.isEmpty()) popularGenres.add(name);
                        }
                    }
                } catch (Exception ignored) {}

                // Bind up to 6 cards
                for (int i = 0; i < cards.length; i++) {
                    View card = cards[i];
                    if (card == null) continue;
                    if (i >= popularGenres.size()) {
                        card.setVisibility(View.GONE);
                        continue;
                    }
                    card.setVisibility(View.VISIBLE);
                    String genreName = popularGenres.get(i);

                    // The first child TextView inside card is the label
                    if (card instanceof android.view.ViewGroup) {
                        android.view.ViewGroup vg = (android.view.ViewGroup) card;
                        for (int j = 0; j < vg.getChildCount(); j++) {
                            View child = vg.getChildAt(j);
                            if (child instanceof android.widget.TextView) {
                                ((android.widget.TextView) child).setText(genreName);
                                break;
                            }
                        }
                    }

                    card.setOnClickListener(v -> loadByGenre(genreName));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void searchSongs(String keyword) {
        Call<SongResponse> call = apiService.searchSongs(keyword);
        call.enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SongResponse songResponse = response.body();
                    if (songResponse.isSuccess() && songResponse.getData() != null) {
                        int count = songResponse.getData().size();
                        Toast.makeText(getContext(), "Tìm thấy " + count + " bài hát", Toast.LENGTH_SHORT).show();
                        if (adapter != null) adapter.setSongs(songResponse.getData());
                    } else {
                        String message = songResponse.getMessage();
                        Toast.makeText(getContext(), message != null ? message : "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadByGenre(String genre) {
        apiService.getSongsByGenre(genre).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    if (adapter != null) adapter.setSongs(response.body().getData());
                } else {
                    Toast.makeText(getContext(), "Không có dữ liệu " + genre, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
