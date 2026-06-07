import requests

BASE_URL = "http://localhost:8080"
TIMEOUT = 30
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

def test_post_api_blocking_show_should_display_overlay_when_permission_granted():
    # Step 1: Verify overlay permission is granted by trying to GET /api/permissions/overlay
    permission_url = f"{BASE_URL}/api/permissions/overlay"
    try:
        perm_resp = requests.get(permission_url, headers=HEADERS, timeout=TIMEOUT)
        perm_resp.raise_for_status()
    except requests.RequestException as e:
        raise AssertionError(f"Failed to verify overlay permission before test: {e}")

    # Assert permission granted - expecting 200 OK
    assert perm_resp.status_code == 200, f"Overlay permission not granted, status code {perm_resp.status_code}"

    # Step 2: Call POST /api/blocking/show to display the blocking overlay
    blocking_show_url = f"{BASE_URL}/api/blocking/show"
    try:
        response = requests.post(blocking_show_url, headers=HEADERS, timeout=TIMEOUT)
        response.raise_for_status()
    except requests.HTTPError as e:
        # If 403, permission is not granted, test fails
        if response.status_code == 403:
            raise AssertionError("Overlay permission not granted - access forbidden (403) when posting to /api/blocking/show")
        else:
            raise AssertionError(f"HTTP error when posting to /api/blocking/show: {e}")
    except requests.RequestException as e:
        raise AssertionError(f"Request failed when posting to /api/blocking/show: {e}")

    # Assert HTTP status 200 OK
    assert response.status_code == 200, f"Expected status 200 from /api/blocking/show but got {response.status_code}"

    # Optionally, verify response content if any overlay status or message is expected
    # Since the PRD does not specify response body, only check status code

test_post_api_blocking_show_should_display_overlay_when_permission_granted()