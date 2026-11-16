# PerformaX 7.2.2-pxn candidate

## Update flow

Startup checks app_versions for at most four seconds. A newer version shows its version name and scrollable release notes with Download & Update and Later actions. There is no automatic download. Failed checks continue to the existing session flow. Session storage loading is awaited for up to two seconds instead of a fixed sleep.

Downloads stream in 64 KiB chunks into an app-private temporary file, report actual bytes/progress, reject HTTP errors and incomplete payloads, and can be canceled. Failed downloads can be retried. Package name, newer version, advertised version, and signing certificates are validated before invoking the Android installer. Users can continue into the app after denying install permission or canceling installation. Android retains installation consent.

## Weak-network changes

Only the visible home tab instantiates its data-loading ViewModel. Global profile loading no longer waits for optional FCM, warnings, or announcements; optional tasks have separate bounded jobs. Initial independent attendance reads run concurrently, and retries cancel the previous load. Attendance photos are resized to a maximum dimension of 1280 pixels and JPEG quality 75 off the main thread. Location queries select only fields required by the map. Coroutine cancellation propagates through the shared request wrapper.

These changes do not provide offline attendance submission, guaranteed GPS delivery, or a local database cache. Server confirmation is still required before attendance is successful.

## Publication gate

Do not register version 722 in app_versions until an APK signed with the installed application's key is available and installation has been tested. The 7.2.1 test APK previously delivered had signer SHA-256 d04f3c5c862aa09f061b2b5e9ea23d8570fa204cd9685463cffec539de7021fa. That private key is not present in the retained workspace or the searched Library files. A newly generated debug key cannot update it. The original 7.2.0 release certificate is also different. No release row or APK storage object has been published for 7.2.2.

## Device acceptance checks still required

- Slow/offline version check opens the existing app after the bounded check; attendance never shows success without server confirmation.
- Long release notes and enlarged fonts leave both update and skip accessible.
- Interrupt/retry/cancel download; verify partial files are removed and real progress is shown.
- Reject wrong package, same/older version, mismatched advertised version, invalid APK and mismatched certificate.
- Cancel installer or deny install permission, then continue into app.
- Verify weak-network attendance and all three home tabs on a physical device.

Library integration tests that mutate live production data must not be run as a routine build check.

## Verification on 2026-11-16

`:app:testDebugUnitTest :app:assembleDebug` completed successfully. Eight app unit tests passed (four updater policy tests, three attendance policy tests, one existing example test), with zero failures/errors/skips. APK metadata confirms package id.biz.yuka.performax, versionCode 722, versionName 7.2.2-pxn, ARM64. APK signature verification passes, but the generated debug certificate SHA-256 c70e5e6c68afc56de2032ae9794f1940c2833e86393748836d34b7999a462a23 differs from the delivered 7.2.1 certificate, so this artifact is not a compatible update and is not published.

Static peer review findings about clipped release notes and optional jobs blocking refresh were corrected. Installer cancellation recovery was added. Device/install/network-throttling acceptance tests remain pending. GitHub write was attempted again and rejected with HTTP 403 Resource not accessible by integration. Source is retained in a cumulative patch against main commit 42fda65d1e1d101b2bab2255a0279f993ab8bb1b; do not apply the already-deployed database migration again.
