import requests

base_url = "http://localhost:8080"
timeout = 30
headers = {"Content-Type": "application/json"}

def test_putapirestrictionspackagename_should_update_existing_restriction():
    # Create a new restriction first (POST /api/restrictions)
    create_payload = {
        "packageName": "com.example.testapp",
        "dailyLimit": 60
    }
    try:
        create_resp = requests.post(f"{base_url}/api/restrictions", json=create_payload, headers=headers, timeout=timeout)
        assert create_resp.status_code == 201, f"Setup failed: Expected status 201, got {create_resp.status_code}"
        
        # Update the daily limit for the existing restriction (PUT /api/restrictions/{packageName})
        update_payload = {
            "dailyLimit": 120
        }
        put_resp = requests.put(f"{base_url}/api/restrictions/{create_payload['packageName']}", json=update_payload, headers=headers, timeout=timeout)
        assert put_resp.status_code == 200, f"Expected status 200 on update, got {put_resp.status_code}"
        
        # Verify the update by fetching the restriction (GET /api/restrictions/{packageName})
        get_resp = requests.get(f"{base_url}/api/restrictions/{create_payload['packageName']}", headers=headers, timeout=timeout)
        assert get_resp.status_code == 200, f"Expected status 200 on get, got {get_resp.status_code}"
        
        restriction = get_resp.json()
        assert restriction.get("packageName") == create_payload["packageName"], "Returned packageName does not match"
        assert restriction.get("dailyLimit") == 120, "Daily limit was not updated correctly"
    finally:
        # Clean up - delete the created restriction (DELETE /api/restrictions/{packageName})
        requests.delete(f"{base_url}/api/restrictions/{create_payload['packageName']}", headers=headers, timeout=timeout)

test_putapirestrictionspackagename_should_update_existing_restriction()