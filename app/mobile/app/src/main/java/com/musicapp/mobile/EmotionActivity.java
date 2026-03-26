package com.musicapp.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.util.concurrent.ListenableFuture;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.EmotionRequest;
import com.musicapp.mobile.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutionException;

public class EmotionActivity extends AppCompatActivity {
    private ImageView imageViewCaptured;
    private TextView textViewEmotion;
    private TextView textViewConfidence;
    private Bitmap capturedBitmap;
    private ImageCapture imageCapture;
    private ApiService apiService;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_EMOTION = "emotion";
    private static final String KEY_CONFIDENCE = "confidence";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_RECOMMENDED_SONGS = "recommendedSongs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion);

        PreviewView previewView = findViewById(R.id.previewView);
        Button buttonCapture = findViewById(R.id.buttonCapture);
        Button buttonAnalyze = findViewById(R.id.buttonAnalyze);
        ImageView buttonBack = findViewById(R.id.buttonBack);
        imageViewCaptured = findViewById(R.id.imageViewCaptured);
        textViewEmotion = findViewById(R.id.textViewEmotion);
        textViewConfidence = findViewById(R.id.textViewConfidence);

        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();

        if (checkCameraPermission()) {
            startCamera(previewView);
        } else {
            requestCameraPermission();
        }

        buttonCapture.setOnClickListener(v -> captureImage());

        buttonBack.setOnClickListener(v -> finish());

        buttonAnalyze.setOnClickListener(v -> analyzeEmotion());
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            PreviewView previewView = findViewById(R.id.previewView);
            startCamera(previewView);
        } else {
            Toast.makeText(this, "Cần quyền camera để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera(PreviewView previewView) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture != null) {
            File outputFile = new File(getCacheDir(), "emotion_capture_" + System.currentTimeMillis() + ".jpg");
            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(outputFile).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    capturedBitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                    if (capturedBitmap == null) {
                        Toast.makeText(EmotionActivity.this, "Không đọc được ảnh vừa chụp", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    imageViewCaptured.setImageBitmap(capturedBitmap);
                    imageViewCaptured.setVisibility(View.VISIBLE);
                    Toast.makeText(EmotionActivity.this, "Ảnh đã được chụp", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(EmotionActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void analyzeEmotion() {
        if (capturedBitmap == null) {
            Toast.makeText(EmotionActivity.this, "Vui lòng chụp ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = bitmapToBase64(capturedBitmap);
        EmotionRequest request = new EmotionRequest(imageBase64);
        Long userId = getStoredUserId();

        Call<JsonObject> call = apiService.analyzeEmotion(userId, request);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                handleAnalyzeResponse(response);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(EmotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private String buildConfidenceAndRecommendationText(double confidence, JsonObject response) {
        StringBuilder builder = new StringBuilder();
        builder.append("Độ tin cậy: ").append(String.format("%.2f%%", confidence * 100));

        if (response.has(KEY_RECOMMENDED_SONGS) && response.get(KEY_RECOMMENDED_SONGS).isJsonArray()) {
            JsonArray recommendedSongs = response.getAsJsonArray(KEY_RECOMMENDED_SONGS);
            if (recommendedSongs.size() > 0) {
                builder.append("\nGợi ý: ");
                builder.append(buildRecommendationListText(recommendedSongs));
            }
        }

        return builder.toString();
    }

    private void handleAnalyzeResponse(Response<JsonObject> response) {
        if (!response.isSuccessful() || response.body() == null) {
            Toast.makeText(EmotionActivity.this, "Không nhận được dữ liệu từ server", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject jsonResponse = response.body();
        if (!isSuccessResponse(jsonResponse)) {
            Toast.makeText(EmotionActivity.this, getStringValue(jsonResponse, KEY_MESSAGE, "Phân tích thất bại"), Toast.LENGTH_SHORT).show();
            return;
        }

        String emotion = getStringValue(jsonResponse, KEY_EMOTION, "Không rõ");
        double confidence = getDoubleValue(jsonResponse, KEY_CONFIDENCE, 0.0);

        textViewEmotion.setText("Cảm xúc: " + emotion);
        textViewConfidence.setText(buildConfidenceAndRecommendationText(confidence, jsonResponse));
    }

    private Long getStoredUserId() {
        android.content.SharedPreferences prefs = getSharedPreferences("MusicApp", MODE_PRIVATE);
        return prefs.getLong("userId", 1L);
    }

    private boolean isSuccessResponse(JsonObject response) {
        return response.has(KEY_SUCCESS) && response.get(KEY_SUCCESS).getAsBoolean();
    }

    private String buildRecommendationListText(JsonArray recommendedSongs) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, recommendedSongs.size());

        for (int i = 0; i < limit; i++) {
            JsonElement element = recommendedSongs.get(i);
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject song = element.getAsJsonObject();
            String title = getStringValue(song, KEY_TITLE, "Không tên");
            String artist = getStringValue(song, KEY_ARTIST, "Không rõ");
            builder.append(title).append(" - ").append(artist);
            if (i < limit - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private String getStringValue(JsonObject object, String key, String defaultValue) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsString();
        }
        return defaultValue;
    }

    private double getDoubleValue(JsonObject object, String key, double defaultValue) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return object.get(key).getAsDouble();
        }
        return defaultValue;
    }
}
