package com.musicapp.controller;

import com.musicapp.model.EmotionLog;
import com.musicapp.model.Song;
import com.musicapp.repository.EmotionLogRepository;
import com.musicapp.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emotion")
@CrossOrigin(origins = "*")
public class EmotionController {
    @Autowired
    private EmotionLogRepository emotionLogRepository;

    @Autowired
    private SongRepository songRepository;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeEmotion(@RequestParam Long userId, @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String imageBase64 = request.get("image");
        if (imageBase64 == null || imageBase64.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không có ảnh để phân tích");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Gọi AI service để phân tích cảm xúc
            Map<String, String> aiRequest = new HashMap<>();
            aiRequest.put("image", imageBase64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(aiRequest, headers);

            ResponseEntity<Map> aiResponse = restTemplate.postForEntity(
                aiServiceUrl + "/analyze-emotion", 
                entity, 
                Map.class
            );

            if (aiResponse.getStatusCode() == HttpStatus.OK && aiResponse.getBody() != null) {
                Map<String, Object> aiResult = aiResponse.getBody();
                String emotion = (String) aiResult.get("emotion");
                Double confidence = (Double) aiResult.get("confidence");

                // Lưu log cảm xúc
                EmotionLog emotionLog = new EmotionLog();
                emotionLog.setUserId(userId);
                emotionLog.setEmotion(emotion);
                emotionLog.setConfidence(BigDecimal.valueOf(confidence));
                emotionLog.setImageUrl("data:image/jpeg;base64," + imageBase64);
                emotionLogRepository.save(emotionLog);

                // Lấy danh sách nhạc phù hợp với cảm xúc
                List<Song> recommendedSongs = getRecommendedSongsByEmotion(emotion);

                response.put("success", true);
                response.put("emotion", emotion);
                response.put("confidence", confidence);
                response.put("recommendedSongs", recommendedSongs);
            } else {
                response.put("success", false);
                response.put("message", "Không thể phân tích cảm xúc");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi gọi AI service: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Use this endpoint when emotion is predicted on-device (mobile).
     * It will still log the emotion + confidence and return recommended songs,
     * without calling the external AI service.
     *
     * Body example: { "emotion": "Happy", "confidence": 0.82 }
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommendByEmotion(@RequestParam Long userId, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        Object emotionObj = request.get("emotion");
        if (emotionObj == null || String.valueOf(emotionObj).trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Thiếu emotion");
            return ResponseEntity.badRequest().body(response);
        }

        String emotion = String.valueOf(emotionObj).trim();
        Double confidence = null;
        Object confidenceObj = request.get("confidence");
        if (confidenceObj instanceof Number) {
            confidence = ((Number) confidenceObj).doubleValue();
        } else if (confidenceObj != null) {
            try {
                confidence = Double.parseDouble(String.valueOf(confidenceObj));
            } catch (Exception ignored) {
                confidence = null;
            }
        }

        try {
            // Save log (no image)
            EmotionLog emotionLog = new EmotionLog();
            emotionLog.setUserId(userId);
            emotionLog.setEmotion(emotion);
            if (confidence != null) {
                emotionLog.setConfidence(BigDecimal.valueOf(confidence));
            }
            emotionLog.setImageUrl(null);
            emotionLogRepository.save(emotionLog);

            // Recommend songs
            List<Song> recommendedSongs = getRecommendedSongsByEmotion(emotion);

            response.put("success", true);
            response.put("emotion", emotion);
            if (confidence != null) {
                response.put("confidence", confidence);
            }
            response.put("recommendedSongs", recommendedSongs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi recommend: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private List<Song> getRecommendedSongsByEmotion(String emotion) {
        // Mapping cảm xúc với thể loại nhạc
        String mood = mapEmotionToMood(emotion);
        return songRepository.findByMood(mood);
    }

    private String mapEmotionToMood(String emotion) {
        switch (emotion.toLowerCase()) {
            case "vui":
            case "happy":
            case "surprise":
                return "Vui";
            case "buồn":
            case "sad":
            case "fear":
                return "Buồn";
            case "thư giãn":
            case "relaxed":
                return "Thư giãn";
            case "căng thẳng":
            case "stressed":
            case "anxious":
            // DB seed currently uses moods: Vui / Buồn / Thư giãn / Bình thường
            // Map stress/anxiety to an existing mood so recommendations are not empty.
            return "Thư giãn";
            case "tức giận":
            case "angry":
            case "disgust":
            case "neutral":
                return "Bình thường";
            default:
                return "Bình thường";
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getEmotionHistory(@PathVariable Long userId) {
        List<EmotionLog> logs = emotionLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", logs);
        return ResponseEntity.ok(response);
    }
}
