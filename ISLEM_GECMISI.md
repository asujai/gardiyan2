# İŞLEM GEÇMİŞİ

## [2026-08-21 00:38] - Play Console CLI (gpc) Entegrasyonu ve Kurulumu

* **Model:** Antigravity
* **Etkilenen Dosyalar:** `[YENİ]` `AGENTS.md`, `SON_DURUM.md`, `ISLEM_GECMISI.md`, `[GÜNCELLENDİ]` `.gitignore`
* **Yapılan İşlem:** `playconsole-cli` (gpc) v0.5.15 indirildi, Windows PATH'e eklendi. Service account yetkilendirmesi `com.gardiyan.app` için yapılandırıldı. Güvenlik anahtarları `.gitignore` kapsamına alındı.
* **Doğrulama:** `gpc doctor`, `gpc tracks list`, `gpc listings list`, `gpc bundles list` komutlarıyla Google Play Developer API erişimi başarıyla test edildi. Canlıdaki Production v14 ve mağaza bilgileri çekildi.
* **Bilinen Sorunlar:** Reporting API (vitals/crash) için Google Cloud üzerinde ilgili API'nin tek tıkla açılması önerildi.
* **Sonraki Öneri:** Yeni sürüm dağıtımı veya mağaza metinleri güncellemeleri doğrudan `gpc` ile yürütülebilir.
