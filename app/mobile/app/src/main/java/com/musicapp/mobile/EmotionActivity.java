package com.musicapp.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
import com.musicapp.mobile.api.EmotionLabelRequest;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.ml.EmotionTFLiteClassifier;
import com.musicapp.mobile.ml.ImageProxyUtils;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.ByteArrayOutputStream;
import android.graphics.ImageFormat;
import java.util.concurrent.ExecutionException;

public class EmotionActivity extends AppCompatActivity {
    private PreviewView previewView;
    private Button buttonCapture;
    private Button buttonAnalyze;
    private ImageView imageViewCaptured;
    private TextView textViewEmotion;
    private TextView textViewConfidence;
    private ImageButton buttonBack;

    private TextView tvStatus1Label;
    private TextView tvStatus1Value;
    private ProgressBar progressHappy;
    private TextView tvStatus2Label;
    private TextView tvStatus2Value;
    private ProgressBar progressNeutral;
    private TextView tvStatus3Label;
    private TextView tvStatus3Value;
    private ProgressBar progressSad;
    private Bitmap capturedBitmap;
    private ImageCapture imageCapture;
    private ApiService apiService;
    private EmotionTFLiteClassifier localClassifier;
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
        buttonBack = findViewById(R.id.buttonBack);

        tvStatus1Label = findViewById(R.id.tvStatus1Label);
        tvStatus1Value = findViewById(R.id.tvStatus1Value);
        progressHappy = findViewById(R.id.progressHappy);
        tvStatus2Label = findViewById(R.id.tvStatus2Label);
        tvStatus2Value = findViewById(R.id.tvStatus2Value);
        progressNeutral = findViewById(R.id.progressNeutral);
        tvStatus3Label = findViewById(R.id.tvStatus3Label);
        tvStatus3Value = findViewById(R.id.tvStatus3Value);
        progressSad = findViewById(R.id.progressSad);

        resetStatusUI();

        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();

        // Try local on-device model first (assets/emotion_model.tflite). If missing, we'll fall back to backend.
        try {
            localClassifier = new EmotionTFLiteClassifier(this);
        } catch (Exception e) {
            localClassifier = null;
        }

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        buttonBack.setOnClickListener(v -> finish());

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

                imageCapture = new ImageCapture.Builder()
                        // Prefer YUV for consistent processing, but ImageProxyUtils also supports JPEG.
                        .setBufferFormat(ImageFormat.YUV_420_888)
                        .build();

                cameraProvider.unbindAll();

                // Prefer front camera, but some emulators/devices may not support it.
                try {
                    CameraSelector front = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.bindToLifecycle(this, front, preview, imageCapture);
                } catch (Exception frontErr) {
                    CameraSelector back = CameraSelector.DEFAULT_BACK_CAMERA;
                    cameraProvider.bindToLifecycle(this, back, preview, imageCapture);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Không mở được camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture != null) {
            buttonCapture.setEnabled(false);
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy image) {
                    try {
                        Bitmap raw = ImageProxyUtils.toBitmap(image);
                        int rotation = image.getImageInfo().getRotationDegrees();
                        capturedBitmap = rotateBitmap(raw, rotation);
                        imageViewCaptured.setImageBitmap(capturedBitmap);
                        imageViewCaptured.setVisibility(View.VISIBLE);
                        textViewEmotion.setText("Ảnh đã chụp");
                        textViewConfidence.setText("Bấm “Gợi ý nhạc phù hợp” để phân tích");
                        buttonCapture.setText("Chụp lại");
                        Toast.makeText(EmotionActivity.this, "Ảnh đã được chụp", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(EmotionActivity.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    image.close();
                    buttonCapture.setEnabled(true);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(EmotionActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonCapture.setEnabled(true);
                }
            });
        }
    }

    private void analyzeEmotion() {
        buttonAnalyze.setEnabled(false);
        textViewConfidence.setText("Đang phân tích...");

        // Local (on-device) inference if available
        if (localClassifier != null) {
            try {
                EmotionTFLiteClassifier.Result r = localClassifier.classify(capturedBitmap);
                textViewEmotion.setText("Cảm xúc: " + r.emotion);
                textViewConfidence.setText("Độ tin cậy: " + String.format("%.2f%%", r.confidence * 100));
                updateTop3(r.probabilities);

                // Call backend to get recommended songs (no need to upload the image)
                android.content.SharedPreferences prefs = getSharedPreferences("MusicApp", MODE_PRIVATE);
                Long userId = prefs.getLong("userId", 1L);
                Call<ResponseBody> call = apiService.recommendByEmotion(
                        userId,
                        new EmotionLabelRequest(r.emotion, (double) r.confidence)
                );
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String bodyStr;
                                JSONObject json;
                                try {
                                    bodyStr = response.body().string();
                                    json = new JSONObject(bodyStr);
                                } catch (Exception parseErr) {
                                    Toast.makeText(EmotionActivity.this, "Lỗi parse response: " + parseErr.getMessage(), Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if (json.optBoolean("success", false)) {
                                    JSONArray songs = json.optJSONArray("recommendedSongs");
                                    int n = songs != null ? songs.length() : 0;
                                    textViewConfidence.setText("Độ tin cậy: " + String.format("%.2f%%", r.confidence * 100) + " • Gợi ý: " + n + " bài");

                                    // Navigate to results list for playback/browsing
                                    if (songs != null) {
                                        android.content.Intent intent = new android.content.Intent(EmotionActivity.this, EmotionResultActivity.class);
                                        intent.putExtra(EmotionResultActivity.EXTRA_EMOTION, r.emotion);
                                        intent.putExtra(EmotionResultActivity.EXTRA_CONFIDENCE, (double) r.confidence);
                                        intent.putExtra(EmotionResultActivity.EXTRA_RECOMMENDED_SONGS_JSON, songs.toString());
                                        startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(EmotionActivity.this, json.optString("message", "Không lấy được gợi ý"), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String msg = "Không lấy được gợi ý (HTTP " + response.code() + ")";
                                try {
                                    if (response.errorBody() != null) {
                                        String err = response.errorBody().string();
                                        if (err != null && !err.isEmpty()) msg += ": " + err;
                                    }
                                } catch (Exception ignored) {}
                                Toast.makeText(EmotionActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        } finally {
                            buttonAnalyze.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(EmotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonAnalyze.setEnabled(true);
                    }
                });
                return; // stop here, we already handled local inference path
            } catch (Exception e) {
                // If local fails for any reason, fall back to backend for now.
            }
        }

        String imageBase64 = bitmapToBase64(capturedBitmap);
        
        try {
            EmotionRequest request = new EmotionRequest(imageBase64);

            // Get userId from SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("MusicApp", MODE_PRIVATE);
            Long userId = prefs.getLong("userId", 1L); // Default to 1 if not found

            Call<ResponseBody> call = apiService.analyzeEmotion(userId, request);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String bodyStr = response.body().string();
                            JSONObject jsonResponse = new JSONObject(bodyStr);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                String emotion = jsonResponse.getString("emotion");
                                double confidence = jsonResponse.getDouble("confidence");

                                textViewEmotion.setText("Cảm xúc: " + emotion);
                                textViewConfidence.setText("Độ tin cậy: " + String.format("%.2f%%", confidence * 100));

                                // Show recommended songs
                                JSONArray recommendedSongs = jsonResponse.getJSONArray("recommendedSongs");
                                textViewConfidence.setText("Độ tin cậy: " + String.format("%.2f%%", confidence * 100) + " • Gợi ý: " + recommendedSongs.length() + " bài");
                            } else {
                                String message = jsonResponse.getString("message");
                                Toast.makeText(EmotionActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(EmotionActivity.this, "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }
                    buttonAnalyze.setEnabled(true);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(EmotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonAnalyze.setEnabled(true);
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            buttonAnalyze.setEnabled(true);
        }
    }

    private void resetStatusUI() {
        tvStatus1Label.setText("—");
        tvStatus1Value.setText("--");
        progressHappy.setProgress(0);

        tvStatus2Label.setText("—");
        tvStatus2Value.setText("--");
        progressNeutral.setProgress(0);

        tvStatus3Label.setText("—");
        tvStatus3Value.setText("--");
        progressSad.setProgress(0);
    }

    private void updateTop3(java.util.Map<String, Float> probs) {
        if (probs == null || probs.isEmpty()) return;

        java.util.List<java.util.Map.Entry<String, Float>> list = new java.util.ArrayList<>(probs.entrySet());
        list.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        setRow(1, list.size() > 0 ? list.get(0) : null);
        setRow(2, list.size() > 1 ? list.get(1) : null);
        setRow(3, list.size() > 2 ? list.get(2) : null);
    }

    private void setRow(int idx, java.util.Map.Entry<String, Float> e) {
        String label = e != null ? e.getKey() : "—";
        int pct = e != null ? Math.round(e.getValue() * 100f) : 0;
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;

        if (idx == 1) {
            tvStatus1Label.setText(label);
            tvStatus1Value.setText(pct + "%");
            progressHappy.setProgress(pct);
        } else if (idx == 2) {
            tvStatus2Label.setText(label);
            tvStatus2Value.setText(pct + "%");
            progressNeutral.setProgress(pct);
        } else if (idx == 3) {
            tvStatus3Label.setText(label);
            tvStatus3Value.setText(pct + "%");
            progressSad.setProgress(pct);
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private static Bitmap rotateBitmap(Bitmap src, int degrees) {
        if (degrees == 0) return src;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localClassifier != null) {
            localClassifier.close();
        }
    }
}
