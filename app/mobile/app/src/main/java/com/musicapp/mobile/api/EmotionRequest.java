package com.musicapp.mobile.api;

import com.google.gson.annotations.SerializedName;

public class EmotionRequest {
    @SerializedName("image")
    private String image;

    public EmotionRequest() {
    }

    public EmotionRequest(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
