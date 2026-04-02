-- Seed data for local dev (H2)
-- Password for both users below: password123
-- BCrypt hash generated for "password123"

INSERT INTO users (username, email, password, full_name, role, is_active, deleted)
VALUES
('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'ROLE_ADMIN', TRUE, FALSE),
('testuser', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User', 'ROLE_USER', TRUE, FALSE);

INSERT INTO songs (title, album, mood, duration, file_url, thumbnail_url, play_count)
VALUES
('Happy Song', 'Happy Album', 'Vui', 180, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'https://picsum.photos/seed/song1/300/300', 0),
('Sad Ballad', 'Emotional Album', 'Buồn', 240, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'https://picsum.photos/seed/song2/300/300', 0),
('Chill Vibes', 'Relax Album', 'Thư giãn', 200, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'https://picsum.photos/seed/song3/300/300', 0),
('Rock Anthem', 'Rock Album', 'Bình thường', 220, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'https://picsum.photos/seed/song4/300/300', 0),
('EDM Party', 'Party Album', 'Vui', 190, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'https://picsum.photos/seed/song5/300/300', 0);

INSERT INTO artists (name) VALUES
('Pop Artist'), ('Ballad Singer'), ('Chill Artist'), ('Rock Band'), ('EDM DJ');

INSERT INTO genres (name) VALUES
('Pop'), ('Ballad'), ('Chill'), ('Rock'), ('EDM');

INSERT INTO song_artists (song_id, artist_id) VALUES
(1,1), (2,2), (3,3), (4,4), (5,5);

INSERT INTO song_genres (song_id, genre_id) VALUES
(1,1), (2,2), (3,3), (4,4), (5,5);

