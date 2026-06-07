import requests
import uuid

BASE_URL = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
TIMEOUT = 30

def test_deleteapirestrictionspackagename_should_remove_restriction():
    package_name = f"test.package.{uuid.uuid4().hex[:8]}"
    post_url = f"{BASE_URL}/api/restrictions"
    delete_url = f"{BASE_URL}/api/restrictions/{package_name}"

    # Create a restriction to ensure it exists before deletion
    create_payload = {
        "packageName": package_name,
        "dailyLimitMinutes": 60
    }

    try:
        post_resp = requests.post(post_url, json=create_payload, headers=HEADERS, timeout=TIMEOUT)
        assert post_resp.status_code == 201, f"Setup failed, could not add restriction: {post_resp.text}"

        # Now delete the restriction
        delete_resp = requests.delete(delete_url, headers=HEADERS, timeout=TIMEOUT)
        assert delete_resp.status_code == 200, f"Deletion failed: {delete_resp.text}"

        # Verify restriction no longer exists by getting the list and checking
        get_resp = requests.get(f"{BASE_URL}/api/restrictions", headers=HEADERS, timeout=TIMEOUT)
        assert get_resp.status_code == 200, f"Failed to get restrictions: {get_resp.text}"
        restrictions = get_resp.json()
        assert all(r.get("packageName") != package_name for r in restrictions), "Restriction still present after deletion"

    finally:
        # Cleanup: just in case delete did not succeed above
        requests.delete(delete_url, headers=HEADERS, timeout=TIMEOUT)

test_deleteapirestrictionspackagename_should_remove_restriction()