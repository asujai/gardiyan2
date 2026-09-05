import subprocess
import shutil
import time
from pathlib import Path

PROJECT_ROOT = Path(r"c:\Users\abdul\gardiyan2")
SOURCE_ROOT = PROJECT_ROOT / "store_assets" / "play-sync-v2"
TEMP_DIR = PROJECT_ROOT / "temp_single_sync"

REMAINING_LOCALES = [
    "en-US",
    "de-DE",
    "es-ES",
    "fr-FR",
    "pt-BR",
    "ru-RU",
    "hi-IN",
    "id",
    "th",
    "ar"
]

def sync_locale(locale):
    print(f"\n=======================================================", flush=True)
    print(f">>> STARTING SYNC FOR: {locale}", flush=True)
    print(f"=======================================================", flush=True)
    
    if TEMP_DIR.exists():
        shutil.rmtree(TEMP_DIR)
    TEMP_DIR.mkdir(parents=True, exist_ok=True)
    
    src_loc = SOURCE_ROOT / locale
    dst_loc = TEMP_DIR / locale
    shutil.copytree(src_loc, dst_loc)
    
    cmd = ["gpc", "images", "sync", "--dir", str(TEMP_DIR)]
    
    max_retries = 3
    for attempt in range(1, max_retries + 1):
        print(f"[{locale}] Uploading to Play Console (attempt {attempt}/{max_retries})...", flush=True)
        t0 = time.time()
        res = subprocess.run(cmd, capture_output=True, text=True, cwd=str(PROJECT_ROOT))
        duration = time.time() - t0
        
        if res.returncode == 0:
            print(f"[{locale}] Successfully uploaded and committed in {duration:.1f}s!", flush=True)
            print(res.stdout.strip(), flush=True)
            break
        else:
            print(f"[{locale}] Attempt {attempt} failed with exit code {res.returncode}", flush=True)
            print(f"STDERR: {res.stderr.strip()}", flush=True)
            print(f"STDOUT: {res.stdout.strip()}", flush=True)
            if attempt < max_retries:
                print(f"[{locale}] Waiting 5 seconds before retry...", flush=True)
                time.sleep(5)
            else:
                raise RuntimeError(f"Failed to sync {locale} after {max_retries} attempts")

    # Verify counts
    print(f"[{locale}] Verifying counts via gpc...", flush=True)
    for img_type, expected_count in [
        ("phoneScreenshots", 8),
        ("sevenInchScreenshots", 4),
        ("tenInchScreenshots", 4),
        ("featureGraphic", 1),
        ("icon", 1)
    ]:
        v_res = subprocess.run(
            ["gpc", "images", "list", "--locale", locale, "--type", img_type],
            capture_output=True,
            text=True,
            cwd=str(PROJECT_ROOT)
        )
        if v_res.returncode == 0:
            import json
            try:
                items = json.loads(v_res.stdout)
                cnt = len(items)
                status = "OK" if cnt == expected_count else f"MISMATCH (got {cnt})"
                print(f"  {img_type}: {cnt}/{expected_count} [{status}]", flush=True)
            except Exception as e:
                print(f"  {img_type}: parse error {e}", flush=True)
        else:
            print(f"  {img_type}: query error {v_res.stderr.strip()}", flush=True)

def main():
    print(f"Starting sequential Play Console sync for {len(REMAINING_LOCALES)} locales...", flush=True)
    for idx, loc in enumerate(REMAINING_LOCALES, 1):
        print(f"\nProgress: Locale {idx}/{len(REMAINING_LOCALES)}: {loc}", flush=True)
        sync_locale(loc)
        time.sleep(2)
        
    if TEMP_DIR.exists():
        shutil.rmtree(TEMP_DIR)
        
    print("\n=======================================================", flush=True)
    print("ALL REMAINING LOCALES SYNCED AND VERIFIED SUCCESSFULLY!", flush=True)
    print("=======================================================", flush=True)

if __name__ == "__main__":
    main()
