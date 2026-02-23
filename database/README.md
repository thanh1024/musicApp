# Database Schema

File SQL để tạo các bảng cho ứng dụng nghe nhạc.

## Cài đặt

1. Tạo database:
```sql
CREATE DATABASE music_app_db;
```

2. Chạy script:
```bash
mysql -u root -p music_app_db < schema.sql
```

Hoặc trong MySQL:
```sql
USE music_app_db;
SOURCE schema.sql;
```

## Cấu trúc bảng

- **users**: Thông tin người dùng
- **songs**: Danh sách bài hát
- **playlists**: Playlist cá nhân
- **playlist_songs**: Quan hệ nhiều-nhiều giữa playlist và bài hát
- **favorites**: Danh sách yêu thích
- **listening_history**: Lịch sử nghe nhạc
- **emotion_logs**: Log cảm xúc từ AI
- **recommended_songs**: Danh sách nhạc đề xuất theo cảm xúc
- **jwt_tokens**: Token đăng nhập (optional)

## Dữ liệu mẫu

Script đã bao gồm một số dữ liệu mẫu để test:
- 1 user test (username: testuser, password: password123)
- 5 bài hát mẫu với các thể loại và tâm trạng khác nhau
