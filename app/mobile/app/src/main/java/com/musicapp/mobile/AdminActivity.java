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
    private Button buttonHome;
    private Button buttonUsers;
    private Button buttonSongs;
    private Button buttonArtists;
    private Button buttonGenres;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        RetrofitClient.init(this);
        SharedPreferences sharedPreferences = getSharedPreferences("MusicApp", MODE_PRIVATE);
        String userRole = sharedPreferences.getString("role", "ROLE_USER");

        // Kiểm tra quyền admin
        if (!"ROLE_ADMIN".equals(userRole)) {
            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        buttonHome = findViewById(R.id.buttonHome);
        buttonUsers = findViewById(R.id.buttonUsers);
        buttonSongs = findViewById(R.id.buttonSongs);
        buttonArtists = findViewById(R.id.buttonArtists);
        buttonGenres = findViewById(R.id.buttonGenres);

        buttonHome.setOnClickListener(v -> {
            setActiveTab(buttonHome);
            loadFragment(new AdminHomeFragment());
        });

        buttonUsers.setOnClickListener(v -> {
            setActiveTab(buttonUsers);
            loadFragment(new AdminUsersFragment());
        });

        buttonSongs.setOnClickListener(v -> {
            setActiveTab(buttonSongs);
            loadFragment(new AdminSongsFragment());
        });

        if (buttonArtists != null) {
            buttonArtists.setOnClickListener(v -> {
                setActiveTab(buttonArtists);
                loadFragment(new AdminArtistsFragment());
            });
        }
        if (buttonGenres != null) {
            buttonGenres.setOnClickListener(v -> {
                setActiveTab(buttonGenres);
                loadFragment(new AdminGenresFragment());
            });
        }

        // Load mặc định dashboard admin
        setActiveTab(buttonHome);
        loadFragment(new AdminHomeFragment());
    }

    private void setActiveTab(Button activeButton) {
        buttonHome.setSelected(false);
        buttonUsers.setSelected(false);
        buttonSongs.setSelected(false);
        if (buttonArtists != null) buttonArtists.setSelected(false);
        if (buttonGenres != null) buttonGenres.setSelected(false);
        activeButton.setSelected(true);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment);
        transaction.commit();
    }

    // Called from AdminHomeFragment quick actions
    public void openUsers() {
        setActiveTab(buttonUsers);
        loadFragment(new AdminUsersFragment());
    }

    public void openSongs() {
        setActiveTab(buttonSongs);
        loadFragment(new AdminSongsFragment());
    }

    public void openArtists() {
        if (buttonArtists == null) return;
        setActiveTab(buttonArtists);
        loadFragment(new AdminArtistsFragment());
    }

    public void openGenres() {
        if (buttonGenres == null) return;
        setActiveTab(buttonGenres);
        loadFragment(new AdminGenresFragment());
    }
}
