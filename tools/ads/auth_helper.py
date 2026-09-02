#!/usr/bin/env python
"""
Google Ads OAuth2 Refresh Token Alma Yardımcısı
Kullanıcının Client ID ve Client Secret ile kolayca Refresh Token üretmesini sağlar.
"""

import sys
import os
from pathlib import Path

# Windows Terminal UTF-8 uyumluluğu
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

def generate_refresh_token():
    print("=" * 70)
    print("       GOOGLE ADS API - OAUTH2 YETKİLENDİRME YARDIMCISI")
    print("=" * 70)
    print("\nBu araç, Google Cloud Console'dan aldığınız Client ID ve Client Secret")
    print("bilgilerini kullanarak bir 'Refresh Token' üretmenizi sağlar.\n")

    client_id = input("1. OAuth Client ID girin: ").strip()
    if not client_id:
        print("[!] Client ID boş bırakılamaz.")
        return

    client_secret = input("2. OAuth Client Secret girin: ").strip()
    if not client_secret:
        print("[!] Client Secret boş bırakılamaz.")
        return

    developer_token = input("3. Developer Token girin: ").strip()
    customer_id = input("4. Google Ads Customer ID (10 haneli): ").strip().replace("-", "")

    try:
        from google_auth_oauthlib.flow import InstalledAppFlow
    except ImportError:
        print("\n[!] Gerekli yetkilendirme paketi bulunamadı.")
        print("    Lütfen şu komutu çalıştırın: pip install google-auth-oauthlib")
        return

    client_config = {
        "installed": {
            "client_id": client_id,
            "client_secret": client_secret,
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "redirect_uris": ["urn:ietf:wg:oauth:2.0:oob", "http://localhost:8080/"]
        }
    }

    scopes = ["https://www.googleapis.com/auth/adwords"]

    print("\n[*] Tarayıcınızda Google oturum açma sayfası açılacak...")
    print("    Lütfen Google Ads hesabınızın bağlı olduğu Google hesabını seçin ve izin verin.\n")

    try:
        flow = InstalledAppFlow.from_client_config(client_config, scopes=scopes)
        # Yerel sunucu ile otomatik yakalama
        credentials = flow.run_local_server(port=8080, prompt="consent", access_type="offline")
        refresh_token = credentials.refresh_token

        print("\n" + "=" * 70)
        print("[✓] Yetkilendirme Başarılı! Refresh Token Üretildi.")
        print("=" * 70)
        print(f"Refresh Token: {refresh_token}")

        # Otomatik google-ads.yaml oluşturma
        target_path = Path(__file__).parent / "config" / "google-ads.yaml"
        yaml_content = f"""# Google Ads API Yapılandırması (Otomatik Oluşturuldu)
developer_token: "{developer_token}"
client_id: "{client_id}"
client_secret: "{client_secret}"
refresh_token: "{refresh_token}"
customer_id: "{customer_id}"
use_proto_plus: True
"""
        with open(target_path, "w", encoding="utf-8") as f:
            f.write(yaml_content)

        print(f"\n[✓] Yapılandırma dosyası otomatik kaydedildi: {target_path}")
        print("Artık 'python tools/ads/ads_cli.py test-connection' komutunu çalıştırabilirsiniz!")

    except Exception as e:
        print(f"\n[X] Yetkilendirme sırasında hata oluştu: {e}")

if __name__ == "__main__":
    generate_refresh_token()
