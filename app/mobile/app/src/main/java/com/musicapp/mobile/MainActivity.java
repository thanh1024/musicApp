package com.musicapp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;

    private View miniPlayer;
    private TextView tvMiniTitle;
    private TextView tvMiniElapsed;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RetrofitClient.init(this);
        sharedPreferences = getSharedPreferences("MusicApp", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        ensureUserId();

        bindMiniPlayer();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_search) {
                    selectedFragment = new SearchFragment();
                } else if (itemId == R.id.nav_emotion) {
                    Intent intent = new Intent(MainActivity.this, EmotionActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_playlist) {
                    selectedFragment = new PlaylistFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }

                return true;
            }
        });

        // Load default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    private void bindMiniPlayer() {
        miniPlayer = findViewById(R.id.mini_player);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniElapsed = findViewById(R.id.tvMiniElapsed);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniClose = findViewById(R.id.btnMiniClose);

        if (btnMiniPlayPause != null) btnMiniPlayPause.setOnClickListener(v -> AudioPlayer.togglePlayPause());
        if (btnMiniClose != null) btnMiniClose.setOnClickListener(v -> AudioPlayer.stop());

        AudioPlayer.setListener((isPlaying, title, positionMs) -> runOnUiThread(() -> {
            if (miniPlayer == null) return;
            if (title == null && positionMs <= 0 && !isPlaying) {
                miniPlayer.setVisibility(View.GONE);
                return;
            }
            miniPlayer.setVisibility(View.VISIBLE);
            if (tvMiniTitle != null) tvMiniTitle.setText(title != null ? title : "Đang phát");
            if (tvMiniElapsed != null) tvMiniElapsed.setText((positionMs / 1000) + "s");
            if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }));
    }

    private void ensureUserId() {
        Long userId = SessionManager.getUserId(this);
        if (userId != null) return;
        ApiService api = RetrofitClient.getApiService(this);
        api.me(null, SessionManager.getUsername(this)).enqueue(new Callback<JSONObject>() {
            @Override
            public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject data = response.body().optJSONObject("data");
                    if (data != null && data.has("id")) {
                        SessionManager.saveUserId(MainActivity.this, data.optLong("id"));
                    }
                }
            }
            @Override
            public void onFailure(Call<JSONObject> call, Throwable t) { }
        });
    }
}
