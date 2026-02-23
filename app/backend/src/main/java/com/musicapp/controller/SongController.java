package com.musicapp.controller;

import com.musicapp.model.Song;
import com.musicapp.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping
    public ResponseEntity<?> getAllSongs() {
        List<Song> songs = songRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
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
        response.put("data", songOpt.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSongs(@RequestParam String keyword) {
        List<Song> songs = songRepository.searchByKeyword(keyword);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<?> getSongsByGenre(@PathVariable String genre) {
        List<Song> songs = songRepository.findByGenre(genre);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mood/{mood}")
    public ResponseEntity<?> getSongsByMood(@PathVariable String mood) {
        List<Song> songs = songRepository.findByMood(mood);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
        return ResponseEntity.ok(response);
    }
}
