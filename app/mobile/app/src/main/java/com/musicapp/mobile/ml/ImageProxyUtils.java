package com.musicapp.mobile.ml;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageProxyUtils {
    private ImageProxyUtils() {}

    public static Bitmap toBitmap(@NonNull ImageProxy image) {
        // Some devices/emulators return JPEG (256) for ImageCapture, others return YUV_420_888.
        if (image.getFormat() == ImageFormat.JPEG) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        if (image.getFormat() == ImageFormat.YUV_420_888) {
            byte[] nv21 = yuv420888ToNv21(image);
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
            byte[] jpegBytes = out.toByteArray();
            return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        }

        throw new IllegalArgumentException("Unsupported image format: " + image.getFormat());
    }

    private static byte[] yuv420888ToNv21(@NonNull ImageProxy image) {
        // Robust conversion that respects rowStride/pixelStride to avoid crashes/garbled images.
        final int width = image.getWidth();
        final int height = image.getHeight();

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride(); // usually 1
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        byte[] nv21 = new byte[width * height + (width * height) / 2];

        int yPos = 0;
        for (int row = 0; row < height; row++) {
            int yRowStart = row * yRowStride;
            for (int col = 0; col < width; col++) {
                int yIndex = yRowStart + col * yPixelStride;
                nv21[yPos++] = yBuffer.get(yIndex);
            }
        }

        int uvPos = width * height;
        int uvHeight = height / 2;
        int uvWidth = width / 2;

        for (int row = 0; row < uvHeight; row++) {
            int uRowStart = row * uRowStride;
            int vRowStart = row * vRowStride;
            for (int col = 0; col < uvWidth; col++) {
                int uIndex = uRowStart + col * uPixelStride;
                int vIndex = vRowStart + col * vPixelStride;
                // NV21 expects V then U
                nv21[uvPos++] = vBuffer.get(vIndex);
                nv21[uvPos++] = uBuffer.get(uIndex);
            }
        }

        return nv21;
    }
}

