package com.musicapp.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listening_history")
public class ListeningHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "listened_at")
    private LocalDateTime listenedAt;

    @Column(name = "listen_duration")
    private Integer listenDuration;

    @Column(name = "completed")
    private Boolean completed = false;

    @PrePersist
    protected void onCreate() {
        listenedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSongId() {
        return songId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
    }

    public LocalDateTime getListenedAt() {
        return listenedAt;
    }

    public void setListenedAt(LocalDateTime listenedAt) {
        this.listenedAt = listenedAt;
    }

    public Integer getListenDuration() {
        return listenDuration;
    }

    public void setListenDuration(Integer listenDuration) {
        this.listenDuration = listenDuration;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}

