# Limitra: AppBlock - Google Ads Otomasyon ve Yönetim Altyapısı

Bu altyapı, **Limitra: AppBlock** (`com.gardiyan.app`) mobil uygulamasının Google Ads üzerindeki **Uygulama Yükleme Kampanyalarını (Universal App Campaigns - UAC)** otomatik yönetmek, metrikleri çekmek ve optimize etmek için hazırlanmıştır.

---

## 1. Güvenlik ve Gizlilik Kuralları (ÖNEMLİ)

1. **GitHub Depo Görünürlüğü:**
   - Depo **Public** ise veya gizli anahtarlar yüklenirse `.gitignore` kuralları devrededir.
   - `google-ads.yaml`, `credentials.json`, `client_secret*.json`, `*.secret.json` dosyaları `.gitignore`'a eklenmiştir ve kesinlikle Git'e **gönderilmez**.
2. **Onaysız Reklam Açılmaz:**
   - `ads_cli.py create` komutu `--confirm` bayrağı olmadan işlem yapmaz.
   - Oluşturulan tüm kampanyalar ilk olarak **PAUSED (Duraklatılmış)** olarak açılır, sen onaylayıp `status-set --status ENABLED` diyene kadar para harcamaz.

---

## 2. Gereksinimler ve Kurulum

Gerekli Python paketlerini yükleyin:

```bash
pip install -r tools/ads/requirements.txt
```

---

## 3. Google Ads API Kimlik Bilgilerini Alma (Tek Seferlik)

1. **Google Cloud Console'da Proje Açın:**
   - [Google Cloud Console](https://console.cloud.google.com)'a gidin.
   - Yeni bir proje oluşturun ve **Google Ads API**'yi etkinleştirin.
   - **OAuth Consent Screen** (OAuth İzin Ekranı) yapılandırın (User Type: External/Harici veya Internal).
   - **Credentials (Kimlik Bilgileri)** -> **Create Credentials** -> **OAuth client ID** (Application type: *Desktop App* veya *Web Application*) oluşturup `Client ID` ve `Client Secret` değerlerini alın.

2. **Google Ads Developer Token Alın:**
   - [Google Ads](https://ads.google.com) Yönetici (MCC) hesabınıza giriş yapın.
   - `Araçlar ve Ayarlar` -> `Kurulum` -> `API Merkezi` bölümünden **Developer Token** alın (Test veya Temel erişim).

3. **Google Play Console ile Ads Hesabını Bağlayın:**
   - Google Ads panelinde `Araçlar ve Ayarlar` -> `Bağlı Hesaplar` -> `Google Play` seçeneğine tıklayın.
   - `com.gardiyan.app` uygulamanızı bağlayın. (Bu sayede yüklemeler ve dönüşümler otomatik eşleşir).

4. **Yapılandırma Dosyasını Hazırlayın:**
   - `tools/ads/config/google-ads.yaml.example` dosyasını `tools/ads/config/google-ads.yaml` olarak kopyalayın ve bilgilerinizi doldurun.

---

## 4. Kullanılabilir Komutlar

### A. Kampanya Planlarını ve Metinlerini İnceleme
Kampanya taslaklarını, 5 başlığı, 5 açıklamayı ve bütçeleri görmek için:
```bash
python tools/ads/ads_cli.py plan
```
Belirli bir şablon için:
```bash
python tools/ads/ads_cli.py plan --template-id limitra_tr_focus_v1
```

### B. Özel Bir Kampanya JSON Dosyasını Doğrulama
```bash
python tools/ads/ads_cli.py validate --file tools/ads/config/campaign_templates.json
```

### C. API Bağlantısını Test Etme
```bash
python tools/ads/ads_cli.py test-connection
```

### D. Kampanya Oluşturma (Onaylı)
```bash
python tools/ads/ads_cli.py create --template limitra_tr_focus_v1 --confirm
```

### E. Kampanya Durumunu Değiştirme (Yayına Alma / Durdurma)
```bash
# Yayına almak için:
python tools/ads/ads_cli.py status-set --campaign-id 123456789 --status ENABLED

# Duraklatmak için:
python tools/ads/ads_cli.py status-set --campaign-id 123456789 --status PAUSED
```

### F. Performans ve Harcama Raporu
```bash
python tools/ads/ads_cli.py status
```
Bu komut; gösterim, tıklama, yükleme (install), toplam harcama ve gerçekleşen indirme başı maliyeti (CPI) tablosunu ekrana basar.
