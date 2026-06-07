import requests
import time

BASE_URL = "http://localhost:8080"
TIMEOUT = 30
HEADERS = {"Content-Type": "application/json"}


def test_postapiunblockstart_and_confirm_should_unlock_after_five_second_hold():
    unblock_start_url = f"{BASE_URL}/api/unblock/start"
    unblock_confirm_url = f"{BASE_URL}/api/unblock/confirm"

    # Start the unblock process (five-second hold timer starts)
    try:
        resp_start = requests.post(unblock_start_url, headers=HEADERS, timeout=TIMEOUT)
        assert resp_start.status_code == 200, f"Expected status 200 for unblock start, got {resp_start.status_code}"

        # Hold for 5 seconds as required by the protected unlock mechanism
        time.sleep(5)

        # Confirm the unblock after hold
        resp_confirm = requests.post(unblock_confirm_url, headers=HEADERS, timeout=TIMEOUT)
        assert resp_confirm.status_code == 200, f"Expected status 200 for unblock confirm, got {resp_confirm.status_code}"

        # Optionally verify response content if any (not specified)

    except requests.RequestException as e:
        assert False, f"Request to unblock API failed: {e}"


test_postapiunblockstart_and_confirm_should_unlock_after_five_second_hold()