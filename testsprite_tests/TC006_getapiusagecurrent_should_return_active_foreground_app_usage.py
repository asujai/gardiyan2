import requests

BASE_URL = "http://localhost:8080"

def test_getapiusagecurrent_should_return_active_foreground_app_usage():
    url = f"{BASE_URL}/api/usage/current"
    headers = {
        "Accept": "application/json",
    }
    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
    except requests.exceptions.RequestException as e:
        assert False, f"Request failed: {e}"
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"
    data = response.json()
    # Validate the expected fields in response
    assert "packageName" in data, "Response JSON missing 'packageName'"
    assert isinstance(data["packageName"], str) and data["packageName"], "'packageName' should be a non-empty string"
    assert "usageState" in data, "Response JSON missing 'usageState'"
    assert isinstance(data["usageState"], str) and data["usageState"], "'usageState' should be a non-empty string"

# Execute the test case
test_getapiusagecurrent_should_return_active_foreground_app_usage()