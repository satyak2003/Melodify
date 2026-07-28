# Melodify — Android & Desktop Music Player

<img width="256" height="256" alt="melodify" src="https://github.com/user-attachments/assets/d1f06f62-3389-4b58-acc0-964a34790485" />

An ad-free, open-source music player that imports your Spotify playlists and streams audio at the highest available quality via YouTube Music.

## Features
- 🎵 Stream music from YouTube Music (highest quality — up to 256kbps Opus)
- 📋 Import your Spotify playlists with one click
- 🎵 FLAC playback for locally imported music files
- 📱 Android app with background playback & lock screen controls
- 🖥️ Desktop app (Windows/Mac/Linux) with system tray
- 🎮 Discord Rich Presence (Desktop)
- 🎤 Synced lyrics
- 📁 Local music library (FLAC, MP3, AAC, OGG, WAV)

## Tech Stack
- **Language**: Kotlin Multiplatform (shared codebase)
- **UI**: Compose Multiplatform (Material 3)
- **Android Audio**: Media3 ExoPlayer + MediaSession
- **Desktop Audio**: vlcj (requires VLC installed)
- **Music Source**: YouTube InnerTube API
- **Spotify**: Spotify Web API (OAuth2 PKCE)
- **Database**: SQLDelight
- **DI**: Koin
- **Networking**: Ktor

## Setup

### Prerequisites
- JDK 17+ (JDK 25 works)
- Android Studio (for Android builds)
- Android SDK (API 24+)
- VLC Media Player (for Desktop audio)

### Android Setup
1. Install Android Studio and Android SDK
2. Update `local.properties` with your SDK path:
   ```
   sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```
3. Connect your Android device (USB debugging enabled) or start an emulator
4. Run: `./gradlew :androidApp:installDebug`

### Desktop Setup
1. Install VLC Media Player: https://www.videolan.org/vlc/
2. Run: `./gradlew :desktopApp:run`

### Spotify Integration
1. Go to https://developer.spotify.com/dashboard
2. Open your Melodify app settings
3. Add Redirect URI: `melodify://callback`
4. Copy your **Client ID** — you'll enter it in Melodify's Settings screen

## Build Commands

```bash
# Run desktop app
./gradlew :desktopApp:run

# Build Android debug APK
./gradlew :androidApp:assembleDebug

# Run all tests
./gradlew :shared:test

# Build desktop installer
./gradlew :desktopApp:packageMsi  # Windows
./gradlew :desktopApp:packageDmg  # macOS
./gradlew :desktopApp:packageDeb  # Linux
```

## Project Structure

```
MusicPlayer/
├── shared/          # KMP shared module (business logic, APIs, ViewModels)
├── androidApp/      # Android app (Compose UI, ExoPlayer, MediaSession)
└── desktopApp/      # Desktop app (Compose for Desktop, vlcj, System Tray)
```

## License
MIT — Personal use only. Not affiliated with Spotify or YouTube.
