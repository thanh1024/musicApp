package com.musicapp.mobile.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.Map;

public interface ApiService {
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("api/songs")
    Call<SongResponse> getAllSongs();

    @GET("api/songs/{id}")
    Call<SongResponse> getSongById(@Path("id") Long id);

    @GET("api/songs/search")
    Call<SongResponse> searchSongs(@Query("keyword") String keyword);

    @GET("api/songs/genre/{genre}")
    Call<SongResponse> getSongsByGenre(@Path("genre") String genre);

    @GET("api/songs/mood/{mood}")
    Call<SongResponse> getSongsByMood(@Path("mood") String mood);

    @POST("api/emotion/analyze")
    Call<JsonObject> analyzeEmotion(@Query("userId") Long userId, @Body EmotionRequest request);

    @GET("api/emotion/history/{userId}")
    Call<JsonObject> getEmotionHistory(@Path("userId") Long userId);

    // Admin APIs - Users
    @GET("api/admin/users")
    Call<JsonObject> getAdminUsers();

    @GET("api/admin/users/{id}")
    Call<JsonObject> getAdminUserById(@Path("id") Long id);

    @PATCH("api/admin/users/{id}")
    Call<JsonObject> updateUser(@Path("id") Long id, @Body Map<String, Object> updates);

    @POST("api/admin/users/{id}/lock")
    Call<JsonObject> lockUser(@Path("id") Long id);

    @POST("api/admin/users/{id}/unlock")
    Call<JsonObject> unlockUser(@Path("id") Long id);

    @DELETE("api/admin/users/{id}")
    Call<JsonObject> deleteUser(@Path("id") Long id);

    // Admin APIs - Songs
    @GET("api/admin/songs")
    Call<JsonObject> getAdminSongs();

    @GET("api/admin/songs/{id}")
    Call<JsonObject> getAdminSongById(@Path("id") Long id);

    @POST("api/admin/songs")
    Call<JsonObject> createSong(@Body Map<String, Object> songData);

    @PATCH("api/admin/songs/{id}")
    Call<JsonObject> updateSong(@Path("id") Long id, @Body Map<String, Object> updates);

    @DELETE("api/admin/songs/{id}")
    Call<JsonObject> deleteSong(@Path("id") Long id);
}
