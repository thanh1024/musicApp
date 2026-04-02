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
import okhttp3.ResponseBody;
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
    private List<Map<String, Object>> filteredUsers = new ArrayList<>();
    private UsersAdapter adapter;
    private TextView chipAll;
    private TextView chipAdmins;
    private TextView chipActive;
    private TextView chipLocked;
    private String currentFilter = "ALL"; // ALL | ADMINS | ACTIVE | LOCKED

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        apiService = RetrofitClient.getApiService(getContext());
        sharedPreferences = getActivity().getSharedPreferences("MusicApp", 0);

        Button buttonRefresh = view.findViewById(R.id.buttonRefreshUsers);
        buttonRefresh.setOnClickListener(v -> loadUsers());
        Button buttonAddUser = view.findViewById(R.id.buttonAddUser);
        if (buttonAddUser != null) {
            buttonAddUser.setOnClickListener(v -> showCreateUserDialog());
        }
        EditText editSearch = view.findViewById(R.id.editSearchUsers);
        if (editSearch != null) {
            editSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsers(s.toString()); }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });
        }

        adapter = new UsersAdapter();
        recyclerViewUsers.setAdapter(adapter);

        bindChips(view);
        loadUsers();

        return view;
    }

    private void bindChips(View view) {
        chipAll = view.findViewById(R.id.chipUsersAll);
        chipAdmins = view.findViewById(R.id.chipUsersAdmins);
        chipActive = view.findViewById(R.id.chipUsersActive);
        chipLocked = view.findViewById(R.id.chipUsersLocked);

        if (chipAll != null) chipAll.setOnClickListener(v -> { currentFilter = "ALL"; updateChipUI(); applyFilter(); });
        if (chipAdmins != null) chipAdmins.setOnClickListener(v -> { currentFilter = "ADMINS"; updateChipUI(); applyFilter(); });
        if (chipActive != null) chipActive.setOnClickListener(v -> { currentFilter = "ACTIVE"; updateChipUI(); applyFilter(); });
        if (chipLocked != null) chipLocked.setOnClickListener(v -> { currentFilter = "LOCKED"; updateChipUI(); applyFilter(); });
        updateChipUI();
    }

    private void updateChipUI() {
        TextView[] chips = new TextView[]{chipAll, chipAdmins, chipActive, chipLocked};
        for (TextView c : chips) {
            if (c == null) continue;
            boolean selected =
                    (c == chipAll && "ALL".equals(currentFilter)) ||
                    (c == chipAdmins && "ADMINS".equals(currentFilter)) ||
                    (c == chipActive && "ACTIVE".equals(currentFilter)) ||
                    (c == chipLocked && "LOCKED".equals(currentFilter));
            c.setBackgroundResource(selected ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            c.setTextColor(getResources().getColor(selected ? R.color.text_primary : R.color.text_secondary));
        }
    }

    private void applyFilter() {
        filteredUsers.clear();
        for (Map<String, Object> u : usersList) {
            String role = String.valueOf(u.get("role"));
            boolean isActive = Boolean.TRUE.equals(u.get("isActive"));
            if ("ADMINS".equals(currentFilter) && !"ROLE_ADMIN".equals(role)) continue;
            if ("ACTIVE".equals(currentFilter) && !isActive) continue;
            if ("LOCKED".equals(currentFilter) && isActive) continue;
            filteredUsers.add(u);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadUsers() {
        Call<ResponseBody> call = apiService.getAdminUsers();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonResponse = new JSONObject(json);
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
                            filteredUsers.clear();
                            // apply chip filter after reload
                            applyFilter();
                            adapter.notifyDataSetChanged();
                            TextView count = getView() != null ? getView().findViewById(R.id.textUsersCount) : null;
                            if (count != null) count.setText(String.valueOf(usersList.size()));
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
                        SessionManager.clearSession(requireContext());
                        startActivity(new android.content.Intent(getActivity(), LoginActivity.class));
                        if (getActivity() != null) getActivity().finish();
                    } else if (response.code() == 403) {
                        errorMsg = "Không có quyền admin. Vui lòng đăng nhập với tài khoản admin.";
                    } else if (response.code() >= 500) {
                        errorMsg = "Lỗi server";
                    }
                    Toast.makeText(getContext(), errorMsg + " (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
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
            Map<String, Object> user = filteredUsers.get(position);
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
            // item_admin_user.xml chỉ có 1 nút lock/unlock; đổi hành vi theo trạng thái.
            holder.buttonLockUser.setText(isActive ? "🔒 Khóa Tài Khoản" : "🔓 Mở Khóa");
            holder.buttonLockUser.setOnClickListener(v -> {
                if (isActive) {
                    lockUser(userId, position);
                } else {
                    unlockUser(userId, position);
                }
            });

            holder.buttonViewDetail.setOnClickListener(v ->
                    showUserDetailDialog(user));
            holder.buttonEditUser.setOnClickListener(v ->
                    showEditUserDialog(user, position));
        }

        @Override
        public int getItemCount() {
            return filteredUsers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewUsername, textViewEmail, textViewFullName, textViewRole, textViewStatus;
            Button buttonViewDetail, buttonEditUser, buttonLockUser;

            ViewHolder(View itemView) {
                super(itemView);
                textViewUsername = itemView.findViewById(R.id.textViewUsername);
                textViewEmail = itemView.findViewById(R.id.textViewEmail);
                textViewFullName = itemView.findViewById(R.id.textViewFullName);
                textViewRole = itemView.findViewById(R.id.textViewRole);
                textViewStatus = itemView.findViewById(R.id.textViewStatus);
                buttonViewDetail = itemView.findViewById(R.id.buttonViewUserDetail);
                buttonEditUser = itemView.findViewById(R.id.buttonEditUser);
                buttonLockUser = itemView.findViewById(R.id.buttonLockUser);
            }
        }
    }

    private void lockUser(Long userId, int position) {
        Call<ResponseBody> call = apiService.lockUser(userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getBoolean("success")) {
                            loadUsers();
                            Toast.makeText(getContext(), "Đã khóa tài khoản", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unlockUser(Long userId, int position) {
        Call<ResponseBody> call = apiService.unlockUser(userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getBoolean("success")) {
                            loadUsers();
                            Toast.makeText(getContext(), "Đã mở khóa tài khoản", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUser(Long userId, int position) {
        Call<ResponseBody> call = apiService.deleteUser(userId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getBoolean("success")) {
                            loadUsers();
                            Toast.makeText(getContext(), "Đã xóa user", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String keyword) {
        String q = keyword != null ? keyword.trim().toLowerCase() : "";
        // base set by chip filter
        applyFilter();
        if (!q.isEmpty()) {
            java.util.Iterator<Map<String, Object>> it = filteredUsers.iterator();
            while (it.hasNext()) {
                Map<String, Object> u = it.next();
                String username = String.valueOf(u.get("username")).toLowerCase();
                String email = String.valueOf(u.get("email")).toLowerCase();
                String fullName = String.valueOf(u.get("fullName")).toLowerCase();
                if (!(username.contains(q) || email.contains(q) || fullName.contains(q))) it.remove();
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showUserDetailDialog(Map<String, Object> user) {
        if (user == null) return;
        String msg = "ID: " + user.get("id") + "\n"
                + "Username: " + user.get("username") + "\n"
                + "Email: " + user.get("email") + "\n"
                + "Tên: " + user.get("fullName") + "\n"
                + "Role: " + user.get("role") + "\n"
                + "Trạng thái: " + (((Boolean) user.get("isActive")) ? "Hoạt động" : "Đã khóa");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Chi tiết người dùng")
                .setMessage(msg)
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Xóa user", (d, w) -> {
                    Long userId = ((Number) user.get("id")).longValue();
                    int pos = filteredUsers.indexOf(user);
                    if (pos >= 0) deleteUser(userId, pos);
                })
                .show();
    }

    private void showEditUserDialog(Map<String, Object> user, int position) {
        if (user == null) return;
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 0);

        EditText edFullName = new EditText(getContext());
        edFullName.setHint("Full name");
        edFullName.setText(String.valueOf(user.get("fullName")));
        layout.addView(edFullName);

        EditText edEmail = new EditText(getContext());
        edEmail.setHint("Email");
        edEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edEmail.setText(String.valueOf(user.get("email")));
        layout.addView(edEmail);

        EditText edRole = new EditText(getContext());
        edRole.setHint("ROLE_USER hoặc ROLE_ADMIN");
        edRole.setText(String.valueOf(user.get("role")));
        layout.addView(edRole);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật user")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    Long userId = ((Number) user.get("id")).longValue();
                    Map<String, Object> body = new HashMap<>();
                    body.put("fullName", edFullName.getText().toString().trim());
                    body.put("email", edEmail.getText().toString().trim());
                    body.put("role", edRole.getText().toString().trim());
                    apiService.updateUser(userId, body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                Toast.makeText(getContext(), "Cập nhật thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                JSONObject obj = new JSONObject(response.body().string());
                                if (obj.optBoolean("success", false)) {
                                    Toast.makeText(getContext(), "Đã cập nhật user", Toast.LENGTH_SHORT).show();
                                    loadUsers();
                                } else {
                                    Toast.makeText(getContext(), "Lỗi: " + obj.optString("message", "Cập nhật thất bại"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showCreateUserDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 0);
        EditText edUsername = new EditText(getContext()); edUsername.setHint("Username");
        EditText edEmail = new EditText(getContext()); edEmail.setHint("Email");
        EditText edPassword = new EditText(getContext()); edPassword.setHint("Password");
        EditText edFullName = new EditText(getContext()); edFullName.setHint("Full Name");
        EditText edRole = new EditText(getContext()); edRole.setHint("ROLE_USER / ROLE_ADMIN");
        edRole.setText("ROLE_USER");
        layout.addView(edUsername); layout.addView(edEmail); layout.addView(edPassword); layout.addView(edFullName); layout.addView(edRole);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Thêm người dùng")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thêm", (d, w) -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("username", edUsername.getText().toString().trim());
                    body.put("email", edEmail.getText().toString().trim());
                    body.put("password", edPassword.getText().toString().trim());
                    body.put("fullName", edFullName.getText().toString().trim());
                    body.put("role", edRole.getText().toString().trim());
                    apiService.createAdminUser(body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                Toast.makeText(getContext(), "Thêm thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                JSONObject obj = new JSONObject(response.body().string());
                                if (obj.optBoolean("success", false)) {
                                    Toast.makeText(getContext(), "Đã thêm người dùng", Toast.LENGTH_SHORT).show();
                                    loadUsers();
                                } else {
                                    Toast.makeText(getContext(), "Lỗi: " + obj.optString("message", "Thêm thất bại"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
