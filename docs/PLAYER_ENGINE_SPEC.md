# Player Engine Specification

- Engine: AndroidX Media3 ExoPlayer.
- Ownership: one application-scoped PlayerSessionManager.
- Sources: content URIs and supported direct network media URIs.
- Audio-only: same ExoPlayer instance and MediaItem pipeline.
- Speed: accepted range 0.25x–3.0x.
- Seek: non-negative millisecond positions.
- Track selection, subtitle selection/import, queue and display modes are follow-up implementation tasks.
- Unsupported codecs, malformed sources, DRM and protected streams must fail safely with user-readable errors; never bypass content protection.
- Do not log signed URLs, cookies, tokens or authorization headers.
