package com.musicapp.mobile.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmotionTFLiteClassifier {
    public static final String DEFAULT_MODEL_ASSET = "emotion_model.tflite";

    // Keep ordering consistent with the Python demo dict in detection_emotion/emotionRecognition.py
    public static final String[] LABELS = new String[]{
            "Angry", "Disgust", "Fear", "Happy", "Neutral", "Sad", "Surprise"
    };

    private final Interpreter interpreter;
    private final Context contextRef;
    private final int inputH;
    private final int inputW;
    private final int inputC;

    // Inference tweaks (no retrain needed)
    // Temperature < 1 -> sharpen probabilities (helps reduce Neutral dominance)
    private static final float TEMPERATURE = 0.65f;

    public EmotionTFLiteClassifier(Context context) throws IOException {
        this(context, DEFAULT_MODEL_ASSET);
    }

    public EmotionTFLiteClassifier(Context context, String assetModelPath) throws IOException {
        this.contextRef = context.getApplicationContext();
        MappedByteBuffer model = loadModelFile(context, assetModelPath);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(model, options);

        int[] shape = interpreter.getInputTensor(0).shape(); // e.g. [1, 96, 96, 1] or [1, 96, 96, 3]
        if (shape == null || shape.length != 4) {
            throw new IllegalStateException("Unexpected input tensor shape");
        }
        inputH = shape[1];
        inputW = shape[2];
        inputC = shape[3];
        if (!(inputC == 1 || inputC == 3)) {
            throw new IllegalStateException("Unsupported input channels: " + inputC);
        }
    }

    public void close() {
        interpreter.close();
    }

    public Result classify(Bitmap bitmap) {
        // Better face ROI using ML Kit (falls back internally)
        Bitmap face = FaceCropper.cropFaceOrCenter(contextRef, bitmap);
        float[] outProbs = predictWithTta(face);

        int argmax = 0;
        float best = outProbs[0];
        for (int i = 1; i < outProbs.length; i++) {
            if (outProbs[i] > best) {
                best = outProbs[i];
                argmax = i;
            }
        }

        Map<String, Float> probs = new LinkedHashMap<>();
        for (int i = 0; i < LABELS.length; i++) {
            probs.put(LABELS[i], outProbs[i]);
        }
        return new Result(LABELS[argmax], best, probs);
    }

    /**
     * Predict with a tiny Test-Time Augmentation:
     * - center-crop square
     * - lighting normalization (histogram equalization on luminance)
     * - average probs of original + horizontally flipped
     * - temperature scaling
     */
    private float[] predictWithTta(Bitmap src) {
        float[] p1 = predictOnce(src, false);
        float[] p2 = predictOnce(src, true);
        float[] avg = new float[LABELS.length];
        for (int i = 0; i < avg.length; i++) {
            avg[i] = (p1[i] + p2[i]) * 0.5f;
        }
        return softmaxTemperature(avg, TEMPERATURE);
    }

    private float[] predictOnce(Bitmap src, boolean flipHorizontal) {
        float[][] output = new float[1][LABELS.length];
        ByteBuffer input = preprocess(src, flipHorizontal);
        interpreter.run(input, output);
        float[] out = new float[LABELS.length];
        System.arraycopy(output[0], 0, out, 0, LABELS.length);
        return out;
    }

    private ByteBuffer preprocess(Bitmap src, boolean flipHorizontal) {
        Bitmap cropped = centerCropSquare(src);
        Bitmap resized = Bitmap.createScaledBitmap(cropped, inputW, inputH, true);

        // Load pixels
        int[] pixels = new int[inputH * inputW];
        resized.getPixels(pixels, 0, inputW, 0, 0, inputW, inputH);

        // Histogram equalization on luminance (fast, helps low-light Neutral bias)
        equalizeLumaInPlace(pixels);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputH * inputW * inputC);
        buffer.order(ByteOrder.nativeOrder());

        for (int y = 0; y < inputH; y++) {
            for (int x = 0; x < inputW; x++) {
                int xx = flipHorizontal ? (inputW - 1 - x) : x;
                int p = pixels[y * inputW + xx];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;

                if (inputC == 1) {
                    float gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
                    buffer.putFloat(gray);
                } else {
                    // Model was trained RGB in train.py, but Android Bitmap is effectively sRGB; this is fine.
                    buffer.putFloat(r / 255.0f);
                    buffer.putFloat(g / 255.0f);
                    buffer.putFloat(b / 255.0f);
                }
            }
        }

        buffer.rewind();
        return buffer;
    }

    private static Bitmap centerCropSquare(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.min(w, h);
        int x0 = (w - size) / 2;
        int y0 = (h - size) / 2;
        return Bitmap.createBitmap(src, x0, y0, size, size);
    }

    /**
     * Simple luminance histogram equalization (not full CLAHE but helps a lot).
     * Works in-place on ARGB pixels.
     */
    private static void equalizeLumaInPlace(int[] argb) {
        if (argb == null || argb.length == 0) return;
        int[] hist = new int[256];
        int n = argb.length;
        int[] yVals = new int[n];

        for (int i = 0; i < n; i++) {
            int p = argb[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            // ITU-R BT.601 luma
            int y = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            if (y < 0) y = 0;
            if (y > 255) y = 255;
            yVals[i] = y;
            hist[y]++;
        }

        int[] cdf = new int[256];
        int cum = 0;
        for (int i = 0; i < 256; i++) {
            cum += hist[i];
            cdf[i] = cum;
        }

        // Find first non-zero CDF
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] != 0) { cdfMin = cdf[i]; break; }
        }

        for (int i = 0; i < n; i++) {
            int p = argb[i];
            int a = (p >>> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            int y = yVals[i];
            int yEq = (int) ((cdf[y] - cdfMin) * 255.0f / Math.max(1, (n - cdfMin)));
            if (yEq < 0) yEq = 0;
            if (yEq > 255) yEq = 255;

            // Scale RGB by ratio of newY/oldY (keep chroma roughly)
            float ratio = (y <= 0) ? 1.0f : (yEq / (float) y);
            int nr = clamp255((int) (r * ratio));
            int ng = clamp255((int) (g * ratio));
            int nb = clamp255((int) (b * ratio));
            argb[i] = Color.argb(a, nr, ng, nb);
        }
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    private static float[] softmaxTemperature(float[] p, float T) {
        // Treat model output as probabilities, convert to log-space then temperature-scale.
        double max = -Double.MAX_VALUE;
        double[] z = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            double pi = Math.max(1e-9, Math.min(1.0, p[i]));
            double li = Math.log(pi) / Math.max(1e-6, T);
            z[i] = li;
            if (li > max) max = li;
        }
        double sum = 0.0;
        for (int i = 0; i < z.length; i++) {
            z[i] = Math.exp(z[i] - max);
            sum += z[i];
        }
        float[] out = new float[p.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (float) (z[i] / Math.max(1e-12, sum));
        }
        return out;
    }

    private static MappedByteBuffer loadModelFile(Context context, String assetPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static class Result {
        public final String emotion;
        public final float confidence;
        public final Map<String, Float> probabilities;

        public Result(String emotion, float confidence, Map<String, Float> probabilities) {
            this.emotion = emotion;
            this.confidence = confidence;
            this.probabilities = probabilities;
        }
    }
}

