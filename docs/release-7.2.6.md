# PerformaX 7.2.6 — Insight/Growth navigation fix

The deferred LazyColumn content previously read mutable selectedTab while holding nullable ViewModels from an earlier composition. Selecting Insight or Growth could therefore dereference null and terminate the activity. HomeScreen now captures the selection once during composition and uses the same value for ViewModel creation, tab header and content; forced nullable dereferences are removed. rememberSaveable preserves the selected tab across temporary screen recreation. ViewModels still initialize only on demand.

Source: 7ef028f10901b904707adbc0701cdd93e2cd0b3b, PR #2. GitHub Actions 44037699340 passed, including 24 Android tests (zero errors/failures/skips), backend mocked contracts and APK assembly. Existing tests do not replace an on-device tab navigation test; no physical-device testing is claimed.

Backend checks were read-only transactions using authenticated Owner session claims. The active-account guard, vw_task_details query and get_historical_salary query all completed successfully. No attendance, payroll or access-policy changes were needed.

APK verified: id.biz.yuka.performax, versionCode 726, versionName 7.2.6-pxn, arm64-v8a. SHA-256: 04bf96487bff2d56a79021f011ba21303fa68f5b5460b506a1bc7cf76969bd22. Certificate SHA-256: c70e5e6c68afc56de2032ae9794f1940c2833e86393748836d34b7999a462a23 (existing signer).
