package com.musicapp.controller;

import com.musicapp.model.User;
import com.musicapp.repository.UserRepository;
import com.musicapp.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String fullName = request.get("fullName");

        if (username == null || email == null || password == null) {
            response.put("success", false);
            response.put("message", "Username, email và password là bắt buộc");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByUsername(username)) {
            response.put("success", false);
            response.put("message", "Username đã tồn tại");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email đã tồn tại");
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setIsActive(true);
        user.setRole("ROLE_USER"); // Luôn là user thường khi đăng ký
        user.setDeleted(false);

        userRepository.save(user);

        String token = jwtService.generateToken(username, user.getRole());

        response.put("success", true);
        response.put("message", "Đăng ký thành công");
        response.put("token", token);
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "role", user.getRole() != null ? user.getRole() : "ROLE_USER"
        ));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            response.put("success", false);
            response.put("message", "Username và password là bắt buộc");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            response.put("success", false);
            response.put("message", "Username hoặc password không đúng");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userOpt.get();
        if (!user.getIsActive()) {
            response.put("success", false);
            response.put("message", "Tài khoản đã bị khóa");
            return ResponseEntity.badRequest().body(response);
        }

        String token = jwtService.generateToken(username, user.getRole());

        response.put("success", true);
        response.put("message", "Đăng nhập thành công");
        response.put("token", token);
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "role", user.getRole() != null ? user.getRole() : "ROLE_USER"
        ));

        return ResponseEntity.ok(response);
    }
}
