package com.musicapp.mobile.api;

import org.json.JSONObject;
import okhttp3.ResponseBody;
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

    @GET("api/genres")
    Call<ResponseBody> getGenres();

    @POST("api/emotion/analyze")
    Call<ResponseBody> analyzeEmotion(@Query("userId") Long userId, @Body EmotionRequest request);

    // Use when emotion is predicted on-device; backend will map emotion -> mood and return recommended songs.
    @POST("api/emotion/recommend")
    Call<ResponseBody> recommendByEmotion(@Query("userId") Long userId, @Body EmotionLabelRequest request);

    @GET("api/emotion/history/{userId}")
    Call<ResponseBody> getEmotionHistory(@Path("userId") Long userId);

    // User (authenticated)
    @GET("api/me")
    Call<JSONObject> me(@Query("userId") Long userId, @Query("username") String username);

    @PATCH("api/me")
    Call<JSONObject> updateMe(@Query("userId") Long userId, @Query("username") String username, @Body Map<String, Object> body);

    // Favorites (authenticated)
    @GET("api/favorites")
    Call<SongResponse> getFavorites(@Query("userId") Long userId, @Query("username") String username);

    @POST("api/favorites/{songId}")
    Call<JSONObject> addFavorite(@Path("songId") Long songId, @Query("userId") Long userId, @Query("username") String username);

    @DELETE("api/favorites/{songId}")
    Call<JSONObject> removeFavorite(@Path("songId") Long songId, @Query("userId") Long userId, @Query("username") String username);

    // Listening history (authenticated)
    @GET("api/history")
    Call<SongResponse> getHistory(@Query("userId") Long userId, @Query("username") String username);

    @POST("api/history")
    Call<JSONObject> addHistory(@Body Map<String, Object> body);

    // Playlists (authenticated)
    @GET("api/playlists")
    Call<ResponseBody> getPlaylists(@Query("userId") Long userId, @Query("username") String username);

    @POST("api/playlists")
    Call<JSONObject> createPlaylist(@Body Map<String, Object> body);

    @GET("api/playlists/{playlistId}/songs")
    Call<SongResponse> getPlaylistSongs(@Path("playlistId") Long playlistId, @Query("userId") Long userId, @Query("username") String username);

    @POST("api/playlists/{playlistId}/songs/{songId}")
    Call<JSONObject> addSongToPlaylist(@Path("playlistId") Long playlistId, @Path("songId") Long songId, @Query("userId") Long userId, @Query("username") String username);

    @DELETE("api/playlists/{playlistId}/songs/{songId}")
    Call<JSONObject> removeSongFromPlaylist(@Path("playlistId") Long playlistId, @Path("songId") Long songId, @Query("userId") Long userId, @Query("username") String username);

    // Admin APIs - Users
    @GET("api/admin/users")
    Call<ResponseBody> getAdminUsers();

    @POST("api/admin/users")
    Call<ResponseBody> createAdminUser(@Body Map<String, Object> body);

    @GET("api/admin/users/{id}")
    Call<JSONObject> getAdminUserById(@Path("id") Long id);

    @PATCH("api/admin/users/{id}")
    Call<ResponseBody> updateUser(@Path("id") Long id, @Body Map<String, Object> updates);

    @POST("api/admin/users/{id}/lock")
    Call<ResponseBody> lockUser(@Path("id") Long id);

    @POST("api/admin/users/{id}/unlock")
    Call<ResponseBody> unlockUser(@Path("id") Long id);

    @DELETE("api/admin/users/{id}")
    Call<ResponseBody> deleteUser(@Path("id") Long id);

    // Admin APIs - Songs
    @GET("api/admin/songs")
    Call<ResponseBody> getAdminSongs();

    @GET("api/admin/songs/{id}")
    Call<JSONObject> getAdminSongById(@Path("id") Long id);

    @POST("api/admin/songs")
    Call<ResponseBody> createSong(@Body Map<String, Object> songData);

    @PATCH("api/admin/songs/{id}")
    Call<ResponseBody> updateSong(@Path("id") Long id, @Body Map<String, Object> updates);

    @DELETE("api/admin/songs/{id}")
    Call<ResponseBody> deleteSong(@Path("id") Long id);

    // Admin APIs - Artists / Genres / Stats
    @GET("api/admin/artists")
    Call<ResponseBody> getAdminArtists();

    @POST("api/admin/artists")
    Call<ResponseBody> createAdminArtist(@Body Map<String, Object> body);

    @DELETE("api/admin/artists/{id}")
    Call<ResponseBody> deleteAdminArtist(@Path("id") Long id);

    @GET("api/admin/genres")
    Call<ResponseBody> getAdminGenres();

    @POST("api/admin/genres")
    Call<ResponseBody> createAdminGenre(@Body Map<String, Object> body);

    @DELETE("api/admin/genres/{id}")
    Call<ResponseBody> deleteAdminGenre(@Path("id") Long id);

    @GET("api/admin/stats")
    Call<ResponseBody> getAdminStats();
}
