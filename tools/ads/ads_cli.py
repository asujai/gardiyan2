#!/usr/bin/env python
"""
Limitra (AppBlock) Google Ads Yönetim CLI Aracı
Google Ads App Campaigns (UAC) oluşturma, planlama ve raporlama aracı.
"""

import sys
import os
import json
import argparse
from pathlib import Path

# Modülleri içe aktarabilmek için path ayarı
sys.path.insert(0, str(Path(__file__).parent))

# Windows Terminal UTF-8 uyumluluğu
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

from src.ads_client import AdsClientProvider
from src.app_campaign_manager import AppCampaignManager, AppCampaignPlan
from src.reporting import AdsReporter

TEMPLATES_FILE = Path(__file__).parent / "config" / "campaign_templates.json"


def load_templates():
    if not TEMPLATES_FILE.exists():
        print(f"[!] Şablon dosyası bulunamadı: {TEMPLATES_FILE}")
        return []
    with open(TEMPLATES_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
        return data.get("templates", [])


def cmd_plan(args):
    """Kampanya planlarını ve reklam metinlerini listeler."""
    templates = load_templates()
    if not templates:
        print("Kayıtlı kampanya şablonu bulunamadı.")
        return

    provider = AdsClientProvider()
    manager = AppCampaignManager(provider)

    template_id = args.template_id
    selected = [t for t in templates if t["id"] == template_id] if template_id else templates

    if not selected:
        print(f"[!] '{template_id}' id'sine sahip şablon bulunamadı.")
        print("Mevcut şablonlar:", ", ".join([t["id"] for t in templates]))
        return

    for t in selected:
        plan = AppCampaignPlan(**t)
        print(manager.generate_plan_summary(plan))
        print("-> Bu kampanyayı API ile oluşturmak için:")
        print(f"   python tools/ads/ads_cli.py create --template {t['id']} --confirm\n")


def cmd_validate(args):
    """Verilen JSON dosyasını veya şablonu Google Ads kurallarına göre doğrular."""
    file_path = Path(args.file)
    if not file_path.exists():
        print(f"[!] Dosya bulunamadı: {file_path}")
        sys.exit(1)

    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    try:
        items = data.get("templates", [data]) if isinstance(data, dict) and "templates" in data else ([data] if isinstance(data, dict) else data)
        for item in items:
            plan = AppCampaignPlan(**item)
            print(f"[✓] Kampanya Planı Geçerli: '{plan.name}'")
            print(f"    Başlık sayısı: {len(plan.headlines)} (Tümü <= 30 karakter)")
            print(f"    Açıklama sayısı: {len(plan.descriptions)} (Tümü <= 90 karakter)")
            print(f"    Hedef Uygulama: {plan.app_id}")
    except Exception as e:
        print(f"[X] Doğrulama Hatası:\n{e}")
        sys.exit(1)


def cmd_test_connection(args):
    """Google Ads API bağlantısını ve kimlik bilgilerini test eder."""
    provider = AdsClientProvider(args.config)
    print("[*] Google Ads yapılandırması kontrol ediliyor...")
    
    if not provider.is_configured():
        print("[!] Yapılandırma dosyası (google-ads.yaml) bulunamadı.")
        print("    Lütfen 'tools/ads/config/google-ads.yaml.example' dosyasını kopyalayıp bilgilerinizi girin.")
        return

    try:
        res = provider.test_connection()
        if res.get("success"):
            print("\n[✓] Google Ads API Bağlantısı Başarılı!")
            print(f"    Müşteri ID    : {res['customer_id']}")
            print(f"    Hesap Adı     : {res.get('descriptive_name') or 'Belirtilmemiş'}")
            print(f"    Para Birimi   : {res.get('currency_code')}")
            print(f"    Saat Dilimi   : {res.get('time_zone')}")
            print(f"    Hesap Durumu  : {res.get('status')}")
        else:
            print(f"[!] Bağlantı sağlanamadı: {res.get('message')}")
    except Exception as e:
        print(f"\n[X] Bağlantı Hatası: {e}")


def cmd_create(args):
    """Yeni bir uygulama kampanyası oluşturur (Onay gerektirir)."""
    if not args.confirm:
        print("[!] DİKKAT: Gerçek reklam kampanyası oluşturmak için lütfen komutun sonuna '--confirm' parametresini ekleyin.")
        print("    Örnek: python tools/ads/ads_cli.py create --template limitra_tr_focus_v1 --confirm")
        return

    templates = load_templates()
    template = next((t for t in templates if t["id"] == args.template), None)
    if not template:
        print(f"[!] '{args.template}' şablonu bulunamadı.")
        return

    plan = AppCampaignPlan(**template)
    provider = AdsClientProvider(args.config)
    manager = AppCampaignManager(provider)

    print("[*] Kampanya oluşturuluyor...")
    try:
        result = manager.create_campaign(plan)
        print("\n[✓] Kampanya başarıyla oluşturuldu!")
        print(f"    Kampanya ID   : {result['campaign_id']}")
        print(f"    Resource Name : {result['campaign_resource_name']}")
        print(f"    Durum         : {result['status']}")
        print("\nNot: Kampanya güvenlik amacıyla PAUSED (Duraklatılmış) olarak açıldı.")
        print(f"Yayına almak için: python tools/ads/ads_cli.py status-set --campaign-id {result['campaign_id']} --status ENABLED")
    except Exception as e:
        print(f"\n[X] Kampanya oluşturulurken hata meydana geldi: {e}")


def cmd_status(args):
    """Mevcut kampanyaları ve harcama/yükleme durumlarını listeler."""
    provider = AdsClientProvider(args.config)
    reporter = AdsReporter(provider)

    print("[*] Kampanyalar sorgulanıyor...\n")
    try:
        campaigns = reporter.get_campaign_summaries()
        print(reporter.format_campaign_table(campaigns))
    except Exception as e:
        print(f"[X] Rapor alınamadı: {e}")


def cmd_status_set(args):
    """Kampanyayı duraklatır (PAUSED) veya yayına alır (ENABLED)."""
    provider = AdsClientProvider(args.config)
    manager = AppCampaignManager(provider)

    try:
        manager.set_campaign_status(args.campaign_id, args.status)
        print(f"[✓] Kampanya ({args.campaign_id}) durumu '{args.status}' olarak güncellendi.")
    except Exception as e:
        print(f"[X] Durum güncellenemedi: {e}")


def main():
    parser = argparse.ArgumentParser(
        description="Limitra: AppBlock - Google Ads Otomasyon ve Yönetim Aracı",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    subparsers = parser.add_subparsers(dest="command", help="Kullanılabilir komutlar")

    # plan
    parser_plan = subparsers.add_parser("plan", help="Kampanya planlarını ve reklam metinlerini listeler")
    parser_plan.add_argument("--template-id", help="Belirli bir şablon ID'si")

    # validate
    parser_val = subparsers.add_parser("validate", help="Kampanya JSON dosyasını doğrular")
    parser_val.add_argument("--file", required=True, help="Doğrulanacak JSON dosyası")

    # test-connection
    parser_test = subparsers.add_parser("test-connection", help="Google Ads API bağlantısını test eder")
    parser_test.add_argument("--config", help="Özel google-ads.yaml dosya yolu")

    # create
    parser_create = subparsers.add_parser("create", help="Google Ads üzerinde yeni UAC kampanyası oluşturur")
    parser_create.add_argument("--template", required=True, help="Kullanılacak şablon ID")
    parser_create.add_argument("--confirm", action="store_true", help="Oluşturmayı onayla")
    parser_create.add_argument("--config", help="Özel google-ads.yaml dosya yolu")

    # status / report
    parser_status = subparsers.add_parser("status", help="Kampanyaları ve performans metriklerini listeler")
    parser_status.add_argument("--config", help="Özel google-ads.yaml dosya yolu")

    # status-set
    parser_set = subparsers.add_parser("status-set", help="Kampanya durumunu değiştirir (ENABLED, PAUSED, REMOVED)")
    parser_set.add_argument("--campaign-id", required=True, help="Hedef kampanya ID")
    parser_set.add_argument("--status", required=True, choices=["ENABLED", "PAUSED", "REMOVED"], help="Yeni durum")
    parser_set.add_argument("--config", help="Özel google-ads.yaml dosya yolu")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(0)

    if args.command == "plan":
        cmd_plan(args)
    elif args.command == "validate":
        cmd_validate(args)
    elif args.command == "test-connection":
        cmd_test_connection(args)
    elif args.command == "create":
        cmd_create(args)
    elif args.command == "status":
        cmd_status(args)
    elif args.command == "status-set":
        cmd_status_set(args)


if __name__ == "__main__":
    main()
