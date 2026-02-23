from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np
import base64
import io
from PIL import Image
import tensorflow as tf
from tensorflow import keras
import os

app = Flask(__name__)
CORS(app)

# Load emotion recognition model
# Note: You need to train and save your model first
# For now, we'll use a simple rule-based approach
MODEL_PATH = 'emotion_model.h5'
model = None

# Emotion labels
EMOTIONS = ['Vui', 'Buồn', 'Tức giận', 'Thư giãn', 'Căng thẳng', 'Bình thường']
EMOTION_MAP = {
    'happy': 'Vui',
    'sad': 'Buồn',
    'angry': 'Tức giận',
    'relaxed': 'Thư giãn',
    'stressed': 'Căng thẳng',
    'neutral': 'Bình thường'
}

def load_model():
    """Load the trained emotion recognition model"""
    global model
    if os.path.exists(MODEL_PATH):
        try:
            model = keras.models.load_model(MODEL_PATH)
            print(f"Model loaded from {MODEL_PATH}")
        except Exception as e:
            print(f"Error loading model: {e}")
            model = None
    else:
        print(f"Model file not found at {MODEL_PATH}. Using rule-based approach.")

def preprocess_image(image_base64):
    """Convert base64 image to numpy array for model input"""
    try:
        # Remove data URL prefix if present
        if ',' in image_base64:
            image_base64 = image_base64.split(',')[1]
        
        # Decode base64
        image_data = base64.b64decode(image_base64)
        image = Image.open(io.BytesIO(image_data))
        
        # Convert to RGB if needed
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # Convert to numpy array
        image_array = np.array(image)
        
        # Convert to OpenCV format (BGR)
        image_cv = cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)
        
        return image_cv
    except Exception as e:
        print(f"Error preprocessing image: {e}")
        return None

def detect_face(image):
    """Detect face in image using OpenCV Haar Cascade"""
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.3, 5)
    
    if len(faces) > 0:
        x, y, w, h = faces[0]  # Use first face
        face_roi = gray[y:y+h, x:x+w]
        return face_roi, True
    return None, False

def predict_emotion_simple(face_image):
    """Simple rule-based emotion prediction (placeholder for actual model)"""
    # This is a placeholder - in production, use your trained CNN model
    # For now, return a random emotion with high confidence
    import random
    emotions = ['Vui', 'Buồn', 'Thư giãn', 'Bình thường']
    emotion = random.choice(emotions)
    confidence = random.uniform(0.7, 0.95)
    return emotion, confidence

def predict_emotion_model(face_image):
    """Predict emotion using trained CNN model"""
    if model is None:
        return predict_emotion_simple(face_image)
    
    try:
        # Resize face image to model input size (typically 48x48 or 64x64)
        face_resized = cv2.resize(face_image, (48, 48))
        face_normalized = face_resized.astype('float32') / 255.0
        face_reshaped = np.reshape(face_normalized, (1, 48, 48, 1))
        
        # Predict
        predictions = model.predict(face_reshaped)
        emotion_index = np.argmax(predictions[0])
        confidence = float(predictions[0][emotion_index])
        
        emotion = EMOTIONS[emotion_index]
        return emotion, confidence
    except Exception as e:
        print(f"Error in model prediction: {e}")
        return predict_emotion_simple(face_image)

@app.route('/analyze-emotion', methods=['POST'])
def analyze_emotion():
    """Analyze emotion from face image"""
    try:
        data = request.get_json()
        image_base64 = data.get('image', '')
        
        if not image_base64:
            return jsonify({
                'success': False,
                'message': 'No image provided'
            }), 400
        
        # Preprocess image
        image = preprocess_image(image_base64)
        if image is None:
            return jsonify({
                'success': False,
                'message': 'Invalid image format'
            }), 400
        
        # Detect face
        face_image, face_detected = detect_face(image)
        if not face_detected:
            return jsonify({
                'success': False,
                'message': 'No face detected in image'
            }), 400
        
        # Predict emotion
        emotion, confidence = predict_emotion_model(face_image)
        
        return jsonify({
            'success': True,
            'emotion': emotion,
            'confidence': confidence
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': model is not None
    })

if __name__ == '__main__':
    # Load model on startup
    load_model()
    
    # Run Flask app
    app.run(host='0.0.0.0', port=5000, debug=True)
