# Implementation Status

## Verified baseline
- Modular Gradle project structure and Kotlin/Compose application shell.
- Single shared Media3 ExoPlayer session manager.
- SAF multi-file picker and `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent routing.
- Shared ExoPlayer queue API for multiple selected/shared media items.
- Forwarding of URI read grants through `ClipData` when receiving shared media.
- HTTPS-first WebView with restricted file/content access, browser navigation controls, search/address handling, and WebView teardown on screen disposal.
- Room playback-history schema and DAO.
- Four-endpoint server-selection policy and pure Kotlin unit tests.
- Debug CI and manually triggered signed-release workflow definitions.

## Implemented in current work; CI verification pending
- Browser back, forward, reload, progress and page-title display.
- HTTPS-only top-level browser navigation; non-HTTPS navigation is rejected.
- WebView is stopped and destroyed when the Browser screen is disposed.
- Multi-file selection and share intents are passed to the one shared ExoPlayer queue.

## Not complete / requires implementation or verification
- End-to-end playback history updates and resume-position persistence.
- Browser media-source detection and safe handoff to the shared Media3 player (site-dependent; DRM/protected streams remain unsupported).
- Subtitle import/selection, audio-track UI, playback queue UI, gesture controls, zoom/crop modes.
- Persistent bookmarks, browser history, tabs, full library indexing/search, settings persistence, accessibility and localization.
- Automated instrumentation/UI/security tests and physical-device QA.
- Signed release build, certificate fingerprint verification, install/upgrade verification.
- VPN Proof of Concept for browser-only traffic isolation, followed by DNS/IPv4/IPv6/TCP/UDP/QUIC leak tests. No VPN endpoint is configured and no VPN connection is currently implemented.

A phase is not complete merely because source code exists. Completion requires successful CI, relevant automated tests, and physical-device QA. No release is considered production-ready before these checks pass.
