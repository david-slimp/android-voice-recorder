# Changelog

## 0.4.3 — 2026-11-25
- Add timestamped labeled bookmarks for reported capture failures and, on Android 10+, microphone silencing/restoration. Android 9 does not reliably report all silencing.

## 0.4.2 — 2026-11-25
- Keep the user-started PCM capture session open while paused, discard paused samples and release capture on Stop. AAC uses the platform pause API. Android may still interrupt or override microphone access.
- Clarify microphone use in the paused UI and persistent notification.

## 0.4.1 — 2026-11-25
- Added upper-left menu and About page with app version, author, privacy notes and license text.
- Added AGPL-3.0-only license for original app code; retained separate upstream codec licenses and notices.
- Updated README, portable build defaults, and public repository exclusions.

## 0.4.0 — 2026-11-25
- Added FLAC, MP3 and Ogg/Opus recording using native FLAC and streaming compact Java encoders.
- Verified formats on Galaxy Note 8 and matched MP3 recording parameters against a reference app.
- Preserved AAC/WAV, bookmarks, offline operation and visible foreground recording.

## 0.3.0 — 2026-11-25
- Added lossless WAV (44.1 kHz mono, 16-bit PCM) and format selector.
- Verified short WAV pause/resume, playback and sharing.

## 0.2.1 — 2026-11-25
- Compact dark recorder UI with small red indicator and all primary controls on one screen.
- Removed periodic UI updates while idle.

## 0.2.0 — 2026-11-25
- First functional AAC/M4A recorder with background foreground service, pause/resume, stop/save, bookmarks, library/playback and share chooser.

## 0.1.1 — 2026-11-25
- Renamed app DBS-VocRec; bright red and white recording icon.

## 0.1.0 — 2026-11-25
- Initial native Android install and button smoke test.
