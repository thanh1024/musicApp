package com.musicapp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private TextView textViewUsername;
    private Button buttonLogout;
    private SharedPreferences sharedPreferences;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        
        textViewUsername = view.findViewById(R.id.textViewUsername);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        Button buttonProfile = view.findViewById(R.id.buttonProfile);
        Button buttonChangePassword = view.findViewById(R.id.buttonChangePassword);
        
        sharedPreferences = getActivity().getSharedPreferences("MusicApp", getActivity().MODE_PRIVATE);
        apiService = RetrofitClient.getApiService(getContext());
        String username = sharedPreferences.getString("username", "");
        String role = sharedPreferences.getString("role", "ROLE_USER");
        textViewUsername.setText("Username: " + username);
        loadMe();
        
        // Chỉ hiển thị nút Admin nếu là admin
        Button buttonAdmin = view.findViewById(R.id.buttonAdmin);
        if (buttonAdmin != null) {
            if ("ROLE_ADMIN".equals(role)) {
                buttonAdmin.setVisibility(View.VISIBLE);
                buttonAdmin.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), AdminActivity.class);
                    startActivity(intent);
                });
            } else {
                buttonAdmin.setVisibility(View.GONE);
            }
        }
        
        buttonLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });

        if (buttonProfile != null) {
            buttonProfile.setOnClickListener(v -> showEditProfileDialog());
        }
        if (buttonChangePassword != null) {
            buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
        
        return view;
    }

    private void loadMe() {
        apiService.me(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext())).enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject data = response.body().optJSONObject("data");
                    if (data != null) {
                        String username = data.optString("username", "");
                        String fullName = data.optString("fullName", "");
                        if (!username.isEmpty()) textViewUsername.setText("Username: " + username + (fullName.isEmpty() ? "" : ("\nTên: " + fullName)));
                    }
                }
            }

            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) { }
        });
    }

    private void showEditProfileDialog() {
        View form = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, null);
        EditText inputName = new EditText(getContext());
        inputName.setHint("Tên hiển thị");
        inputName.setInputType(InputType.TYPE_CLASS_TEXT);
        inputName.setText(sharedPreferences.getString("fullName", ""));

        EditText inputEmail = new EditText(getContext());
        inputEmail.setHint("Email");
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setText(sharedPreferences.getString("email", ""));

        android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 0);
        container.addView(inputName);
        container.addView(inputEmail);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật thông tin cá nhân")
                .setView(container)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .setPositiveButton("Lưu", (d, w) -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("fullName", inputName.getText().toString().trim());
                    body.put("email", inputEmail.getText().toString().trim());
                    apiService.updateMe(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext()), body)
                            .enqueue(new Callback<JSONObject>() {
                                @Override
                                public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                                    if (response.isSuccessful()) {
                                        sharedPreferences.edit()
                                                .putString("fullName", inputName.getText().toString().trim())
                                                .putString("email", inputEmail.getText().toString().trim())
                                                .apply();
                                        loadMe();
                                        Toast.makeText(getContext(), "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Cập nhật thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JSONObject> call, Throwable t) {
                                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }

    private void showChangePasswordDialog() {
        EditText inputPwd = new EditText(getContext());
        inputPwd.setHint("Mật khẩu mới");
        inputPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(inputPwd)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String pwd = inputPwd.getText().toString().trim();
                    if (pwd.length() < 6) {
                        Toast.makeText(getContext(), "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("password", pwd);
                    apiService.updateMe(SessionManager.getUserId(getContext()), SessionManager.getUsername(getContext()), body)
                            .enqueue(new Callback<JSONObject>() {
                                @Override
                                public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Đổi mật khẩu thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JSONObject> call, Throwable t) {
                                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }).show();
    }
}
