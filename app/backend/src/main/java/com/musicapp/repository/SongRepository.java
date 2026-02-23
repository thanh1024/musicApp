package com.musicapp.repository;

import com.musicapp.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByTitleContainingIgnoreCase(String title);
    List<Song> findByArtistContainingIgnoreCase(String artist);
    List<Song> findByGenre(String genre);
    List<Song> findByMood(String mood);
    
    @Query("SELECT s FROM Song s WHERE s.title LIKE %:keyword% OR s.artist LIKE %:keyword%")
    List<Song> searchByKeyword(@Param("keyword") String keyword);
}
