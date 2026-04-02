package com.musicapp.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.api.SongResponse;

import org.json.JSONObject;
import org.json.JSONArray;
import okhttp3.ResponseBody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    public interface Listener {
        void onSongClicked(SongResponse.Song song);
    }

    private final Context context;
    private final Listener listener;
    private final ApiService apiService;
    private final Long userId;
    private final String username;

    private final List<SongResponse.Song> songs = new ArrayList<>();
    private final Set<Long> favoriteSongIds = new HashSet<>();

    public SongAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.apiService = RetrofitClient.getApiService(context);
        this.userId = SessionManager.getUserId(context);
        this.username = SessionManager.getUsername(context);
    }

    public void setSongs(List<SongResponse.Song> newSongs) {
        songs.clear();
        if (newSongs != null) songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public void setFavoriteSongIds(Set<Long> ids) {
        favoriteSongIds.clear();
        if (ids != null) favoriteSongIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SongResponse.Song song = songs.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(song.getTitle() != null ? song.getTitle() : "");
        holder.tvArtist.setText(song.getArtist() != null ? song.getArtist() : "");

        boolean liked = song.getId() != null && favoriteSongIds.contains(song.getId());
        holder.btnLike.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClicked(song);
        });

        holder.btnLike.setOnClickListener(v -> toggleFavorite(song));
        holder.btnMore.setOnClickListener(v -> showAddToPlaylistDialog(song));
    }

    private void toggleFavorite(SongResponse.Song song) {
        if (song == null || song.getId() == null) return;
        Long songId = song.getId();
        boolean liked = favoriteSongIds.contains(songId);

        Call<JSONObject> call = liked
                ? apiService.removeFavorite(songId, userId, username)
                : apiService.addFavorite(songId, userId, username);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful()) {
                    if (liked) favoriteSongIds.remove(songId); else favoriteSongIds.add(songId);
                    notifyDataSetChanged();
                } else {
                    Toast.makeText(context, "Like thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddToPlaylistDialog(SongResponse.Song song) {
        if (song == null || song.getId() == null) return;
        if (userId == null || username == null) {
            Toast.makeText(context, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load playlists then show picker
        apiService.getPlaylists(userId, username).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(context, "Không tải được playlist (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONArray arr;
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    arr = obj.optJSONArray("data");
                } catch (Exception e) {
                    Toast.makeText(context, "Lỗi parse playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (arr == null || arr.length() == 0) {
                    Toast.makeText(context, "Bạn chưa có playlist nào", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Long> playlistIds = new ArrayList<>();
                List<String> playlistNames = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.optJSONObject(i);
                    if (p == null) continue;
                    long pid = p.optLong("id", -1L);
                    String name = p.optString("name", "");
                    if (pid > 0 && name != null && !name.trim().isEmpty()) {
                        playlistIds.add(pid);
                        playlistNames.add(name.trim());
                    }
                }

                if (playlistIds.isEmpty()) {
                    Toast.makeText(context, "Không có playlist hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = "Thêm vào playlist";
                String songTitle = song.getTitle() != null ? song.getTitle().trim() : "";
                if (!songTitle.isEmpty()) title = title + "\n" + songTitle;

                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setItems(playlistNames.toArray(new String[0]), (d, which) -> {
                            Long playlistId = playlistIds.get(which);
                            addSongToPlaylist(song.getId(), playlistId);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSongToPlaylist(Long songId, Long playlistId) {
        if (songId == null || playlistId == null) return;
        apiService.addSongToPlaylist(playlistId, songId, userId, username).enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Thêm thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvTitle;
        TextView tvArtist;
        ImageButton btnLike;
        ImageButton btnMore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvIndex);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}

