import requests

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

def test_getapiusagecurrent_should_return_403_when_permissions_missing():
    url = f"{BASE_URL}/api/usage/current"
    try:
        response = requests.get(url, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"
    assert response.status_code == 403, f"Expected status 403 but got {response.status_code}"

test_getapiusagecurrent_should_return_403_when_permissions_missing()