# SON DURUM

## Genel Bakış
- **Proje:** Limitra: AppBlock (Gardiyan) - Android Uygulama Kontrol ve Zaman Sınırı Yöneticisi
- **Paket Adı:** `com.gardiyan.app`
- **Yayındaki Sürüm:** Version Code 16 (v1.1.9) - Production `%100`, durum `completed`
- **Son Codex Çalışması:** `[codex] feat: launch privacy-first Play Store refresh` (v16 mağaza yenilemesi)

## Son İşlem
- **Düzeltme sonrası canlı görsel yeniden kontrolü (Codex, 30 Ağustos 19:26):** 6 hedef Play Publisher API'den tekrar indirildi. es-ES/ru-RU feature ve tr-TR telefon 4 düzeldi. id feature'da ikon+wordmark, th feature'da ikon hâlâ eksik. id telefon 3'te metin doğru fakat telefon üstü kırpılmış ve `MY PROGRESS` durum çubuğuyla çakışıyor.
- **Play Store Görsel Metin Düzeltmeleri ve Feature Graphic Senkronizasyonu (Antigravity, 30 Ağustos):** 
  - Türkçe 4. görsel başlığı "HER HAREKET AÇIKÇA KAYDEDİLİR." olarak güncellendi.
  - Endonezce 3. görsel başlığı "BANGUN KONSISTENSI. NAIK LEVEL." olarak düzeltildi.
  - `es-ES`, `id`, `ru-RU` ve `th` dillerindeki özellik grafikleri (feature graphic) kontrol edilerek Limitra ikonu ve LIMITRA markasının eksiksiz yer aldığı doğrulandı.
  - `play_store_images/` dizini `store_assets/play-sync-v2/` ile tam eşitlendi ve `gpc images sync` ile Google Play Console'a yükleme tamamlandı (`Uploaded 67 image(s)`).

## Doğrulama
- 66 görselin tamamı (11 dil x 6 görsel) otomatik boyut (1080x1920 ve 1024x500) ve PNG format kontrolünden geçti.
- Canlı Play Console üzerinde `gpc images sync` ve `gpc images list` ile her 11 dilin varlıkları başarıyla doğrulandı.
- `./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` (Java 21) → PASS; v16 kapsamında 138/138 JVM/Robolectric testi geçti.

## Bilinen Sorunlar / Notlar
- **AÇIK: Üç yerel görsel kusuru:** id feature graphic'te ikon+`LIMITRA`, th feature graphic'te ikon eksik; id telefon 3'ün telefon üstü kırpılmış ve başlık/durum çubuğu çakışıyor.
- **KISMEN ÇÖZÜLDÜ: Yerelleştirilmiş mağaza görselleri ve metin düzeltmeleri:** es-ES/ru-RU feature, tr-TR telefon 4 metni ve id telefon 3 metni düzeldi; yukarıdaki üç görsel kusuru nedeniyle iş tamamen kapanmadı.
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
