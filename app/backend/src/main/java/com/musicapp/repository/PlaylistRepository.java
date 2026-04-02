package com.musicapp.repository;

import com.musicapp.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Playlist> findByIdAndUserId(Long id, Long userId);
}

