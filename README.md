# Music App với AI Nhận Diện Cảm Xúc

Ứng dụng nghe nhạc Android với tính năng AI nhận diện cảm xúc khuôn mặt và đề xuất nhạc phù hợp.

## Cấu trúc dự án

```
.
├── app/
│   ├── backend/          # Spring Boot Backend API
│   └── mobile/           # Android App (Java)
├── AI/                   # Python AI Service (Flask)
└── database/             # SQL Schema
```

## Yêu cầu hệ thống

### Backend
- Java 11+
- Maven 3.6+
- MySQL 8.0+ hoặc PostgreSQL

### Mobile App
- Android Studio
- Android SDK 24+ (Android 7.0+)
- Java 8+

### AI Service
- Python 3.8+
- pip

## Cài đặt và chạy

### 1. Database

Tạo database và chạy script SQL:
```bash
mysql -u root -p
CREATE DATABASE music_app_db;
USE music_app_db;
SOURCE database/schema.sql;
```

### 2. Backend

```bash
cd app/backend
# Cấu hình database trong src/main/resources/application.properties
mvn clean install
mvn spring-boot:run
```

Backend sẽ chạy tại `http://localhost:8080`

### 3. AI Service

```bash
cd AI
python -m venv venv
source venv/bin/activate  # Linux/Mac
# hoặc venv\Scripts\activate  # Windows
pip install -r requirements.txt
python app.py
```

AI Service sẽ chạy tại `http://localhost:5000`

### 4. Mobile App

1. Mở Android Studio
2. Import project từ thư mục `app/mobile`
3. Cấu hình BASE_URL trong `RetrofitClient.java` (đổi `10.0.2.2` thành IP máy tính nếu dùng thiết bị thật)
4. Build và chạy trên emulator hoặc thiết bị thật

## Tính năng

### Người dùng
- ✅ Đăng ký / Đăng nhập
- ✅ Chỉnh sửa thông tin cá nhân
- ✅ Lưu lịch sử nghe nhạc
- ✅ Lưu danh sách yêu thích

### Nghe nhạc
- ✅ Hiển thị danh sách bài hát
- ✅ Phát / Tạm dừng / Chuyển bài
- ✅ Điều chỉnh âm lượng
- ✅ Nghe theo thể loại, tâm trạng

### AI Nhận diện cảm xúc
- ✅ Chụp ảnh khuôn mặt
- ✅ Phân tích cảm xúc (Vui, Buồn, Tức giận, Thư giãn, Căng thẳng, Bình thường)
- ✅ Đề xuất nhạc theo cảm xúc

### Playlist
- ✅ Tạo playlist cá nhân
- ✅ Thêm / Xóa bài hát
- ✅ Phát nhạc theo playlist

### Tìm kiếm
- ✅ Tìm theo tên bài hát, ca sĩ, thể loại
- ✅ Gợi ý kết quả

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập

### Songs
- `GET /api/songs` - Danh sách bài hát
- `GET /api/songs/{id}` - Chi tiết bài hát
- `GET /api/songs/search?keyword=...` - Tìm kiếm
- `GET /api/songs/genre/{genre}` - Theo thể loại
- `GET /api/songs/mood/{mood}` - Theo tâm trạng

### Emotion
- `POST /api/emotion/analyze?userId=...` - Phân tích cảm xúc
- `GET /api/emotion/history/{userId}` - Lịch sử cảm xúc

## Mapping Cảm xúc → Nhạc

- **Vui** → Pop, Dance, EDM
- **Buồn** → Ballad, Acoustic
- **Thư giãn** → Chill, Lofi
- **Căng thẳng** → Nhạc nhẹ, Piano
- **Tức giận** → Rock, Metal
- **Bình thường** → Tất cả thể loại

## Lưu ý

1. **Database**: Đảm bảo MySQL/PostgreSQL đang chạy và đã tạo database
2. **Network**: Mobile app cần kết nối được với backend (cấu hình IP đúng)
3. **Camera**: Cần cấp quyền camera cho app để sử dụng tính năng nhận diện cảm xúc
4. **AI Model**: Hiện tại AI service sử dụng rule-based approach. Cần train model CNN để có kết quả chính xác hơn.

## Phát triển tiếp

- [ ] Train và tích hợp model CNN cho nhận diện cảm xúc chính xác
- [ ] Tích hợp Spotify API / SoundCloud API
- [ ] Thêm tính năng streaming nhạc
- [ ] Cải thiện UI/UX
- [ ] Thêm unit tests
- [ ] Thêm JWT authentication middleware
- [ ] Thêm pagination cho API

## License

MIT
