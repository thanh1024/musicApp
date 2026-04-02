package com.musicapp.controller;

import com.musicapp.model.Song;
import com.musicapp.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
public class SongController {
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> toSongResponse(Song song) {
        String artistFromJoin = String.join(", ", jdbcTemplate.queryForList(
                "SELECT a.name FROM song_artists sa JOIN artists a ON sa.artist_id = a.id WHERE sa.song_id = ?",
                String.class, song.getId()));
        String genreFromJoin = String.join(", ", jdbcTemplate.queryForList(
                "SELECT g.name FROM song_genres sg JOIN genres g ON sg.genre_id = g.id WHERE sg.song_id = ?",
                String.class, song.getId()));

        Map<String, Object> m = new HashMap<>();
        m.put("id", song.getId());
        m.put("title", song.getTitle());
        m.put("artist", artistFromJoin);
        m.put("album", song.getAlbum());
        m.put("genre", genreFromJoin);
        m.put("mood", song.getMood());
        m.put("duration", song.getDuration());
        m.put("fileUrl", song.getFileUrl());
        m.put("thumbnailUrl", song.getThumbnailUrl());
        m.put("spotifyId", song.getSpotifyId());
        m.put("soundcloudId", song.getSoundcloudId());
        m.put("playCount", song.getPlayCount());
        return m;
    }

    @GetMapping
    public ResponseEntity<?> getAllSongs() {
        List<Song> songs = songRepository.findAll();
        List<Map<String, Object>> mapped = songs.stream().map(this::toSongResponse).collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mapped);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSongById(@PathVariable Long id) {
        Optional<Song> songOpt = songRepository.findById(id);
        Map<String, Object> response = new HashMap<>();
        
        if (songOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy bài hát");
            return ResponseEntity.notFound().build();
        }

        response.put("success", true);
        response.put("data", toSongResponse(songOpt.get()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSongs(@RequestParam String keyword) {
        List<Song> songs = songRepository.searchByKeyword(keyword);
        List<Map<String, Object>> mapped = songs.stream().map(this::toSongResponse).collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mapped);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<?> getSongsByGenre(@PathVariable String genre) {
        List<Song> songs = songRepository.findByGenreName(genre);
        List<Map<String, Object>> mapped = songs.stream().map(this::toSongResponse).collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mapped);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mood/{mood}")
    public ResponseEntity<?> getSongsByMood(@PathVariable String mood) {
        List<Song> songs = songRepository.findByMood(mood);
        List<Map<String, Object>> mapped = songs.stream().map(this::toSongResponse).collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mapped);
        return ResponseEntity.ok(response);
    }
}
