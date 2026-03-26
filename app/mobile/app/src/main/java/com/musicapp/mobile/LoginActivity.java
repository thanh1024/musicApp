package com.musicapp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.AuthResponse;
import com.musicapp.mobile.api.LoginRequest;
import com.musicapp.mobile.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String PREF_NAME = "MusicApp";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ROLE = "role";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RetrofitClient.init(this);
        ApiService apiService = RetrofitClient.getApiService();

        EditText editTextUsername = findViewById(R.id.editTextUsername);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        
        Button buttonLogin = findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(v -> handleLogin(editTextUsername, editTextPassword, apiService));

        Button buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin(EditText editTextUsername, EditText editTextPassword, ApiService apiService) {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        login(username, password, apiService);
    }

    private void login(String username, String password, ApiService apiService) {
        try {
            LoginRequest request = new LoginRequest(username, password);
            Call<AuthResponse> call = apiService.login(request);
            call.enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    handleLoginResponse(response, username);
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLoginResponse(Response<AuthResponse> response, String username) {
        if (response.isSuccessful() && response.body() != null) {
            AuthResponse authResponse = response.body();
            
            if (authResponse.isSuccess()) {
                saveUserCredentials(authResponse, username);
                navigateToMainActivity();
            } else {
                String message = authResponse.getMessage();
                Toast.makeText(LoginActivity.this, message != null ? message : "Đăng nhập thất bại", 
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserCredentials(AuthResponse authResponse, String username) {
        String token = authResponse.getToken();
        AuthResponse.UserInfo user = authResponse.getUser();
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USERNAME, username);
        
        if (user != null) {
            if (user.getId() != null) {
                editor.putLong(KEY_USER_ID, user.getId());
            }
            if (user.getFullName() != null) {
                editor.putString(KEY_FULL_NAME, user.getFullName());
            }
            String role = user.getRole() != null ? user.getRole() : DEFAULT_ROLE;
            editor.putString(KEY_ROLE, role);
        }
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
