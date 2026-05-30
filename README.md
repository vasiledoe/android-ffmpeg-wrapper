# ffmpeg-wrapper

An Android library for FFmpeg-based video processing. It wraps [ffmpeg-kit](https://github.com/arthenica/ffmpeg-kit) and exposes two high-level use cases: **compress** a video and **compress + concatenate** a list of videos with optional background audio.

Distributed via [JitPack](https://jitpack.io) as an AAR.

---

## Requirements

| Item | Value |
|---|---|
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin |
| DI | Koin 3.x |

---

## Installation

### 1. Add JitPack to your repositories

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency

```groovy
// build.gradle (app module)
dependencies {
    implementation 'com.github.floodin:ffmpeg-wrapper:1.0.50'
}
```

### 3. Add the FileProvider to your manifest

The library returns output files via `FileProvider`. Declare it in `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

---

## Setup

Initialize the library's Koin module alongside your own app modules:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                libModule,   // from com.floodin.ffmpeg_wrapper.di
                appModule    // your own module
            )
        }
    }
}
```

Then inject the use cases wherever you need them:

```kotlin
private val compressVideoUseCase: CompressVideoUseCase by inject()
private val concatVideosUseCase: ConcatVideosUseCase by inject()
```

---

## Usage

### Compress a single video

```kotlin
val result: FFmpegResult = compressVideoUseCase.executeSync(
    inputVideo = VideoInput(
        id          = "my-video-1",
        absolutePath = "/storage/emulated/0/DCIM/video.mp4",
        orientation  = VideoOrientation.HORIZONTAL,
        userRotationDegrees = 0
    ),
    resolution = VideoResolution(width = 1280, height = 720),
    duration   = 300f,   // cap output at 5 minutes; omit to use default (5 min)
    appId      = packageName,
    appName    = getString(R.string.app_name)
)

when (result) {
    is FFmpegResult.Success -> {
        val output: VideoOutput = result.data
        // output.absolutePath, output.uri, output.size
    }
    is FFmpegResult.Error  -> Log.e("TAG", result.message)
    is FFmpegResult.Cancel -> { /* user cancelled */ }
}
```

### Compress multiple videos in parallel

```kotlin
// suspend fun — call from a coroutine
val results: List<FFmpegResult> = compressVideoUseCase.executeSync(
    inputVideos = listOf(video1, video2, video3),
    resolution  = VideoResolution(1280, 720),
    appId       = packageName,
    appName     = getString(R.string.app_name)
)
```

### Compress and concatenate

Compresses each video to fit a shared duration budget, then joins them into a single file. Optionally mixes in a background audio track.

```kotlin
val result: FFmpegResult = concatVideosUseCase.executeSync(
    inputVideos = listOf(video1, video2, video3),
    inputAudio  = AudioInput(
        videoLevel          = 80,   // original audio volume 0–100
        trackLevel          = 40,   // background track volume 0–100
        trackAbsolutePath   = "/storage/emulated/0/Music/background.mp3"
    ),
    resolution  = VideoResolution(1280, 720),
    duration    = 600f,   // total budget in seconds; omit to use default (10 min)
    appId       = packageName,
    appName     = getString(R.string.app_name)
)
```

Pass `inputAudio = null` to concatenate without a background track.

---

## Data models

### `VideoInput`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Caller-assigned identifier; echoed back in `VideoOutput` |
| `absolutePath` | `String` | Filesystem path to the source video |
| `orientation` | `VideoOrientation` | `HORIZONTAL` or `VERTICAL` |
| `userRotationDegrees` | `Int` | Extra rotation to apply (0, 90, 180, 270) |

### `VideoResolution`

```kotlin
VideoResolution(width = 1280, height = 720)   // HD
VideoResolution(width = 1920, height = 1080)  // FHD
```

### `AudioInput`

| Field | Type | Description |
|---|---|---|
| `videoLevel` | `Int` | Volume of the original video audio (0–100) |
| `trackLevel` | `Int` | Volume of the background track (0–100) |
| `trackAbsolutePath` | `String` | Filesystem path to the audio file |

### `VideoOutput`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Mirrors the input `id` |
| `uri` | `Uri` | FileProvider URI — safe to share with other apps |
| `absolutePath` | `String` | Direct filesystem path |
| `size` | `Long` | File size in bytes |

### `FFmpegResult` (sealed)

```kotlin
sealed class FFmpegResult {
    data class Success(val data: VideoOutput) : FFmpegResult()
    data class Error(val message: String)     : FFmpegResult()
    object Cancel                             : FFmpegResult()
}
```

---

## v1.0.50 — Google Play compliance update

This release contains mandatory changes required to publish on Google Play. No public API was changed.

### What changed

| Area | Before | After |
|---|---|---|
| Target / Compile SDK | 34 | **35** |
| Android Gradle Plugin | 7.3.1 | **8.7.3** |
| Gradle wrapper | 7.4 | **8.9** |
| Kotlin plugin | 1.6.21 | **1.9.24** |
| JNI packaging | legacy (compressed `.so`) | **`useLegacyPackaging = false`** |
| ffmpeg-kit binary | `ffmpeg-kit-min-gpl-6.0` (4 KB aligned) | **`ffmpeg-kit-lts-ndk-r25-16k`** (16 KB aligned) |

### Why

**API 35 (Android 15):** Google Play rejects app updates that do not target API 35 as of November 2025.

**16 KB page size:** Pixel 9 and newer devices run a Linux kernel configured for 16 KB memory pages. APKs that ship native `.so` files with 4 KB internal alignment crash on these devices. Two changes together fix this:

1. `useLegacyPackaging = false` — stores `.so` files uncompressed and page-aligned inside the APK.
2. `ffmpeg-kit-lts-ndk-r25-16k.aar` — a build of ffmpeg-kit compiled with NDK r25 and the `-Wl,-z,max-page-size=16384` linker flag, replacing the archived 6.0 release whose prebuilt binaries only had 4 KB alignment.
