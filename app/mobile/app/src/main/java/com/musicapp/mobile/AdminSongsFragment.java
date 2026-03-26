package com.musicapp.mobile;

import android.os.Bundle;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSongsFragment extends Fragment {
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_DATA = "data";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_GENRE = "genre";
    private static final String KEY_MOOD = "mood";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_FILE_URL = "fileUrl";
    private static final String ERROR_PREFIX = "Lỗi: ";

    private ApiService apiService;
    private List<Map<String, Object>> songsList = new ArrayList<>();
    private SongsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_songs, container, false);

        RecyclerView recyclerViewSongs = view.findViewById(R.id.recyclerViewAdminSongs);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(getContext()));

        apiService = RetrofitClient.getApiService(getContext());

        Button buttonRefresh = view.findViewById(R.id.buttonRefreshSongs);
        buttonRefresh.setOnClickListener(v -> loadSongs());

        Button buttonAddSong = view.findViewById(R.id.buttonAddSong);
        buttonAddSong.setOnClickListener(v -> showAddSongDialog());

        adapter = new SongsAdapter();
        recyclerViewSongs.setAdapter(adapter);

        loadSongs();

        return view;
    }

    private void loadSongs() {
        Call<JsonObject> call = apiService.getAdminSongs();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                handleLoadSongsResponse(response);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                handleLoadSongsFailure(t);
            }
        });
    }

    private void handleLoadSongsResponse(Response<JsonObject> response) {
        if (response.isSuccessful() && response.body() != null) {
            try {
                JsonObject jsonResponse = response.body();
                if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                    JsonArray songs = jsonResponse.getAsJsonArray(KEY_DATA);
                    parseSongsData(songs);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), ERROR_PREFIX + getString(jsonResponse, KEY_MESSAGE),
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Lỗi xử lý dữ liệu: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            showErrorMessage(response.code());
        }
    }

    private void parseSongsData(JsonArray songs) {
        songsList.clear();
        for (int i = 0; i < songs.size(); i++) {
            JsonObject song = songs.get(i).getAsJsonObject();
            Map<String, Object> songMap = createSongMap(song);
            songsList.add(songMap);
        }
    }

    private Map<String, Object> createSongMap(JsonObject song) {
        Map<String, Object> songMap = new HashMap<>();
        songMap.put(KEY_ID, song.get(KEY_ID).getAsLong());
        songMap.put(KEY_TITLE, getString(song, KEY_TITLE));
        songMap.put(KEY_ARTIST, getString(song, KEY_ARTIST));
        songMap.put(KEY_ALBUM, getString(song, KEY_ALBUM));
        songMap.put(KEY_GENRE, getString(song, KEY_GENRE));
        songMap.put(KEY_MOOD, getString(song, KEY_MOOD));
        songMap.put(KEY_DURATION, song.has(KEY_DURATION) ? song.get(KEY_DURATION).getAsInt() : 0);
        songMap.put(KEY_FILE_URL, getString(song, KEY_FILE_URL));
        return songMap;
    }

    private void handleLoadSongsFailure(Throwable t) {
        String errorMsg = "Lỗi kết nối: " + t.getMessage();
        if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
            errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
        } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
            errorMsg = "Kết nối timeout. Vui lòng thử lại.";
        }
        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
    }

    private void showErrorMessage(int code) {
        String errorMsg = "Lỗi kết nối";
        if (code == 401) {
            errorMsg = "Chưa đăng nhập hoặc token hết hạn";
        } else if (code == 403) {
            errorMsg = "Không có quyền admin. Vui lòng đăng nhập với tài khoản admin.";
        } else if (code >= 500) {
            errorMsg = "Lỗi server";
        }
        Toast.makeText(getContext(), errorMsg + " (Code: " + code + ")", Toast.LENGTH_LONG).show();
    }

    private void showAddSongDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_song, null);
        builder.setView(dialogView);
        builder.setTitle("Thêm bài hát mới");
        builder.setPositiveButton("Thêm", (dialog, which) -> createSongFromDialog(dialogView));
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void createSongFromDialog(View dialogView) {
        EditText editTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editArtist = dialogView.findViewById(R.id.editTextArtist);
        EditText editGenre = dialogView.findViewById(R.id.editTextGenre);
        EditText editFileUrl = dialogView.findViewById(R.id.editTextFileUrl);

        Map<String, Object> songData = new HashMap<>();
        songData.put(KEY_TITLE, editTitle.getText().toString());
        songData.put(KEY_ARTIST, editArtist.getText().toString());
        songData.put(KEY_GENRE, editGenre.getText().toString());
        songData.put(KEY_FILE_URL, editFileUrl.getText().toString());

        createSong(songData);
    }

    private void createSong(Map<String, Object> songData) {
        Call<JsonObject> call = apiService.createSong(songData);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                            Toast.makeText(getContext(), "Đã thêm bài hát", Toast.LENGTH_SHORT).show();
                            loadSongs();
                        } else {
                            Toast.makeText(getContext(), ERROR_PREFIX + getString(jsonResponse, KEY_MESSAGE),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), ERROR_PREFIX + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getContext(), ERROR_PREFIX + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> song = songsList.get(position);
            holder.textViewTitle.setText((String) song.get(KEY_TITLE));
            holder.textViewArtist.setText((String) song.get(KEY_ARTIST));
            holder.textViewGenre.setText((String) song.get(KEY_GENRE));

            Long songId = (Long) song.get(KEY_ID);
            holder.buttonDelete.setOnClickListener(v -> deleteSong(songId, position));
        }

        @Override
        public int getItemCount() {
            return songsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewTitle;
            TextView textViewArtist;
            TextView textViewGenre;
            Button buttonDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textViewTitle = itemView.findViewById(R.id.textViewSongTitle);
                textViewArtist = itemView.findViewById(R.id.textViewSongArtist);
                textViewGenre = itemView.findViewById(R.id.textViewSongGenre);
                buttonDelete = itemView.findViewById(R.id.buttonDeleteSong);
            }
        }
    }

    private void deleteSong(Long songId, int position) {
        Call<JsonObject> call = apiService.deleteSong(songId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                            songsList.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(getContext(), "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), ERROR_PREFIX + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getContext(), ERROR_PREFIX + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getString(JsonObject object, String key) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return "";
    }
}
