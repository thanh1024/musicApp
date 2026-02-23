# API Documentation - Admin Endpoints

## Tổng Quan

Tất cả các API Admin yêu cầu:
- **Authentication**: JWT Token trong header `Authorization: Bearer <token>`
- **Authorization**: User phải có role `ROLE_ADMIN`

## Base URL
```
http://localhost:8080/api/admin
```

## Authentication

Tất cả requests phải gửi kèm JWT token:
```
Authorization: Bearer <your-jwt-token>
```

## User Management APIs

### 1. Lấy danh sách users

**GET** `/api/admin/users`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "fullName": "User One",
      "avatarUrl": "https://example.com/avatar.jpg",
      "role": "ROLE_USER",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "total": 1
}
```

### 2. Xem chi tiết user

**GET** `/api/admin/users/{id}`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com",
    "fullName": "User One",
    "avatarUrl": "https://example.com/avatar.jpg",
    "role": "ROLE_USER",
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### 3. Cập nhật user

**PATCH** `/api/admin/users/{id}`

**Request Body:**
```json
{
  "username": "newusername",
  "email": "newemail@example.com",
  "fullName": "New Full Name",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "password": "newpassword",
  "role": "ROLE_ADMIN"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Cập nhật user thành công",
  "data": {
    "id": 1,
    "username": "newusername",
    "email": "newemail@example.com",
    "fullName": "New Full Name",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "role": "ROLE_ADMIN",
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### 4. Khóa tài khoản user

**POST** `/api/admin/users/{id}/lock`

**Response:**
```json
{
  "success": true,
  "message": "Đã khóa tài khoản user",
  "data": {
    "id": 1,
    "username": "user1",
    "isActive": false
  }
}
```

### 5. Mở khóa tài khoản user

**POST** `/api/admin/users/{id}/unlock`

**Response:**
```json
{
  "success": true,
  "message": "Đã mở khóa tài khoản user",
  "data": {
    "id": 1,
    "username": "user1",
    "isActive": true
  }
}
```

### 6. Xóa user (soft delete)

**DELETE** `/api/admin/users/{id}`

**Response:**
```json
{
  "success": true,
  "message": "Đã xóa user thành công"
}
```

## Song Management APIs

### 1. Lấy danh sách songs

**GET** `/api/admin/songs`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Song Title",
      "artist": "Artist Name",
      "album": "Album Name",
      "genre": "Pop",
      "mood": "Happy",
      "duration": 240,
      "fileUrl": "https://example.com/songs/song.mp3",
      "thumbnailUrl": "https://example.com/thumbnails/song.jpg",
      "spotifyId": "spotify:track:abc123",
      "soundcloudId": "soundcloud:track:xyz789",
      "playCount": 0,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "total": 1
}
```

### 2. Xem chi tiết song

**GET** `/api/admin/songs/{id}`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Song Title",
    "artist": "Artist Name",
    "album": "Album Name",
    "genre": "Pop",
    "mood": "Happy",
    "duration": 240,
    "fileUrl": "https://example.com/songs/song.mp3",
    "thumbnailUrl": "https://example.com/thumbnails/song.jpg",
    "spotifyId": "spotify:track:abc123",
    "soundcloudId": "soundcloud:track:xyz789",
    "playCount": 0,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### 3. Tạo bài hát mới

**POST** `/api/admin/songs`

**Request Body:**
```json
{
  "title": "New Song",
  "artist": "New Artist",
  "album": "New Album",
  "genre": "Rock",
  "mood": "Energetic",
  "duration": 180,
  "fileUrl": "https://example.com/songs/newsong.mp3",
  "thumbnailUrl": "https://example.com/thumbnails/newsong.jpg",
  "spotifyId": "spotify:track:new123",
  "soundcloudId": "soundcloud:track:new456"
}
```

**Required Fields:**
- `title` (string)
- `artist` (string)
- `genre` (string)
- `fileUrl` (string)

**Response:**
```json
{
  "success": true,
  "message": "Tạo bài hát thành công",
  "data": {
    "id": 2,
    "title": "New Song",
    "artist": "New Artist",
    "album": "New Album",
    "genre": "Rock",
    "mood": "Energetic",
    "duration": 180,
    "fileUrl": "https://example.com/songs/newsong.mp3",
    "thumbnailUrl": "https://example.com/thumbnails/newsong.jpg",
    "spotifyId": "spotify:track:new123",
    "soundcloudId": "soundcloud:track:new456",
    "playCount": 0,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### 4. Cập nhật bài hát

**PATCH** `/api/admin/songs/{id}`

**Request Body:**
```json
{
  "title": "Updated Title",
  "artist": "Updated Artist",
  "genre": "Jazz",
  "mood": "Relaxed",
  "duration": 200
}
```

**Response:**
```json
{
  "success": true,
  "message": "Cập nhật bài hát thành công",
  "data": {
    "id": 1,
    "title": "Updated Title",
    "artist": "Updated Artist",
    "album": "Album Name",
    "genre": "Jazz",
    "mood": "Relaxed",
    "duration": 200,
    "fileUrl": "https://example.com/songs/song.mp3",
    "thumbnailUrl": "https://example.com/thumbnails/song.jpg",
    "spotifyId": "spotify:track:abc123",
    "soundcloudId": "soundcloud:track:xyz789",
    "playCount": 0,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### 5. Xóa bài hát

**DELETE** `/api/admin/songs/{id}`

**Response:**
```json
{
  "success": true,
  "message": "Đã xóa bài hát thành công"
}
```

## Error Responses

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Unauthorized"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied. Admin role required."
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Không tìm thấy user/song"
}
```

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation error message"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Lỗi khi xử lý: <error message>"
}
```

## Tạo Admin User

Để tạo user với role ADMIN, có thể:

1. **Sử dụng database trực tiếp:**
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE username = 'admin';
```

2. **Sử dụng API update user (nếu đã có admin khác):**
```bash
PATCH /api/admin/users/{id}
{
  "role": "ROLE_ADMIN"
}
```

3. **Tạo user mới với role ADMIN trong code:**
   - Sửa AuthController để cho phép set role khi register (chỉ trong dev)
   - Hoặc tạo script migration

## Testing với Postman/cURL

### Example: Lấy danh sách users
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Example: Tạo bài hát mới
```bash
curl -X POST http://localhost:8080/api/admin/songs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Song",
    "artist": "Test Artist",
    "genre": "Pop",
    "fileUrl": "https://example.com/test.mp3"
  }'
```
