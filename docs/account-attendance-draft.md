# Account and attendance changes — deployment blocked

Status: Android draft compiles; backend SQL and Edge code are NOT deployed. Do not distribute its APK. Current main remains the released 7.2.4.

Requested behavior:
- Pending outside-radius, late-arrival and early-departure requests permit the matching attendance action. Manager/Owner decisions remain separate.
- Current email is visible to self/Owner; only a newly entered password is revealable.
- Owner creates Owner/Manager/Admin/Staff; Manager creates Staff only.
- Disabled accounts lose login and existing-session access.

Implemented client contracts:
- `current_account_access()` -> `{active:boolean}` is required for login/resume and foreground polling.
- `get_attendance_exception_access(p_user_id,p_schedule_id)` -> status fields is required for attendance.
- `employee-account` Edge endpoint handles create, get_email, update_credentials and disable.

Verified: initial Android build + 24 app unit tests passed. Password-retention review fix receives another build/test pass. No live employee accounts/passwords/attendance were changed.

Blocking server work:
1. Obtain deployment access to the self-hosted Capsule Cloud functions runtime. Studio currently exposes function inventory but no source edit/deploy controls; connected HostData server hosts other projects, not this stack.
2. Read and back up current create_user_admin/update_user_admin source; secure or replace legacy paths so old clients cannot bypass new role/credential rules.
3. Validate account_access.sql on staging, retain existing PostgREST hooks, enforce disabled-session access on Storage/Realtime/other Edge endpoints as appropriate.
4. Validate pending_attendance_access.sql exact anchors against installed process_attendance. Complete retrospective approval/rejection reconciliation for attendance statuses, KPI and payroll; the existing approval trigger only creates legacy bypass rows.
5. Exercise owner/manager/staff scope, disable with an existing token, pending attendance then approval/rejection, email/password update, and device lifecycle tests. Then deploy server, bump APK version, rebuild/sign and release.

Prepared SQL is deliberately labelled a draft; do not apply blindly or claim the requested end-to-end behavior is live.
