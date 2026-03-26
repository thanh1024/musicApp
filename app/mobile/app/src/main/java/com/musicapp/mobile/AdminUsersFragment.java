package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class AdminUsersFragment extends Fragment {
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_DATA = "data";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ID = "id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IS_ACTIVE = "isActive";
    private static final String ERROR_PREFIX = "Lỗi: ";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private ApiService apiService;
    private List<Map<String, Object>> usersList = new ArrayList<>();
    private UsersAdapter adapter;
    private TextView textUsersCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        RecyclerView recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        textUsersCount = view.findViewById(R.id.textUsersCount);

        apiService = RetrofitClient.getApiService(getContext());

        Button buttonRefresh = view.findViewById(R.id.buttonRefreshUsers);
        buttonRefresh.setOnClickListener(v -> loadUsers());

        adapter = new UsersAdapter();
        recyclerViewUsers.setAdapter(adapter);

        loadUsers();

        return view;
    }

    private void loadUsers() {
        Call<JsonObject> call = apiService.getAdminUsers();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                handleLoadUsersResponse(response);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getContext(), ERROR_PREFIX + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoadUsersResponse(Response<JsonObject> response) {
        if (response.isSuccessful() && response.body() != null) {
            try {
                JsonObject jsonResponse = response.body();
                if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                    JsonArray users = jsonResponse.getAsJsonArray(KEY_DATA);
                    parseUsersData(users);
                    adapter.notifyDataSetChanged();
                    textUsersCount.setText(String.valueOf(usersList.size()));
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

    private void parseUsersData(JsonArray users) {
        usersList.clear();
        for (int i = 0; i < users.size(); i++) {
            JsonObject user = users.get(i).getAsJsonObject();
            Map<String, Object> userMap = createUserMap(user);
            usersList.add(userMap);
        }
    }

    private Map<String, Object> createUserMap(JsonObject user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put(KEY_ID, user.get(KEY_ID).getAsLong());
        userMap.put(KEY_USERNAME, getString(user, KEY_USERNAME));
        userMap.put(KEY_EMAIL, getString(user, KEY_EMAIL));
        userMap.put(KEY_FULL_NAME, getString(user, KEY_FULL_NAME));
        userMap.put(KEY_ROLE, user.has(KEY_ROLE) ? getString(user, KEY_ROLE) : DEFAULT_ROLE);
        userMap.put(KEY_IS_ACTIVE, user.has(KEY_IS_ACTIVE) && user.get(KEY_IS_ACTIVE).getAsBoolean());
        return userMap;
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
            holder.textViewUsername.setText((String) user.get(KEY_USERNAME));
            holder.textViewEmail.setText((String) user.get(KEY_EMAIL));
            holder.textViewFullName.setText((String) user.get(KEY_FULL_NAME));
            holder.textViewRole.setText((String) user.get(KEY_ROLE));
            
            boolean isActive = (Boolean) user.get(KEY_IS_ACTIVE);
            holder.textViewStatus.setText(isActive ? "Hoạt động" : "Đã khóa");
            holder.textViewStatus.setTextColor(isActive ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.holo_red_dark));

            Long userId = (Long) user.get(KEY_ID);
            holder.buttonLock.setOnClickListener(v -> lockUser(userId, position));
            holder.buttonUnlock.setOnClickListener(v -> unlockUser(userId, position));
            holder.buttonDelete.setOnClickListener(v -> deleteUser(userId, position));
        }

        @Override
        public int getItemCount() {
            return usersList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewUsername;
            TextView textViewEmail;
            TextView textViewFullName;
            TextView textViewRole;
            TextView textViewStatus;
            Button buttonLock;
            Button buttonUnlock;
            Button buttonDelete;

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
        Call<JsonObject> call = apiService.lockUser(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                            usersList.get(position).put(KEY_IS_ACTIVE, false);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(getContext(), "Đã khóa tài khoản", Toast.LENGTH_SHORT).show();
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

    private void unlockUser(Long userId, int position) {
        Call<JsonObject> call = apiService.unlockUser(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                            usersList.get(position).put(KEY_IS_ACTIVE, true);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(getContext(), "Đã mở khóa tài khoản", Toast.LENGTH_SHORT).show();
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

    private void deleteUser(Long userId, int position) {
        Call<JsonObject> call = apiService.deleteUser(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        if (jsonResponse.has(KEY_SUCCESS) && jsonResponse.get(KEY_SUCCESS).getAsBoolean()) {
                            usersList.remove(position);
                            adapter.notifyItemRemoved(position);
                            textUsersCount.setText(String.valueOf(usersList.size()));
                            Toast.makeText(getContext(), "Đã xóa user", Toast.LENGTH_SHORT).show();
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
