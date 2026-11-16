# Employee attendance and location implementation plan

Goal: configurable photo, primary office/work days, unrestricted attendance with normal late deductions, fresh map locations.
Architecture: retain Compose/repositories and existing payroll RPC; persist employee preferences separately with admin-only writes and self/admin reads. Preserve manual schedules and attendance history. Do not treat unrestricted attendance as an approved shift exemption.

- [x] Add preference storage, authenticated validation and automatic schedule generation from primary office/work days.
- [x] Add employee settings form and load/save through the existing repository.
- [x] Allow photo-free submission with current location, preserving server radius/time/status checks.
- [x] Keep lateness calculation independent of admission window; disable maximum-time input for unrestricted attendance.
- [x] Reject old GPS fixes, cancel timed-out requests, poll maps only while screen is started, clear obsolete markers.
- [x] Test time windows/GPS age, validate SQL in a rollback transaction, compile Android and inspect APK/signature.

Review focus: denied permissions; stale GPS/failed upload; unset preferences use existing defaults; manual schedules and historical attendance preserved; late status/deductions still computed.
