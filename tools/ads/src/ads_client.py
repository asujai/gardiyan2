import os
from pathlib import Path
from typing import Optional, Dict, Any

class AdsClientProvider:
    """
    Google Ads API İstemci Yöneticisi
    Yapılandırma dosyasını yükler ve GoogleAdsClient örneği oluşturur.
    """
    
    DEFAULT_CONFIG_LOCATIONS = [
        Path(__file__).parent.parent / "config" / "google-ads.yaml",
        Path(__file__).parent.parent / "google-ads.yaml",
        Path.cwd() / "google-ads.yaml",
        Path.home() / "google-ads.yaml",
    ]

    def __init__(self, config_path: Optional[str] = None):
        self.config_path = self._resolve_config_path(config_path)
        self._client = None

    def _resolve_config_path(self, custom_path: Optional[str]) -> Optional[Path]:
        if custom_path:
            p = Path(custom_path)
            if p.exists():
                return p
            raise FileNotFoundError(f"Belirtilen Google Ads yapılandırma dosyası bulunamadı: {custom_path}")

        for loc in self.DEFAULT_CONFIG_LOCATIONS:
            if loc.exists():
                return loc
        return None

    def is_configured(self) -> bool:
        """Yapılandırma dosyasının var olup olmadığını kontrol eder."""
        return self.config_path is not None and self.config_path.exists()

    def get_config_data(self) -> Dict[str, Any]:
        """Yapılandırma dosyasını okur."""
        if not self.is_configured():
            return {}
        try:
            import yaml
            with open(self.config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f) or {}
        except ImportError:
            # Basit key: value okuma fallback
            data = {}
            with open(self.config_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and ":" in line:
                        k, v = line.split(":", 1)
                        data[k.strip()] = v.strip().strip('"').strip("'")
            return data

    def get_customer_id(self) -> Optional[str]:
        """Yapılandırmadaki hedef customer_id değerini döner."""
        cfg = self.get_config_data()
        raw_id = cfg.get("customer_id")
        if raw_id:
            return str(raw_id).replace("-", "").strip()
        return None

    def get_client(self):
        """
        GoogleAdsClient nesnesini döndürür.
        """
        if self._client is not None:
            return self._client

        if not self.is_configured():
            raise FileNotFoundError(
                "Google Ads yapılandırma dosyası (google-ads.yaml) bulunamadı!\n"
                "Lütfen 'tools/ads/config/google-ads.yaml.example' dosyasını 'google-ads.yaml' olarak kopyalayın "
                "ve Developer Token, OAuth bilgilerinizi girin."
            )

        try:
            from google.ads.googleads.client import GoogleAdsClient
        except ImportError:
            raise ImportError(
                "'google-ads' Python paketi yüklü değil.\n"
                "Lütfen şu komutla yükleyin: pip install -r tools/ads/requirements.txt"
            )

        self._client = GoogleAdsClient.load_from_storage(str(self.config_path))
        return self._client

    def test_connection(self) -> Dict[str, Any]:
        """
        Google Ads API bağlantısını test eder ve müşteri hesap bilgisini sorgular.
        """
        client = self.get_client()
        customer_id = self.get_customer_id()
        if not customer_id:
            raise ValueError("google-ads.yaml içinde 'customer_id' belirtilmemiş!")

        ga_service = client.get_service("GoogleAdsService")
        query = """
            SELECT
                customer.id,
                customer.descriptive_name,
                customer.currency_code,
                customer.time_zone,
                customer.status
            FROM customer
            LIMIT 1
        """
        
        response = ga_service.search(customer_id=customer_id, query=query)
        for row in response:
            return {
                "success": True,
                "customer_id": row.customer.id,
                "descriptive_name": row.customer.descriptive_name,
                "currency_code": row.customer.currency_code,
                "time_zone": row.customer.time_zone,
                "status": row.customer.status.name,
            }
        return {"success": False, "message": "Müşteri bilgisi alınamadı."}
