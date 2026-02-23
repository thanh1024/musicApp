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
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", songs);
            response.put("total", songs.size());
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
            response.put("data", songOpt.get());
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo bài hát thành công");
            response.put("data", song);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật bài hát thành công");
            response.put("data", song);
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
}
