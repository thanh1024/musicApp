import base64
import os
from typing import Tuple

import cv2
import numpy as np
from flask import Flask, jsonify, request

# ─────────────────────────────────────────────
# Helpers – image loading & preprocessing
# ─────────────────────────────────────────────

def _strip_data_url_prefix(s: str) -> str:
    if not s:
        return s
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


# ─────────────────────────────────────────────
# Face detection  (DNN → Haar → center-crop)
# ─────────────────────────────────────────────

_DNN_PROTO = os.path.abspath(os.path.join(
    os.path.dirname(__file__), "..", "detection_emotion",
    "faceDetection", "models", "dnn", "deploy.prototxt"))
_DNN_MODEL = os.path.abspath(os.path.join(
    os.path.dirname(__file__), "..", "detection_emotion",
    "faceDetection", "models", "dnn",
    "res10_300x300_ssd_iter_140000.caffemodel"))

_dnn_net = None

def _get_dnn_net():
    global _dnn_net
    if _dnn_net is None and os.path.exists(_DNN_PROTO) and os.path.exists(_DNN_MODEL):
        _dnn_net = cv2.dnn.readNetFromCaffe(_DNN_PROTO, _DNN_MODEL)
    return _dnn_net


def _detect_face_bgr(img_bgr: np.ndarray) -> np.ndarray:
    """
    Priority: OpenCV DNN (most robust) → Haar cascade → center crop.
    Also pads the crop by 10 % so the full face is included.
    """
    h, w = img_bgr.shape[:2]

    # 1. DNN detector ─────────────────────────────────────────────────
    net = _get_dnn_net()
    if net is not None:
        blob = cv2.dnn.blobFromImage(
            cv2.resize(img_bgr, (300, 300)), 1.0, (300, 300),
            (104.0, 177.0, 123.0))
        net.setInput(blob)
        detections = net.forward()
        best_conf, best_box = 0.0, None
        for i in range(detections.shape[2]):
            conf = float(detections[0, 0, i, 2])
            if conf > best_conf:
                best_conf = conf
                best_box = detections[0, 0, i, 3:7]
        if best_conf > 0.5 and best_box is not None:
            x1 = max(0, int(best_box[0] * w))
            y1 = max(0, int(best_box[1] * h))
            x2 = min(w, int(best_box[2] * w))
            y2 = min(h, int(best_box[3] * h))
            fw, fh = x2 - x1, y2 - y1
            if fw > 0 and fh > 0:
                # pad 10 %
                px, py = int(fw * 0.10), int(fh * 0.10)
                x1 = max(0, x1 - px); y1 = max(0, y1 - py)
                x2 = min(w, x2 + px); y2 = min(h, y2 + py)
                return img_bgr[y1:y2, x1:x2]

    # 2. Haar cascade ─────────────────────────────────────────────────
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    cascade = cv2.CascadeClassifier(
        os.path.join(cv2.data.haarcascades, "haarcascade_frontalface_default.xml"))
    faces = cascade.detectMultiScale(
        gray, scaleFactor=1.05, minNeighbors=3, minSize=(40, 40))
    if len(faces) > 0:
        x, y, fw, fh = max(faces, key=lambda f: f[2] * f[3])
        px, py = int(fw * 0.10), int(fh * 0.10)
        x1 = max(0, x - px); y1 = max(0, y - py)
        x2 = min(w, x + fw + px); y2 = min(h, y + fh + py)
        return img_bgr[y1:y2, x1:x2]

    # 3. Center crop fallback ─────────────────────────────────────────
    size = min(h, w)
    y0 = (h - size) // 2
    x0 = (w - size) // 2
    return img_bgr[y0:y0 + size, x0:x0 + size]


# ─────────────────────────────────────────────
# CLAHE-enhanced preprocessing
# ─────────────────────────────────────────────

def _clahe_enhance(face_bgr: np.ndarray) -> np.ndarray:
    """
    Apply CLAHE to the L channel of LAB colour space.
    This normalises uneven lighting which is the #1 cause of Neutral bias.
    """
    lab = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(4, 4))
    l = clahe.apply(l)
    lab = cv2.merge([l, a, b])
    return cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)


def _preprocess(face_bgr: np.ndarray, shape: Tuple[int, int, int]) -> np.ndarray:
    h, w, c = shape
    face_bgr = _clahe_enhance(face_bgr)
    if c == 1:
        gray = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2GRAY)
        resized = cv2.resize(gray, (w, h), interpolation=cv2.INTER_AREA)
        x = resized.astype(np.float32) / 255.0
        x = np.expand_dims(x, axis=(0, -1))       # (1,H,W,1)
        return x
    else:
        rgb = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2RGB)
        resized = cv2.resize(rgb, (w, h), interpolation=cv2.INTER_AREA)
        x = resized.astype(np.float32) / 255.0
        x = np.expand_dims(x, axis=0)              # (1,H,W,3)
        return x


# ─────────────────────────────────────────────
# Model loading
# ─────────────────────────────────────────────

def _load_model_and_shape(model_path: str):
    import tensorflow as tf
    model = tf.keras.models.load_model(model_path, compile=False)
    input_shape = model.input_shape           # (None, H, W, C)
    if not input_shape or len(input_shape) != 4:
        raise RuntimeError(f"Unexpected input_shape: {input_shape}")
    _, h, w, c = input_shape
    if c not in (1, 3):
        raise RuntimeError(f"Unsupported channel count: {c}")
    return model, (h, w, c)


# ─────────────────────────────────────────────
# Prediction with TTA + temperature scaling
# ─────────────────────────────────────────────

LABELS = ["Angry", "Disgust", "Fear", "Happy", "Neutral", "Sad", "Surprise"]

# Temperature < 1.0  →  sharpen the distribution (less Neutral dominance)
# Tune between 0.5 and 0.8; lower = more confident predictions.
TEMPERATURE = 0.6


def _softmax_with_temperature(logits: np.ndarray, T: float) -> np.ndarray:
    """Apply temperature scaling then softmax."""
    scaled = logits / T
    scaled -= scaled.max()          # numerical stability
    exp = np.exp(scaled)
    return exp / exp.sum()


def _predict_tta(model, x: np.ndarray, shape: Tuple[int, int, int]) -> np.ndarray:
    """
    Test-Time Augmentation: average probabilities from
    original + horizontally-flipped + slightly brightened crops.
    """
    # Build augmented batch (before temperature)
    raw_logits_list = []

    def _logits(inp):
        """Get raw softmax output (treat as logits for temperature scaling)."""
        return model.predict(inp, verbose=0)[0].astype(float)

    # original
    raw_logits_list.append(_logits(x))

    # horizontal flip
    if shape[2] == 1:
        x_flip = x[:, :, ::-1, :]
    else:
        x_flip = x[:, :, ::-1, :]
    raw_logits_list.append(_logits(x_flip))

    # average raw probs then apply temperature scaling on the average
    avg_probs = np.mean(raw_logits_list, axis=0)
    # convert back to logit-space via log for temperature scaling
    avg_probs = np.clip(avg_probs, 1e-9, 1.0)
    log_probs = np.log(avg_probs)
    return _softmax_with_temperature(log_probs, TEMPERATURE)


# ─────────────────────────────────────────────
# Flask app
# ─────────────────────────────────────────────

app = Flask(__name__)

MODEL_PATH = os.environ.get(
    "EMOTION_MODEL_PATH",
    os.path.abspath(os.path.join(
        os.path.dirname(__file__), "..", "detection_emotion", "my_emotion_model_pro.h5")),
)

_model, _shape = _load_model_and_shape(MODEL_PATH)


@app.get("/health")
def health():
    return jsonify({
        "ok": True,
        "model_path": MODEL_PATH,
        "input_shape": [None, *_shape],
        "temperature": TEMPERATURE,
        "dnn_face_detector": _get_dnn_net() is not None,
    })


@app.post("/analyze-emotion")
def analyze_emotion():
    data = request.get_json(silent=True) or {}
    image_b64 = data.get("image", "")
    if not image_b64:
        return jsonify({"success": False, "message": "Thiếu field 'image' base64"}), 400

    try:
        img  = _decode_base64_image(image_b64)
        face = _detect_face_bgr(img)
        x    = _preprocess(face, _shape)

        probs   = _predict_tta(_model, x, _shape)
        idx     = int(np.argmax(probs))
        emotion = LABELS[idx]
        confidence = float(probs[idx])

        return jsonify({
            "emotion":       emotion,
            "confidence":    confidence,
            "probabilities": dict(zip(LABELS, [float(p) for p in probs])),
        })
    except Exception as e:
        return jsonify({"success": False, "message": f"Lỗi phân tích: {e}"}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
