import requests

BASE_URL = "http://localhost:8080"
TIMEOUT = 30
HEADERS = {"Content-Type": "application/json"}


def test_postapiblockingcheck_should_indicate_block_needed_when_limit_reached():
    url = f"{BASE_URL}/api/blocking/check"
    # Payload to represent usage exceeding daily limit - assumed schema based on description
    payload = {
        "package": "com.example.blockedapp",
        "usageMinutesToday": 125,  # Usage exceeding limit (e.g., limit could be 120)
        "dailyLimitMinutes": 120
    }
    try:
        response = requests.post(url, json=payload, headers=HEADERS, timeout=TIMEOUT)
        response.raise_for_status()
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"

    assert response.status_code == 200, f"Expected status code 200 but got {response.status_code}"

    try:
        json_response = response.json()
    except ValueError:
        assert False, "Response is not valid JSON"

    # The response should indicate block is needed; assuming a boolean field "blockNeeded": true
    assert "blockNeeded" in json_response, "'blockNeeded' key not in response JSON"
    assert json_response["blockNeeded"] is True, "blockNeeded should be True when limit reached"


test_postapiblockingcheck_should_indicate_block_needed_when_limit_reached()