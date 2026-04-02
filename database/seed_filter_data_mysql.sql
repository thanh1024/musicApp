-- Run ONCE on MySQL to create sample data + links for filters (artists/genres/join tables)
-- Safe-ish rerun: deletes only rows with the sample titles/names before inserting.

-- Songs (delete by title to avoid duplicates)
DELETE FROM songs WHERE title IN ('Happy Song','Sad Ballad','Chill Vibes','Rock Anthem','EDM Party','Piano Dreams','Hip Hop Heat','Pop Classic','Chill Night','Rock Legends');

INSERT INTO songs (title, album, mood, duration, file_url, thumbnail_url, play_count)
VALUES
('Happy Song', 'Happy Album', 'Vui', 180, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'https://picsum.photos/seed/song1/300/300', 0),
('Sad Ballad', 'Emotional Album', 'Buồn', 240, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'https://picsum.photos/seed/song2/300/300', 0),
('Chill Vibes', 'Relax Album', 'Thư giãn', 200, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'https://picsum.photos/seed/song3/300/300', 0),
('Rock Anthem', 'Rock Album', 'Bình thường', 220, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'https://picsum.photos/seed/song4/300/300', 0),
('EDM Party', 'Party Album', 'Vui', 190, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'https://picsum.photos/seed/song5/300/300', 0),
('Piano Dreams', 'Piano Album', 'Bình thường', 210, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'https://picsum.photos/seed/song6/300/300', 0),
('Hip Hop Heat', 'HipHop Album', 'Vui', 205, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'https://picsum.photos/seed/song7/300/300', 0),
('Pop Classic', 'Pop Album', 'Vui', 175, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'https://picsum.photos/seed/song8/300/300', 0),
('Chill Night', 'Chill Album', 'Thư giãn', 195, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'https://picsum.photos/seed/song9/300/300', 0),
('Rock Legends', 'Rock Album', 'Bình thường', 230, 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'https://picsum.photos/seed/song10/300/300', 0);

-- Artists / Genres (delete by name then insert)
DELETE FROM artists WHERE name IN ('Pop Artist','Ballad Singer','Chill Artist','Rock Band','EDM DJ','Piano Artist','HipHop Artist');
DELETE FROM genres WHERE name IN ('Pop','Ballad','Chill','Rock','EDM','Piano','Hip Hop');

INSERT INTO artists (name) VALUES
('Pop Artist'), ('Ballad Singer'), ('Chill Artist'), ('Rock Band'), ('EDM DJ'), ('Piano Artist'), ('HipHop Artist');

INSERT INTO genres (name) VALUES
('Pop'), ('Ballad'), ('Chill'), ('Rock'), ('EDM'), ('Piano'), ('Hip Hop');

-- Rebuild join links for the sample songs
DELETE sa FROM song_artists sa JOIN songs s ON s.id = sa.song_id WHERE s.title IN
('Happy Song','Sad Ballad','Chill Vibes','Rock Anthem','EDM Party','Piano Dreams','Hip Hop Heat','Pop Classic','Chill Night','Rock Legends');
DELETE sg FROM song_genres sg JOIN songs s ON s.id = sg.song_id WHERE s.title IN
('Happy Song','Sad Ballad','Chill Vibes','Rock Anthem','EDM Party','Piano Dreams','Hip Hop Heat','Pop Classic','Chill Night','Rock Legends');

-- Link by lookup (no hard-coded IDs)
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='Pop Artist' WHERE s.title IN ('Happy Song','Pop Classic');
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='Ballad Singer' WHERE s.title='Sad Ballad';
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='Chill Artist' WHERE s.title IN ('Chill Vibes','Chill Night');
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='Rock Band' WHERE s.title IN ('Rock Anthem','Rock Legends');
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='EDM DJ' WHERE s.title='EDM Party';
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='Piano Artist' WHERE s.title='Piano Dreams';
INSERT INTO song_artists(song_id, artist_id)
SELECT s.id, a.id FROM songs s JOIN artists a ON a.name='HipHop Artist' WHERE s.title='Hip Hop Heat';

INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Pop' WHERE s.title IN ('Happy Song','Pop Classic');
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Ballad' WHERE s.title='Sad Ballad';
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Chill' WHERE s.title IN ('Chill Vibes','Chill Night');
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Rock' WHERE s.title IN ('Rock Anthem','Rock Legends');
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='EDM' WHERE s.title='EDM Party';
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Piano' WHERE s.title='Piano Dreams';
INSERT INTO song_genres(song_id, genre_id)
SELECT s.id, g.id FROM songs s JOIN genres g ON g.name='Hip Hop' WHERE s.title='Hip Hop Heat';

