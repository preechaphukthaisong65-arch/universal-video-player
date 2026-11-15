# Implementation and QA Plan

1. Stabilize modular Gradle build and dependency graph.
2. Complete single-player lifecycle and playback controls.
3. Add persistent history/resume and SAF queue.
4. Add browser URL safety and supported media handoff.
5. Add audio/subtitle track selection and accessibility.
6. Add automated tests, security review and device QA.
7. Build signed APK through GitHub Actions and verify certificate fingerprint.

Release criteria: clean CI build, unit/instrumentation tests pass, signing certificate matches the protected release key, APK installs and upgrades on a physical device, and core playback flows pass. No release is called production-ready before this evidence exists.
