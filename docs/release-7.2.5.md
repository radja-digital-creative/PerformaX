# PerformaX 7.2.5 release verification

Released 2026-11-17. Android source commit f46ea368b111c0aa4c985350a6739bd3680f1e53; merged via PR #1. GitHub Actions run 44027279487 completed successfully: 24 Android unit tests, zero failures/errors/skips, app assembleDebug, and 15 mocked backend account cases.

APK: package id.biz.yuka.performax, versionCode 725, versionName 7.2.5-pxn, arm64-v8a. Signed with the existing 7.2.2–7.2.4 certificate. Certificate SHA-256: c70e5e6c68afc56de2032ae9794f1940c2833e86393748836d34b7999a462a23.

APK SHA-256: 4f0f7c1ef59a958493f3789dfcf86cf6ed5cd48cded0ba4653e646e4fde1f892. Size: 112514420 bytes.

Download: https://performx-api.capsule-cloud.my.id/storage/v1/object/public/apk_releases/725/PerformaX-7.2.5-arm64.apk

The public download hash and anonymous app_versions latest-version response were verified after publication. Production metadata points to version 725. Android still requires the device user to confirm installation.

The self-hosted Storage file-size cap was increased to 150 MiB for the APK bucket. Existing other buckets with a null limit were explicitly kept at the previous 50 MiB limit. Large uploads use the internal Storage endpoint; public download works. Compose was backed up before changing its global cap.

All temporary verification containers, restored database, temporary signing key, JRE and release artifacts were removed from Cloud B. Production backups remain private. Physical-device installation and background location behavior still require on-device verification; no claim of device testing is made.
