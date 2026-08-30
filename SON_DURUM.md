# SON DURUM

## Genel Bakış
- **Proje:** Limitra: AppBlock (Gardiyan) - Android Uygulama Kontrol ve Zaman Sınırı Yöneticisi
- **Paket Adı:** `com.gardiyan.app`
- **Yayındaki Sürüm:** Version Code 16 (v1.1.9) - Production `%100`, durum `completed`
- **Son Codex Çalışması:** `[codex] feat: launch privacy-first Play Store refresh` (v16 mağaza yenilemesi)

## Son İşlem
- **Yerelleştirilmiş mağaza görselleri takip denetimi (Codex, 30 Ağustos):** Play'deki 11 dil x 6 varlık (66 görsel) orijinal çözünürlükte indirildi ve incelendi. Tüm dosya sayıları/ölçüler doğru; `es-ES`, `id`, `ru-RU`, `th` feature graphic'lerinde Limitra ikonu ve marka adı eksik. Türkçe 4. görsel ve Endonezce 3. görselde anlam/doğallık düzeltmesi gerekiyor. Ayrıntı `PLAY_STORE_AUDIT_2026-08-29.md` içinde.
- **10 Dilde Play Store Görselleri Üretildi ve Play Console'a Yüklendi (Antigravity, 30 Ağustos):** İngilizce master v2 şablonu baz alınarak Türkçe (`tr-TR`), Almanca (`de-DE`), İspanyolca (`es-ES`), Fransızca (`fr-FR`), Endonezce (`id`), Brezilya Portekizcesi (`pt-BR`), Rusça (`ru-RU`), Hintçe (`hi-IN`), Tayca (`th`) ve Arapça (`ar`) için toplam 60 yeni görsel (her dil için 5 adet 1080x1920 telefon ekranı ve 1 adet 1024x500 özellik grafiği) üretildi.
- Arapça için RTL yerleşimi ve sağa hizalama, Hintçe (Nirmala UI) ve Tayca (Leelawadee UI) için kusursuz glif/hareke dizilimi uygulandı.
- Tüm 11 dilin görsel seti `gpc images sync` ile Google Play Console'a yüklendi ve canlıya alındı (`Uploaded 67 image(s)`).
- **Gizlilik odaklı Play Store yenilemesi tamamlandı (Codex, 29 Ağustos):** v16/1.1.9 production'a `%100` dağıtıldı. Reklam/UMP kodu ve bağımlılıkları kaldırıldı; release manifesti İnternet, ağ durumu, Advertising ID ve AdServices izinlerini içermiyor.

## Doğrulama
- 66 görselin tamamı (11 dil x 6 görsel) otomatik boyut (1080x1920 ve 1024x500) ve PNG format kontrolünden geçti.
- Canlı Play Console üzerinde `gpc images list` ile her 11 dilin varlıkları başarıyla doğrulandı.
- `./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` (Java 21) → PASS; v16 kapsamında 138/138 JVM/Robolectric testi geçti.

## Bilinen Sorunlar / Notlar
- **AÇIK: Dört yerel feature graphic marka hatası:** `es-ES`, `id`, `ru-RU`, `th` görsellerinde ikon ve `LIMITRA` wordmark yok; yeniden üretilip yüklenmeli.
- **AÇIK: İki belirgin yerel metin sorunu:** tr-TR ekran 4 “HER HAREKET ŞEFFAFÇA KAYITTA.” ve id ekran 3 “BANGUN REKOR” düzeltilmeli.
- **KISMEN ÇÖZÜLDÜ: Yerelleştirilmiş mağaza görselleri:** 11 dilin tamamında (`en-US`, `tr-TR`, `de-DE`, `es-ES`, `fr-FR`, `id`, `pt-BR`, `ru-RU`, `hi-IN`, `th`, `ar`) doğru ölçülerde set mevcut; yukarıdaki marka/metin kusurları nedeniyle görsel kalite işi tamamen kapanmadı.
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
