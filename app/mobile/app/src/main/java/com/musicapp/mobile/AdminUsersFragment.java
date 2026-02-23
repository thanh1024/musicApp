package com.musicapp.mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class AdminUsersFragment extends Fragment {
    private RecyclerView recyclerViewUsers;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private List<Map<String, Object>> usersList = new ArrayList<>();
    private UsersAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        apiService = RetrofitClient.getApiService(getContext());
        sharedPreferences = getActivity().getSharedPreferences("MusicApp", 0);

        Button buttonRefresh = view.findViewById(R.id.buttonRefreshUsers);
        buttonRefresh.setOnClickListener(v -> loadUsers());

        adapter = new UsersAdapter();
        recyclerViewUsers.setAdapter(adapter);

        loadUsers();

        return view;
    }

    private void loadUsers() {
        Call<JSONObject> call = apiService.getAdminUsers();
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray users = jsonResponse.getJSONArray("data");
                            usersList.clear();
                            for (int i = 0; i < users.length(); i++) {
                                JSONObject user = users.getJSONObject(i);
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("id", user.getLong("id"));
                                userMap.put("username", user.getString("username"));
                                userMap.put("email", user.getString("email"));
                                userMap.put("fullName", user.optString("fullName", ""));
                                userMap.put("role", user.optString("role", "ROLE_USER"));
                                userMap.put("isActive", user.getBoolean("isActive"));
                                usersList.add(userMap);
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
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> user = usersList.get(position);
            holder.textViewUsername.setText((String) user.get("username"));
            holder.textViewEmail.setText((String) user.get("email"));
            holder.textViewFullName.setText((String) user.get("fullName"));
            holder.textViewRole.setText((String) user.get("role"));
            
            boolean isActive = (Boolean) user.get("isActive");
            holder.textViewStatus.setText(isActive ? "Hoạt động" : "Đã khóa");
            holder.textViewStatus.setTextColor(isActive ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.holo_red_dark));

            Long userId = (Long) user.get("id");
            holder.buttonLock.setOnClickListener(v -> lockUser(userId, position));
            holder.buttonUnlock.setOnClickListener(v -> unlockUser(userId, position));
            holder.buttonDelete.setOnClickListener(v -> deleteUser(userId, position));
        }

        @Override
        public int getItemCount() {
            return usersList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewUsername, textViewEmail, textViewFullName, textViewRole, textViewStatus;
            Button buttonLock, buttonUnlock, buttonDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textViewUsername = itemView.findViewById(R.id.textViewUsername);
                textViewEmail = itemView.findViewById(R.id.textViewEmail);
                textViewFullName = itemView.findViewById(R.id.textViewFullName);
                textViewRole = itemView.findViewById(R.id.textViewRole);
                textViewStatus = itemView.findViewById(R.id.textViewStatus);
                buttonLock = itemView.findViewById(R.id.buttonLock);
                buttonUnlock = itemView.findViewById(R.id.buttonUnlock);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }
    }

    private void lockUser(Long userId, int position) {
        Call<JSONObject> call = apiService.lockUser(userId);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            usersList.get(position).put("isActive", false);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(getContext(), "Đã khóa tài khoản", Toast.LENGTH_SHORT).show();
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

    private void unlockUser(Long userId, int position) {
        Call<JSONObject> call = apiService.unlockUser(userId);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            usersList.get(position).put("isActive", true);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(getContext(), "Đã mở khóa tài khoản", Toast.LENGTH_SHORT).show();
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

    private void deleteUser(Long userId, int position) {
        Call<JSONObject> call = apiService.deleteUser(userId);
        call.enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = response.body();
                        if (jsonResponse.getBoolean("success")) {
                            usersList.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(getContext(), "Đã xóa user", Toast.LENGTH_SHORT).show();
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
