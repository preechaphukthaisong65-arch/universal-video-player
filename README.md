# Universal Video Player

Private Android video and audio player project.

## Product identity
- Application ID: `com.preechaphukthaisong65.universalvideoplayer`
- Language: Kotlin
- UI: Jetpack Compose
- Playback: AndroidX Media3 ExoPlayer (single shared playback engine)
- Browser: Android WebView
- Local persistence: Room
- CI: GitHub Actions

## Architecture
This project follows a modular, Clean Architecture-inspired structure:
- `core-common`: shared platform-independent primitives
- `domain`: playback contracts and use cases
- `data`: Room persistence and repositories
- `player`: the single Media3 playback session
- `browser`: in-app WebView and safe URL handling
- `presentation-theme`: shared Compose theme
- `app`: composition root and navigation

The browser and player communicate through domain contracts; the browser does not own or instantiate a second playback engine.

## Security
- Signing keys and passwords must never be committed.
- Release signing reads GitHub Actions secrets only.
- WebView uses HTTPS by default and restricts file/content access.
- No DRM, paywall, authentication, or website restrictions are bypassed.
- Viewing history remains local to the device.

## Development
See `docs/ARCHITECTURE.md`, `docs/PLAYER_ENGINE_SPEC.md`, and `docs/PHASE_2_IMPLEMENTATION_PLAN.md` for the approved design baseline.

## Build
The Android project is built with Gradle and GitHub Actions. A release APK is not considered verified until the workflow succeeds and the APK is installed and tested on a physical device.
