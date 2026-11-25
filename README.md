# DBS-VocRec

A lightweight, native Android voice recorder by David Slimp (rock808). No advertising, tracking, or internet permission. Capture locally, even with the screen off, after explicitly starting a visible recording.

## Features

- Dark, compact UI, small recording indicator, elapsed time, pause/resume and stop/save.
- User-triggered foreground microphone service and persistent recording notification with Pause/Resume and Stop.
- Automatic timestamped bookmarks record reported interruptions and restoration (Android 10+ silencing callbacks; Android 9 capture errors only). Incomplete files and bookmark sidecars stay available in the library.
- During a paused PCM recording the microphone stays open, but paused samples are discarded. Stop releases it; Android retains system-level microphone priority. AAC pause behavior is platform-managed.
- Timestamp bookmarks saved as CSV alongside the recording.
- Local recording library, playback and Android share chooser. Recordings remain on the device until you choose to share them.
- Streaming encoders, no live spectrum animation or waveform editing.
- Android 7.1 (API 25) minimum, compiled against Android SDK 35. Tested on Samsung Galaxy Note 8 / Android 9.

### Formats

| Format | Encoding |
|---|---|
| AAC / M4A | 44.1 kHz mono, 96 kbps |
| WAV | 44.1 kHz mono, 16-bit PCM |
| FLAC | 44.1 kHz mono, 16-bit lossless; native Android encoder |
| MP3 | 44.1 kHz mono, 128 kbps; Java LAME |
| Ogg / Opus | 48 kHz mono, nominal 64 kbps VBR; Concentus |

Current release: 0.4.1. All five formats have passed short device-level encode/decode tests. One-hour and multi-hour unplugged, locked-screen endurance testing remains in progress. Automatic Drive upload is planned; it is not currently implemented.

## Build and install

Install JDK 17, Android SDK Platform 35, Android Build Tools 35.0.0 and platform-tools. Set `JAVA_HOME` and `ANDROID_HOME` for your system (or create an untracked `local.properties` containing `sdk.dir=/path/to/sdk`). From the repository:

```sh
./gradlew --no-daemon assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper is pinned to Gradle 8.9. Use `./build.sh --offline` after the dependencies are cached. Debug APK is appropriate for local testing; public releases should use a separately managed release signing key. Do not commit keys or personal recordings.

Desktop helper: `tools/convert-recording.sh input.wav flac|mp3|ogg` (requires FFmpeg). No audio editing runs on the phone.

See [CHANGELOG.md](CHANGELOG.md) for release history and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for component licensing and source provenance.

## License

DBS-VocRec's original application code is licensed under GNU Affero General Public License v3.0 only (AGPL-3.0-only), see [LICENSE](LICENSE). Vendored codec code retains applicable upstream copyright and license notices; do not overwrite those notices. Full corresponding source and build instructions are included so users can rebuild the application. The app's About page includes the license notices.
