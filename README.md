# Onda

[![Build & Release APK](https://github.com/mcello23/onda/actions/workflows/build.yml/badge.svg)](https://github.com/mcello23/onda/actions/workflows/build.yml)
[![E2E Tests (Mobilewright)](https://github.com/mcello23/onda/actions/workflows/e2e.yml/badge.svg)](https://github.com/mcello23/onda/actions/workflows/e2e.yml)
[![Latest Release](https://img.shields.io/github/v/release/mcello23/onda)](https://github.com/mcello23/onda/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9335)-3ddc84.svg)](https://developer.android.com/)

An ad-free, open-source Android radio and podcast player built with Kotlin, Jetpack Compose, and Media3 — with full **Android Auto** support and **Material You** home screen widgets. No accounts, no tracking, no aggregator pre-rolls.

---

## Why Onda?

Most mainstream radio apps wrap public broadcaster streams in pre-roll audio ads, banners, and subscription nags. Onda plays each broadcaster's direct stream via [radio-browser.info](https://www.radio-browser.info/) and fetches podcasts directly via iTunes Search + RSS.

| Ad Type                              | Example                                             | Removed in Onda? |
| ------------------------------------ | --------------------------------------------------- | ---------------- |
| **Aggregator ads**                   | TuneIn pre-rolls, banners, video ads, premium nags  | **Yes**          |
| **Station's own on-air commercials** | Standard FM ad breaks inside the broadcast audio    | No               |
| **Server-side inserted digital ads** | Triton / AdsWizz breaks mixed into the audio stream | No               |

---

## Features

### Live Radio & Curated Catalog

- **64 curated stations** bundled in `app/src/main/assets/stations.json` across Spain (15), USA (16), UK (16), Brazil (12), Italy (2), France (2), and Germany (1), with verified streams and high-res logos.
- **Global discovery**: Browse by world genres or search ~50,000 stations across 190+ countries via the open `radio-browser.info` API.
- **Custom stations & backups**: Add custom stations by stream URL, reorder favorites, and export/import your library and settings as JSON.
- **Self-healing favorites**: Saved stations refresh their stream URLs from `radio-browser.info` on startup so rotated edge servers do not break your list.
- **ICY track history**: Captures live song metadata (`IcyInfo`) with one-tap lookups in Spotify and YouTube Music.

### Material You UI (v1.2.0)

- **Five-tab navigation**:
  - **Home** — Collapsible favorites strip, recently played, continue listening podcasts, 2-row Top Stations by country, and World Rock.
  - **Favorites** — Dedicated favorites view with quick playback and reordering.
  - **Explore** — World genre cards, searchable 190+ country picker, and live station search.
  - **Podcasts** — Discover, Following, and Offline sub-tabs.
  - **Settings** — Theme, color palette, audio quality, buffer profile, equalizer, backup/restore, and language.
- **Dynamic styling**: Android 12+ Material You dynamic wallpaper color plus 4 built-in palettes (*Ocean*, *Terracotta*, *Emerald*, *Violet*) and *System / Light / Dark* modes.
- **Player surfaces**: Floating mini-player and full-screen tonal-gradient Now Playing sheet.
- **Bilingual UI**: Instant in-app switching between **Portuguese (PT-BR)** and **English (EN)**.

### Podcasts (Streaming & Offline)

- Search shows via the **iTunes Search API** and parse feeds with a native RSS parser.
- Subscribe to shows, resume with saved playback progress (*Continue Listening*), seek, skip `-15s / +30s`, adjust playback speed (`0.75x`–`2.0x`), and download episodes for offline playback.

### Playback, Audio & Commute Resilience

- **Media3 ExoPlayer + `MediaLibraryService`**: Supports MP3, AAC/AAC+, Ogg/Opus, and HLS (`.m3u8`), with media notification, lock-screen controls, Bluetooth/headset buttons, audio focus handling, and automatic pause on headphone disconnect.
- **Tunnel & dead-zone resilience**: Keeps retrying in `BUFFERING` during signal drops while keeping the foreground service alive, reconnects to live after pause/stop instead of replaying a stale buffer, and fails fast on permanently dead streams (404 / geo-blocked).
- **Audio controls**:
  - **Stream quality**: *Auto HD* (high bitrate on Wi-Fi, AAC+ on cellular), *High (320k)*, or *Data Saver*, with automatic stream-variant fallback.
  - **Buffer profiles**: *Fast*, *Balanced*, or *Stable* (anti-stutter profile for transit and highways).
  - **Audio FX**: Volume normalization (`LoudnessEnhancer`) and a 5-band Equalizer with presets (*Flat*, *Rock*, *Pop*, *Jazz*, *Vocal*, *Bass Boost*, *Custom*).
  - **Sleep Timer** with fade-out and end-of-episode option for podcasts.

### Android Auto

- Built on official **Media3 `MediaLibraryService`** APIs.
- **4 top-level tabs** with vector icons (*Favorites*, *Explore*, *Podcasts*, *Recent*) — avoids the overflow *"More"* menu.
- Large logo grids (`CONTENT_STYLE_GRID`) for stations, generated category artwork for countries and genres via `ArtworkProvider`, native podcast progress bars (`COMPLETION_PERCENTAGE`), and `Offline` badges.
- Voice commands (*"Hey Google, play Cadena SER on Onda"*) and custom Now Playing actions (**Favorite ♥**, **Reconnect Live** for radio, **`-15s / +30s`** and **Playback Speed** for podcasts).

### Home Screen Widgets

- **Onda · Compact (4×1)**: Current station/episode artwork, live title, and `⏮ ⏯ ⏭` controls.
- **Onda · Favorites (4×2 / 4×3)**: Compact header with a scrollable 4-column `GridView` of all favorite station logos and an active playback indicator.
- Both widgets adapt to Material You dynamic colors on Android 12+.

---

## Install & Updates

Onda is distributed as a signed APK directly from GitHub — no Play Store required.

1. Download the latest APK from **[GitHub Releases](https://github.com/mcello23/onda/releases)** (or grab the `onda-apk` artifact from any run of the **Build & Release APK** workflow).
2. Install the APK on Android 8.0+ (API 26+).
3. **Optional automatic updates**: Point **[Obtainium](https://github.com/ImranR98/Obtainium)** at `https://github.com/mcello23/onda` (public repository, no token needed).

### Enabling Onda in Android Auto

Android Auto hides sideloaded media apps by default. To make Onda appear in your car:

1. On your phone, open **Settings → Connected devices → Connection preferences → Android Auto**.
2. Tap **Version** about 10 times to unlock Developer settings.
3. Tap **⋮ → Developer settings** and enable **Unknown sources**.
4. Reconnect to your car. If Onda is still hidden, enable it under **Android Auto → Customize launcher**.

*(Repeat these steps if you ever clear Android Auto's app data.)*

---

## Tech Stack

- **Language & Build**: Kotlin `2.1.0`, Android Gradle Plugin `8.7.3`, Gradle `8.11.1`, Java/JVM `17`
- **Android SDK**: `minSdk 26` (Android 8.0), `compileSdk 35` / `targetSdk 35`
- **UI**: Jetpack Compose (BOM `2024.12.01`, Material 3), `activity-compose`, `lifecycle-viewmodel-compose`
- **Playback**: AndroidX Media3 `1.5.1` (`media3-exoplayer`, `media3-exoplayer-hls`, `media3-session`)
- **Networking & Images**: OkHttp `4.12.0`, Coil `2.7.0` (`coil-compose`, `coil-svg` + `androidsvg`), `kotlinx.serialization`, `kotlinx.coroutines`
- **Persistence**: Lightweight JSON storage in `SharedPreferences` (no Room or DI framework)
- **Testing**: JUnit 4 + Robolectric `4.14.1` + `kotlinx-coroutines-test` (JVM unit tests); [Mobilewright](https://www.npmjs.com/package/mobilewright) + TypeScript (Android emulator E2E tests)

---

## Project Structure

```text
├── .github/workflows/
│   ├── build.yml                       # Unit tests, release APK build/verify, GitHub Releases
│   └── e2e.yml                         # Android emulator Mobilewright E2E workflow
├── app/
│   ├── build.gradle.kts                # App config, dependencies, release signing setup
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # MediaLibraryService, ArtworkProvider, widgets, Auto metadata
│       │   ├── assets/stations.json    # 64 curated stations
│       │   ├── res/                    # Vector drawables, widget layouts, Material You colors/themes
│       │   └── java/io/github/mcello23/onda/
│       │       ├── MainActivity.kt     # Compose host activity
│       │       ├── OndaApp.kt          # Application initialization (Coil, stores)
│       │       ├── data/               # Station, StationRepo, RadioBrowserApi, PodcastApi/Repo, AppSettings, Monogram
│       │       ├── playback/           # PlaybackService (ExoPlayer + MediaSession + Android Auto), ArtworkProvider, AudioFxManager, SleepTimerState
│       │       ├── ui/                 # AppScreen, HomeTab, FavoritesTab, ExploreTab, PodcastsTab, SettingsTab, PlayerSheet, MainViewModel, Theme, Strings
│       │       └── widget/             # OndaWidgetProvider, OndaCompactWidgetProvider, OndaWidgetGridService, WidgetUpdater
│       └── test/java/io/github/mcello23/onda/
│           ├── data/                   # Station, StationRepo, AppSettingsStore, Podcast, Monogram unit tests
│           └── ui/                     # Localization & playback status unit tests
├── e2e/
│   ├── mobilewright.config.ts          # Mobilewright Android E2E configuration
│   ├── package.json                    # E2E scripts and dependencies
│   └── tests/                          # Navigation, Explore/Search, Favorites/Custom URL, Playback/FX, Podcasts, Settings/Language
├── PLAN.md                             # Design notes, version history, and manual test checklist
└── LICENSE                             # MIT License
```

---

## Building Locally

### Prerequisites

- **JDK 17**
- **Android SDK** (API 35 platform & build tools; set `sdk.dir` in `local.properties` or `ANDROID_HOME`)

### Build APKs

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Release APK -> app/build/outputs/apk/release/app-release.apk (unsigned if no signing config is set)
./gradlew assembleRelease
```

### Release Signing

Release keystores are kept out of Git (`*.jks` and `keystore.properties` are gitignored):

- **Local builds**: Create a `keystore.properties` file in the project root:
  ```properties
  storeFile=app/onda-release.jks
  storePassword=your_store_password
  keyAlias=your_key_alias
  keyPassword=your_key_password
  ```
- **CI builds**: Configure GitHub Actions repository secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD`.
- External clones and pull requests without signing secrets automatically fall back to building an unsigned release APK; debug builds always work without signing configuration.

---

## Testing

### JVM Unit Tests (JUnit + Robolectric)

```bash
./gradlew testDebugUnitTest
```

HTML test reports are generated at `app/build/reports/tests/testDebugUnitTest/index.html`.

### Android E2E Tests (Mobilewright)

Requires **Node.js 22+** (CI uses Node 24) and a running Android emulator or connected device visible to `adb`.

```bash
# 1. Build the debug APK
./gradlew assembleDebug

# 2. Install E2E dependencies, typecheck, and run the suite
cd e2e
npm ci
npm run typecheck
npm test

# Optional: open the HTML report
npm run test:report
```

Optional environment variables supported by `e2e/mobilewright.config.ts`:

- `ONDA_APK_PATH` — Custom path to the APK to install.
- `ONDA_SKIP_INSTALL=1` — Skip APK installation if the app is already installed on the device.
- `MOBILEWRIGHT_DEVICE_ID` — Target a specific ADB device serial.

---

## CI/CD Workflows

- **`Build & Release APK` (`.github/workflows/build.yml`)**: Runs on pushes to `main`, pull requests, `v*` tags, and manual dispatch. Runs `./gradlew testDebugUnitTest`, builds and verifies the release APK (`apksigner`), uploads `onda-apk` as a 30-day artifact, and automatically publishes a **GitHub Release** when a `v*` tag is pushed.
- **`E2E Tests (Mobilewright)` (`.github/workflows/e2e.yml`)**: Runs on pushes/PRs to `main` and manual dispatch. Builds the debug APK, typechecks `e2e/`, launches a headless Pixel 6 Android API 34 emulator with KVM acceleration, runs the Mobilewright E2E suite, and uploads the HTML report and failure screenshots/view trees.

---

## Roadmap

- Optional Google Sign-In (Firebase Auth + Firestore) for cross-device sync of favorites, podcast subscriptions/progress, history, and preferences — while keeping full offline/account-free operation.
- Chromecast / Google Cast support.
- Radio alarm clock.
- R8 minification for smaller signed release APKs.
- Experimental automatic volume ducking during Triton/AdsWizz commercial markers.

See [`PLAN.md`](PLAN.md) for architecture notes, release history, and manual verification checklists.

---

## License

MIT — see [`LICENSE`](LICENSE).
