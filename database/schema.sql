-- Database Schema cho ứng dụng nghe nhạc với AI nhận diện cảm xúc
-- Sử dụng MySQL/PostgreSQL

-- Bảng Users - Quản lý người dùng
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Bảng Songs - Quản lý bài hát
CREATE TABLE songs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    artist VARCHAR(100) NOT NULL,
    album VARCHAR(100),
    genre VARCHAR(50) NOT NULL,
    mood VARCHAR(50),
    duration INT NOT NULL COMMENT 'Thời lượng tính bằng giây',
    file_url VARCHAR(500) NOT NULL COMMENT 'URL file nhạc hoặc streaming URL',
    thumbnail_url VARCHAR(500),
    spotify_id VARCHAR(100),
    soundcloud_id VARCHAR(100),
    play_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_artist (artist),
    INDEX idx_genre (genre),
    INDEX idx_mood (mood)
);

-- Bảng Playlists - Quản lý playlist cá nhân
CREATE TABLE playlists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(500),
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);

-- Bảng Playlist_Songs - Quan hệ nhiều-nhiều giữa Playlist và Songs
CREATE TABLE playlist_songs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    playlist_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    position INT NOT NULL COMMENT 'Vị trí bài hát trong playlist',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    UNIQUE KEY unique_playlist_song (playlist_id, song_id),
    INDEX idx_playlist_id (playlist_id),
    INDEX idx_song_id (song_id)
);

-- Bảng Favorites - Danh sách nhạc yêu thích
CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_favorite (user_id, song_id),
    INDEX idx_user_id (user_id),
    INDEX idx_song_id (song_id)
);

-- Bảng Listening_History - Lịch sử nghe nhạc
CREATE TABLE listening_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    listened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    listen_duration INT COMMENT 'Thời gian nghe tính bằng giây',
    completed BOOLEAN DEFAULT FALSE COMMENT 'Đã nghe hết bài hay chưa',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_song_id (song_id),
    INDEX idx_listened_at (listened_at)
);

-- Bảng Emotion_Logs - Lưu log cảm xúc từ AI
CREATE TABLE emotion_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    emotion VARCHAR(50) NOT NULL COMMENT 'Vui, Buồn, Tức giận, Thư giãn, Căng thẳng, Bình thường',
    confidence DECIMAL(5,4) COMMENT 'Độ tin cậy của kết quả (0-1)',
    image_url VARCHAR(500) COMMENT 'URL ảnh khuôn mặt đã phân tích',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_emotion (emotion),
    INDEX idx_created_at (created_at)
);

-- Bảng Recommended_Songs - Lưu danh sách nhạc được đề xuất theo cảm xúc
CREATE TABLE recommended_songs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    emotion_log_id BIGINT,
    song_id BIGINT NOT NULL,
    recommended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_played BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (emotion_log_id) REFERENCES emotion_logs(id) ON DELETE SET NULL,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_emotion_log_id (emotion_log_id),
    INDEX idx_song_id (song_id)
);

-- Bảng JWT_Tokens - Quản lý token đăng nhập (optional, có thể dùng Redis)
CREATE TABLE jwt_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);

-- Insert dữ liệu mẫu cho testing
-- Mật khẩu: password123 (đã hash bằng BCrypt)
INSERT INTO users (username, email, password, full_name) VALUES
('testuser', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User');

-- Insert một số bài hát mẫu
INSERT INTO songs (title, artist, album, genre, mood, duration, file_url, thumbnail_url) VALUES
('Happy Song', 'Pop Artist', 'Happy Album', 'Pop', 'Vui', 180, 'https://example.com/songs/happy.mp3', 'https://example.com/thumbnails/happy.jpg'),
('Sad Ballad', 'Ballad Singer', 'Emotional Album', 'Ballad', 'Buồn', 240, 'https://example.com/songs/sad.mp3', 'https://example.com/thumbnails/sad.jpg'),
('Chill Vibes', 'Chill Artist', 'Relax Album', 'Chill', 'Thư giãn', 200, 'https://example.com/songs/chill.mp3', 'https://example.com/thumbnails/chill.jpg'),
('Rock Anthem', 'Rock Band', 'Rock Album', 'Rock', 'Bình thường', 220, 'https://example.com/songs/rock.mp3', 'https://example.com/thumbnails/rock.jpg'),
('EDM Party', 'EDM DJ', 'Party Album', 'EDM', 'Vui', 190, 'https://example.com/songs/edm.mp3', 'https://example.com/thumbnails/edm.jpg');
