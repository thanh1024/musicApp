package com.musicapp.mobile;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SessionManager() {}

    public static Long getUserId(Context context) {
        if (context == null) return null;
        SharedPreferences sp = context.getSharedPreferences("MusicApp", Context.MODE_PRIVATE);
        if (!sp.contains("userId")) return null;
        long id = sp.getLong("userId", -1L);
        return id > 0 ? id : null;
    }

    public static String getUsername(Context context) {
        if (context == null) return null;
        SharedPreferences sp = context.getSharedPreferences("MusicApp", Context.MODE_PRIVATE);
        String username = sp.getString("username", null);
        if (username == null || username.trim().isEmpty()) return null;
        return username.trim();
    }

    public static void saveUserId(Context context, Long userId) {
        if (context == null || userId == null || userId <= 0) return;
        SharedPreferences sp = context.getSharedPreferences("MusicApp", Context.MODE_PRIVATE);
        sp.edit().putLong("userId", userId).apply();
    }

    public static void clearSession(Context context) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences("MusicApp", Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }
}

