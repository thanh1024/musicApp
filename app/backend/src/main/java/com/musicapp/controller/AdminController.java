package com.musicapp.controller;

import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> songToMap(Song song) {
        String artist = String.join(", ", jdbcTemplate.queryForList(
                "SELECT a.name FROM song_artists sa JOIN artists a ON sa.artist_id = a.id WHERE sa.song_id = ?",
                String.class, song.getId()));
        String genre = String.join(", ", jdbcTemplate.queryForList(
                "SELECT g.name FROM song_genres sg JOIN genres g ON sg.genre_id = g.id WHERE sg.song_id = ?",
                String.class, song.getId()));

        Map<String, Object> m = new HashMap<>();
        m.put("id", song.getId());
        m.put("title", song.getTitle());
        m.put("artist", artist);
        m.put("album", song.getAlbum());
        m.put("genre", genre);
        m.put("mood", song.getMood());
        m.put("duration", song.getDuration());
        m.put("fileUrl", song.getFileUrl());
        m.put("thumbnailUrl", song.getThumbnailUrl());
        m.put("spotifyId", song.getSpotifyId());
        m.put("soundcloudId", song.getSoundcloudId());
        m.put("playCount", song.getPlayCount());
        m.put("createdAt", song.getCreatedAt());
        m.put("updatedAt", song.getUpdatedAt());
        return m;
    }

    // ========== USER MANAGEMENT APIs ==========

    /**
     * GET /api/admin/users
     * Lấy danh sách tất cả users (không bao gồm deleted)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findByDeletedFalse();
            List<Map<String, Object>> userList = users.stream()
                .map(this::userToMap)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userList);
            response.put("total", userList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách users: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        try {
            String username = body.get("username") != null ? body.get("username").toString().trim() : "";
            String email = body.get("email") != null ? body.get("email").toString().trim() : "";
            String password = body.get("password") != null ? body.get("password").toString() : "";
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "username, email, password là bắt buộc"));
            }
            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username đã tồn tại"));
            }
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email đã tồn tại"));
            }
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(body.get("fullName") != null ? body.get("fullName").toString() : null);
            user.setAvatarUrl(body.get("avatarUrl") != null ? body.get("avatarUrl").toString() : null);
            user.setIsActive(true);
            user.setDeleted(false);
            String role = body.get("role") != null ? body.get("role").toString() : "ROLE_USER";
            user.setRole("ROLE_ADMIN".equals(role) ? "ROLE_ADMIN" : "ROLE_USER");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Tạo user thành công", "data", userToMap(user)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Lỗi khi tạo user: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/users/{id}
     * Xem chi tiết user
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(id);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy user");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userToMap(userOpt.get()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * PATCH /api/admin/users/{id}
     * Cập nhật thông tin user
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(id);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy user");
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();

            // Update các trường được cung cấp
            if (updates.containsKey("username")) {
                String newUsername = (String) updates.get("username");
                if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Username đã tồn tại");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setUsername(newUsername);
            }

            if (updates.containsKey("email")) {
                String newEmail = (String) updates.get("email");
                if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Email đã tồn tại");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setEmail(newEmail);
            }

            if (updates.containsKey("fullName")) {
                user.setFullName((String) updates.get("fullName"));
            }

            if (updates.containsKey("avatarUrl")) {
                user.setAvatarUrl((String) updates.get("avatarUrl"));
            }

            if (updates.containsKey("password")) {
                String newPassword = (String) updates.get("password");
                user.setPassword(passwordEncoder.encode(newPassword));
            }

            if (updates.containsKey("role")) {
                String role = (String) updates.get("role");
                if (role.equals("ROLE_ADMIN") || role.equals("ROLE_USER")) {
                    user.setRole(role);
                }
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật user thành công");
            response.put("data", userToMap(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/admin/users/{id}/lock
     * Khóa tài khoản user
     */
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(id);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy user");
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setIsActive(false);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã khóa tài khoản user");
            response.put("data", userToMap(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi khóa user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/admin/users/{id}/unlock
     * Mở khóa tài khoản user
     */
    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(id);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy user");
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setIsActive(true);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã mở khóa tài khoản user");
            response.put("data", userToMap(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi mở khóa user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * DELETE /api/admin/users/{id}
     * Xóa user (soft delete)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(id);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy user");
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            user.setDeleted(true);
            user.setIsActive(false);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa user thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ========== SONG MANAGEMENT APIs ==========

    /**
     * GET /api/admin/songs
     * Lấy danh sách tất cả songs
     */
    @GetMapping("/songs")
    public ResponseEntity<?> getAllSongs() {
        try {
            List<Song> songs = songRepository.findAll();
            List<Map<String, Object>> mapped = songs.stream().map(this::songToMap).collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mapped);
            response.put("total", mapped.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách songs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/admin/songs/{id}
     * Xem chi tiết song
     */
    @GetMapping("/songs/{id}")
    public ResponseEntity<?> getSongById(@PathVariable Long id) {
        try {
            Optional<Song> songOpt = songRepository.findById(id);
            if (songOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy song");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", songToMap(songOpt.get()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin song: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/admin/songs
     * Thêm bài hát mới
     */
    @PostMapping("/songs")
    public ResponseEntity<?> createSong(@RequestBody Map<String, Object> songData) {
        try {
            Song song = new Song();
            song.setTitle((String) songData.get("title"));
            song.setArtist((String) songData.get("artist"));
            song.setAlbum((String) songData.get("album"));
            song.setGenre((String) songData.get("genre"));
            song.setMood((String) songData.get("mood"));
            
            if (songData.get("duration") != null) {
                song.setDuration(Integer.parseInt(songData.get("duration").toString()));
            } else {
                song.setDuration(180);
            }
            
            song.setFileUrl((String) songData.get("fileUrl"));
            song.setThumbnailUrl((String) songData.get("thumbnailUrl"));
            song.setSpotifyId((String) songData.get("spotifyId"));
            song.setSoundcloudId((String) songData.get("soundcloudId"));
            song.setPlayCount(0);

            // Validation
            if (song.getTitle() == null || song.getTitle().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Title là bắt buộc");
                return ResponseEntity.badRequest().body(response);
            }

            if (song.getArtist() == null || song.getArtist().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Artist là bắt buộc");
                return ResponseEntity.badRequest().body(response);
            }

            if (song.getGenre() == null || song.getGenre().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Genre là bắt buộc");
                return ResponseEntity.badRequest().body(response);
            }

            if (song.getFileUrl() == null || song.getFileUrl().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "FileUrl là bắt buộc");
                return ResponseEntity.badRequest().body(response);
            }

            songRepository.save(song);
            syncSongArtistsAndGenres(song.getId(), song.getArtist(), song.getGenre());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo bài hát thành công");
            response.put("data", songToMap(song));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tạo song: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * PATCH /api/admin/songs/{id}
     * Cập nhật bài hát
     */
    @PatchMapping("/songs/{id}")
    public ResponseEntity<?> updateSong(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<Song> songOpt = songRepository.findById(id);
            if (songOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy song");
                return ResponseEntity.notFound().build();
            }

            Song song = songOpt.get();

            if (updates.containsKey("title")) {
                song.setTitle((String) updates.get("title"));
            }
            if (updates.containsKey("artist")) {
                song.setArtist((String) updates.get("artist"));
            }
            if (updates.containsKey("album")) {
                song.setAlbum((String) updates.get("album"));
            }
            if (updates.containsKey("genre")) {
                song.setGenre((String) updates.get("genre"));
            }
            if (updates.containsKey("mood")) {
                song.setMood((String) updates.get("mood"));
            }
            if (updates.containsKey("duration")) {
                song.setDuration(Integer.parseInt(updates.get("duration").toString()));
            } else if (song.getDuration() == null) {
                song.setDuration(180);
            }
            if (updates.containsKey("fileUrl")) {
                song.setFileUrl((String) updates.get("fileUrl"));
            }
            if (updates.containsKey("thumbnailUrl")) {
                song.setThumbnailUrl((String) updates.get("thumbnailUrl"));
            }
            if (updates.containsKey("spotifyId")) {
                song.setSpotifyId((String) updates.get("spotifyId"));
            }
            if (updates.containsKey("soundcloudId")) {
                song.setSoundcloudId((String) updates.get("soundcloudId"));
            }

            songRepository.save(song);
            syncSongArtistsAndGenres(song.getId(), song.getArtist(), song.getGenre());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật bài hát thành công");
            response.put("data", songToMap(song));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật song: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * DELETE /api/admin/songs/{id}
     * Xóa bài hát
     */
    @DeleteMapping("/songs/{id}")
    public ResponseEntity<?> deleteSong(@PathVariable Long id) {
        try {
            Optional<Song> songOpt = songRepository.findById(id);
            if (songOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy song");
                return ResponseEntity.notFound().build();
            }

            songRepository.delete(songOpt.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xóa bài hát thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa song: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Helper method
    private Map<String, Object> userToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        userMap.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        userMap.put("role", user.getRole() != null ? user.getRole() : "ROLE_USER");
        userMap.put("isActive", user.getIsActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        return userMap;
    }

    private void syncSongArtistsAndGenres(Long songId, String artistCsv, String genreCsv) {
        if (songId == null) return;
        if (artistCsv != null) {
            jdbcTemplate.update("DELETE FROM song_artists WHERE song_id = ?", songId);
            for (String raw : artistCsv.split(",")) {
                String name = raw.trim();
                if (name.isEmpty()) continue;
                Long artistId = getOrCreateId("artists", name);
                jdbcTemplate.update("INSERT INTO song_artists(song_id, artist_id) VALUES(?, ?)", songId, artistId);
            }
        }
        if (genreCsv != null) {
            jdbcTemplate.update("DELETE FROM song_genres WHERE song_id = ?", songId);
            for (String raw : genreCsv.split(",")) {
                String name = raw.trim();
                if (name.isEmpty()) continue;
                Long genreId = getOrCreateId("genres", name);
                jdbcTemplate.update("INSERT INTO song_genres(song_id, genre_id) VALUES(?, ?)", songId, genreId);
            }
        }
    }

    private Long getOrCreateId(String table, String name) {
        String selectSql = "SELECT id FROM " + table + " WHERE LOWER(name) = LOWER(?) LIMIT 1";
        java.util.List<Long> ids = jdbcTemplate.query(selectSql, (rs, rowNum) -> rs.getLong("id"), name);
        if (!ids.isEmpty()) return ids.get(0);
        String insertSql = "INSERT INTO " + table + "(name) VALUES(?)";
        jdbcTemplate.update(insertSql, name);
        return jdbcTemplate.queryForObject(selectSql, Long.class, name);
    }

    // ========== ARTIST / GENRE MANAGEMENT ==========

    @GetMapping("/artists")
    public ResponseEntity<?> listArtists() {
        try {
            List<Map<String, Object>> artists = jdbcTemplate.queryForList(
                    "SELECT id, name FROM artists ORDER BY LOWER(name) ASC"
            );
            return ResponseEntity.ok(Map.of("success", true, "data", artists, "total", artists.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/artists")
    public ResponseEntity<?> createArtist(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name") != null ? body.get("name").toString().trim() : "";
            if (name.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "name is required"));
            Long id = getOrCreateId("artists", name);
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", id, "name", name)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/artists/{id}")
    public ResponseEntity<?> deleteArtist(@PathVariable Long id) {
        try {
            if (id == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "id is required"));
            // Remove joins then delete
            jdbcTemplate.update("DELETE FROM song_artists WHERE artist_id = ?", id);
            int deleted = jdbcTemplate.update("DELETE FROM artists WHERE id = ?", id);
            return ResponseEntity.ok(Map.of("success", true, "deleted", deleted));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/genres")
    public ResponseEntity<?> listGenres() {
        try {
            List<Map<String, Object>> genres = jdbcTemplate.queryForList(
                    "SELECT id, name FROM genres ORDER BY LOWER(name) ASC"
            );
            return ResponseEntity.ok(Map.of("success", true, "data", genres, "total", genres.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/genres")
    public ResponseEntity<?> createGenre(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name") != null ? body.get("name").toString().trim() : "";
            if (name.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "name is required"));
            Long id = getOrCreateId("genres", name);
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of("id", id, "name", name)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/genres/{id}")
    public ResponseEntity<?> deleteGenre(@PathVariable Long id) {
        try {
            if (id == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "id is required"));
            jdbcTemplate.update("DELETE FROM song_genres WHERE genre_id = ?", id);
            int deleted = jdbcTemplate.update("DELETE FROM genres WHERE id = ?", id);
            return ResponseEntity.ok(Map.of("success", true, "deleted", deleted));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== DASHBOARD STATS ==========
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try {
            long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE deleted = false", Long.class);
            long totalSongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM songs", Long.class);
            long totalPlaylists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM playlists", Long.class);
            long totalFavorites = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM favorites", Long.class);
            long totalHistory = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM listening_history", Long.class);
            Map<String, Object> data = new HashMap<>();
            data.put("totalUsers", totalUsers);
            data.put("totalSongs", totalSongs);
            data.put("totalPlaylists", totalPlaylists);
            data.put("totalFavorites", totalFavorites);
            data.put("totalHistory", totalHistory);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
