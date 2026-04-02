package com.musicapp.controller;

import com.musicapp.model.Playlist;
import com.musicapp.model.PlaylistSong;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.PlaylistRepository;
import com.musicapp.repository.PlaylistSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private UserRepository userRepository;

    private Long resolveUserId(User user, Long userIdParam, String usernameParam) {
        if (user != null) return user.getId();
        if (userIdParam != null && userRepository.findById(userIdParam).isPresent()) return userIdParam;
        if (usernameParam != null && !usernameParam.trim().isEmpty()) {
            return userRepository.findByUsername(usernameParam.trim()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal User user, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        List<Playlist> playlists = playlistRepository.findByUserIdOrderByUpdatedAtDesc(resolvedUserId);
        return ResponseEntity.ok(Map.of("success", true, "data", playlists, "total", playlists.size()));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        Long userIdFromBody = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : null;
        String usernameFromBody = body.get("username") != null ? body.get("username").toString() : null;
        Long resolvedUserId = resolveUserId(user, userIdFromBody, usernameFromBody);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));

        String name = body.get("name") != null ? body.get("name").toString().trim() : "";
        if (name.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "name is required"));

        Playlist p = new Playlist();
        p.setUserId(resolvedUserId);
        p.setName(name);
        if (body.get("description") != null) p.setDescription(body.get("description").toString());
        if (body.get("coverImageUrl") != null) p.setCoverImageUrl(body.get("coverImageUrl").toString());
        if (body.get("isPublic") != null) p.setIsPublic(Boolean.parseBoolean(body.get("isPublic").toString()));
        playlistRepository.save(p);

        return ResponseEntity.ok(Map.of("success", true, "data", p));
    }

    @GetMapping("/{playlistId}/songs")
    public ResponseEntity<?> listSongs(@AuthenticationPrincipal User user, @PathVariable Long playlistId, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        Optional<Playlist> playlistOpt = playlistRepository.findByIdAndUserId(playlistId, resolvedUserId);
        if (playlistOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Playlist not found"));

        List<PlaylistSong> ps = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        List<Long> songIds = ps.stream().map(PlaylistSong::getSongId).collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() : songRepository.findAllById(songIds);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
        response.put("total", songs.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<?> addSong(@AuthenticationPrincipal User user, @PathVariable Long playlistId, @PathVariable Long songId, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        Optional<Playlist> playlistOpt = playlistRepository.findByIdAndUserId(playlistId, resolvedUserId);
        if (playlistOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Playlist not found"));
        if (songRepository.findById(songId).isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Song not found"));

        if (playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId).isEmpty()) {
            int nextPos = playlistSongRepository.maxPosition(playlistId) + 1;
            PlaylistSong ps = new PlaylistSong();
            ps.setPlaylistId(playlistId);
            ps.setSongId(songId);
            ps.setPosition(nextPos);
            playlistSongRepository.save(ps);
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Added"));
    }

    @DeleteMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<?> removeSong(@AuthenticationPrincipal User user, @PathVariable Long playlistId, @PathVariable Long songId, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        Optional<Playlist> playlistOpt = playlistRepository.findByIdAndUserId(playlistId, resolvedUserId);
        if (playlistOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Playlist not found"));
        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Removed"));
    }
}

