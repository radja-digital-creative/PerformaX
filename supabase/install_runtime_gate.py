"""Apply the live-session gate to an existing self-hosted Edge main router.

Run with the functions directory after backing up main/index.ts. Refuses an
unknown router shape. Contains no credentials; runtime reads its own env.
"""
import pathlib, sys

root = pathlib.Path(sys.argv[1])
main = root / 'main/index.ts'
source = main.read_text()
if 'const sessionDenied = await requireLiveSession(req)' not in source:
    anchor = '  const url = new URL(req.url)'
    if source.count(anchor) != 1:
        raise SystemExit('Unrecognized Edge router; no changes made')
    source = "import { requireLiveSession } from '../_shared/live-session.ts'\n" + source
    source = source.replace(anchor, '  const sessionDenied = await requireLiveSession(req)\n  if (sessionDenied) return sessionDenied\n\n' + anchor)
    main.write_text(source)
print('Runtime session gate installed')
