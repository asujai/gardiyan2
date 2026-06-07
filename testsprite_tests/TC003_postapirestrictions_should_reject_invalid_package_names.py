import requests

BASE_URL = "http://localhost:8080"
TIMEOUT = 30
HEADERS = {"Content-Type": "application/json"}

def test_postapirestrictions_should_reject_invalid_package_names():
    # Get the current list of restrictions to compare later
    get_url = f"{BASE_URL}/api/restrictions"
    try:
        get_resp_before = requests.get(get_url, timeout=TIMEOUT)
        get_resp_before.raise_for_status()
        restrictions_before = get_resp_before.json()
    except Exception as e:
        assert False, f"Failed to get current restrictions before test: {e}"

    # Test invalid package names - empty string and invalid formats
    invalid_payloads = [
        {"packageName": "", "dailyLimit": 60},
        {"packageName": "   ", "dailyLimit": 60},
        {"packageName": "!@#$%^&*()", "dailyLimit": 60},
        {"packageName": "com.example.app!", "dailyLimit": 60},
    ]

    post_url = f"{BASE_URL}/api/restrictions"

    for payload in invalid_payloads:
        resp = requests.post(post_url, json=payload, headers=HEADERS, timeout=TIMEOUT)
        assert resp.status_code == 400, \
            f"Expected status 400 for payload {payload}, got {resp.status_code}"

        # Optionally, verify response content for validation error keys if present
        try:
            err_json = resp.json()
            assert "error" in err_json or "message" in err_json or "validation" in err_json, \
                f"Response JSON missing expected error info: {err_json}"
        except Exception:
            # If not JSON, ignore as presence of 400 is mandatory
            pass

        # Confirm the restriction list was not changed
        try:
            get_resp_after = requests.get(get_url, timeout=TIMEOUT)
            get_resp_after.raise_for_status()
            restrictions_after = get_resp_after.json()
            assert restrictions_before == restrictions_after, \
                f"Restriction list changed after invalid POST {payload}"
        except Exception as e:
            assert False, f"Failed to get current restrictions after test: {e}"

test_postapirestrictions_should_reject_invalid_package_names()
