# Kiến Trúc Nghe Nhạc An Toàn - Hướng Dẫn Học Tập

## Tổng Quan

Tài liệu này giải thích kiến trúc nghe nhạc an toàn và hợp pháp cho mục đích học tập, không vi phạm bản quyền.

## Nguyên Tắc Cơ Bản

### ❌ KHÔNG ĐƯỢC LÀM

1. **KHÔNG stream trực tiếp từ Spotify/Apple Music/YouTube**
   - Vi phạm Terms of Service của các platform này
   - Vi phạm bản quyền
   - Có thể bị khóa API key hoặc tài khoản

2. **KHÔNG download nhạc từ các nguồn không có bản quyền**
   - Vi phạm luật bản quyền
   - Rủi ro pháp lý

### ✅ ĐƯỢC PHÉP LÀM

1. **Sử dụng Spotify API để lấy metadata**
   - Lấy thông tin bài hát (title, artist, album, artwork)
   - Lấy preview 30 giây (nếu có)
   - Không stream full track

2. **Phát nhạc từ nguồn hợp pháp**
   - File MP3 local (tự tạo hoặc có bản quyền)
   - Public URL từ nguồn hợp pháp (Free Music Archive, Jamendo, etc.)
   - Demo tracks từ các artist cho phép

## Kiến Trúc Hệ Thống

### 1. Backend API

```
GET /api/songs
Response: {
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
      "fileUrl": "https://example.com/songs/song.mp3",  // Public URL hoặc local path
      "thumbnailUrl": "https://example.com/thumbnails/song.jpg",
      "spotifyId": "spotify:track:abc123",  // Chỉ để lấy metadata
      "playCount": 0
    }
  ]
}
```

### 2. Android Client - ExoPlayer

#### Cài Đặt ExoPlayer

Thêm vào `build.gradle`:

```gradle
dependencies {
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
    implementation 'com.google.android.exoplayer:exoplayer-ui:2.19.1'
}
```

#### Sử Dụng ExoPlayer

```java
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

public class MusicPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        playerView = findViewById(R.id.playerView);
        
        // Khởi tạo ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Lấy URL từ API response
        String audioUrl = getIntent().getStringExtra("AUDIO_URL");
        
        // Tạo MediaItem từ URL
        MediaItem mediaItem = MediaItem.fromUri(audioUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}
```

### 3. Tích Hợp Spotify API (Chỉ Metadata)

#### Lấy Metadata từ Spotify

```java
// Sử dụng Spotify Web API để lấy thông tin bài hát
// KHÔNG stream audio từ Spotify

public class SpotifyMetadataService {
    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";
    
    // Lấy thông tin track
    public void getTrackMetadata(String spotifyId, String accessToken) {
        // GET https://api.spotify.com/v1/tracks/{id}
        // Response: title, artist, album, artwork, preview_url (30s)
    }
    
    // Lấy preview 30 giây (nếu có)
    public void getPreviewUrl(String spotifyId) {
        // Preview URL có thể phát bằng ExoPlayer
        // Nhưng chỉ là 30 giây, không phải full track
    }
}
```

### 4. Quy Trình Hoạt Động

```
1. User chọn bài hát
   ↓
2. App gọi API: GET /api/songs/{id}
   ↓
3. Backend trả về thông tin bài hát (bao gồm fileUrl)
   ↓
4. App sử dụng ExoPlayer để phát audio từ fileUrl
   ↓
5. Nếu có spotifyId, có thể lấy thêm metadata từ Spotify API
   (nhưng KHÔNG stream từ Spotify)
```

## Các Nguồn Nhạc Hợp Pháp

### 1. Free Music Archive (FMA)
- URL: https://freemusicarchive.org/
- Cung cấp nhạc miễn phí với license rõ ràng
- Có API để lấy metadata

### 2. Jamendo
- URL: https://www.jamendo.com/
- Nền tảng nhạc độc lập
- Có API và nhiều track miễn phí

### 3. Internet Archive
- URL: https://archive.org/details/audio
- Kho lưu trữ nhạc công cộng
- Nhiều bản ghi cũ đã hết bản quyền

### 4. Tự Tạo Demo Tracks
- Tạo file MP3 demo ngắn
- Upload lên server hoặc sử dụng CDN
- Sử dụng trong development/testing

## Best Practices

### 1. Caching
- Cache metadata từ Spotify API
- Cache artwork/images
- Không cache audio files (trừ khi có permission)

### 2. Error Handling
```java
player.addListener(new Player.Listener() {
    @Override
    public void onPlayerError(PlaybackException error) {
        // Xử lý lỗi phát nhạc
        Log.e("ExoPlayer", "Playback error: " + error.getMessage());
    }
});
```

### 3. Background Playback
- Sử dụng MediaSession
- Notification với controls
- Foreground service để phát nhạc nền

### 4. Offline Support
- Chỉ cache metadata, không cache audio
- Hoặc chỉ cache với permission rõ ràng

## Lưu Ý Pháp Lý

1. **Luôn kiểm tra license** của audio files
2. **Không stream** từ các service có bản quyền
3. **Chỉ sử dụng** nguồn công khai hoặc có permission
4. **Ghi rõ nguồn** trong app (attribution)
5. **Tuân thủ** Terms of Service của các API sử dụng

## Kết Luận

Kiến trúc này đảm bảo:
- ✅ Tuân thủ pháp luật về bản quyền
- ✅ An toàn cho mục đích học tập
- ✅ Có thể mở rộng và maintain
- ✅ Sử dụng công nghệ hiện đại (ExoPlayer)

**Nhớ**: Mục đích là học tập, không phải tạo một Spotify clone vi phạm bản quyền!
