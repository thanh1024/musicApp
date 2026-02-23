package com.musicapp.mobile.api;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class JwtInterceptor implements Interceptor {
    private Context context;
    private static final String PREFS_NAME = "MusicApp";
    private static final String TOKEN_KEY = "token";

    public JwtInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Lấy token từ SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(TOKEN_KEY, null);

        // Nếu có token, thêm vào header Authorization
        if (token != null && !token.isEmpty()) {
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token);
            return chain.proceed(requestBuilder.build());
        }

        // Nếu không có token, gửi request bình thường
        return chain.proceed(originalRequest);
    }
}
