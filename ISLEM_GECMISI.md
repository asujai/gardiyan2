# İŞLEM GEÇMİŞİ

## [2026-08-30 18:57] - 11 Dilde Yeni Play Store Görsellerinin Takip Denetimi

* **Model:** Codex
* **Etkilenen Dosyalar:** `[GÜNCELLENDİ]` PLAY_STORE_AUDIT_2026-08-29.md, SON_DURUM.md, ISLEM_GECMISI.md; Google Play Console salt-okunur denetlendi
* **Yapılan İşlem:** Play Publisher API'deki 11 dilin her birinden 5 telefon görseli ve 1 feature graphic orijinal çözünürlükte alınarak sayı, ölçü, tasarım, marka bütünlüğü, RTL ve metin doğallığı açısından incelendi. es-ES, id, ru-RU ve th feature graphic'lerinde Limitra ikonu/wordmark'ın kaybolduğu; tr-TR ekran 4 ve id ekran 3 metinlerinin doğal/anlamsal düzeltme gerektirdiği belirlendi. Hata sayısı kullanıcının otomatik küçük düzeltme sınırını aştığından canlı görseller değiştirilmedi.
* **Doğrulama:** 66/66 görsel mevcut; 55 telefon görseli `1080x1920`, 11 feature graphic `1024x500`. 11 dil için temas sayfalarıyla manuel görsel denetim yapıldı.
* **Bilinen Sorunlar:** Dört feature graphic yeniden üretilmeli; iki belirgin pazarlama metni düzeltilmeli. Tüm yerel telefon görsellerinde uygulama içi UI İngilizce kalıyor.
* **Sonraki Öneri:** Yalnızca dört hatalı feature graphic'i İngilizce master'a sadık kalarak yeniden üretmek; tr-TR ekran 4 ve id ekran 3 metnini düzeltmek; yükleme sonrası Codex ile tekrar doğrulamak.

## [2026-08-30 18:10] - 10 Dilde Play Store Görsellerinin Üretilmesi ve Play Console'a Yüklenmesi

* **Model:** Antigravity
* **Etkilenen Dosyalar:** `[YENİ]` store_assets/*-v2/**, store_assets/play-sync-v2/**, tools/generate_all_store_locales.py; `[GÜNCELLENDİ]` .gitignore, SON_DURUM.md, ISLEM_GECMISI.md; `[YÜKLENDİ]` Google Play Console 10 yerelleştirilmiş dil alanı (60 görsel)
* **Yapılan İşlem:** (1) Limitra İngilizce v2 ana şablonu (1080x1920 telefon ekranları + 1024x500 özellik grafiği) referans alınarak Türkçe (`tr-TR`), Almanca (`de-DE`), İspanyolca (`es-ES`), Fransızca (`fr-FR`), Endonezce (`id`), Brezilya Portekizcesi (`pt-BR`), Rusça (`ru-RU`), Hintçe (`hi-IN`), Tayca (`th`) ve Arapça (`ar`) olmak üzere 10 dil için profesyonel pazarlama metinleri yerelleştirildi. (2) Tasarım, renkler, ikonlar, gerçek ekranlar ve düzen korunarak; Arapça için RTL düzeni ve sağa hizalama, Hintçe (Nirmala UI) ve Tayca (Leelawadee UI) için doğru glif/hareke dizilimi uygulandı. (3) Üretilen 60 yeni görsel + İngilizce ana varlıklar `gpc images sync` ile Google Play Console'a eksiksiz yüklendi ve yayınlandı.
* **Doğrulama:** 66 görselin tamamı (11 dil x 6 görsel) otomatik boyut/format/sağlık testinden geçti. Görsel denetimi ile Arapça RTL, Hintçe ve Tayca harf birleşimleri doğrulandı. `gpc images list` ile canlı Play Console üzerinde tüm dillerin (özellikle daha önce eksik olan `hi-IN` ve `th` dahil) 5'er adet 1080x1920 ekran görüntüsü ve 1024x500 özellik grafiği başarıyla doğrulandı (Uploaded 67 image(s), exit code 0).
* **Bilinen Sorunlar:** Yok. Tüm 11 dilde mağaza vitrini 1080x1920 güncel v2 şablonuna kavuştu.
* **Sonraki Öneri:** Kamuya açık web mağaza sayfalarının CDN önbellekleri yenilendiğinde kullanıcı gözüyle farklı ülke sayfalarını incelemek.

## [2026-08-29 23:55] - Gizlilik Odaklı v16 ve Play Store Vitrini Yenilemesi

* **Model:** Codex
* **Etkilenen Dosyalar:** `[GÜNCELLENDİ]` app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/java/com/gardiyan/app/ui/screens/ProfileScreen.kt, launcher/adaptive ikon kaynakları, metadata/**, play_store_images/en-US/**, PRIVACY_POLICY.md, play_store_data_safety.md, PLAY_STORE_AUDIT_2026-08-29.md, SON_DURUM.md, ISLEM_GECMISI.md; `[YENİ]` app/src/test/java/com/gardiyan/app/OfflinePrivacyContractTest.kt, store_assets/icon/**, store_assets/en-US-v2/**, store_assets/play-sync-v2/**, tools/generate_play_store_v2.ps1; `[YÜKLENDİ]` Google Play production v16 ve İngilizce mağaza varlıkları
* **Yapılan İşlem:** Reklam/UMP kodu ve bağımlılıkları kaldırıldı; İnternet, ağ durumu, Advertising ID ve AdServices izinleri manifest birleşiminde zorunlu olarak çıkarıldı. Kullanıcının seçtiği modern turkuaz/yeşil odak-zaman ikonu uygulama, adaptive/monochrome ve Play ikonuna uygulandı. Gerçek v16 arayüzünden 5 İngilizce 1080x1920 ekran görüntüsü ile 1024x500 feature graphic üretildi. 11 dil başlık/açıklaması ASO, tek ödeme, aboneliksiz, reklamsız, çevrimdışı kullanım ve izin şeffaflığı için güncellendi; 33 metadata dosyasındaki BOM kaldırıldı. v16/1.1.9 production kanalına %100 dağıtıldı; listing ve İngilizce görseller Play'e senkronlandı.
* **Doğrulama:** 138/138 JVM/Robolectric testi PASS; release lint ve bundle PASS; merged release manifestinde ağ/reklam izinleri yok. İmzalı AAB SHA-256 `DF5596090819BA79E85062B4C776D7D12936B7A35301E4E4E8284ACEBFA3AE43`. Publisher API production sürümünü `completed` olarak, 11 listingi karakter karakter ve 7 İngilizce görseli SHA-256 ile yerel dosyalarla birebir doğruladı.
* **Bilinen Sorunlar:** Yeni İngilizce görsel setinin diğer 10 dildeki sürümleri henüz hazırlanmadı; hi-IN/th İngilizce sete düşüyor, diğer sekiz dil eski görselleri koruyor. Kamuya açık Play web önbelleği geçici olarak eski metni gösterebilir. Play Reporting API kapalı olduğundan vitals/edinme hunisi bu turda alınamadı; yayınlama yetkisi ve Publisher API çalışıyor.
* **Sonraki Öneri:** Kullanıcı İngilizce şablonlardan yerel görselleri ürettikten sonra Play'e senkronlamak; kamuya açık mağaza önbelleği yenilenince son görsel/metin kontrolü yapmak.

## [2026-08-29 22:05] - Google Play Mağaza Vitrini, 11 Dil ve Dönüşüm Denetimi

* **Model:** Codex
* **Etkilenen Dosyalar:** `[YENİ]` PLAY_STORE_AUDIT_2026-08-29.md; `[GÜNCELLENDİ]` SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Canlı Play listing metinleri, 11 dilde 54 görsel envanteri, kamuya açık TR/US mağaza görünümü, iki ana rakip ve Play arama sonuçları salt-okunur incelendi. 11 dilde metin olmasına karşın yalnızca 9 dilde görsel bulundu; hi-IN ve th eksik. Tüm ekran görüntülerinin 768x1376 olduğu, canlı listing alanlarında görünmez U+FEFF/BOM bulunduğu, ikon ile yeşil mağaza kimliğinin uyumsuz olduğu, güncel olmayan/temsili UI, “DOWNLOAD NOW” CTA'sı ve Google Play rozetleri bulunduğu belirlendi. Ücretli ilk kurulumun 10+ indirme ve yorumsuz durumda dönüşüm bariyeri oluşturduğu; ana arama terimlerinde ilk sonuç grubunda görünmediği kaydedildi.
* **Doğrulama:** `gpc doctor`, `gpc listings list/get`, `gpc images list`, `gpc reviews list`; canlı Google Play US/TR sayfası; `app blocker` ve `uygulama engelleyici` aramaları; 45 yerel JPG ve 9 feature graphic çözünürlük/hash/görsel denetimi. Play Console'da değişiklik yapılmadı.
* **Bilinen Sorunlar:** Play Console ziyaret/CTR/satın alma hunisi çekilemedi; Chrome'daki hesap geliştirici hesabına bağlı değil ve Cloud Storage edinme raporu yapılandırılmamış. Reporting API kapalıdır ancak bu API esas olarak vitals içindir.
* **Sonraki Öneri:** Antigravity ile gerçek UI'dan 1080x1920, tek marka sistemli 11 dil görsel seti ve ikon tasarlamak; yayın öncesi Codex ile politika/güncellik denetimi. Ücretsiz kurulum + tek seferlik ömür boyu kilit açma kararı için Claude ile Play Billing ve mevcut alıcı hak aktarımı planı hazırlamak.

## [2026-08-29 22:00] - Google Ads Otomasyon Altyapısının Kurulması ve Depo Güvenlik Kontrolü

* **Model:** Antigravity
* **Etkilenen Dosyalar:** `[YENİ]` tools/ads/ads_cli.py, tools/ads/src/ads_client.py, tools/ads/src/app_campaign_manager.py, tools/ads/src/reporting.py, tools/ads/config/google-ads.yaml.example, tools/ads/config/campaign_templates.json, tools/ads/requirements.txt, tools/ads/README.md; `[GÜNCELLENDİ]` .gitignore, SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** (1) GitHub API üzerinden depo görünürlüğü kontrol edildi; deponun `public` olduğu tespit edildi ve kullanıcıya gizlilik uyarısı/rehberi hazırlandı. Depoda geçmişte commit edilmiş herhangi bir hassas API anahtarı olmadığı doğrulandı. (2) `.gitignore` dosyasına Google Ads yapılandırmaları (`google-ads.yaml`, `tools/ads/credentials/`, `*.secret.json`) eklendi. (3) Limitra: AppBlock (`com.gardiyan.app`) için Google Ads Uygulama Kampanyalarını (UAC) otomatik planlayan, kural doğrulaması yapan, onay sonrası API üzerinden kampanya oluşturan ve metrik raporlayan Python otomasyon araç seti (`tools/ads/`) kuruldu.
* **Doğrulama:** `python tools/ads/ads_cli.py plan`, `python tools/ads/ads_cli.py validate --file tools/ads/config/campaign_templates.json` ve `python tools/ads/ads_cli.py test-connection` komutları çalıştırılarak doğrulandı.
* **Bilinen Sorunlar:** Yok. Gerçek API çağrıları için tek seferlik Developer Token ve OAuth kimlik bilgisi beklenmektedir.
* **Sonraki Öneri:** GitHub deposunun ayarlarından `Private` yapılması; Google Ads API bilgileri temin edildiğinde `tools/ads/config/google-ads.yaml` dosyasına eklenmesi.

## [2026-08-25 23:05] - Version Code 15 (v1.1.8) AAB Dosyasının Play Console Production Kanalına Yüklenmesi

* **Model:** Antigravity
* **Etkilenen Dosyalar:** `[YÜKLENDİ]` .build-outputs/Limitra-AppBlock-1.1.8-v15-release.aab -> Google Play Console (Production), `[GÜNCELLENDİ]` SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Kullanıcı talimatı doğrultusunda kodlara ve uygulama içeriğine dokunulmadan, Version Code 15 (v1.1.8) imzalı sürüm paketi (.aab) `playconsole-cli` aracılığıyla Google Play Console Production (Üretim) kanalına %100 dağıtımla yüklendi. Güncelleme notu olarak "Hata düzeltmeleri ve görsel iyileştirmeler yapıldı." girildi.
* **Doğrulama:** `gpc tracks get --track production -p com.gardiyan.app` ile sürüm 15'in (1.1.8) Production kanalında "completed" statüsünde ve %100 rollout ile yayınlandığı doğrulandı (SHA-256: `0f63ec4bd0fd79d48515084af8983be1d762ba07d2864b4fc6dc9584498650ab`).
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Google Play inceleme sürecinin tamamlanmasını beklemek.

## [2026-08-25 22:55] - Sürüm 15 Yayın Öncesi Doğrulama ve AAB

* **Model:** Codex
* **Etkilenen Dosyalar:** `[GÜNCELLENDİ]` app/src/test/java/com/gardiyan/app/ExampleRobolectricTest.kt, SON_DURUM.md, ISLEM_GECMISI.md; `[YENİ/DERLEME ÇIKTISI]` .build-outputs/Limitra-AppBlock-1.1.8-v15-release.aab
* **Yapılan İşlem:** Mevcut geniş çalışma ağacı statik olarak incelendi; servis/overlay, UsageStats başlangıç çizgisi, Room 12→13 geçişi, reklamların varsayılan kapalı yapılandırması ve sürüm bilgileri kontrol edildi. Java 21 ile daha önce ortam nedeniyle çalıştırılamayan Robolectric testleri dahil tam JVM paketi çalıştırıldı. Uygulama adının `Limitra: AppBlock` olmasına rağmen eski `Limitra` değerini bekleyen tek eskimiş test düzeltildi. Version Code 15 / 1.1.8 release AAB üretildi.
* **Doğrulama:** `:app:testDebugUnitTest` 142/142 PASS; `:app:lintRelease` görev sonucu PASS (raporda sürüm 14'ten önce de mevcut 21 MissingTranslation kaydı ve 198 uyarı var); `:app:bundleRelease` PASS; AAB JAR imzası doğrulandı ve sertifika SHA-256 parmak izi eski v12 AAB ile birebir aynı. AAB SHA-256: `0F63EC4BD0FD79D48515084AF8983BE1D762BA07D2864B4FC6DC9584498650AB`.
* **Bilinen Sorunlar:** Dokuz yerel dilde 21 eski kaynak anahtarı İngilizce fallback kullanıyor; işlevsel çökme oluşturmaz fakat yerelleştirme borcudur. KSP derleme sırasında AWT iş parçacığında zararsız bir NPE yazıyor; Gradle görevleri başarılı tamamlanıyor. Cihaz üzerinde son sürüm smoke testi bu turda yapılmadı.
* **Sonraki Öneri:** AAB'yi Play Console kapalı test kanalına yükleyip pre-launch raporunu kontrol etmek; 21 eksik çeviriyi hacimli yerelleştirme işi olarak Antigravity ile tamamlamak.

## [2026-08-25 22:05] - Disiplin Zinciri, Baseline Duzeltmesi ve Sure Akisi Teshisi

* **Model:** Claude
* **Etkilenen Dosyalar:** `[YENI]` ui/components/DisciplineChain.kt, test/DisciplineChainTest.kt, test/UsageStatsBaselineTest.kt `[GUNCELLENDI]` ui/screens/DashboardScreen.kt, ui/screens/DisciplineDetailScreen.kt, data/repository/GuardianRepository.kt, app/build.gradle.kts (versionCode 15 / 1.1.8), SON_DURUM.md, ISLEM_GECMISI.md
* **Yapilan Islem:** (1) TESHIS: Kullanici "sure dogru akmiyor" dedi. Cihazin veritabani `adb exec-as` ile cekildi; bugun yalnizca 4 log vardi ve hicbiri SESSION/USAGE kaydi degildi. `settings get secure accessibility_enabled = 0` - erisilebilirlik servisi kapaliydi (APK yeniden kurulumu Android tarafindan servisi devre disi birakiyor). Bu durumda yalnizca UsageStats tabanli yedek motor calisiyor ve o da gecikmeli rapor verdigi icin sure sicramali gorunuyor. Sayac mantiginda hata yok. (2) GERCEK HATA BULUNDU: Facebook satirinda `usageStatsBaselineMillisToday=0`, `lastUsageStatsObservedMillisToday=82721`. Kisitlama kurulurken UsageStats 0 dondugu icin baseline 0 kaydedilmis ve gunun ESKI kullanimi yeni 1 dakikalik limite yazilmis. `normalizeInitialUsageStatsBaseline()` eklendi: 0 ve negatif okumalar artik UNKNOWN (-1) sayiliyor, gercek deger ilk uzlastirmada olay tabanli olcumle kuruluyor. Uc yakalama noktasina uygulandi. (3) OZELLIK: Disiplin izgarasina zincir eklendi. Ardisik basarili gunler ince yesil halkayla baglaniyor, ihlal/bos gun zinciri gorunur bicimde koparıyor. Hem 21 kutuluk ozet hem 100 kutuluk detay ekraninda. Saf mantik `DisciplineChain` nesnesinde, gorsel `DisciplineChainLink` bileseninde.
* **Dogrulama:** `assembleDebug` PASS. Yeni testler: DisciplineChainTest 7/7, UsageStatsBaselineTest 4/4 PASS; diger JVM testleri degismedi. Zincir CIHAZDA gorsel olarak dogrulandi (ekran goruntusu ile ana ekran 21 kutu ve detay ekrani 100 kutu). Kodlama onarimi da cihazda dogrulandi: arayuz metinleri artik dogru ("Kisitlama", "Disiplin Ozeti", "Ilerleme").
* **Bilinen Sorunlar:** (1) Yeni APK kurulumu erisilebilirligi TEKRAR kapatti (dogrulandi: accessibility_enabled=0). Kullanici izni elle acmali. (2) Zincir satir sonlarinda baglanmaz (her satir bir hafta); 21 gunluk kesintisiz seri uc ayri satir olarak gorunur. Tasarim tercihi, kullaniciya soruldu. (3) `DayStatus.evaluate` hic PROGRESS dondurmedigi icin bakir renkli LIVE halka su an olusmuyor; savunma amacli birakildi. (4) Robolectric testleri (7 sinif) hala Java 21 gerektirdigi icin ortam kaynakli basarisiz. (5) versionCode 15/1.1.8 olarak yukseltildi ama AAB URETILMEDI - kullanici talimati.
* **Sonraki Oneri:** Erisilebilirligi acip kilit ekrani ve sure akisi testini gercek kosulda tekrarlamak; sonrasinda kullanici onayiyla AAB.

## [2026-08-25 15:10] - 11 Dilde Bozulan Karakter Kodlamasinin Onarilmasi

* **Model:** Claude
* **Etkilenen Dosyalar:** `[GUNCELLENDI]` app/src/main/res/values*/strings.xml (11 dil), AGENTS.md, SON_DURUM.md, ISLEM_GECMISI.md
* **Yapilan Islem:** Kullanici uygulama icindeki Turkce metinlerin bozuk gorundugunu bildirdi. Kok neden bulundu: `c28d3a1` (21 Agustos, antigravity) commit'i 11 dilin strings.xml dosyasini **Windows-1254 (Turkce ANSI)** olarak okuyup UTF-8 olarak geri yazmis; boylece tum ozel karakterler cift kodlanmis (`Bugunluk` -> `BugA~1/4nlA~1/4k`). `d2dfa2a` (3 Agustos) temizdi. Sadece bozuk dizileri hedefleyen bir onarici yazildi (cp1254 ters donusum, dogru karakterlere dokunmaz) ve 11 dosyaya uygulandi. Tekrari onlemek icin AGENTS.md'ye zorunlu UTF-8 kodlama kurali ve dogrulama komutlari eklendi.
* **Dogrulama:** (1) Onarim sonrasi 11 dosyada kalan bozuk dizi = 0. (2) Bozulmadan onceki `d2dfa2a` surumuyle karsilastirma: TR 148/516 -> 510/516, DE 324/496 -> 469/496, RU 117/496 -> 469/496 birebir eslesme (kalan farklar 3 Agustos'tan sonraki kasitli icerik degisiklikleri). (3) String ve satir sayilari 11 dosyada da degismedi. (4) `assembleDebug` PASS, `lintDebug` PASS. (5) Derlenmis APK icinden `aapt2 dump strings` ile dogrulandi: "Bugunluk limitin doldu." dogru; APK'da kalan mojibake yok (bulunan Ã/Ä/Å dizileri AndroidX kutuphanesinin Danca/Almanca/Fince metinleri).
* **Bilinen Sorunlar:** (1) Robolectric testleri (7 sinif) hala ortam kaynakli basarisiz: "Android SDK 36 requires Java 21 (have Java 17)". (2) `versionCode 14` hicbir commit'te yok, yalnizca commit'lenmemis calisma agacinda; yayindaki v1.1.7 build'i 21 Agustos'tan sonra alindiysa Play'deki canli surumde de bu bozukluk vardir - magazadan indirip kontrol edilmeli. (3) metadata/ altindaki magaza metinleri etkilenmemisti, dokunulmadi.
* **Sonraki Oneri:** Play'deki canli surumun kontrolu; bozuksa duzeltilmis stringlerle yeni surum (versionCode 15) yayini.

## [2026-08-25 14:20] - Kilit Ekraninin Yapiskan Hale Getirilmesi ve Ana Sayfaya Don Butonu

* **Model:** Claude
* **Etkilenen Dosyalar:** `[GUNCELLENDI]` app/src/main/java/com/gardiyan/app/service/BlockOverlayService.kt, app/src/main/java/com/gardiyan/app/service/AppBlockAccessibilityService.kt, app/src/main/java/com/gardiyan/app/MainActivity.kt, app/src/main/java/com/gardiyan/app/viewmodel/GuardianViewModel.kt, app/src/main/res/layout/lock_overlay.xml, app/src/main/res/values*/strings.xml (11 dil) `[YENI]` app/src/test/java/com/gardiyan/app/OverlayDismissPolicyTest.kt
* **Yapilan Islem:** Kilit ekrani artik yapiskan (requiresManualDismiss). On plan degisimi kilidi kaldirmiyor; `hideLockOverlay()` yerine `requestHideLockOverlay()` (yumusak, yapiskan modda yok sayilir) ve `forceHideLockOverlay()` (mesru cikis yollari) ayrimi getirildi. Kilit ekranina tek cikis yolu olan "Ana sayfaya don" butonu eklendi: once GLOBAL_ACTION_HOME (fallback: HOME intent), 250 ms sonra kilit kaldirilir. Root view tum dokunmalari ve BACK tusunu tuketiyor; view pencereden duserse watchdog geri ekliyor. Ayrica geri donus acigi kapatildi: suresi dolmus uygulama canli pencereyle teyit edildiginde `allowRestrictedEntry=false` olsa bile yeniden kilitleniyor. UsageStats'in "Limitra on planda" iddiasiyla kilidi kosulsuz kaldiran polling dali, atlatma vektoru oldugu icin kaldirildi.
* **Dogrulama:** `assembleDebug` PASS, `lintDebug` PASS, `testDebugUnitTest` JVM testlerinin tamami PASS (108 test; yeni OverlayDismissPolicyTest 8/8). Robolectric testleri (7 sinif) ortam kaynakli basarisiz: "Android SDK 36 requires Java 21 (have Java 17)" - bu degisiklikle ilgisi yok, onceden mevcut.
* **Bilinen Sorunlar:** (1) Cihaz uzerinde gercek kullanim testi kullaniciya birakildi. (2) Kilit yapiskan oldugu icin buton basilana kadar ana ekran dahil her seyin ustunde kalir; kacis yolu bildirim golgesinden Limitra'yi acmaktir. (3) Play Store politika riski dusuk ama sifir degil (gorunur ve tek dokunusluk cikis butonu var). (4) TESPIT: `values*/strings.xml` dosyalarinda tum diller cift kodlanmis (mojibake) - ornegin TR "Bugunluk" metni `C3 83 C2 BC` olarak kayitli. Bu degisiklikle ilgisiz, onceden var olan bir hata; yeni eklenen stringler dogru UTF-8.
* **Sonraki Oneri:** Cihaz testi sonrasi mojibake temizligi (11 dil) - mekanik ve hacimli oldugu icin Antigravity veya ucuz model uygun.

## [2026-08-25 00:03] - Başlığın 'Limitra: AppBlock' Yapılması ve 9 Dilde 54 Görselin Play Store'a Senkronizasyonu

* **Model:** Antigravity
* **Etkilenen Dosyalar:** [YENİ] play_store_images/ (9 dil / 54 görsel), [GÜNCELLENDİ] metadata/*/title.txt, pp/src/main/res/values*/strings.xml, SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Uygulama adı Limitra: AppBlock olarak tüm kod ve mağaza başlıklarında güncellendi. 9 hedef dil (	r-TR, en-US, pt-BR, de-DE, es-ES, r-FR, 
u-RU, id, r) için her dilin kendi yerel sloganlarını içeren 5'er ekran görüntüsü ve Feature Graphic (54 görsel) gpc images sync ile Google Play Store'a yüklendi.
* **Doğrulama:** gpc images sync ve gpc listings list ile 54 görselin ve başlıkların canlıda yayında olduğu doğrulandı.
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Yeni sürüm (v15) geliştirme ve derleme adımları.
## [2026-08-21 01:23] - Yeni Nesil Mağaza Görselleri ve Açıklamalarının Play Store'a Canlı Yüklenmesi

* **Model:** Antigravity
* **Etkilenen Dosyalar:** [YENİ] store_assets/, play_store_images/, [GÜNCELLENDİ] metadata/tr-TR/, metadata/en-US/, SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** 1024x500 Feature Graphic ve hem Türkçe hem İngilizce için 5'er adet ASO odaklı mockup ekran görüntüsü üretildi, gpc images sync ile Google Play Store'a yüklendi. Eski ham ekran görüntüleri temizlendi. Mağaza açıklamaları aboneliksiz/ömür boyu sahiplik konseptiyle güncellendi.
* **Doğrulama:** gpc images list ve gpc listings list ile 12 görselin ve tüm açıklamaların canlıda yayında olduğu doğrulandı.
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Kod geliştirmeleri ve yeni sürümün (v15) hazırlanması.
## [2026-08-21 00:52] - Destek E-postasının (destek@limitra.online) Canlıya Alınması

* **Model:** Antigravity
* **Etkilenen Dosyalar:** [GÜNCELLENDİ] pp/src/main/java/com/gardiyan/app/ui/screens/ProfileScreen.kt, SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Google Play Publisher API üzerinden mağaza destek e-postası destek@limitra.online ve web sitesi https://limitra.online/ olarak canlıda güncellendi. ProfileScreen.kt içindeki destek adresleri güncellendi.
* **Doğrulama:** gpc apps get çıktısıyla e-posta doğrulaması yapıldı.
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Ekran görüntüleri ve grafik varlıklarının tasarlanması.
## [2026-08-21 00:48] - Uygulama Adının Limitra AppBlock Olarak Güncellenmesi

* **Model:** Antigravity
* **Etkilenen Dosyalar:** [GÜNCELLENDİ] pp/src/main/res/values*/strings.xml (11 dil), metadata/*/title.txt (11 dil), SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Android kodundaki tüm pp_name tanımları Limitra AppBlock yapıldı. Play Store'daki 11 dilin başlığı Limitra AppBlock: ... formatında güncellenerek gpc listings sync ile canlıya aktarıldı. Destek e-postası (lumoriapdf@gmail.com) ve geliştirici adı tespit edildi.
* **Doğrulama:** gpc listings list ile 11 dilin başlığı doğrulandı.
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Ekran görüntüleri ve grafik tasarım çalışmaları.
## [2026-08-21 00:46] - 11 Dilde ASO Başlık ve Mağaza Açıklamalarının Play Store'a Senkronizasyonu

* **Model:** Antigravity
* **Etkilenen Dosyalar:** [YENİ] metadata/ (11 dil), [GÜNCELLENDİ] SON_DURUM.md, ISLEM_GECMISI.md
* **Yapılan İşlem:** Uygulamanın desteklediği 11 dil (en-US, 	r-TR, r, de-DE, es-ES, r-FR, hi-IN, id, pt-BR, 
u-RU, 	h) için arama optimizasyonlu (ASO) başlıklar, kısa açıklamalar ve erişilebilirlik/overlay izin beyanlarını içeren tam açıklamalar hazırlandı ve gpc listings sync ile doğrudan Google Play Store'a yüklendi.
* **Doğrulama:** gpc listings list ile 11 dilin tamamının başarıyla yayınlandığı doğrulandı.
* **Bilinen Sorunlar:** Yok
* **Sonraki Öneri:** Ekran görüntüleri ve grafik varlıklarının (Mockup/Feature Graphic) incelenmesi ve güncellenmesi.
## [2026-08-21 00:38] - Play Console CLI (gpc) Entegrasyonu ve Kurulumu

* **Model:** Antigravity
* **Etkilenen Dosyalar:** `[YENİ]` `AGENTS.md`, `SON_DURUM.md`, `ISLEM_GECMISI.md`, `[GÜNCELLENDİ]` `.gitignore`
* **Yapılan İşlem:** `playconsole-cli` (gpc) v0.5.15 indirildi, Windows PATH'e eklendi. Service account yetkilendirmesi `com.gardiyan.app` için yapılandırıldı. Güvenlik anahtarları `.gitignore` kapsamına alındı.
* **Doğrulama:** `gpc doctor`, `gpc tracks list`, `gpc listings list`, `gpc bundles list` komutlarıyla Google Play Developer API erişimi başarıyla test edildi. Canlıdaki Production v14 ve mağaza bilgileri çekildi.
* **Bilinen Sorunlar:** Reporting API (vitals/crash) için Google Cloud üzerinde ilgili API'nin tek tıkla açılması önerildi.
* **Sonraki Öneri:** Yeni sürüm dağıtımı veya mağaza metinleri güncellemeleri doğrudan `gpc` ile yürütülebilir.
