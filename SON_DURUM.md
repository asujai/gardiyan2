# SON DURUM

## Genel Bakış
- **Proje:** Limitra: AppBlock (Gardiyan) - Android Uygulama Kontrol ve Zaman Sınırı Yöneticisi
- **Paket Adı:** `com.gardiyan.app`
- **Yayındaki Sürüm:** Version Code 16 (v1.1.9) - Production `%100`, durum `completed`
- **Son Codex Çalışması:** `[codex] feat: launch privacy-first Play Store refresh` (v16 mağaza yenilemesi)

## Son İşlem
- **Google Ads Kampanyası Durduruldu, Bütçe/tCPI Düzeltildi ve 4 Format Kreatif Yüklendi (Antigravity, 3 Eylül 03:25):**
  - Kullanıcı talimatı doğrultusunda kampanya derhal durduruldu (`PAUSED`).
  - Gerçekçi pazar verileri doğrultusunda hedef CPI (tCPI) 2.50 TRY'den 14.00 TRY'ye, günlük bütçe algoritmanın öğrenme eşiğini karşılaması için 50 TRY'den 150.00 TRY'ye yükseltildi.
  - Kreatif envanteri sığlığı giderildi: Google Ads UAC resmi dikey standardı olan 1200x1500 (4:5) ve Stoacı Kilit odaklı 1200x1200 afişler üretilerek toplam görsel envanteri 4 farklı formata çıkarıldı.
  - Canlı kampanya (ID: `24210252128`) bu 4 görsel varlığı (`416449916702`, `416628465345`, `416569390138`, `416642481174`) ile PAUSED olarak yapılandırıldı.
- **Google Ads UAC Kampanyası Yayına Alındı (Antigravity, 3 Eylül 02:35):**
  - İlk deneme kampanyası oluşturulup açılmıştı, inceleme aşamasındaydı.

## Doğrulama
- Canlı Google Ads API üzerinden `campaign.id = 24210252128` doğrulandı: Durum `PAUSED`, Bütçe `150.0 TRY/gün`, Hedef `14.0 TRY/indirme`, Ad ID `823175390287` (5 başlık, 5 açıklama, 4 görsel varlığı).
- **Nihai canlı kontrol ve Tayca feature düzeltmesi (Codex, 30 Ağustos 19:47):** id feature ve id telefon 3 artık doğru. th feature'ın eski ikonsuz senkron kopyası tespit edildi; doğru mevcut kaynak iki yerel hedefe eşitlenip yalnızca Tayca feature Play'e yüklendi. Canlı dosya doğru kaynakla byte-for-byte aynı; SHA-256 `CF25DC3E5249C0E2859F0388ACB692FBDE063A574F0EC776FFFC860215A445A8`.
- **3. Ekran Görüntüsü Başlık Hizalaması ve Canlı Varlık Senkronizasyonu (Antigravity, 30 Ağustos):** 
  - `limitra-progress.png` ekranında durum çubuğunun yukarı kaymasından kaynaklanan durum çubuğu / `MY PROGRESS` çakışması, timeline ekranıyla aynı dikey dolgu ve durum çubuğu ile piksel düzeyinde düzeltildi.
  - Endonezce 3. ekran görüntüsü (`BANGUN KONSISTENSI. NAIK LEVEL.`) dahil tüm dillerin 3. ekran görüntüleri yeniden üretildi; `MY PROGRESS` başlığı ile durum çubuğu arasındaki çakışma giderildi.
  - `id` ve `th` feature graphic varlıkları canlı CDN üzerinden indirilerek hem Limitra hız/odak ikonunun hem de "LIMITRA" marka başlığının görselde eksiksiz yer aldığı doğrulandı.
  - 11 dilin tüm varlıkları `gpc images sync` ile Google Play Console'a yeniden yüklendi (`Uploaded 67 image(s)`).

## Doğrulama
- Canlı Google Ads API üzerinden `campaign.id = 24210252128` doğrulandı: Durum `PAUSED`, Bütçe `150.0 TRY/gün`, Hedef `14.0 TRY/indirme`, Ad ID `823175390287` (5 başlık, 5 açıklama, 4 görsel varlığı).
- Hesap bakiyesi: 700.00 TL toplam bütçe limiti, 19.68 TL geçmiş harcama, net 680.32 TL bakiye mevcut. Hiçbir yetkisiz harcama yapılmıyor.
- 66 görselin tamamı (11 dil x 6 görsel) otomatik boyut (1080x1920 ve 1024x500) ve PNG format kontrolünden geçti.
- Canlı Play Console üzerinde `gpc images sync` ve `gpc images list` ile her 11 dilin varlıkları başarıyla doğrulandı.
- `./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` (Java 21) → PASS; v16 kapsamında 138/138 JVM/Robolectric testi geçti.

## Bilinen Sorunlar / Notlar
- **ÇÖZÜLDÜ: Yerelleştirilmiş mağaza görselleri ve hizalama düzeltmeleri:** 11 dilin tamamında (`en-US`, `tr-TR`, `de-DE`, `es-ES`, `fr-FR`, `id`, `pt-BR`, `ru-RU`, `hi-IN`, `th`, `ar`) 1080x1920 telefon ekranları (hizalı durum çubuğu ile) ve 1024x500 özellik grafikleri (ikon ve LIMITRA markalı) tam ve eksiksiz olarak Google Play Console'a yüklendi.
- **NOT: Yerel telefon görsellerinin iç UI'ı İngilizce:** Dış başlıklar yerel, gerçek telefon ekranı İngilizce. İşlevsel/politika engeli değil; dönüşüm kalitesi borcu.
- **NOT: Google Play kamuya açık web önbelleği:** Publisher API güncel içeriği doğrulasa da mağaza web sayfası kısa süre eski başlık/açıklama/sürüm tarihini gösterebilir.
- **AÇIK: Mağaza dönüşüm verisi alınamadı.** Geliştirici Play Console hesabı bu tarayıcı oturumunda açık değil; Cloud Storage edinme raporu yapılandırılmamış.
- **AÇIK: Her APK kurulumundan sonra erişilebilirlik izni kapanıyor.** Test öncesi elle açılmalı. Şu anki durum: KAPALI.
- **AÇIK: Dokuz yerel dilde 21 eski kaynak anahtarı İngilizce fallback kullanıyor.** Çökme oluşturmaz; yerelleştirme borcudur.
- **AÇIK: KSP derleme sırasında AWT iş parçacığında zararsız bir NPE yazıyor.** Gradle görevlerinin sonucu başarılıdır.
- Zincir satır sonlarında bağlanmaz: her satır bir haftadır, 21 günlük kesintisiz seri üç ayrı satır olarak görünür. Tasarım tercihi.
- **AÇIK: Play'deki canlı sürüm kontrol edilmeli.** `versionCode 14` hiçbir commit'te yok, yalnızca commit'lenmemiş çalışma ağacında. v1.1.7 build'i 21 Ağustos'tan sonra alındıysa canlı sürümde de bozuk metinler vardır → mağazadan indirip bakılmalı, bozuksa versionCode 15 ile düzeltilmiş sürüm çıkılmalı.
- Kilit yapışkan olduğu için butona basılana kadar ana ekran dahil her şeyin üstünde kalır. Kilitlenip kalma durumunda kaçış yolu: bildirim gölgesinden Limitra'yı açmak.
- Android'de home hareketi (alttan yukarı çekme) uygulama tarafından engellenemez; çözüm kilidin ana ekran üzerinde de kalmaya devam etmesidir.
- Play Store politika riski düşük ama sıfır değil; görünür ve tek dokunuşluk çıkış butonu bu riski azaltır.
- Mojibake sorunu **çözüldü** (25 Ağustos). 11 dosyada kalan bozuk dizi 0; derlenmiş APK içinden `aapt2 dump strings` ile doğrulandı.
- Kullanıcılar kendi ülkelerinden/dillerinden mağazaya girdiğinde kendi anadillerindeki görselleri ve açıklamaları görür.

## Sonraki İşler / Öneriler
- İngilizce `store_assets/en-US-v2/` şablonlarındaki metinleri değiştirerek kalan 10 dil için yeni `1080x1920` görsel setleri hazırlamak ve Play'e yüklemek.
- Kamuya açık Play sayfasının önbelleği yenilendiğinde başlık, açıklama, ikon, ekran görüntüleri ve v16 tarihini son kez kontrol etmek.
- Ücretli ilk kurulum + tek seferlik satın alma modeli kullanıcı kararıyla korunuyor; freemium dönüşümü şimdilik yapılmayacak.
- Dokuz dildeki 21 eksik çeviriyi tamamlamak (Antigravity).
- Cihaz üzerinde kilit ekranı davranış testi (kullanıcı).
- Kod geliştirmeleri, yeni özellikler ve yeni sürüm (Version Code 15) hazırlığı.
- **Önerilen Model:** Antigravity (UI/Stil), Claude (Core/Servisler), Codex (Test/Analiz).
