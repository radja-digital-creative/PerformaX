# Account and attendance release 7.2.5

Backend deployed on the self-hosted PerformaX stack on 2026-11-17 after an isolated database restore, SQL regression tests, real Auth/REST/Edge integration tests, and a production transaction dry run. Android builds through GitHub Actions; the candidate must be re-signed using the existing private keystore before distribution. Never commit that key.

Behavior:
- Pending outside-radius, late-arrival and early-departure requests permit the matching attendance action. Manager/Owner decisions remain separate.
- Only approved, direction-matching exceptions waive KPI/payroll consequences. A later rejection restores the original monetary penalty. No deployment-time historical backfill is performed.
- Current email is visible to self/Owner; only a newly entered password is revealable.
- Owner creates Owner/Manager/Admin/Staff; Manager creates Staff in assigned stores only.
- Disabled accounts lose login, refresh, REST/Edge and authenticated table access. The Android foreground gate checks every 30 seconds and on resume.

Backend:
- Apply database/account_access.sql, database/pending_attendance_access.sql, database/session_data_access.sql in one transaction. Preserve any existing PostgREST pre-request hook before installing public.require_active_account.
- Deploy functions/_shared together with employee-account, create_user_admin and update_user_admin. Install the runtime gate with supabase/install_runtime_gate.py against the existing functions directory; restart Edge runtime.
- Preserve backups before changing live functions or database objects.

Verification:
- App unit tests and Android compilation run in .github/workflows/android-draft.yml.
- node --experimental-transform-types tests/test_employee_account.mjs: 15 mocked endpoint contract/security cases.
- tests/account_access_regression.sql: role scope, legacy/direct escalation denial, revocation, existing JWT rejection.
- database/tests/pending_attendance_access.sql: pending direction access, early checkout gate, approval/rejection/deletion KPI restoration, monetary penalty restoration, preserved timestamps.
- tests/session_data_access_regression.sql: authenticated-role valid/revoked/inactive session table reads.
- Real isolated Auth/REST/Edge integration: owner creates owner/manager, manager creates staff and cannot create owner/manager, self profile edit, owner credential change/new login, disable and stale-token/login denial.

Limits: physical Android device testing remains required. Existing signed Storage URLs last until expiry, previously downloaded data cannot be revoked, and anonymous access policies are unchanged. Legacy direct attendance timestamp mutation is a separate security audit; this change enforces the attendance RPC gates and prevents client self-approval of exceptions. Future database tables need the session policy too.
