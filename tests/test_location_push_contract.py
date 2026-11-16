"""Source contract check; does not simulate FCM delivery or Android services."""
from pathlib import Path
import re
import unittest
ROOT = Path(__file__).resolve().parents[1]
SENDER = ROOT / 'lib-performx/src/main/java/id/biz/yuka/libperformx/utils/LocationRefreshHelper.kt'
RECEIVER = ROOT / 'app/src/main/java/id/biz/yuka/performax/service/MyFirebaseMessagingService.kt'
class LocationPushContractTest(unittest.TestCase):
    def test_sender_is_compatible_with_shipped_receivers(self):
        sent = re.search(r'put\("type", "([A-Z_]+)"\)', SENDER.read_text()).group(1)
        self.assertEqual('REQUEST_LIVE_LOCATION', sent)
    def test_receiver_accepts_both_deployed_command_names(self):
        accepted = set(re.findall(r'incomingType == "([A-Z_]+)"', RECEIVER.read_text()))
        self.assertTrue({'REQUEST_LOCATION', 'REQUEST_LIVE_LOCATION'} <= accepted)
if __name__ == '__main__':
    unittest.main()
