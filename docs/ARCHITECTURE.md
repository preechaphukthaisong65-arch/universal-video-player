# Architecture

## Modules
- `app`: Android entry point, dependency composition root, navigation and intent dispatch.
- `core-common`: shared platform-independent primitives.
- `domain`: pure Kotlin playback contracts and use cases.
- `data`: Room persistence and local repositories.
- `player`: sole Media3 ExoPlayer owner and player UI.
- `browser`: isolated WebView browser surface.
- `presentation-theme`: shared Compose theme.

## Dependency direction
- domain -> core-common
- data -> domain, core-common
- player -> domain, core-common
- browser -> domain, core-common
- app -> all feature modules
- presentation-theme -> Compose UI only

The browser must not instantiate an independent playback engine. A single `PlayerSessionManager` owns at most one ExoPlayer instance. Domain contracts are the boundary between features.

## Lifecycle
- Use application context for the player owner.
- Pause playback when the app moves to background.
- Do not automatically resume on foreground.
- Release the player when the owning application session is explicitly closed.
- Configuration changes must not create another player instance.

## Storage
Use Android Storage Access Framework URIs and persistable read permission where offered. Do not request broad external storage access for ordinary playback.
