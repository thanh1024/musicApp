package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.api.SongResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private ApiService apiService;
    private final List<SongResponse.Song> recentSongs = new ArrayList<>();
    private RecentSongsAdapter recentSongsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerRecentSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentSongsAdapter = new RecentSongsAdapter();
        recyclerView.setAdapter(recentSongsAdapter);

        apiService = RetrofitClient.getApiService(getContext());
        loadSongs();

        return view;
    }

    private void loadSongs() {
        Call<SongResponse> call = apiService.getAllSongs();
        call.enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                handleLoadResponse(response);
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoadResponse(Response<SongResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            SongResponse songResponse = response.body();
            handleSuccessResponse(songResponse);
        } else {
            handleErrorResponse(response.code());
        }
    }

    private void handleSuccessResponse(SongResponse songResponse) {
        if (songResponse.isSuccess() && songResponse.getData() != null) {
            recentSongs.clear();
            recentSongs.addAll(songResponse.getData());
            recentSongsAdapter.notifyDataSetChanged();
        } else {
            String message = songResponse.getMessage();
            Toast.makeText(getContext(), message != null ? message : "Không có dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleErrorResponse(int code) {
        String errorMsg = "Lỗi tải danh sách bài hát";
        if (code == 401) {
            errorMsg = "Chưa đăng nhập";
        } else if (code == 403) {
            errorMsg = "Không có quyền truy cập";
        } else if (code >= 500) {
            errorMsg = "Lỗi server";
        }
        Toast.makeText(getContext(), errorMsg + " (Code: " + code + ")", Toast.LENGTH_SHORT).show();
    }

    private class RecentSongsAdapter extends RecyclerView.Adapter<RecentSongsAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SongResponse.Song song = recentSongs.get(position);
            holder.titleView.setText(song.getTitle() != null ? song.getTitle() : "Chưa có tiêu đề");
            holder.artistView.setText(song.getArtist() != null ? song.getArtist() : "Chưa có nghệ sĩ");
            holder.itemView.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Đang phát: " + holder.titleView.getText(), Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return recentSongs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            TextView artistView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.textRecentTitle);
                artistView = itemView.findViewById(R.id.textRecentArtist);
            }
        }
    }
}
