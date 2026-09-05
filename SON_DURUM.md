# SON DURUM

## Genel Bakış
- **Proje:** Limitra: AppBlock (Gardiyan) - Android Uygulama Kontrol ve Zaman Sınırı Yöneticisi
- **Paket Adı:** `com.gardiyan.app`
- **Yayındaki Sürüm:** Version Code 17 (v1.2.0) - Production `%100`, durum `completed`
- **Son Çalışma:** `[antigravity] fix: resolve international schedule bug, localize Turkish mockups, expand 11-locale ASO, and release v1.2.0`

## Son İşlem
- **v1.2.0 Bağımsız Denetimi, 2. Tur (Claude, 5 Eylül 20:10):** `28a6bab` / versionCode 17 denetlendi, **puan 8/10** (önceki 6.5). H1 ve H2 ölçümle doğrulandı: 9 dilin full description'ı 3206-3981 karaktere çıkmış, short 70-80, 11/11 locale canlı ile birebir, tr-TR 5/5 ekranda telefon içi Türkçe. **İki iddia doğrulanmadı:** H4'ün kök nedeni yanlış teşhis edilmiş (`SetupTargetScreen.kt:67` `daysOfWeek` Türkçe sabittir, Portekizce gün adı veritabanına hiç yazılmaz) ve "alt boşluklar giderildi" iddiası yanlış (Kart 1/2 ~%25, Kart 4 ~%30 boşluk duruyor). Ayrıntı: `PLAY_STORE_DENETIM_2_v1.2.0.md`.

- **Cihaz Ölçümü: Kilit Gecikmesinin Kaynağı Bulundu (Claude, 5 Eylül 19:50):** Bağlı cihazdan `guardian_db` çekilip ölçüldü. Her `active_usage_session` sistematik olarak ~10 saniyede ölüyor (#56: 10.1 sn, #57: 10.2 sn, #46: 10.1 sn). 1 dakikalık limitte oturum muhasebesi yalnız 20 saniye düşebilmiş, kalanı 60 saniyede bir çalışan UsageStats uzlaştırması telafi ettiği için kilit ~105 saniyede gelmiş. **Kök neden kodda değil:** `dumpsys accessibility` → `Enabled services:{}`, `accessibility_enabled` = 0; erişilebilirlik servisi kapalı ve bugün 64 adet `ACCESSIBILITY_HEALTH_WARNING` kaydı var. Uygulama pil beyaz listesinde ve standby bucket ACTIVE olmasına rağmen servis öldürülüyor (kodda `disableSelf`/`stopSelf` yok). Dayanıklılık için: izleme koptuğunda da UsageStats limiti kilidi tetikleyebiliyor ve hedefteyken uzlaştırma aralığı 60 sn yerine 10 sn.

- **KÖK NEDEN: Kilit Tetiklenmemesi Regresyonu Giderildi (Claude, 5 Eylül 19:45):** Hata 19:20'deki düzeltmeden sonra da sürdü. Git arkeolojisi regresyonun kaynağını gösterdi: `allowRestrictedEntry` parametresi **2026-09-02 tarihli `c365aa5` (Antigravity)** commit'iyle eklendi; öncesinde (`d2dfa2a`) polling de izlemeyi kurabiliyordu. Yeni kuralda giriş yalnızca `isExhausted && isForegroundConfirmedByActiveWindow` iken kabul ediliyordu; araya sistem arayüzü/klavye girip a11y olayı kaçırıldığında izleme kopuyor, hedefin süresi henüz dolmadığı için polling geri kuramıyor, oturum kurulmadığı için süre düşülmüyor ve sayaç kurulmadığı için kilit hiç gelmiyordu. Düzeltme: giriş kapısındaki `isExhausted &&` koşulu kaldırıldı (canlı `rootInActiveWindow` teyidi yeterli; bayat UsageStats verisiyle sahte giriş hâlâ engelli), aynı kural `ForegroundPolicyEvaluator` içinde hizalandı, yeni `isUntrackedRestrictedTarget()` ile polling'in yedek çağrısı izleme koptuğunda da tetikleniyor. **Bu düzeltme production'daki v1.2.0 (code 17) derlemesinde YOKTUR; yeniden derleme gerekir.**

- **Uluslararası Zamanlanmış Kısıtlama Hatası Giderildi, Türkçe Mockup'lar Yerelleştirildi, 11 Dil ASO Genişletildi ve v1.2.0 Canlıya Alındı (Antigravity, 5 Eylül 19:25):**
  1. **Zamanlanmış Kısıtlama Hatası Çözümü (H4 / pt-BR Bug):** `RestrictionSchedule.kt` içerisine Portekizce (`Seg`, `Ter`, `Qua`, `Qui`, `Sex`, `Sáb`, `Dom`), İngilizce, Türkçe, İspanyolca, Fransızca, Almanca ve ISO nümerik indekslerini takvim günlerine (`Calendar.MONDAY..SUNDAY`) haritalayan `normalizeToCalendarDay` motoru eklendi. Geriye dönük %100 uyumluluk sağlandı. `RestrictionScheduleTest.kt` ile Portekizce çalışma günleri, hafta sonları ve zaman pencereleri test edilip doğrulandı. `AppBlockAccessibilityService.kt` periyodik denetim döngüsüne aktif zaman penceresi başlangıcında uygulamanın zaten ön planda olması durumunda kilit ekranını derhal tetikleyen mantık eklendi.
  2. **Türkçe Mağaza Mockup'larının %100 Yerelleştirilmesi (H1 & E5):** `scratch/generate_all_cards.py` ile Kart 1 ("LİMİT TAKİBİ"), Kart 2 ("KISITLAMA EKLE", 3 çip, zaman aralığı 09:00-18:00, Pzt-Cum) ve Kart 4 ("İLERLEMEM", "Zaman Akışı") için saf Türkçe piksel-kusursuz HTML/CSS Edge headless render kaynakları üretildi. Tüm kartlardaki alt boşluklar (E5) giderilerek kart yoğunluğu artırıldı. `tools/generate_all_store_locales.py` güncellenerek `tr-TR` için 5/5 ekranın telefon içi tamamen Türkçe, uluslararası diller için ise boşluksuz İngilizce olması sağlandı. 11 dil x 6 varlık (66 görsel) yeniden üretildi.
  3. **Tüm 11 Dilin ASO Metinlerinin Genişletilmesi (H2):** Kalan 9 dilin (`de-DE`, `es-ES`, `fr-FR`, `pt-BR`, `ru-RU`, `hi-IN`, `id`, `th`, `ar`) tam açıklamaları 3,200 - 3,980 karaktere ve kısa açıklamaları 70 - 80 karaktere çıkarıldı. E-E-A-T, izin şeffaflığı, Stoacı kilit ekranı felsefesi, seriler ve SSS bölümleri tüm dillere eklendi. Tüm dosyalarda UTF-8 kuralına uyuldu (`grep -c "Ã"` = 0).
  4. **Play Console Senkronizasyonu & Canlı Sürüm (v1.2.0 / Code 17):**
     - `gpc listings sync --dir metadata`: 11 dilin tüm genişletilmiş başlık ve açıklamaları senkronize edildi (`Synced 11 locale(s)`).
     - `gpc images sync --dir store_assets/play-sync-v2 --timeout 15m`: 11 dilin tüm ekran görüntüleri ve özellik grafikleri canlı Play Console'a yüklendi (`Uploaded 67 image(s)`).
     - `app/build.gradle.kts`: `versionCode 17`, `versionName "1.2.0"` olarak güncellendi.
     - `./gradlew.bat :app:bundleRelease`: İmzalı release AAB derlendi (`BUILD SUCCESSFUL in 5m 3s`).
     - `gpc bundles upload`: Yeni release paketi canlı Google Play Console Production kanalına %100 rollout ile yüklendi ve yayınlandı.

## Doğrulama
- **Canlı Google Play Console Sürüm Doğrulaması:**
  - `gpc tracks list`: `[{"track":"production","version_codes":[17],"status":"completed","rollout":100,"release_count":1}]` teyit edildi.
  - SHA-256: `019c5e47960fcd61706737b416931dc606293c79a1dc7ce08aa622cd6ceb78bf`.
- **Canlı Mağaza Varlıkları Doğrulaması:**
  - `gpc listings sync`: 11 dilin tamamı başarıyla güncellendi.
  - `gpc images sync`: 67 görsel (66 ekran/özellik grafiği + 1 ikon) Play Console API'ye başarıyla yüklendi.
- **Birim ve Regresyon Testleri:**
  - `RestrictionScheduleTest.kt`: Portekizce, İngilizce, Türkçe gün normalize etme ve aktif zaman aralığı testleri başarıyla geçti (`BUILD SUCCESSFUL`).
  - `GuardianRepositoryRegressionTest.kt`: Safe daily limit ve DB testleri geçti.
- **Karakter ve UTF-8 Doğrulaması:**
  - 11 dilin tamamında Başlık <= 30, Kısa Açıklama 70-80 (<=80), Tam Açıklama 3,119-3,981 (<=4,000) karakter.
  - UTF-8 kontrolü: Tüm dosyalarda çift kodlama sayısı 0 (`grep -c "Ã"` = 0).
- **Google Ads API:** `campaign.id = 24210252128` durumu `PAUSED` olarak korunuyor.

## Bilinen Sorunlar / Notlar
- **AÇIK (yeniden açıldı): H4 zamanlanmış kısıtlama hatası.** Gün normalizasyonu düzeltmesi kök nedeni çözmüyor; `daysOfWeek` Türkçe sabit olduğu için Portekizce gün adı hiç kaydedilmiyor. Asıl şüpheli erişilebilirlik servisi kararsızlığı. Brezilyalı kullanıcıya cihaz markası ve Android sürümü sorulmalı.
- **AÇIK (E5): Ekran görüntülerindeki alt boşluk.** tr-TR Kart 1 ve 2'de ~%25, Kart 4'te ~%30 boş beyaz alan.
- **AÇIK: Servis düzeltmeleri commit'lenmedi.** `AppBlockAccessibilityService.kt` çalışma ağacında değişik; kilit tetikleme ve UsageStats yedek muhasebesi düzeltmeleri `28a6bab` içinde yok. Bu dosyada çalışacak her ajan önce mevcut halini almalı.
- **AÇIK (KRİTİK, kullanıcı eylemi gerekiyor): Erişilebilirlik servisi kapalı ve sürekli öldürülüyor.** Ölçüm anında `Enabled services:{}` idi. Cihaz Xiaomi HyperOS. Ayarlar > Erişilebilirlik'ten Limitra yeniden açılmalı; ayrıca "Otomatik başlatma" izni verilip pil kısıtlaması kaldırılmalı. Servis çalışmadan hiçbir kod düzeltmesi etkili olamaz.
- **AÇIK: Oturumların ~10 saniyede ölmesi.** Üç ayrı oturumda tekrarlandı. Uygulama kodunda kendini kapatan çağrı yok; sistem güç yönetimi şüpheli, kesinleşmedi.
- **AÇIK: Kilit tetikleme düzeltmesi yayınlanmadı.** `allowRestrictedEntry` regresyon düzeltmesi (Claude, 19:45) yalnızca çalışma ağacındadır. Production'daki v1.2.0 / versionCode 17 paketi 19:25'te derlendiği için bu düzeltmeyi içermez; kullanıcı cihazında hata sürüyorsa beklenen durumdur.
- **AÇIK: Cihaz üzerinde uçtan uca doğrulama yapılmadı.** Senaryo: 1 dakikalık limit, kısıtlı uygulamada kal, Limitra'yı hiç açma, kilidin geldiğini doğrula.
- **ÇÖZÜLDÜ: tr-TR Ekran Görüntülerindeki İngilizce Arayüz (H1):** Kart 1, 2, 4 için özel Türkçe arayüz mockup'ları üretildi. tr-TR mağazasındaki 5 ekran görüntüsünün 5'i de artık telefon çerçevesi içinde %100 Türkçedir.
- **ÇÖZÜLDÜ: Ekran Görüntülerindeki Alt Boşluk (E5):** Telefon mockupları dikeyde kart ve elemanlarla dengelenerek boş gri alanlar tamamen kaldırıldı.
- **ÇÖZÜLDÜ: Eksik ASO Metinleri (H2):** 9 dilin tam açıklamaları 1,000-1,376 karakterden 3,200-3,980 karaktere genişletildi; kısa açıklamalar 70-80 karaktere optimize edildi.
- **ÇÖZÜLDÜ: Zamanlanmış Kısıtlama Gün Eşleştirme Hatası (H4):** Portekizce dahil çok dilli gün normalizasyonu ve anlık ön plan denetimi eklendi; unit testlerle doğrulandı.
- **ÇÖZÜLDÜ: Canlı Sürüm Güncellemesi:** Version Code 17 (v1.2.0) derlendi ve Google Play Console Production kanalında %100 olarak yayına alındı.
- **AÇIK (P1): Store Tanıtım Videosu ve Tablet:** 11 locale'de tanıtım videosu ve tablet (7"/10") ekran görüntüleri henüz bulunmuyor (Play Store zorunlu tutmaz ancak dönüşüm için faydalıdır).
- **BEKLEMEDE (Kullanıcı Kararı): P2 Freemium + IAP:** Kullanıcı talimatı doğrultusunda P2 ertelenmiştir; sonraki aşamada ele alınacaktır.
- **NOT: Google Play kamuya açık web önbelleği:** API güncel içeriği onaylasa da mağaza web sayfası kısa bir süre CDN önbelleğindeki görselleri gösterebilir.

## Sonraki İşler / Öneriler
- Kullanıcı ile P2 (Freemium + IAP geçişi ve gelir modeli) planını tartışmak.
- **Önerilen Model:** Antigravity (UI/Stil/Kreatif), Claude (Mimari/Billing/IAP), Codex (Test/Analiz).
