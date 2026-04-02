package com.musicapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/genres")
@CrossOrigin(origins = "*")
public class GenreController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<?> list() {
        List<Map<String, Object>> genres = jdbcTemplate.queryForList(
                "SELECT id, name FROM genres ORDER BY LOWER(name) ASC"
        );
        return ResponseEntity.ok(Map.of("success", true, "data", genres, "total", genres.size()));
    }
}

