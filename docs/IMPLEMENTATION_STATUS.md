# Implementation Status

## CI-verified source checkpoints

- Foundation commit `a293de0e5bf391566476f165c28c7a9acf12eda9`: Android CI run 57 passed unit tests, lint and debug APK build.
- Player phase commit `771882c551b7d3f8203cc8f437d0459fdaa14648`: Android CI run 65 passed unit tests, lint and debug APK build.
- Library/Browser phase commit `4e13bb9ea0eb4bd2938f8603f2b3e08ac9ff7fac`: Android CI run 67 passed unit tests, lint and debug APK build.
- Settings/accessibility phase commit `7eef53571694e97c761d207269153fe037664e80`: CI verification is tracked separately; do not treat this phase as passed until its run completes successfully.

CI success proves source compilation and the configured checks only. It does not prove physical-device behavior.

## Implemented in source

### Player
- One application-scoped Media3 ExoPlayer.
- Multi-file queue, previous/next, queue selection, seek and speed controls.
- Audio/text track selection and external SRT/VTT attachment.
- Fit/Zoom/Fill modes, touch lock and keep-screen-awake preference.
- Audio-focus and noisy-route handling.
- Room playback history with periodic position persistence and resume from a selected history item.
- User-visible playback error message.

### File intake and library
- ACTION_VIEW, ACTION_SEND and ACTION_SEND_MULTIPLE routing.
- SAF multi-file picker.
- User-selected SAF folder indexing with a bounded scan depth and file count.
- Search across indexed media and playback history.
- No broad all-files permission.

### Browser
- HTTPS-first WebView; file/content access disabled, mixed content blocked, Safe Browsing enabled, third-party cookies disabled and no JavaScript bridge.
- Up to eight in-memory tabs.
- Local bookmarks and history; stored URLs omit query strings and fragments.
- User-triggered cookie/session clearing.
- JavaScript preference is stored locally and applied to active WebViews.

### Settings and release preparation
- Local preferences for keep-screen-awake and Browser JavaScript.
- Basic TalkBack descriptions for preference switches.
- Centered maximum-width content layout for larger displays.
- Manual signed APK/AAB workflow with expected certificate fingerprint verification.
- Draft privacy policy and Play release checklist.

## Still requires implementation

- Player gesture seek/double-tap controls.
- Browser downloads (only when explicitly permitted), fullscreen HTML5 video and safe media handoff to the shared Player.
- Library sorting and richer file metadata.
- Complete settings, localization and remaining accessibility polish.

## Still requires verification

- Instrumentation/UI tests for lifecycle, interruptions, queue transitions, persisted SAF grants and Browser tab lifecycle.
- Physical-device testing on at least two Android devices for MP4, MKV, WebM, audio-only and SRT/VTT.
- Test SAF access after reboot and after provider permission revocation.
- Browser tests across at least ten website categories, memory/renderer stability, redirects and cookie/session behavior.
- TalkBack, Android font scaling, tablet and foldable QA.
- RAM, battery, long-playback and supported-Android-version regression checks.
- Run the signed release workflow; verify APK/AAB signatures, package ID and certificate fingerprint.
- Install fresh and perform an update over an existing signed installation using the same certificate.
- Publish and verify the final privacy policy, Data Safety form, app icon, screenshots and Play listing.

## VPN

VPN is excluded from Release 1. No VPN endpoint, proxy tunnel or VPN connection is configured. VPN remains a separate project after product, cost, legal and Google Play policy review.

## Release rule

A phase is not complete merely because code exists or CI passes. Its acceptance gate must include relevant automated tests and physical-device QA. Do not merge stacked PRs out of order and do not publish until the Release 1 checklist is complete.
