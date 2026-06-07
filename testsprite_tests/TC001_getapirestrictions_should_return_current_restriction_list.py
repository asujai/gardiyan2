import requests

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

def test_getapirestrictions_should_return_current_restriction_list():
    url = f"{BASE_URL}/api/restrictions"
    headers = {
        "Accept": "application/json"
    }
    try:
        response = requests.get(url, headers=headers, timeout=TIMEOUT)
        response.raise_for_status()
        assert response.status_code == 200
        json_data = response.json()
        assert isinstance(json_data, list) or isinstance(json_data, dict)
    except requests.RequestException as e:
        assert False, f"Request failed: {e}"

test_getapirestrictions_should_return_current_restriction_list()