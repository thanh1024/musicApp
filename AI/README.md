# AI Emotion Recognition Service

Service AI để nhận diện cảm xúc từ ảnh khuôn mặt sử dụng Python, OpenCV và TensorFlow.

## Yêu cầu

- Python 3.8+
- pip

## Cài đặt

1. Tạo virtual environment (khuyến nghị):
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# hoặc
venv\Scripts\activate  # Windows
```

2. Cài đặt dependencies:
```bash
pip install -r requirements.txt
```

## Chạy service

```bash
python app.py
```

Service sẽ chạy tại `http://localhost:5000`

## API Endpoints

### POST /analyze-emotion
Phân tích cảm xúc từ ảnh base64

**Request:**
```json
{
    "image": "base64_encoded_image_string"
}
```

**Response:**
```json
{
    "success": true,
    "emotion": "Vui",
    "confidence": 0.85
}
```

### GET /health
Kiểm tra trạng thái service

## Model Training

Để train model nhận diện cảm xúc:

1. Chuẩn bị dataset (ví dụ: FER2013, CK+, JAFFE)
2. Train model CNN sử dụng TensorFlow/Keras
3. Lưu model vào file `emotion_model.h5`
4. Service sẽ tự động load model khi khởi động

## Lưu ý

- Hiện tại service sử dụng rule-based approach nếu không có model
- Cần train và cung cấp model file `emotion_model.h5` để có kết quả chính xác
- Model input size mặc định: 48x48 pixels (grayscale)
