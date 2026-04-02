package com.musicapp.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.ExoPlayer;

public class AudioPlayer {
    public interface Listener {
        void onStateChanged(boolean isPlaying, @Nullable String title, long positionMs);
    }

    private AudioPlayer() {}

    private static ExoPlayer player;
    private static String currentTitle = null;
    private static Listener listener;
    private static Context appContext;
    private static final String TAG = "AudioPlayer";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (player != null) {
                notifyListener();
                if (player.isPlaying()) {
                    handler.postDelayed(this, 500);
                }
            }
        }
    };

    public static void setListener(@Nullable Listener l) {
        listener = l;
        notifyListener();
    }

    private static void ensurePlayer(Context context) {
        if (player != null) return;
        appContext = context.getApplicationContext();
        player = new ExoPlayer.Builder(appContext).build();

        // Ensure audio is treated as music media
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build();
            player.setAudioAttributes(attrs, true);
            player.setVolume(1.0f);
        } catch (Exception ignored) {}

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                notifyListener();
                if (isPlaying) {
                    handler.removeCallbacks(ticker);
                    handler.post(ticker);
                }
            }

            @Override public void onPlaybackStateChanged(int playbackState) {
                notifyListener();
                if (playbackState == Player.STATE_BUFFERING) {
                    Log.d(TAG, "Buffering...");
                } else if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Ready");
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Ended");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error: " + (error != null ? error.getMessage() : "unknown"), error);
                if (appContext != null) {
                    Toast.makeText(appContext, "Không phát được: " + (error != null ? error.getMessage() : "unknown"), Toast.LENGTH_LONG).show();
                }
                notifyListener();
            }
        });
    }

    private static void notifyListener() {
        if (listener == null || player == null) return;
        try {
            listener.onStateChanged(player.isPlaying(), currentTitle, player.getCurrentPosition());
        } catch (Exception ignored) {}
    }

    public static void play(Context context, String url) {
        play(context, url, null);
    }

    public static void play(Context context, String url, @Nullable String title) {
        if (context == null || url == null || url.trim().isEmpty()) {
            if (context != null) Toast.makeText(context, "Không có URL nhạc", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ensurePlayer(context);
            currentTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;

            MediaItem item = MediaItem.fromUri(url.trim());
            player.setMediaItem(item);
            player.prepare();
            player.play();

            Toast.makeText(context, "Đang phát...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi phát nhạc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
        notifyListener();
    }

    public static void stop() {
        try {
            if (player != null) {
                player.stop();
                player.release();
            }
        } catch (Exception ignored) {
        } finally {
            player = null;
            currentTitle = null;
            handler.removeCallbacks(ticker);
            notifyListener();
        }
    }
}

