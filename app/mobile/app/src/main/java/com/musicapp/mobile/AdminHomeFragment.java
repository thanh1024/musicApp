package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);
        loadStats(view);
        bindQuickActions(view);
        return view;
    }

    private void bindQuickActions(View view) {
        View goUsers = view.findViewById(R.id.cardGoUsers);
        View goSongs = view.findViewById(R.id.cardGoSongs);
        View goArtists = view.findViewById(R.id.cardGoArtists);
        View goGenres = view.findViewById(R.id.cardGoGenres);

        if (goUsers != null) goUsers.setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity) ((AdminActivity) getActivity()).openUsers();
        });
        if (goSongs != null) goSongs.setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity) ((AdminActivity) getActivity()).openSongs();
        });
        if (goArtists != null) goArtists.setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity) ((AdminActivity) getActivity()).openArtists();
        });
        if (goGenres != null) goGenres.setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity) ((AdminActivity) getActivity()).openGenres();
        });
    }

    private void loadStats(View view) {
        ApiService api = RetrofitClient.getApiService(getContext());
        api.getAdminStats().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), "Không tải được thống kê (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONObject data = obj.optJSONObject("data");
                    if (data == null) return;

                    setNumber(view, R.id.textDashboardUsers, data.optLong("totalUsers", 0));
                    setNumber(view, R.id.textDashboardSongs, data.optLong("totalSongs", 0));
                    setNumber(view, R.id.textDashboardPlaylists, data.optLong("totalPlaylists", 0));
                    setNumber(view, R.id.textDashboardLikes, data.optLong("totalFavorites", 0));
                    setNumber(view, R.id.textDashboardLikesMini, data.optLong("totalFavorites", 0));
                    setNumber(view, R.id.textDashboardPlays, data.optLong("totalHistory", 0));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi parse stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setNumber(View root, int id, long n) {
        TextView tv = root.findViewById(id);
        if (tv == null) return;
        tv.setText(String.format("%,d", n));
    }
}
