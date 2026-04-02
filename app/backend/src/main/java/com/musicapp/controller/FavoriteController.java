package com.musicapp.controller;

import com.musicapp.model.Favorite;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.FavoriteRepository;
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
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

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
        Map<String, Object> response = new HashMap<>();
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));

        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(resolvedUserId);
        List<Long> songIds = favorites.stream().map(Favorite::getSongId).collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() : songRepository.findAllById(songIds);

        response.put("success", true);
        response.put("data", songs);
        response.put("total", songs.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{songId}")
    public ResponseEntity<?> add(@AuthenticationPrincipal User user, @PathVariable Long songId, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));

        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Song not found"));

        if (!favoriteRepository.existsByUserIdAndSongId(resolvedUserId, songId)) {
            Favorite fav = new Favorite();
            fav.setUserId(resolvedUserId);
            fav.setSongId(songId);
            favoriteRepository.save(fav);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Added to favorites"));
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<?> remove(@AuthenticationPrincipal User user, @PathVariable Long songId, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Long resolvedUserId = resolveUserId(user, userId, username);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        favoriteRepository.deleteByUserIdAndSongId(resolvedUserId, songId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Removed from favorites"));
    }
}

