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
    List<Song> findByMood(String mood);

    @Query(value = "SELECT DISTINCT s.* FROM songs s " +
            "LEFT JOIN song_artists sa ON sa.song_id = s.id " +
            "LEFT JOIN artists a ON a.id = sa.artist_id " +
            "WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<Song> searchByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT DISTINCT s.* FROM songs s " +
            "JOIN song_genres sg ON sg.song_id = s.id " +
            "JOIN genres g ON g.id = sg.genre_id " +
            "WHERE LOWER(g.name) = LOWER(:genre)", nativeQuery = true)
    List<Song> findByGenreName(@Param("genre") String genre);
}
