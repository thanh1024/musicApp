package com.musicapp.mobile.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SongResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private List<Song> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Song> getData() {
        return data;
    }

    public void setData(List<Song> data) {
        this.data = data;
    }

    public static class Song {
        @SerializedName("id")
        private Long id;
        
        @SerializedName("title")
        private String title;
        
        @SerializedName("artist")
        private String artist;
        
        @SerializedName("album")
        private String album;
        
        @SerializedName("genre")
        private String genre;
        
        @SerializedName("mood")
        private String mood;
        
        @SerializedName("duration")
        private Integer duration;
        
        @SerializedName("fileUrl")
        private String fileUrl;
        
        @SerializedName("thumbnailUrl")
        private String thumbnailUrl;
        
        @SerializedName("spotifyId")
        private String spotifyId;
        
        @SerializedName("playCount")
        private Integer playCount;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getMood() {
            return mood;
        }

        public void setMood(String mood) {
            this.mood = mood;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getSpotifyId() {
            return spotifyId;
        }

        public void setSpotifyId(String spotifyId) {
            this.spotifyId = spotifyId;
        }

        public Integer getPlayCount() {
            return playCount;
        }

        public void setPlayCount(Integer playCount) {
            this.playCount = playCount;
        }
    }
}
