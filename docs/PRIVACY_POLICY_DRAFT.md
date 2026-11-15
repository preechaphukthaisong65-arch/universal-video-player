# Privacy Policy — Release 1 draft

**Status: draft for review. Do not publish this text until the developer contact details and the final Play Store build have been verified.**

## Information stored on the device

Universal Video Player currently stores the following information locally on the Android device:

- Playback history, media titles/URIs, last-played timestamps and resume positions in the app's Room database.
- A user-selected folder URI and its persisted Android Storage Access Framework read grant, when the user chooses a folder for the media library.
- Browser bookmarks and browsing history in local app preferences. Stored browser URLs omit query strings and fragments.
- Website cookies and session data managed by Android WebView so that websites can maintain their own sessions.
- Local app preferences, including the keep-screen-awake and Browser JavaScript settings.

The app does not upload playback history, bookmarks, browsing history or selected media files to a Universal Video Player server in the current implementation.

## Website connections

When a user visits a website in the in-app Browser, the device connects to that website. The website may receive ordinary connection information, set cookies, and process information under that website's own privacy policy. Universal Video Player does not operate a VPN or proxy in Release 1.

The Browser uses HTTPS navigation by default, blocks non-HTTPS top-level navigation, disables WebView file/content access, blocks mixed content and does not expose a JavaScript bridge to websites.

## Files and permissions

The app accesses media only after the user selects files or a folder through Android's system file picker (Storage Access Framework). The current implementation does not request broad all-files access.

## Sharing and third parties

The current project does not include an account system, remote history synchronization, advertising SDK, analytics SDK or crash-reporting SDK. This statement must be rechecked against the exact release artifact before publication.

## Retention and deletion

Playback history can be cleared from the Library screen. Browser history and cookies can be cleared from the Browser screen. Android app data can be cleared through Android system settings to remove locally stored app data, including preferences and bookmarks.

## Security

Local app data is stored in Android app-private storage. The app does not intentionally log media URLs, browser cookies, authentication headers or full signed URLs.

## Changes to this policy

This policy must be updated before adding advertising, analytics, crash reporting, remote services, VPN infrastructure or any new data collection/sharing behavior.

## Developer contact

**Developer contact:** [Add a monitored support email before publication]

**Effective date:** [Set on publication]
