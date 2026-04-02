package com.musicapp.config;

import com.musicapp.model.User;
import com.musicapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed minimal data so the app can run end-to-end out of the box.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create a default admin if none exists
            boolean hasAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRole() != null && u.getRole().contains("ADMIN") && (u.getDeleted() == null || !u.getDeleted()));

            if (!hasAdmin) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@musicapp.local");
                admin.setFullName("System Admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setIsActive(true);
                admin.setRole("ROLE_ADMIN");
                admin.setDeleted(false);
                userRepository.save(admin);
            }
        };
    }
}

