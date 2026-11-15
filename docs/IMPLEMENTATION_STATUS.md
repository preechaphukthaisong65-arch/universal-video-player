# Implementation Status

## Baseline committed
- Modular Gradle project structure
- Kotlin/Compose Android application shell
- Single Media3 ExoPlayer session manager
- SAF media picker and basic VIEW/SEND intent routing
- HTTPS-first WebView shell with restricted file/content access
- Room playback-history schema and DAO
- Debug CI and manually triggered signed release workflows

## Not yet complete / requires verification
- Persisting playback history and resume position end-to-end
- Robust multi-intent and ACTION_SEND_MULTIPLE handling
- Browser media-source handoff to the shared Media3 engine (site-dependent; DRM and protected streams remain unsupported)
- Subtitle import/selection, audio-track UI, queue, gesture controls, zoom/crop modes
- Full library indexing, search, settings persistence, accessibility and localization
- Unit, instrumentation, UI, security and physical-device test suites
- Gradle build and APK installation verification

A phase is not complete merely because a source file exists. Completion requires automated build/test evidence and physical-device QA.
