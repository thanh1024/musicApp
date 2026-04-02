package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminArtistsFragment extends Fragment {
    private ApiService api;
    private RecyclerView recycler;
    private final List<Map<String, Object>> items = new ArrayList<>();
    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_artists, container, false);
        api = RetrofitClient.getApiService(getContext());
        recycler = view.findViewById(R.id.recyclerAdminArtists);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        Button btnRefresh = view.findViewById(R.id.buttonRefreshArtists);
        Button btnAdd = view.findViewById(R.id.buttonAddArtist);
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> load());
        if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddDialog());
        load();
        return view;
    }

    private void load() {
        api.getAdminArtists().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), "Lỗi tải artists (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONArray data = obj.optJSONArray("data");
                    items.clear();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject a = data.optJSONObject(i);
                            if (a == null) continue;
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", a.optLong("id"));
                            m.put("name", a.optString("name", ""));
                            items.add(m);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi parse artists: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Tên nghệ sĩ");
        input.setTextColor(getResources().getColor(R.color.text_primary));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Thêm nghệ sĩ")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thêm", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    api.createAdminArtist(Map.of("name", name)).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Đã thêm nghệ sĩ", Toast.LENGTH_SHORT).show();
                                load();
                            } else {
                                Toast.makeText(getContext(), "Thêm thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void delete(long id) {
        api.deleteAdminArtist(id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa", Toast.LENGTH_SHORT).show();
                    load();
                } else {
                    Toast.makeText(getContext(), "Xóa thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_simple, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Map<String, Object> m = items.get(position);
            holder.title.setText(String.valueOf(m.get("name")));
            long id = ((Number) m.get("id")).longValue();
            holder.btnDelete.setOnClickListener(v -> delete(id));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final android.widget.TextView title;
            final android.widget.Button btnDelete;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textTitle);
                btnDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }
    }
}

