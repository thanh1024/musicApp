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
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSongsFragment extends Fragment {
    private RecyclerView recyclerViewSongs;
    private ApiService apiService;
    private List<Map<String, Object>> songsList = new ArrayList<>();
    private SongsAdapter adapter;
    private View dialogView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_songs, container, false);

        recyclerViewSongs = view.findViewById(R.id.recyclerViewAdminSongs);
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
        Call<JSONObject> call = apiService.getAdminSongs();
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray songs = jsonResponse.getJSONArray("data");
                            songsList.clear();
                            for (int i = 0; i < songs.length(); i++) {
                                JSONObject song = songs.getJSONObject(i);
                                Map<String, Object> songMap = new HashMap<>();
                                songMap.put("id", song.getLong("id"));
                                songMap.put("title", song.getString("title"));
                                songMap.put("artist", song.getString("artist"));
                                songMap.put("album", song.optString("album", ""));
                                songMap.put("genre", song.getString("genre"));
                                songMap.put("mood", song.optString("mood", ""));
                                songMap.put("duration", song.getInt("duration"));
                                songMap.put("fileUrl", song.getString("fileUrl"));
                                songsList.add(songMap);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Lỗi kết nối";
                    if (response.code() == 401) {
                        errorMsg = "Chưa đăng nhập hoặc token hết hạn";
                    } else if (response.code() == 403) {
                        errorMsg = "Không có quyền admin. Vui lòng đăng nhập với tài khoản admin.";
                    } else if (response.code() >= 500) {
                        errorMsg = "Lỗi server";
                    }
                    Toast.makeText(getContext(), errorMsg + " (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
                } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMsg = "Kết nối timeout. Vui lòng thử lại.";
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddSongDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogView = inflater.inflate(R.layout.dialog_add_song, null);
        builder.setView(dialogView);
        builder.setTitle("Thêm bài hát mới");
        builder.setPositiveButton("Thêm", (dialog, which) -> {
            EditText editTitle = dialogView.findViewById(R.id.editTextTitle);
            EditText editArtist = dialogView.findViewById(R.id.editTextArtist);
            EditText editGenre = dialogView.findViewById(R.id.editTextGenre);
            EditText editFileUrl = dialogView.findViewById(R.id.editTextFileUrl);

            Map<String, Object> songData = new HashMap<>();
            songData.put("title", editTitle.getText().toString());
            songData.put("artist", editArtist.getText().toString());
            songData.put("genre", editGenre.getText().toString());
            songData.put("fileUrl", editFileUrl.getText().toString());

            createSong(songData);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void createSong(Map<String, Object> songData) {
        Call<JSONObject> call = apiService.createSong(songData);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Đã thêm bài hát", Toast.LENGTH_SHORT).show();
                            loadSongs();
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            holder.textViewTitle.setText((String) song.get("title"));
            holder.textViewArtist.setText((String) song.get("artist"));
            holder.textViewGenre.setText((String) song.get("genre"));

            Long songId = (Long) song.get("id");
            holder.buttonDelete.setOnClickListener(v -> deleteSong(songId, position));
        }

        @Override
        public int getItemCount() {
            return songsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewTitle, textViewArtist, textViewGenre;
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
        Call<JSONObject> call = apiService.deleteSong(songId);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            songsList.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(getContext(), "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
