package com.musicapp.controller;

import com.musicapp.model.ListeningHistory;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.ListeningHistoryRepository;
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
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired
    private ListeningHistoryRepository listeningHistoryRepository;

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

        List<ListeningHistory> items = listeningHistoryRepository.findByUserIdOrderByListenedAtDesc(resolvedUserId);
        List<Long> songIds = items.stream().map(ListeningHistory::getSongId).collect(Collectors.toList());
        List<Song> songs = songIds.isEmpty() ? List.of() : songRepository.findAllById(songIds);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", songs);
        response.put("total", songs.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> add(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        Long userIdFromBody = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : null;
        String usernameFromBody = body.get("username") != null ? body.get("username").toString() : null;
        Long resolvedUserId = resolveUserId(user, userIdFromBody, usernameFromBody);
        if (resolvedUserId == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));

        Object songIdObj = body.get("songId");
        if (songIdObj == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "songId is required"));

        Long songId = Long.parseLong(songIdObj.toString());
        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Song not found"));

        // Increment playCount when user plays a song (history add is treated as a play event)
        try {
            Song song = songOpt.get();
            Integer current = song.getPlayCount();
            song.setPlayCount((current != null ? current : 0) + 1);
            songRepository.save(song);
        } catch (Exception ignored) {}

        ListeningHistory h = new ListeningHistory();
        h.setUserId(resolvedUserId);
        h.setSongId(songId);
        if (body.get("listenDuration") != null) h.setListenDuration(Integer.parseInt(body.get("listenDuration").toString()));
        if (body.get("completed") != null) h.setCompleted(Boolean.parseBoolean(body.get("completed").toString()));

        listeningHistoryRepository.save(h);
        return ResponseEntity.ok(Map.of("success", true, "message", "Saved history"));
    }
}

