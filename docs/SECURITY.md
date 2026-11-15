# Security Baseline

- Never commit signing material, Base64 keystores, passwords, or private keys.
- Release signing material is injected from GitHub Actions Secrets and decoded only into the ignored Gradle build directory.
- WebView defaults to HTTPS navigation, blocks cleartext navigation, disables file/content access, disallows mixed content, and requires a user gesture for media playback.
- Do not add JavaScript bridges without a reviewed, minimal interface and origin allowlist.
- Do not bypass DRM, paywalls, authentication, or site access controls.
- Do not log full media URLs, query parameters, cookies, authorization headers, or private file paths.
- Use Android Storage Access Framework and content URI grants instead of broad storage permissions.
- Playback history remains local; no telemetry or remote history sync is implemented.
