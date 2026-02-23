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

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerViewSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        apiService = RetrofitClient.getApiService(getContext());
        loadSongs();
        
        return view;
    }

    private void loadSongs() {
        Call<SongResponse> call = apiService.getAllSongs();
        call.enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SongResponse songResponse = response.body();
                    if (songResponse.isSuccess() && songResponse.getData() != null) {
                        // Set adapter với danh sách bài hát
                        // SongAdapter adapter = new SongAdapter(songResponse.getData());
                        // recyclerView.setAdapter(adapter);
                        Toast.makeText(getContext(), "Đã tải " + songResponse.getData().size() + " bài hát", Toast.LENGTH_SHORT).show();
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
}
