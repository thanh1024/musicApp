package com.musicapp.mobile.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous face cropper using ML Kit Face Detection.
 * Returns largest detected face with 10% padding; falls back to center-crop square.
 */
public final class FaceCropper {
    private FaceCropper() {}

    public static Bitmap cropFaceOrCenter(Context context, Bitmap src) {
        Bitmap face = detectLargestFaceSync(context, src);
        if (face != null) return face;
        return centerCropSquare(src);
    }

    @Nullable
    private static Bitmap detectLargestFaceSync(Context context, Bitmap src) {
        try {
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                            .build();
            FaceDetector detector = FaceDetection.getClient(options);
            InputImage image = InputImage.fromBitmap(src, 0);
            CountDownLatch latch = new CountDownLatch(1);
            final Bitmap[] out = new Bitmap[1];
            Task<List<Face>> task = detector.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> faces) {
                            try {
                                if (faces == null || faces.isEmpty()) return;
                                // pick largest
                                Face best = null;
                                int bestArea = -1;
                                for (Face f : faces) {
                                    Rect b = f.getBoundingBox();
                                    int area = Math.max(0, b.width()) * Math.max(0, b.height());
                                    if (area > bestArea) { bestArea = area; best = f; }
                                }
                                if (best == null) return;
                                Rect b = best.getBoundingBox();
                                // clamp + 10% padding
                                int w = src.getWidth(), h = src.getHeight();
                                int padX = (int) (b.width() * 0.1f);
                                int padY = (int) (b.height() * 0.1f);
                                int x1 = Math.max(0, b.left - padX);
                                int y1 = Math.max(0, b.top - padY);
                                int x2 = Math.min(w, b.right + padX);
                                int y2 = Math.min(h, b.bottom + padY);
                                int cw = Math.max(1, x2 - x1);
                                int ch = Math.max(1, y2 - y1);
                                // enforce square crop by expanding shorter side if possible
                                int size = Math.min(Math.min(w, h), Math.max(cw, ch));
                                int cx = x1 + cw / 2;
                                int cy = y1 + ch / 2;
                                int sx = Math.max(0, cx - size / 2);
                                int sy = Math.max(0, cy - size / 2);
                                if (sx + size > w) sx = w - size;
                                if (sy + size > h) sy = h - size;
                                if (sx < 0) sx = 0;
                                if (sy < 0) sy = 0;
                                out[0] = Bitmap.createBitmap(src, sx, sy, size, size);
                            } finally {
                                latch.countDown();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            latch.countDown();
                        }
                    });
            latch.await(400, TimeUnit.MILLISECONDS); // keep UI responsive
            detector.close();
            return out[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap centerCropSquare(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int size = Math.min(w, h);
        int x0 = (w - size) / 2;
        int y0 = (h - size) / 2;
        return Bitmap.createBitmap(src, x0, y0, size, size);
    }
}

