# PerformaX 7.2.3 (723)

Employee Home starts a visible, pausable foreground location service. Fresh GPS fixes are uploaded at a target interval of 15–30 seconds, including when stationary. The owner map polls every 10 seconds while visible. Actual latency depends on GPS, network and Android execution; force-stop and offline devices cannot provide live updates.

- Home and the ongoing notification expose tracking status and Pause. Paused tracking stays paused until Activate is pressed. Logout stops tracking.
- FCM accepts both deployed refresh commands, queues background-permitted one-time work, and skips duplicate work while continuous tracking runs. Periodic work remains a best-effort fallback.
- Token rotation schedules durable token synchronization; jobs await session restoration.
- Latest location is queried per active employee so frequent reporters cannot hide other employees behind the history response limit.
- Old, unknown and future-dated observations are clearly labeled. Failed refreshes retain the last coordinates and never manufacture a newer observation time.

Install on the owner and employee devices, open the employee app after upgrading, enable precise location and notification permissions, then confirm the ongoing location notification. Allow background location for fallback jobs. Use the recovered 7.2.2 signer; do not generate a replacement key.

Device acceptance still required: verify moving and stationary updates, screen off, network loss/recovery, Pause persistence, logout/account switch and Android battery restrictions on actual employee phones. No production database integration tests are run by the build.
