package com.musicapp.mobile.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

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
    private final int inputH;
    private final int inputW;
    private final int inputC;

    public EmotionTFLiteClassifier(Context context) throws IOException {
        this(context, DEFAULT_MODEL_ASSET);
    }

    public EmotionTFLiteClassifier(Context context, String assetModelPath) throws IOException {
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
        float[][] output = new float[1][LABELS.length];
        ByteBuffer input = preprocess(bitmap);
        interpreter.run(input, output);

        int argmax = 0;
        float best = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > best) {
                best = output[0][i];
                argmax = i;
            }
        }

        Map<String, Float> probs = new LinkedHashMap<>();
        for (int i = 0; i < LABELS.length; i++) {
            probs.put(LABELS[i], output[0][i]);
        }
        return new Result(LABELS[argmax], best, probs);
    }

    private ByteBuffer preprocess(Bitmap src) {
        Bitmap resized = Bitmap.createScaledBitmap(src, inputW, inputH, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputH * inputW * inputC);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputH * inputW];
        resized.getPixels(pixels, 0, inputW, 0, 0, inputW, inputH);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            if (inputC == 1) {
                // Approx luma, normalized like Python demo (/255.0)
                float gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
                buffer.putFloat(gray);
            } else {
                buffer.putFloat(r / 255.0f);
                buffer.putFloat(g / 255.0f);
                buffer.putFloat(b / 255.0f);
            }
        }

        buffer.rewind();
        return buffer;
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

