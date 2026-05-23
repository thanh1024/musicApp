package com.musicapp.mobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
/**
 * Full player screen opened from the mini-player.
 * Uses the existing UI from {@code fragment_player.xml}.
 */
public class PlayerActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvPlayingFrom;
    private TextView tvContextLabel;
    private TextView tvCurrent;
    private TextView tvTotal;
    private ImageButton btnCollapse;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnPrev;
    private SeekBar seekBar;

    private ApiService api;
    private final Set<Long> favoriteIds = new HashSet<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean userSeeking = false;

    private final AudioPlayer.Listener listener = (isPlaying, title, positionMs) -> runOnUiThread(() -> {
        if (tvTitle != null && title != null) tvTitle.setText(title);
        if (btnPlayPause != null) btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        syncProgress();
    });

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            syncProgress();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_player);

        tvTitle = findViewById(R.id.tvPlayerSongTitle);
        tvArtist = findViewById(R.id.tvPlayerArtist);
        tvPlayingFrom = findViewById(R.id.tvPlayingFrom);
        tvContextLabel = findViewById(R.id.tvContextLabel);
        tvCurrent = findViewById(R.id.tvPlayerCurrentTime);
        tvTotal = findViewById(R.id.tvPlayerTotalTime);
        btnCollapse = findViewById(R.id.btnCollapsePlayer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrevious);
        seekBar = findViewById(R.id.seekBarPlayer);

        api = RetrofitClient.getApiService(this);

        // Collapse/back
        if (btnCollapse != null) btnCollapse.setOnClickListener(v -> finish());

        // Controls
        if (btnPlayPause != null) btnPlayPause.setOnClickListener(v -> AudioPlayer.togglePlayPause());
        if (btnNext != null) btnNext.setOnClickListener(v -> AudioPlayer.next());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> AudioPlayer.previous());
        if (findViewById(R.id.btnPlayerFavorite) instanceof ImageButton) {
            ImageButton btnFav = (ImageButton) findViewById(R.id.btnPlayerFavorite);
            btnFav.setOnClickListener(v -> toggleFavoriteCurrent());
        }

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    long targetMs = ((long) seekBar.getProgress()) * 1000L;
                    AudioPlayer.seekTo(targetMs);
                    userSeeking = false;
                }
            });
        }

        // Basic initial UI
        bindSongMeta();
        if (btnPlayPause != null) btnPlayPause.setImageResource(AudioPlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        // Context label (e.g., emotion/playlist)
        String ctx = AudioPlayer.getQueueContextLabel();
        if (tvPlayingFrom != null) tvPlayingFrom.setText(ctx != null ? "ĐANG PHÁT TỪ" : "ĐANG PHÁT TỪ DANH SÁCH");
        if (tvContextLabel != null) tvContextLabel.setText(ctx != null ? ctx : "Danh sách");
    }

    @Override
    protected void onStart() {
        super.onStart();
        AudioPlayer.addListener(listener);
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        syncProgress();
        loadFavoritesOnce();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AudioPlayer.removeListener(listener);
        handler.removeCallbacks(ticker);
    }

    private void syncProgress() {
        if (userSeeking) return;
        long posMs = AudioPlayer.getPositionMs();
        long durMs = AudioPlayer.getDurationMs();
        bindSongMeta();
        if (seekBar != null) {
            int maxSec = (int) Math.max(0L, durMs / 1000L);
            int posSec = (int) Math.max(0L, posMs / 1000L);
            seekBar.setMax(maxSec);
            seekBar.setProgress(Math.min(posSec, maxSec));
        }
        if (tvCurrent != null) tvCurrent.setText(formatTime(posMs));
        if (tvTotal != null) tvTotal.setText(durMs > 0 ? formatTime(durMs) : "--:--");
    }

    private void bindSongMeta() {
        String title = AudioPlayer.getCurrentTitle();
        if (tvTitle != null && title != null) tvTitle.setText(title);
        com.musicapp.mobile.api.SongResponse.Song cur = AudioPlayer.getCurrentSong();
        if (tvArtist != null) {
            String artist = (cur != null && cur.getArtist() != null) ? cur.getArtist().trim() : "";
            tvArtist.setText(artist);
        }
        String ctx = AudioPlayer.getQueueContextLabel();
        if (tvPlayingFrom != null) tvPlayingFrom.setText(ctx != null ? "ĐANG PHÁT TỪ" : "ĐANG PHÁT TỪ DANH SÁCH");
        if (tvContextLabel != null) tvContextLabel.setText(ctx != null ? ctx : "Danh sách");
    }
    private String formatTime(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return min + ":" + (sec < 10 ? "0" + sec : sec);
    }

    private void loadFavoritesOnce() {
        Long uid = SessionManager.getUserId(this);
        String username = SessionManager.getUsername(this);
        if (uid == null || username == null) return;
        api.getFavorites(uid, username).enqueue(new retrofit2.Callback<com.musicapp.mobile.api.SongResponse>() {
            @Override public void onResponse(retrofit2.Call<com.musicapp.mobile.api.SongResponse> call, retrofit2.Response<com.musicapp.mobile.api.SongResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) return;
                favoriteIds.clear();
                for (com.musicapp.mobile.api.SongResponse.Song s : response.body().getData()) {
                    if (s != null && s.getId() != null) favoriteIds.add(s.getId());
                }
                updateFavoriteIcon();
            }
            @Override public void onFailure(retrofit2.Call<com.musicapp.mobile.api.SongResponse> call, Throwable t) { }
        });
    }

    private void updateFavoriteIcon() {
        ImageButton btnFav = findViewById(R.id.btnPlayerFavorite);
        if (btnFav == null) return;
        com.musicapp.mobile.api.SongResponse.Song cur = AudioPlayer.getCurrentSong();
        boolean liked = cur != null && cur.getId() != null && favoriteIds.contains(cur.getId());
        btnFav.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void toggleFavoriteCurrent() {
        Long uid = SessionManager.getUserId(this);
        String username = SessionManager.getUsername(this);
        com.musicapp.mobile.api.SongResponse.Song cur = AudioPlayer.getCurrentSong();
        if (uid == null || username == null || cur == null || cur.getId() == null) {
            Toast.makeText(this, "Chưa đăng nhập hoặc không có bài hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }
        Long songId = cur.getId();
        boolean liked = favoriteIds.contains(songId);
        retrofit2.Call<JSONObject> call = liked ? api.removeFavorite(songId, uid, username) : api.addFavorite(songId, uid, username);
        call.enqueue(new retrofit2.Callback<JSONObject>() {
            @Override public void onResponse(retrofit2.Call<JSONObject> call, retrofit2.Response<JSONObject> response) {
                if (response.isSuccessful()) {
                    if (liked) favoriteIds.remove(songId); else favoriteIds.add(songId);
                    updateFavoriteIcon();
                } else {
                    Toast.makeText(PlayerActivity.this, "Thao tác thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(retrofit2.Call<JSONObject> call, Throwable t) {
                Toast.makeText(PlayerActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

