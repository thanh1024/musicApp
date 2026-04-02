package com.musicapp.mobile;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistFragment extends Fragment {
    private ApiService apiService;
    private RecyclerView recyclerPlaylists;
    private PlaylistExpandableAdapter adapter;
    private SongAdapter songAdapter;
    private Button btnCreatePlaylist;
    private View rootView;
    private TextView tabMine;
    private TextView tabFavorites;
    private TextView tabRecent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        rootView = view;
        apiService = RetrofitClient.getApiService(getContext());

        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);
        recyclerPlaylists = view.findViewById(R.id.recyclerPlaylists);
        if (recyclerPlaylists != null) {
            recyclerPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new PlaylistExpandableAdapter(getContext(), new PlaylistExpandableAdapter.Listener() {
                @Override
                public void onPlaylistClicked(long playlistId) {
                    loadPlaylistSongs(playlistId);
                }

                @Override
                public void onSongClicked(SongResponse.Song song) {
                    AudioPlayer.play(getContext(), song != null ? song.getFileUrl() : null, song != null ? song.getTitle() : null);
                    saveHistory(song);
                }
            });
            songAdapter = new SongAdapter(getContext(), song -> {
                AudioPlayer.play(getContext(), song != null ? song.getFileUrl() : null, song != null ? song.getTitle() : null);
                saveHistory(song);
            });
            recyclerPlaylists.setAdapter(adapter); // default: playlists
        }

        if (btnCreatePlaylist != null) {
            btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        }

        bindTopActions(view);
        bindTabs(view);
        selectTab(tabMine);
        loadPlaylists();
        return view;
    }

    private void bindTopActions(View view) {
        View btnBack = view.findViewById(R.id.btnBackPlaylist);
        View btnSearch = view.findViewById(R.id.btnSearchPlaylist);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        requireActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) nav.setSelectedItemId(R.id.nav_home);
            });
        }
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        requireActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) nav.setSelectedItemId(R.id.nav_search);
            });
        }
    }

    private void bindTabs(View view) {
        tabMine = view.findViewById(R.id.tabMine);
        tabFavorites = view.findViewById(R.id.tabFavorites);
        tabRecent = view.findViewById(R.id.tabRecent);

        if (tabMine != null) tabMine.setOnClickListener(v -> {
            selectTab(tabMine);
            if (recyclerPlaylists != null && adapter != null) recyclerPlaylists.setAdapter(adapter);
            loadPlaylists();
        });
        if (tabFavorites != null) tabFavorites.setOnClickListener(v -> { selectTab(tabFavorites); loadFavorites(); });
        if (tabRecent != null) tabRecent.setOnClickListener(v -> { selectTab(tabRecent); loadRecent(); });
    }

    private void selectTab(TextView selected) {
        TextView[] tabs = new TextView[]{tabMine, tabFavorites, tabRecent};
        for (TextView t : tabs) {
            if (t == null) continue;
            if (t == selected) {
                t.setBackgroundResource(R.drawable.bg_chip_active);
                t.setTextColor(getResources().getColor(R.color.color_purple_light));
            } else {
                t.setBackgroundResource(R.drawable.bg_chip_inactive);
                t.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Tên playlist");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getResources().getColor(R.color.text_primary));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Tạo playlist")
                .setView(input)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    Long userId = SessionManager.getUserId(getContext());
                    if (userId != null) body.put("userId", userId);
                    String username = SessionManager.getUsername(getContext());
                    if (username != null) body.put("username", username);
                    apiService.createPlaylist(body).enqueue(new Callback<JSONObject>() {
                        @Override
                        public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Đã tạo playlist", Toast.LENGTH_SHORT).show();
                                loadPlaylists();
                            } else {
                                Toast.makeText(getContext(), "Tạo thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<JSONObject> call, Throwable t) {
                            Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void loadPlaylists() {
        apiService.getPlaylists(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext())).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);
                        JSONArray arr = obj.optJSONArray("data");
                        if (adapter != null) adapter.setPlaylists(arr);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi parse playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Không tải được playlist (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        int[] staticItems = new int[] {R.id.itemPlaylist1, R.id.itemPlaylist2, R.id.itemPlaylist3, R.id.itemPlaylist4, R.id.itemPlaylist5, R.id.itemPlaylist6};
        for (int id : staticItems) {
            View item = rootView != null ? rootView.findViewById(id) : null;
            if (item != null) item.setVisibility(View.GONE);
        }
        View mini = rootView != null ? rootView.findViewById(R.id.miniPlayerPlaylist) : null;
        if (mini != null) mini.setVisibility(View.GONE);
    }

    private void loadFavorites() {
        apiService.getFavorites(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext())).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (recyclerPlaylists != null && songAdapter != null) {
                        recyclerPlaylists.setAdapter(songAdapter);
                        songAdapter.setSongs(response.body().getData());
                    }
                }
            }
            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi tải yêu thích: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecent() {
        apiService.getHistory(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext())).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (recyclerPlaylists != null && songAdapter != null) {
                        recyclerPlaylists.setAdapter(songAdapter);
                        songAdapter.setSongs(response.body().getData());
                    }
                }
            }
            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi tải gần đây: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylistSongs(long playlistId) {
        Long userId = SessionManager.getUserId(getContext());
        String username = SessionManager.getUsername(getContext());
        if (userId == null || username == null) return;

        if (recyclerPlaylists != null && adapter != null) recyclerPlaylists.setAdapter(adapter);
        apiService.getPlaylistSongs(playlistId, userId, username).enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (adapter != null && response.body().getData() != null) {
                        adapter.showSongsForExpanded(playlistId, response.body().getData());
                    } else if (adapter != null) {
                        adapter.showSongsForExpanded(playlistId, java.util.Collections.emptyList());
                    }
                } else {
                    Toast.makeText(getContext(), "Không tải được bài hát trong playlist (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
}
