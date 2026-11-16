# PerformaX 7.2.1-pxn

Employee details now include required-photo, unrestricted attendance, optional check-in cutoff, default assigned office/active shift, and working weekdays. Unrestricted attendance changes admission only: late status and existing salary penalty rules remain active. Photo-free attendance still requires a fresh location and the existing server validation.

Changes to schedule defaults regenerate future automatic schedules from tomorrow. Today's schedule, history, manually generated schedules, and schedules referenced by attendance or requests are retained. Photo/time-only changes do not regenerate schedules.

Live map polls saved locations every 10 seconds while visible and requests fresh employee fixes every 60 seconds independently. Location acquisition rejects fixes older than 30 seconds, tries high/balanced accuracy, and cancels timed-out requests. Android background updates still depend on location permission, GPS, connectivity, and operating-system scheduling; periodic WorkManager interval is 15 minutes.

## Deployment

`database/employee_attendance_preferences.sql` is a one-time migration for the original database version. It has already been applied to the supplied PerformX database. Do not rerun it on that database. No employee preferences were set during deployment. Existing payroll formulas and deduction amounts were retained.

## Release status

The original 7.2.0 APK uses a release certificate absent from this repository. The provided 7.2.1 debug APK is an ARM64 test build, not an in-place update for the released app. It requires a device without the existing app; do not uninstall a working installation to bypass signature mismatch. Obtain the original signing keystore securely before building the production update. Never commit signing keys or passwords.

No physical-device or production employee attendance/payroll end-to-end test has been performed. Existing library integration tests were intentionally excluded because they can modify the live database.

## Validation

` :app:testDebugUnitTest :app:assembleDebug` passed on 2026-11-15. Three attendance-policy/GPS tests passed, with zero failures or errors. APK signature verification passed; its debug certificate differs from the original release certificate. SQL migration was dry-run in a rollback transaction, deployed, and checked for preference table/RLS/policies, unrestricted admission, retained late status, weekday scheduling, and first-save schedule preservation. Peer review findings for initializer order, independent map polling, and first-save preservation were corrected and re-reviewed.
