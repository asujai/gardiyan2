# SON DURUM

## Genel Bakış
- **Proje:** Limitra: AppBlock (Gardiyan) - Android Uygulama Kontrol ve Zaman Sınırı Yöneticisi
- **Paket Adı:** `com.gardiyan.app`
- **Yayındaki Sürüm:** Version Code 16 (v1.1.9) - Production `%100`, durum `completed`
- **Son Codex Çalışması:** `[codex] feat: launch privacy-first Play Store refresh` (v16 mağaza yenilemesi)

## Son İşlem
- **Gizlilik odaklı Play Store yenilemesi tamamlandı (Codex, 29 Ağustos):** v16/1.1.9 production'a `%100` dağıtıldı. Reklam/UMP kodu ve bağımlılıkları kaldırıldı; release manifesti İnternet, ağ durumu, Advertising ID ve AdServices izinlerini içermiyor.
- **Mağaza vitrini yenilendi:** Seçilen turkuaz/yeşil odak-zaman ikonu launcher/adaptive/monochrome/Play varlıklarına uygulandı. Gerçek v16 arayüzünden 5 İngilizce `1080x1920` ekran görüntüsü ve `1024x500` feature graphic üretildi ve Play'e yüklendi.
- **11 dilde ASO ve açıklama düzeltildi:** Yerel uygulama-engelleyici arama terimleri, tek ödeme, aboneliksiz kullanım, reklamsızlık, çevrimdışı çalışma ve izin şeffaflığı eklendi. 33 metadata dosyasındaki BOM kaldırıldı; canlı Publisher API içeriği yerel dosyalarla karakter karakter eşleşti.
- **Ayrıntılı sonuç ve değişiklik öncesi kanıt:** `PLAY_STORE_AUDIT_2026-08-29.md`.
- **Google Play mağaza vitrini denetlendi (Codex, 29 Ağustos):** 11 dil metni, 54 görsel, canlı TR/US sayfası, arama görünürlüğü ve rakipler incelendi. Ayrıntılı rapor: `PLAY_STORE_AUDIT_2026-08-29.md`.
- **Kritik mağaza bulguları:** Görseller yalnızca 9 dilde (hi-IN ve th eksik); tüm telefon görselleri 768x1376 ve Play'in 1080x1920 öneri eşiğinin altında; ikon ile yeşil mağaza kimliği uyumsuz; güncel olmayan/temsili UI, “DOWNLOAD NOW” CTA'sı ve Google Play rozetleri var; canlı listing alanlarının başında görünmez U+FEFF/BOM bulunuyor.
- **Dönüşüm durumu:** Kamuya açık sayfa 10+ indirme ve görünür puan/yorum olmadan kurulum öncesi ücretli (TR ₺29,99 / US $0.49). İncelenen `app blocker` ve `uygulama engelleyici` ilk arama sonuç grubunda görünmedi.
- **Google Ads Otomasyon Altyapısı Kuruldu:** `tools/ads/` altında Google Ads Uygulama Kampanyalarını (UAC) otomatik planlayan, kural denetimi yapan, API üzerinden oluşturan ve metrik raporlayan araç seti tamamlandı.
- **GitHub Gizlilik ve Güvenlik Taraması Yapıldı:** Deponun `asujai/gardiyan2` adresinde `public` olduğu tespit edildi; `.gitignore`'a tüm Google Ads/API anahtarı ve secret şablonları eklendi. Depoda daha önceden commit edilmiş herhangi bir hassas anahtar bulunmadığı doğrulandı.
- **Version Code 15 (v1.1.8) AAB dosyası Google Play Console Production kanalına yüklendi ve yayınlandı.**
- Hiçbir kaynak koduna veya uygulama içeriğine dokunulmadı.
- Güncelleme notu olarak "Hata düzeltmeleri ve görsel iyileştirmeler yapıldı." eklendi.
- Production kanalı %100 rollout ile sürüm 15'e güncellendi.
- **Version Code 15 / 1.1.8 yayın öncesi doğrulandı ve imzalı AAB üretildi.** Java 21 ile Robolectric dahil 142 JVM testi geçti; release lint ve bundle görevleri başarıyla tamamlandı. AAB imza sertifikası eski v12 yayın AAB'siyle aynı.
- Marka değişikliğinden sonra eski `Limitra` adını bekleyen bir Robolectric testi `Limitra: AppBlock` olarak düzeltildi.
- **Disiplin zinciri eklendi.** Ardışık başarılı günler ince yeşil halkayla bağlanıyor; ihlal veya boş gün zinciri görünür biçimde koparıyor. Hem 21 kutuluk özet hem 100 kutuluk detay ekranında. Cihazda görsel olarak doğrulandı.
- **UsageStats başlangıç çizgisi hatası düzeltildi.** Kısıtlama kurulurken UsageStats 0 döndüğünde günün eski kullanımı yeni limite yazılıyordu; artık 0 "bilinmiyor" sayılıyor.
- **Süre akışı tespiti:** Sayacın hatalı görünmesinin sebebi erişilebilirlik servisinin KAPALI olmasıydı (APK kurulumu servisi devre dışı bırakıyor). O durumda yalnızca gecikmeli UsageStats yedek motoru çalışıyor. Sayac mantığında hata yok.
- **11 dilde bozulan karakter kodlaması onarıldı.** `c28d3a1` (21 Ağustos) commit'i `strings.xml` dosyalarını Windows-1254 okuyup UTF-8 yazmış, tüm özel karakterler çift kodlanmıştı. Sadece bozuk dizileri hedefleyen onarıcı ile 11 dosya düzeltildi; tekrarı için `AGENTS.md`'ye zorunlu UTF-8 kuralı eklendi.
- **Kilit ekranı yapışkan hale getirildi.** Kullanıcı alttan yukarı çekip uygulamayı arka plana atmayı yarıda bıraktığında kilit kalkıyor ve geri dönüşte gelmiyordu; kısıtlama tamamen atlatılabiliyordu.
- Artık ön plan değişimi kilidi kaldırmıyor. Tek çıkış yolu kilit ekranındaki **"Ana sayfaya dön"** butonu (ve meşru yollar: Limitra'nın açılması, kısıtlamanın silinmesi, günlük hakkın yeniden doğması).
- Ayrıca geri dönüş açığı kapatıldı: süresi dolmuş uygulama canlı pencereyle teyit edildiğinde, olayın kaynağı ne olursa olsun yeniden kilitleniyor.

## Doğrulama
- `./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` (Java 21) → PASS; v16 kapsamında 138/138 JVM/Robolectric testi geçti.
- Release merged manifest → `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` ve AdServices izinleri yok.
- AAB: `.build-outputs/Limitra-AppBlock-1.1.9-v16-release.aab` (5.341.890 bayt), SHA-256 `DF5596090819BA79E85062B4C776D7D12936B7A35301E4E4E8284ACEBFA3AE43`; JAR imzası geçerli.
- Play Publisher API → production v16 `completed`; 11 listing yerel metadata ile, 7 İngilizce görsel yerel dosya hash'leriyle birebir eşleşiyor.
- `./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:bundleRelease` (Java 21) → PASS
- JVM/Robolectric: 142/142 test PASS, 0 atlandı.
- AAB: `.build-outputs/Limitra-AppBlock-1.1.8-v15-release.aab` (6.019.051 bayt), SHA-256 `0F63EC4BD0FD79D48515084AF8983BE1D762BA07D2864B4FC6DC9584498650AB`.
- AAB JAR imzası geçerli; yükleme sertifikası SHA-256 parmak izi eski v12 AAB ile aynı.
- `./gradlew.bat :app:assembleDebug` → PASS
- `./gradlew.bat :app:lintDebug` → PASS
- `./gradlew.bat :app:testDebugUnitTest` → JVM testlerinin tamamı PASS (108 test). Yeni `OverlayDismissPolicyTest` 8/8 PASS.
- Release lint raporu: görev PASS; 21 eski `MissingTranslation` kaydı ve 198 uyarı mevcut.
- **Cihaz üzerinde gerçek kullanım testi kullanıcıya bırakıldı.**

## Bilinen Sorunlar / Notlar
- **AÇIK: Yerelleştirilmiş mağaza görselleri:** İngilizce yeni şablon hazır ve canlıdır. `ar`, `de-DE`, `es-ES`, `fr-FR`, `id`, `pt-BR`, `ru-RU`, `tr-TR` eski seti kullanıyor; `hi-IN` ve `th` İngilizce varsayılan sete düşüyor. Kullanıcı yeni İngilizce tasarımın metinlerini değiştirerek yerel sürümleri hazırlayacak.
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
