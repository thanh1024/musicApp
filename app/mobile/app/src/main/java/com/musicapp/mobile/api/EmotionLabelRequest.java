package com.musicapp.mobile.api;

import com.google.gson.annotations.SerializedName;

public class EmotionLabelRequest {
    @SerializedName("emotion")
    private String emotion;

    @SerializedName("confidence")
    private Double confidence;

    public EmotionLabelRequest() {}

    public EmotionLabelRequest(String emotion, Double confidence) {
        this.emotion = emotion;
        this.confidence = confidence;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}

