package com.musicapp.mobile;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.musicapp.mobile.api.SongResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EmotionResultActivity extends AppCompatActivity {
    public static final String EXTRA_EMOTION = "extra_emotion";
    public static final String EXTRA_CONFIDENCE = "extra_confidence";
    public static final String EXTRA_RECOMMENDED_SONGS_JSON = "extra_recommended_songs_json";

    private android.view.View miniPlayer;
    private TextView tvMiniTitle;
    private TextView tvMiniElapsed;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Simple results screen: list only (no decorative cards)
        setContentView(R.layout.activity_emotion_result);

        bindMiniPlayer();

        ImageButton back = findViewById(R.id.btnBackResult);
        if (back != null) back.setOnClickListener(v -> finish());

        String emotion = getIntent().getStringExtra(EXTRA_EMOTION);
        double confidence = getIntent().getDoubleExtra(EXTRA_CONFIDENCE, -1.0);
        String songsJson = getIntent().getStringExtra(EXTRA_RECOMMENDED_SONGS_JSON);

        TextView subtitle = findViewById(R.id.tvSubtitle);
        if (subtitle != null) {
            String emo = (emotion != null && !emotion.trim().isEmpty()) ? emotion.trim() : "—";
            if (confidence >= 0.0) {
                subtitle.setText("Cảm xúc: " + emo + " • Độ tin cậy: " + String.format("%.2f%%", confidence * 100));
            } else {
                subtitle.setText("Cảm xúc: " + emo);
            }
        }

        RecyclerView rv = findViewById(R.id.recyclerRecommendedSongs);
        SongAdapter adapter = new SongAdapter(this, (song, position, songs) -> {
            String label = emotion != null && !emotion.trim().isEmpty() ? ("Gợi ý theo cảm xúc: " + emotion) : "Gợi ý theo cảm xúc";
            AudioPlayer.playQueueWithContext(this, songs, position, label);
        });
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(adapter);
            rv.setHasFixedSize(true);
        }

        List<SongResponse.Song> songs = parseSongs(songsJson);
        adapter.setSongs(songs);

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "Không có bài gợi ý", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindMiniPlayer() {
        miniPlayer = findViewById(R.id.mini_player);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniElapsed = findViewById(R.id.tvMiniElapsed);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniClose = findViewById(R.id.btnMiniClose);

        if (btnMiniPlayPause != null) btnMiniPlayPause.setOnClickListener(v -> AudioPlayer.togglePlayPause());
        if (btnMiniClose != null) {
            btnMiniClose.setOnClickListener(v -> AudioPlayer.next());
            btnMiniClose.setOnLongClickListener(v -> {
                AudioPlayer.stop();
                return true;
            });
        }

        AudioPlayer.addListener((isPlaying, title, positionMs) -> runOnUiThread(() -> {
            if (miniPlayer == null) return;
            if (title == null && positionMs <= 0 && !isPlaying) {
                miniPlayer.setVisibility(android.view.View.GONE);
                return;
            }
            miniPlayer.setVisibility(android.view.View.VISIBLE);
            if (tvMiniTitle != null) tvMiniTitle.setText(title != null ? title : "Đang phát");
            if (tvMiniElapsed != null) tvMiniElapsed.setText((positionMs / 1000) + "s");
            if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }));

        // Tap mini-player -> open full player screen (seek/controls)
        if (miniPlayer != null) {
            miniPlayer.setOnClickListener(v ->
                    startActivity(new android.content.Intent(EmotionResultActivity.this, PlayerActivity.class)));
        }
    }

    private List<SongResponse.Song> parseSongs(String songsJson) {
        if (songsJson == null || songsJson.trim().isEmpty()) return new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(songsJson);
            List<SongResponse.Song> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.optJSONObject(i);
                if (s == null) continue;
                SongResponse.Song song = new SongResponse.Song();
                if (s.has("id")) song.setId(s.optLong("id"));
                song.setTitle(s.optString("title", ""));
                song.setArtist(s.optString("artist", ""));
                song.setAlbum(s.optString("album", ""));
                song.setGenre(s.optString("genre", ""));
                song.setMood(s.optString("mood", ""));
                if (s.has("duration")) song.setDuration(s.optInt("duration"));
                song.setFileUrl(s.optString("fileUrl", s.optString("file_url", "")));
                song.setThumbnailUrl(s.optString("thumbnailUrl", s.optString("thumbnail_url", "")));
                if (s.has("spotifyId")) song.setSpotifyId(s.optString("spotifyId", ""));
                if (s.has("playCount")) song.setPlayCount(s.optInt("playCount"));
                out.add(song);
            }
            return out;
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi parse songs: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
    }
}

