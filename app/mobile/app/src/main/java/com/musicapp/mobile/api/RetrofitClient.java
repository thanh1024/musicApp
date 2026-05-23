package com.musicapp.mobile.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RetrofitClient {
    /**
     * API base URL resolution order:
     * 1) SharedPreferences key "api_base_url" (override without rebuilding)
     * 2) Default based on runtime:
     *    - Emulator: http://10.0.2.2:8080/ (host loopback)
     *    - Real device: http://127.0.0.1:8080/ (works with: adb reverse tcp:8080 tcp:8080)
     */
    private static final String PREFS_NAME = "MusicApp";
    private static final String PREF_KEY_API_BASE_URL = "api_base_url";
    private static final String DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080/";
    private static final String DEFAULT_DEVICE_URL = "http://127.0.0.1:8080/";

    private static Retrofit retrofit = null;
    private static Context appContext = null;
    private static boolean jwtInterceptorEnabled = false;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        // If retrofit was created before we had a context, rebuild so we can attach JwtInterceptor.
        if (retrofit != null && !jwtInterceptorEnabled) {
            retrofit = null;
        }
    }

    public static ApiService getApiService() {
        return getApiService(null);
    }

    public static ApiService getApiService(Context context) {
        if (context != null) {
            init(context);
        }

        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            // Thêm JWT interceptor nếu có context
            if (appContext != null) {
                clientBuilder.addInterceptor(new JwtInterceptor(appContext));
                jwtInterceptorEnabled = true;
            } else {
                jwtInterceptorEnabled = false;
            }

            OkHttpClient client = clientBuilder.build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(resolveBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit.create(ApiService.class);
    }

    private static String resolveBaseUrl() {
        String pref = null;
        if (appContext != null) {
            SharedPreferences sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            pref = sp.getString(PREF_KEY_API_BASE_URL, null);
        }

        if (pref != null) {
            String trimmed = pref.trim();
            if (!trimmed.isEmpty()) {
                // Retrofit requires the baseUrl to end with '/'
                return trimmed.endsWith("/") ? trimmed : (trimmed + "/");
            }
        }

        return isEmulator() ? DEFAULT_EMULATOR_URL : DEFAULT_DEVICE_URL;
    }

    private static boolean isEmulator() {
        return (Build.FINGERPRINT != null && (Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown")))
                || (Build.MODEL != null && (Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86")))
                || (Build.MANUFACTURER != null && Build.MANUFACTURER.contains("Genymotion"))
                || (Build.BRAND != null && Build.DEVICE != null && Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
