package com.musicapp.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.common.util.concurrent.ListenableFuture;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.EmotionRequest;
import com.musicapp.mobile.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

public class EmotionActivity extends AppCompatActivity {
    private PreviewView previewView;
    private Button buttonCapture;
    private Button buttonAnalyze;
    private ImageView imageViewCaptured;
    private TextView textViewEmotion;
    private TextView textViewConfidence;
    private Bitmap capturedBitmap;
    private ImageCapture imageCapture;
    private ApiService apiService;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion);

        previewView = findViewById(R.id.previewView);
        buttonCapture = findViewById(R.id.buttonCapture);
        buttonAnalyze = findViewById(R.id.buttonAnalyze);
        imageViewCaptured = findViewById(R.id.imageViewCaptured);
        textViewEmotion = findViewById(R.id.textViewEmotion);
        textViewConfidence = findViewById(R.id.textViewConfidence);

        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        buttonAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturedBitmap != null) {
                    analyzeEmotion();
                } else {
                    Toast.makeText(EmotionActivity.this, "Vui lòng chụp ảnh trước", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
            startCamera();
        } else {
            Toast.makeText(this, "Cần quyền camera để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
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
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy image) {
                    // Convert ImageProxy to Bitmap
                    // Note: This is simplified, you may need to handle rotation and format conversion
                    Toast.makeText(EmotionActivity.this, "Ảnh đã được chụp", Toast.LENGTH_SHORT).show();
                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(EmotionActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void analyzeEmotion() {
        String imageBase64 = bitmapToBase64(capturedBitmap);
        
        try {
            EmotionRequest request = new EmotionRequest(imageBase64);

            // Get userId from SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("MusicApp", MODE_PRIVATE);
            Long userId = prefs.getLong("userId", 1L); // Default to 1 if not found

            Call<JSONObject> call = apiService.analyzeEmotion(userId, request);
            call.enqueue(new Callback<JSONObject>() {
                @Override
                public void onResponse(Call<JSONObject> call, Response<JSONObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject jsonResponse = response.body();
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                String emotion = jsonResponse.getString("emotion");
                                double confidence = jsonResponse.getDouble("confidence");

                                textViewEmotion.setText("Cảm xúc: " + emotion);
                                textViewConfidence.setText("Độ tin cậy: " + String.format("%.2f%%", confidence * 100));

                                // Show recommended songs
                                JSONArray recommendedSongs = jsonResponse.getJSONArray("recommendedSongs");
                                // Handle recommended songs display
                            } else {
                                String message = jsonResponse.getString("message");
                                Toast.makeText(EmotionActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(EmotionActivity.this, "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<JSONObject> call, Throwable t) {
                    Toast.makeText(EmotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
