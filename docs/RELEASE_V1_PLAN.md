# Universal Video Player — Release 1 Execution Plan

## Release boundary

Release 1 ships without VPN. VPN is a separate follow-up project and must not block the first release. No paid server, proxy endpoint, or VPN connection may be provisioned as part of Release 1.

## Phase 1 — Player

### Implemented in source (not yet device-QA approved)
- One application-scoped Media3 ExoPlayer instance.
- Multi-item playback queue, next/previous navigation and queue selection UI.
- Playback speed controls and seek controls.
- Audio and text-track selection UI.
- External SRT/VTT attachment.
- Fit, Zoom and Fill display modes.
- Touch-lock overlay and keep-screen-on while playing.
- Local Room history with periodic position persistence and resume from history.
- Audio-focus handling and pause on Activity stop.

### Remaining work
- Gesture seek/double-tap controls without blocking PlayerView controls.
- Explicit handling and user-visible reporting for decoder/network/permission errors.
- Instrumentation tests for Activity lifecycle, interruptions, queue transitions and persistence.
- Physical tests for MP4, MKV, WebM and audio-only files on at least two Android devices.
- Verify SRT and VTT rendering against real sample files and external subtitle permissions.

### Exit gate
CI unit tests, lint and debug APK build pass; instrumentation tests pass; physical-device matrix passes; resume position survives force-stop/relaunch and the selected history item resumes correctly.

## Phase 2 — File intake and library

### Implemented in source (not yet device-QA approved)
- ACTION_VIEW, ACTION_SEND and ACTION_SEND_MULTIPLE intake.
- SAF multi-file picker.
- User-selected SAF folder indexing with bounded depth and item count.
- Search across indexed file names/URIs and local playback history.
- No broad storage permission is requested.

### Remaining work
- Test file grants and persisted folder access after reboot and provider revocation.
- Sort controls (name, modified date, type, recently played).
- Empty, loading, permission-revoked and provider-error states.
- Validate duplicate URIs, large folders and slow/cloud document providers.
- Instrumented tests for file manager, gallery and chat share flows.

### Exit gate
Single and multiple files open from file manager, gallery and chat; selected folder is rescanned after process restart; no unnecessary storage permission is requested.

## Phase 3 — Browser

### Implemented in source (not yet device-QA approved)
- HTTPS-first WebView with file/content access disabled, Safe Browsing, blocked mixed content and no JavaScript bridge.
- Multiple in-memory tabs (bounded to eight).
- Local bookmarks and history.
- Cookie/session clearing initiated by the user.
- Stored URLs omit query strings and fragments to avoid persisting common signed tokens.

### Remaining work
- Download flow only for user-initiated downloads that the site permits, with safe destination and error handling.
- Fullscreen HTML5 video lifecycle.
- Site-dependent media handoff to the shared ExoPlayer, only for directly playable and permitted media; no DRM/paywall/authentication bypass.
- Tests across at least ten website categories, tab switching, renderer loss, memory stability and cookie behavior.
- Validate redirects, external-app intents, custom schemes and SSL error handling.

### Exit gate
Browser tests pass across the agreed website matrix; tabs survive switching; no unbounded WebView growth; downloads respect site and platform permissions; media handoff is tested per supported site.

## Phase 4 — Settings, UI and accessibility

### Remaining work
- Replace placeholder settings with persisted settings that affect real behavior.
- Ensure every visible action has a working outcome or is disabled with a reason.
- TalkBack labels and focus order; test Android font scaling.
- Responsive layouts for tablets and foldables.
- Localization, loading, empty and error states.
- Verify browser privacy controls and library permissions in the UI.

### Exit gate
No dead buttons or placeholder settings remain; TalkBack and large-font checks pass; phone, tablet and foldable layouts are usable.

## Phase 5 — Release 1 (no VPN)

### Remaining work
- Verify release signing using the existing private keystore and GitHub Actions secrets.
- Verify R8/resource shrinking, APK/AAB installation and same-certificate upgrade.
- Confirm versioning, backup/restore policy and no secrets in repository/artifacts.
- Run memory, battery, startup, playback and network regression checks across supported Android versions.
- Prepare privacy policy, Data Safety declarations, app icon, screenshots and Play listing.
- Set up crash reporting only after reviewing data minimization and consent implications.

### Exit gate
Signed release artifact installs and upgrades correctly; full checklist passes on physical devices; privacy and Play Console declarations match actual behavior.

## Phase 6 — VPN (separate project)

- Decide whether the product is self-hosted VPN or user-supplied server configuration before implementation.
- Prove WebView-only proxy/tunnel isolation in a standalone PoC; ExoPlayer and WebView share an Android UID, so package-based VpnService filtering cannot isolate them.
- Select protocol and infrastructure only after the PoC and cost/privacy/legal review.
- Test DNS, IPv4, IPv6, TCP, UDP, QUIC, network switching, reconnect, kill switch and failover.
- Review current Google Play VpnService policy before any public VPN feature.

## Working rules

- Work on a dedicated branch per phase and use small draft PRs.
- CI results are necessary but not sufficient; no phase is marked complete until its exit gate passes.
- Do not merge a stacked PR before its base PR is accepted.
- Keep VPN out of Release 1.
