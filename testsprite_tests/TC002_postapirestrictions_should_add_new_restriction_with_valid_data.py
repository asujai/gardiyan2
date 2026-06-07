import requests

BASE_URL = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
TIMEOUT = 30

def test_postapirestrictions_should_add_new_restriction_with_valid_data():
    import uuid
    package_name = f"com.example.testapp.{uuid.uuid4().hex[:8]}"
    daily_limit = 120  # in minutes, arbitrary valid number
    
    payload = {
        "packageName": package_name,
        "dailyLimit": daily_limit
    }
    
    try:
        # POST a new restriction
        response = requests.post(f"{BASE_URL}/api/restrictions", json=payload, headers=HEADERS, timeout=TIMEOUT)
        assert response.status_code == 201, f"Expected status 201, got {response.status_code}"
        
        # Verify the new restriction appears in the list with GET
        get_response = requests.get(f"{BASE_URL}/api/restrictions", headers=HEADERS, timeout=TIMEOUT)
        assert get_response.status_code == 200, f"Expected status 200 on GET, got {get_response.status_code}"
        restrictions = get_response.json()
        # Check if the new package is in the restrictions list
        assert any(r.get("packageName") == package_name and r.get("dailyLimit") == daily_limit for r in restrictions), \
            "New restriction not found in the returned list"
    finally:
        # Cleanup: delete the restriction added
        try:
            requests.delete(f"{BASE_URL}/api/restrictions/{package_name}", headers=HEADERS, timeout=TIMEOUT)
        except Exception:
            pass

test_postapirestrictions_should_add_new_restriction_with_valid_data()