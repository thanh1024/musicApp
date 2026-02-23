# Music App Backend

Backend API cho ứng dụng nghe nhạc với AI nhận diện cảm xúc.

## Yêu cầu

- Java 11+
- Maven 3.6+
- MySQL 8.0+ hoặc PostgreSQL

## Cài đặt

1. Tạo database:
```sql
CREATE DATABASE music_app_db;
```

2. Chạy file `database/schema.sql` để tạo các bảng

3. Cấu hình database trong `src/main/resources/application.properties`

4. Build và chạy:
```bash
mvn clean install
mvn spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký tài khoản
- `POST /api/auth/login` - Đăng nhập

### Songs
- `GET /api/songs` - Lấy danh sách tất cả bài hát
- `GET /api/songs/{id}` - Lấy thông tin bài hát theo ID
- `GET /api/songs/search?keyword=...` - Tìm kiếm bài hát
- `GET /api/songs/genre/{genre}` - Lấy bài hát theo thể loại
- `GET /api/songs/mood/{mood}` - Lấy bài hát theo tâm trạng

### Emotion
- `POST /api/emotion/analyze?userId=...` - Phân tích cảm xúc từ ảnh
- `GET /api/emotion/history/{userId}` - Lấy lịch sử cảm xúc

## Cấu hình

Chỉnh sửa `application.properties` để cấu hình:
- Database connection
- JWT secret key
- AI service URL
