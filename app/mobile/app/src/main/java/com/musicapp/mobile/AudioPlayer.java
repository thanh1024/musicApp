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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.musicapp.mobile.api.SongResponse;

public class AudioPlayer {
    public interface Listener {
        void onStateChanged(boolean isPlaying, @Nullable String title, long positionMs);
    }

    private AudioPlayer() {}

    private static ExoPlayer player;
    private static String currentTitle = null;
    private static final List<String> queueTitles = new ArrayList<>();
    private static final List<SongResponse.Song> queueSongs = new ArrayList<>();
    private static String queueContextLabel = null; // e.g. "Nhạc Tâm Trạng", "Playlist: Chill"
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
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
        // Backward compatible: replace all listeners with one
        listeners.clear();
        if (l != null) listeners.add(l);
        notifyListeners();
    }

    public static void addListener(@Nullable Listener l) {
        if (l == null) return;
        if (!listeners.contains(l)) listeners.add(l);
        notifyListeners();
    }

    public static void removeListener(@Nullable Listener l) {
        if (l == null) return;
        listeners.remove(l);
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

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Update title for the new item
                try {
                    int idx = player != null ? player.getCurrentMediaItemIndex() : -1;
                    if (idx >= 0 && idx < queueTitles.size()) {
                        currentTitle = queueTitles.get(idx);
                    }
                } catch (Exception ignored) {}
                notifyListener();
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
        notifyListeners();
    }

    private static void notifyListeners() {
        if (player == null || listeners.isEmpty()) return;
        boolean isPlaying = false;
        long pos = 0L;
        try {
            isPlaying = player.isPlaying();
            pos = player.getCurrentPosition();
        } catch (Exception ignored) {}
        for (Listener l : listeners) {
            try {
                l.onStateChanged(isPlaying, currentTitle, pos);
            } catch (Exception ignored) {}
        }
    }

    public static boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public static long getPositionMs() {
        return player != null ? player.getCurrentPosition() : 0L;
    }

    public static long getDurationMs() {
        long d = player != null ? player.getDuration() : 0L;
        return d > 0 ? d : 0L;
    }

    @Nullable
    public static String getCurrentTitle() {
        return currentTitle;
    }

    public static void seekTo(long positionMs) {
        if (player == null) return;
        try {
            player.seekTo(Math.max(0L, positionMs));
        } catch (Exception ignored) {}
        notifyListeners();
    }

    public static void play(Context context, String url) {
        play(context, url, null);
    }

    public static void play(Context context, String url, @Nullable String title) {
        if (context == null || url == null || url.trim().isEmpty()) {
            if (context != null) Toast.makeText(context, "Không có URL nhạc", Toast.LENGTH_SHORT).show();
            return;
        }
        List<MediaItem> items = new ArrayList<>();
        items.add(MediaItem.fromUri(url.trim()));
        List<String> titles = new ArrayList<>();
        titles.add((title != null && !title.trim().isEmpty()) ? title.trim() : "Đang phát");
        playQueue(context, items, titles, 0);
    }

    public static void playQueue(Context context, List<? extends com.musicapp.mobile.api.SongResponse.Song> songs, int startIndex) {
        if (context == null || songs == null || songs.isEmpty()) return;
        int idx = Math.max(0, Math.min(startIndex, songs.size() - 1));

        List<MediaItem> items = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        for (com.musicapp.mobile.api.SongResponse.Song s : songs) {
            if (s == null) continue;
            String url = s.getFileUrl();
            if (url == null || url.trim().isEmpty()) continue;
            items.add(MediaItem.fromUri(url.trim()));
            String t = s.getTitle();
            titles.add((t != null && !t.trim().isEmpty()) ? t.trim() : "Đang phát");
        }
        if (items.isEmpty()) {
            Toast.makeText(context, "Không có URL nhạc", Toast.LENGTH_SHORT).show();
            return;
        }
        // If some songs were skipped due to empty URL, clamp again
        idx = Math.max(0, Math.min(idx, items.size() - 1));
        // store canonical queue songs (as SongResponse.Song) for metadata
        queueSongs.clear();
        for (com.musicapp.mobile.api.SongResponse.Song s : songs) {
            if (s == null || s.getFileUrl() == null || s.getFileUrl().trim().isEmpty()) continue;
            queueSongs.add(s);
        }

        playQueue(context, items, titles, idx);
    }

    public static void playQueueWithContext(Context context, List<? extends com.musicapp.mobile.api.SongResponse.Song> songs, int startIndex, @Nullable String contextLabel) {
        queueContextLabel = (contextLabel != null && !contextLabel.trim().isEmpty()) ? contextLabel.trim() : null;
        playQueue(context, songs, startIndex);
    }

    private static void playQueue(Context context, List<MediaItem> items, List<String> titles, int startIndex) {
        try {
            ensurePlayer(context);
            queueTitles.clear();
            if (titles != null) queueTitles.addAll(titles);

            player.setMediaItems(items, startIndex, 0L);
            // Set title immediately so mini-player can show even while buffering
            if (startIndex >= 0 && startIndex < queueTitles.size()) {
                currentTitle = queueTitles.get(startIndex);
            } else {
                currentTitle = "Đang phát";
            }
            notifyListener();

            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi phát nhạc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
        notifyListener();
    }

    public static void next() {
        if (player == null) return;
        try {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem();
                player.play();
            }
        } catch (Exception ignored) {}
        notifyListener();
    }

    public static void previous() {
        if (player == null) return;
        try {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem();
                player.play();
            } else {
                // restart current
                player.seekTo(0);
                player.play();
            }
        } catch (Exception ignored) {}
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
            queueTitles.clear();
            queueSongs.clear();
            handler.removeCallbacks(ticker);
            notifyListener();
        }
    }

    @Nullable
    public static SongResponse.Song getCurrentSong() {
        try {
            if (player == null) return null;
            int idx = player.getCurrentMediaItemIndex();
            if (idx < 0 || idx >= queueSongs.size()) return null;
            return queueSongs.get(idx);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static String getQueueContextLabel() {
        return queueContextLabel;
    }

    public static void setQueueContextLabel(@Nullable String label) {
        queueContextLabel = (label != null && !label.trim().isEmpty()) ? label.trim() : null;
        notifyListeners();
    }
}

