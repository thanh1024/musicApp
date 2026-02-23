package com.musicapp.mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.musicapp.mobile.api.RetrofitClient;

public class AdminActivity extends AppCompatActivity {
    private Button buttonUsers, buttonSongs;
    private SharedPreferences sharedPreferences;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        RetrofitClient.init(this);
        sharedPreferences = getSharedPreferences("MusicApp", MODE_PRIVATE);
        userRole = sharedPreferences.getString("role", "ROLE_USER");

        // Kiểm tra quyền admin
        if (!"ROLE_ADMIN".equals(userRole)) {
            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        buttonUsers = findViewById(R.id.buttonUsers);
        buttonSongs = findViewById(R.id.buttonSongs);

        buttonUsers.setOnClickListener(v -> {
            buttonUsers.setSelected(true);
            buttonSongs.setSelected(false);
            loadFragment(new AdminUsersFragment());
        });

        buttonSongs.setOnClickListener(v -> {
            buttonSongs.setSelected(true);
            buttonUsers.setSelected(false);
            loadFragment(new AdminSongsFragment());
        });

        // Load mặc định fragment Users
        buttonUsers.setSelected(true);
        loadFragment(new AdminUsersFragment());
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment);
        transaction.commit();
    }
}
