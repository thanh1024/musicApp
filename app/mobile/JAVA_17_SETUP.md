# Hướng dẫn cấu hình Java 17 cho Android Studio

## Vấn đề
Android Studio đang dùng Java 21, gây lỗi jlink khi build với AGP 8.1.4.

## Giải pháp: Cấu hình Java 17

### Bước 1: Tải và cài Java 17
1. Tải Java 17 từ: https://adoptium.net/temurin/releases/?version=17
2. Chọn Windows x64, JDK 17
3. Cài đặt (ví dụ: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`)

### Bước 2: Cấu hình trong Android Studio
1. **File → Settings** (hoặc **File → Project Structure**)
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. Trong phần **Gradle JDK**, chọn:
   - **Download JDK...** → Chọn version 17
   - Hoặc chọn **jbr-17** nếu có
   - Hoặc chọn đường dẫn đến Java 17 đã cài

### Bước 3: Cấu hình Project Structure
1. **File → Project Structure**
2. **SDK Location** tab
3. **JDK location**: Chọn đường dẫn đến Java 17
   - Ví dụ: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`

### Bước 4: Sync và Build
1. **File → Sync Project with Gradle Files**
2. **Build → Clean Project**
3. **Build → Rebuild Project**

## Nếu không muốn cài Java 17
Có thể thử downgrade AGP xuống 7.4.2, nhưng cần downgrade Gradle xuống 7.5 (không hỗ trợ Java 21).
