package com.musicapp.controller;

import com.musicapp.model.User;
import com.musicapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MeController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user, @RequestParam(required = false) Long userId, @RequestParam(required = false) String username) {
        Map<String, Object> response = new HashMap<>();
        if (user == null && userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null && username != null && !username.trim().isEmpty()) {
            user = userRepository.findByUsername(username.trim()).orElse(null);
        }
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        response.put("success", true);
        response.put("data", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "role", user.getRole() != null ? user.getRole() : "ROLE_USER",
                "isActive", user.getIsActive()
        ));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestBody Map<String, Object> updates) {
        Map<String, Object> response = new HashMap<>();
        if (user == null && userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null && username != null && !username.trim().isEmpty()) {
            user = userRepository.findByUsername(username.trim()).orElse(null);
        }
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        if (updates.containsKey("fullName")) {
            user.setFullName(updates.get("fullName") != null ? updates.get("fullName").toString() : null);
        }
        if (updates.containsKey("email")) {
            String email = updates.get("email") != null ? updates.get("email").toString().trim() : "";
            if (!email.isEmpty() && !email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email đã tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
            if (!email.isEmpty()) user.setEmail(email);
        }
        if (updates.containsKey("avatarUrl")) {
            user.setAvatarUrl(updates.get("avatarUrl") != null ? updates.get("avatarUrl").toString() : null);
        }
        if (updates.containsKey("password")) {
            String pwd = updates.get("password") != null ? updates.get("password").toString() : "";
            if (!pwd.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(pwd));
            }
        }
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Cập nhật thành công");
        response.put("data", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "role", user.getRole() != null ? user.getRole() : "ROLE_USER",
                "isActive", user.getIsActive()
        ));
        return ResponseEntity.ok(response);
    }
}

