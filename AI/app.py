import base64
import os
from typing import Tuple

import cv2
import numpy as np
from flask import Flask, jsonify, request


def _strip_data_url_prefix(s: str) -> str:
    if not s:
        return s
    # e.g. "data:image/jpeg;base64,...."
    if "," in s and s.lower().startswith("data:"):
        return s.split(",", 1)[1]
    return s


def _decode_base64_image(image_b64: str) -> np.ndarray:
    image_b64 = _strip_data_url_prefix(image_b64)
    raw = base64.b64decode(image_b64)
    arr = np.frombuffer(raw, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Không decode được ảnh (base64 không hợp lệ hoặc định dạng không hỗ trợ)")
    return img


def _detect_face_bgr(img_bgr: np.ndarray) -> np.ndarray:
    # Lightweight face detection (no dlib). If not found, fall back to center crop.
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    cascade = cv2.CascadeClassifier(os.path.join(cv2.data.haarcascades, "haarcascade_frontalface_default.xml"))
    faces = cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))
    if len(faces) > 0:
        x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
        return img_bgr[y : y + h, x : x + w]

    h, w = img_bgr.shape[:2]
    size = min(h, w)
    y0 = (h - size) // 2
    x0 = (w - size) // 2
    return img_bgr[y0 : y0 + size, x0 : x0 + size]


def _load_model_and_shape(model_path: str):
    import tensorflow as tf

    model = tf.keras.models.load_model(model_path, compile=False)
    input_shape = model.input_shape  # (None, H, W, C)
    if not input_shape or len(input_shape) != 4:
        raise RuntimeError(f"Unexpected input_shape: {input_shape}")
    _, h, w, c = input_shape
    if c not in (1, 3):
        raise RuntimeError(f"Unsupported channel count: {c}")
    return model, (h, w, c)


def _preprocess(face_bgr: np.ndarray, shape: Tuple[int, int, int]) -> np.ndarray:
    h, w, c = shape
    if c == 1:
        gray = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2GRAY)
        resized = cv2.resize(gray, (w, h), interpolation=cv2.INTER_AREA)
        x = resized.astype(np.float32) / 255.0
        x = np.expand_dims(x, axis=(0, -1))  # (1, H, W, 1)
        return x
    else:
        rgb = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2RGB)
        resized = cv2.resize(rgb, (w, h), interpolation=cv2.INTER_AREA)
        x = resized.astype(np.float32) / 255.0
        x = np.expand_dims(x, axis=0)  # (1, H, W, 3)
        return x


LABELS = ["Angry", "Disgust", "Fear", "Happy", "Neutral", "Sad", "Surprise"]


app = Flask(__name__)

MODEL_PATH = os.environ.get(
    "EMOTION_MODEL_PATH",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "detection_emotion", "my_emotion_model_pro.h5")),
)

_model, _shape = _load_model_and_shape(MODEL_PATH)


@app.get("/health")
def health():
    return jsonify({"ok": True, "model_path": MODEL_PATH, "input_shape": [None, *_shape]})


@app.post("/analyze-emotion")
def analyze_emotion():
    data = request.get_json(silent=True) or {}
    image_b64 = data.get("image", "")
    if not image_b64:
        return jsonify({"success": False, "message": "Thiếu field 'image' base64"}), 400

    try:
        img = _decode_base64_image(image_b64)
        face = _detect_face_bgr(img)
        x = _preprocess(face, _shape)
        probs = _model.predict(x, verbose=0)[0].astype(float).tolist()

        idx = int(np.argmax(probs))
        emotion = LABELS[idx]
        confidence = float(probs[idx])
        return jsonify({"emotion": emotion, "confidence": confidence, "probabilities": dict(zip(LABELS, probs))})
    except Exception as e:
        return jsonify({"success": False, "message": f"Lỗi phân tích: {e}"}), 500


if __name__ == "__main__":
    # Backend expects localhost:5000
    app.run(host="0.0.0.0", port=5000, debug=False)

