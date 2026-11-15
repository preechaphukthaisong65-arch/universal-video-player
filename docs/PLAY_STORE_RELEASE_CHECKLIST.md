# Google Play Release 1 checklist

## Build and signing
- [ ] CI unit tests, lint and debug APK build pass on the release candidate commit.
- [ ] Signed APK and AAB are built using the existing private PKCS#12 keystore injected through GitHub Actions Secrets.
- [ ] APK signature verifies and SHA-256 certificate fingerprint matches the known release certificate.
- [ ] AAB signature verifies.
- [ ] R8 and resource shrinking are enabled and release build has no missing classes or reflection failures.
- [ ] Install fresh on a physical device.
- [ ] Install an update over the previous build; verify package ID and signing certificate are unchanged.
- [ ] Keep a verified offline backup of the keystore and separately stored password.

## Product QA
- [ ] MP4, MKV, WebM and audio-only playback on at least two Android devices.
- [ ] Audio tracks, SRT/VTT subtitles, speed, fit/zoom/fill, queue and resume behavior.
- [ ] Rotation/fold/unfold, app background, incoming call/audio focus and process recreation.
- [ ] SAF file picker, multi-select, selected folder scan, persisted grants and revoked-grant behavior.
- [ ] Browser tabs, bookmarks, history, cookie clearing, HTTPS redirects and ten website categories.
- [ ] TalkBack, large font scale, tablet and foldable layout.
- [ ] RAM, battery, startup and long-playback soak tests.
- [ ] No unimplemented or misleading controls.

## Store materials and declarations
- [ ] Final app icon and screenshots supplied by the developer.
- [ ] Public privacy policy URL published and checked against the exact release build.
- [ ] Data Safety form completed from the actual SDK/network behavior, not from this draft alone.
- [ ] Content rating and target audience declarations completed.
- [ ] App listing, support contact and developer website/contact details supplied.
- [ ] Google Play policy review completed for every enabled feature.
- [ ] VPN is not included in Release 1.

## Release decision
Do not publish until all required product QA and store declarations are verified. CI success alone is not a release approval.
