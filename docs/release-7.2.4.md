# PerformaX 7.2.4 (724)

Employee tracking starts automatically on Home and resumes when the signed-in app returns to the foreground. Remove Activate/Pause controls from Home and the ongoing notification. Legacy 7.2.3 pause flags are no longer consulted. Tracking status and the ongoing location notification remain visible; Android permission and process restrictions still apply. Logout still stops tracking. Update both owner and employee installations using the existing 7.2.2/7.2.3 signer.

This change does not bypass revoked permissions, force-stop, offline devices or disabled GPS. Physical-device background tracking verification remains required.
