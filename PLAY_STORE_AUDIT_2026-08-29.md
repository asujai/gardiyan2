# Limitra: AppBlock — Google Play Store Listing Denetimi

**Tarih:** 2026-08-29  
**Paket:** `com.gardiyan.app`  
**Kapsam:** Canlı mağaza metinleri, 11 dil, görseller, kamuya açık Google Play sayfası, arama görünürlüğü ve rakip vitrini.  
**Değişiklik durumu:** Denetim bulgularının ana düzeltmeleri 2026-08-29 tarihinde production'a uygulandı; aşağıdaki ilk bölüm güncel sonucu özetler, sonraki bölümler değişiklik öncesi kanıtı korur.

## Uygulanan Düzeltmeler — 2026-08-29

- `versionCode 16 / 1.1.9` production kanalına %100 dağıtımla yüklendi ve `completed` durumu API'den doğrulandı.
- Google Mobile Ads ve UMP bağımlılıkları/kodu tamamen kaldırıldı. Release manifestinde `INTERNET`, `ACCESS_NETWORK_STATE`, Google Advertising ID ve Android AdServices izinleri bulunmadığı doğrulandı.
- Seçilen modern Limitra odak/zaman ikonu Play ikonu, klasik/yuvarlak Android launcher ikonları, adaptive/monochrome ikon ve uygulama içi marka görseline uygulandı.
- 11 dilde başlık yerel “uygulama engelleyici” arama terimleriyle güncellendi; tam açıklamalarda tek satın alma, aboneliksiz kullanım, cihaz içi işleme ve gerçek izin gereksinimleri netleştirildi.
- 33 metadata TXT dosyasındaki UTF-8 BOM kaldırıldı. Canlı 11 listing API'den yeniden okunup yerel dosyalarla karakter karakter eşleştirildi; tüm canlı başlıklar `U+004C` (`L`) ile başlıyor.
- İngilizce mağaza seti gerçek v16 arayüzünden üretildi: 5 adet `1080x1920` ekran görüntüsü, `1024x500` feature graphic ve `512x512` ikon. Yüklenen dosyaların SHA-256 değerleri Play API ile birebir eşleşiyor.
- Doğrulama: `138/138` JVM/Robolectric testi geçti; release lint ve AAB üretimi başarılı; ikon lint uyarıları giderildi. AAB SHA-256: `DF5596090819BA79E85062B4C776D7D12936B7A35301E4E4E8284ACEBFA3AE43`.

Kalan mağaza işi yalnızca yerelleştirilmiş görsellerdir: kullanıcı İngilizce şablonların diğer dil sürümlerini hazırlayacak. `ar`, `de-DE`, `es-ES`, `fr-FR`, `id`, `pt-BR`, `ru-RU` ve `tr-TR` için eski görseller şimdilik korunuyor; `hi-IN` ve `th` hâlâ İngilizce varsayılan sete düşüyor. Kamuya açık Play web önbelleği kısa süre eski başlık/metni gösterebilir; Publisher API güncel veriyi doğrulamıştır.

## Yerelleştirilmiş Görsel Takip Denetimi — 2026-08-30

- Play Publisher API üzerinden 11 dilin her birinde 5 telefon ekranı ve 1 feature graphic bulundu: toplam 66 görsel. Orijinal dosyaların tamamı indirildi; telefon görselleri `1080x1920`, feature graphic'ler `1024x500` ve dosya sayıları eksiksiz.
- **Yayın öncesi düzeltilmesi gereken dört feature graphic:** `es-ES`, `id`, `ru-RU` ve `th` grafiklerinde İngilizce ana şablondaki Limitra ikonu ve `LIMITRA` marka adı tamamen kaybolmuş; sol yarı büyük ölçüde boş kalmış.
- **Metin kalitesi:** Türkçe 4. görseldeki “HER HAREKET ŞEFFAFÇA KAYITTA.” doğal Türkçe değil; “HER HAREKET AÇIKÇA KAYDEDİLİR.” önerilir. Endonezce 3. görseldeki “BANGUN REKOR” streak anlamını doğru vermiyor; doğal bir “seri/streak” karşılığıyla değiştirilmelidir.
- Arapça feature graphic RTL için yeniden düzenlenmiş ve okunaklı; ikon/marka korunmuş. Diğer altı yerel feature graphic ana marka yapısını koruyor.
- Bütün yerel telefon görsellerinde dış pazarlama metni çevrilmiş olsa da telefon içindeki gerçek uygulama arayüzü İngilizce kalıyor. Bu bir dosya/politika hatası değil; ancak tam yerelleştirme ve dönüşüm güveni için ileride uygulamanın gerçek yerel UI ekranlarıyla değiştirilmesi önerilir.
- Sorun dört grafik ve birden fazla metin düzeltmesi içerdiği için, kullanıcının “yalnızca bir-iki küçük hatayı otomatik düzelt” sınırına uygun olarak bu denetimde Play Console'a değişiklik yapılmadı.

## Yönetici Özeti

Limitra'nın ana değer önerisi güçlü: uygulama engelleme, cihaz içi gizlilik, reklamsız kullanım ve abonelik yerine tek ödeme. Buna karşın mevcut mağaza vitrini bu değeri tutarlı ve güven verici biçimde aktarmıyor.

En yüksek etkili sorunlar:

1. Tüm telefon ekran görüntüleri `768x1376`; Google Play'in öneri yüzeylerine uygun görsel grupları için tavsiye ettiği en az `1080x1920` eşiğinin altında.
2. 11 dilde metin var ancak yalnızca 9 dilde yerelleştirilmiş görsel mevcut. `hi-IN` ve `th` için telefon ekran görüntüsü ve feature graphic yok.
3. Mağaza ikonu siyah/gri metalik bir kronometre; ekran görüntülerindeki neon yeşil kalkan/Limitra kimliğiyle uyuşmuyor.
4. Görseller gerçek ve güncel uygulama deneyimini güvenilir biçimde göstermiyor. Kilit ekranı görselleri, uygulamadaki güncel “Ana sayfaya dön” davranışından eski; bazı ekranlar kurmaca/temsili UI içeriyor.
5. İngilizce ve Türkçe kısa açıklamalar işlev yerine ödeme modeliyle başlıyor. Diğer 9 dilde ise “tek ödeme/abonelik yok” farkı ve gamification özellikleri anlatılmıyor. Değer önerisi diller arasında tutarsız.
6. Canlı metin alanlarının başında görünmez `U+FEFF` karakteri var. Yerel `metadata/**/*.txt` dosyalarının 33'ü de UTF-8 BOM ile başlıyor ve `gpc` bunu canlı metne içerik olarak taşımış.
7. Kamuya açık mağaza sayfasında `10+` indirme, görünür puan/yorum yok ve uygulama kurulum öncesi ücretli (`₺29,99`, ABD'de `$0.49`). Bu, sosyal kanıt oluşmadan önce yüksek satın alma bariyeri yaratıyor.
8. “app blocker” ve “uygulama engelleyici” aramalarının incelenen ilk sonuç sayfasında Limitra görünmedi. Sonuçlar kişiye ve zamana göre değişebilir, ancak mevcut görünürlük zayıf.

## Canlı Envanter

| Dil | Metin | Telefon görseli | Feature graphic | Not |
|---|---:|---:|---:|---|
| en-US | Var | 5 | 1 | İngilizce varsayılan set |
| ar | Var | 5 | 1 | Üst slogan Arapça, uygulama UI'ı İngilizce |
| de-DE | Var | 5 | 1 | Üst slogan Almanca, uygulama UI'ı İngilizce |
| es-ES | Var | 5 | 1 | Üst slogan İspanyolca, uygulama UI'ı İngilizce |
| fr-FR | Var | 5 | 1 | Üst slogan Fransızca, uygulama UI'ı İngilizce |
| hi-IN | Var | 0 | 0 | İngilizce varsayılan görsellere düşer |
| id | Var | 5 | 1 | Üst slogan Endonezce, uygulama UI'ı İngilizce |
| pt-BR | Var | 5 | 1 | Üst slogan Portekizce, uygulama UI'ı İngilizce |
| ru-RU | Var | 5 | 1 | Rusça başlıkta “APPS” İngilizce kalmış |
| th | Var | 0 | 0 | İngilizce varsayılan görsellere düşer |
| tr-TR | Var | 5 | 1 | Farklı tasarım dili; UI içinde çok sayıda İngilizce metin |

Toplam canlı/yerel görsel: 45 telefon ekran görüntüsü + 9 feature graphic = 54. Dokuz feature graphic aynı SHA-256 değerine sahip; gerçekte tek İngilizce görsel tüm bu dillere kopyalanmış.

Tablet, Chromebook, Wear OS veya TV görseli yok. Uygulama bu cihazları gerçekten desteklemiyorsa zorunlu değil; tablet desteği kaliteli ise ayrı tablet seti keşif yüzeylerini genişletebilir.

## Görsel Denetim

### İkon

- Mevcut ikon siyah/gri metalik kronometre formunda. Küçük boyutta “hız ölçer/temizleyici/VPN” çağrışımı yapıyor.
- Ekran görüntülerindeki yeşil kalkan ve neon turkuaz marka diliyle eşleşmiyor.
- Rakipler tek renk alanı üzerinde basit, yüksek kontrastlı ve küçük boyutta okunabilen ikonlar kullanıyor.
- Öneri: Limitra'ya özgü düz, sade, turkuaz/yeşil “odak + kalkan/zaman” sembolü. Play ikonu, launcher ikonu, uygulama içi logo ve görseller aynı sistemden gelmeli.

### Telefon ekran görüntüleri

- `768x1376` dosyalar Play'de kabul ediliyor; fakat Google'ın büyük öneri formatlarına uygunluk tavsiyesi olan en az `1080x1920` çözünürlüğü karşılamıyor.
- İlk üç görselde güncel gerçek UI yeterince baskın değil. Çok sayıda cihaz çerçevesi, parlak efekt ve temsili ekran kullanılmış.
- Güncel uygulama, kilit ekranında “Ana sayfaya dön” düğmesini kullanıyor. Mevcut 2. görseldeki “10s reflection / slide to unlock / emergency unlock” akışı güncel davranışı yansıtmıyor.
- 4. İngilizce ve türetilmiş görselde “DOWNLOAD NOW” çağrısı bulunuyor. Google, ekran görüntülerinde “Download now/Install now/Try now” gibi CTA metinlerinden kaçınılmasını istiyor.
- Türkçe 3. ve 5. görsellerde Google Play rozeti/logosu var. Google, ekran görüntülerinde Google Play veya başka mağaza rozetlerinden kaçınılmasını öneriyor.
- Rusça ilk görselde “APPS” İngilizce kalmış; başlık görsel olarak da sorunlu.
- Türkçe görsellerde “Daily Limit”, “Focus Score”, “Privacy dashboard”, “Discipline Profile”, “14 Days Streak” gibi İngilizce UI metinleri var. Diğer dillerin tamamında cihaz UI'ı İngilizce.
- Görsellerdeki “%100 güvenli” gibi mutlak iddialar yerine kanıtlanabilir ifadeler kullanılmalı: “Hesap yok”, “Sunucu yok”, “Veriler cihazında kalır”.

### Feature graphic

- Boyut doğru: `1024x500`.
- Tüm dillerde aynı İngilizce grafik kullanılıyor.
- Neon siber güvenlik estetiği, uygulama engelleme/odaklanmadan çok VPN veya antivirüs çağrışımı yapıyor.
- Küçük ekranda okunamayacak kadar fazla detay ve temsili UI içeriyor.
- Öneri: daha sade, metinsiz veya çok az metinli, ikonla uyumlu; dikkat kontrolü ve kazanılan zamanı anlatan tek odak noktası.

### Önerilen yeni 8 görsel sırası

1. **Dikkat dağıtan uygulamaları gerçekten engelle** — güncel gerçek ana ekran.
2. **Limit dolunca anında kilitlenir** — güncel kilit ekranı ve “Ana sayfaya dön”.
3. **Dakikalar içinde kural oluştur** — gerçek kurulum akışı.
4. **Ekran süreni net gör** — gerçek günlük/haftalık analiz.
5. **Disiplin zincirini büyüt** — güncel 21/100 günlük zincir.
6. **Odak moduyla çalış veya ders çalış** — gerçek odak akışı.
7. **Verilerin yalnızca cihazında** — gerçek gizlilik/ayar ekranı.
8. **Sade, reklamsız, çevrimdışı** — güncel uygulama deneyiminden kanıt.

Her dil için ek sloganlar yerelleştirilmeli; ekranlar mümkünse o dilde çalışan uygulamadan gerçek yakalama olmalı. Slogan alanı görüntünün yaklaşık %20'sini aşmamalı.

## Metin ve ASO Denetimi

### Başlık

- Tüm 11 dilde aynı başlık: `Limitra: AppBlock`.
- 30 karakter sınırının yalnızca 17 karakteri kullanılıyor.
- `AppBlock`, 10M+ indirmeli doğrudan rakibin marka adıyla çok yakın. Bu hem marka karışıklığı hem de Play metadata politikası açısından gereksiz risk.
- Öneri: benzersiz marka + yerel ana işlev. Örnekler:
  - İngilizce: `Limitra: Focus App Blocker`
  - Türkçe: `Limitra: Uygulama Engelle`
  - Almanca: `Limitra: App-Sperre & Fokus`
  - İspanyolca: `Limitra: Bloquea Apps`
- Nihai başlıklar anahtar kelime yığınına dönüşmemeli ve her dilde 30 karakter sınırı doğrulanmalı.

### Kısa açıklama

- İngilizce 79/80, Türkçe 76/80 karakter; işlevden önce “abonelik yok/tek ödeme” mesajı geliyor.
- Diğer dokuz dil işlevi anlatıyor ama en güçlü ticari farkı ve çevrimdışı gizlilik değerini aynı düzeyde taşımıyor.
- Önerilen mesaj formülü: **ana işlev + sonuç + kalıcı farklılaştırıcı**.
- İngilizce örnek: `Block distracting apps, cut screen time, and keep your data on-device.`
- Türkçe örnek: `Uygulamaları engelle, ekran süreni azalt; verilerin yalnızca cihazında kalsın.`
- Tek ödeme/abonelik yok mesajı tam açıklamanın ilk paragrafında açık ve dürüst biçimde korunabilir; kısa açıklamada fiyat/promosyon odaklı algıdan kaçınılmalı.

### Tam açıklama

- İngilizce ve Türkçe metinler 1.555/1.743 karakter; diğer diller 826–1.163 karakter. Özellik ve değer önerisi diller arasında eşit değil.
- İngilizce/Türkçe gamification, streak ve tek ödeme değerini anlatırken diğer dillerin çoğu bunları atlıyor.
- İlk 2–3 satır daha keskin olmalı: sorun, sonuç, ayırt edici kanıt.
- İzin açıklamaları şeffaf ve güçlü; metnin son yarısında tutulmalı.
- “%100 güvenli” yerine doğrulanabilir gizlilik özellikleri kullanılmalı.
- Tüm 11 dil native/profesyonel dil kontrolünden geçmeli. Özellikle Almanca “Fokus aufbauen”, Rusça görsel metni ve dil içi İngilizce kelimeler gözden geçirilmeli.

### BOM/veri temizliği

- `metadata` altındaki 33 TXT dosyasının tamamı UTF-8 BOM (`EF BB BF`) içeriyor.
- `gpc listings get` canlı başlık, kısa açıklama ve tam açıklamaların ilk karakterini `U+FEFF` olarak döndürüyor.
- Görsel olarak çoğunlukla görünmez; ancak karakter sınırı, veri temizliği ve indeksleme açısından kaldırılmalı.
- Çözüm: UTF-8 **BOM'suz** kaydetmek ve canlı metinleri yeniden senkronize etmek; senkron sonrası ilk kod noktası doğrulanmalı.

## Görünürlük ve Rakip Karşılaştırması

29 Ağustos 2026 tarihli, ABD İngilizce “app blocker” ve Türkiye Türkçe “uygulama engelleyici” Play aramalarında Limitra incelenen ilk sonuç grubunda görünmedi. Bu sonuç kişiye, cihaza, ülkeye ve zamana göre değişir; kesin sıra ölçümü değildir.

| Uygulama | İndirme | Puan/yorum | Kurulum modeli | Vitrin avantajı |
|---|---:|---:|---|---|
| Limitra | 10+ | Görünür puan/yorum yok | Kurulum öncesi ücretli | Offline, veri toplamıyor, tek ödeme |
| AppBlock | 10M+ | 4,7 / ~238K | Ücretsiz kurulum + IAP | Güçlü sosyal kanıt, 8 görsel, web+app engelleme |
| Stay Focused | 5M+ | 4,4 / ~169K | Ücretsiz kurulum + reklam/IAP | Güçlü anahtar kelimeler, geniş özellik seti |

Google Play; metinlerin yanında puan, yorum, indirme ve kullanıcı davranışlarını da arama sıralamasında dikkate alır. Bu nedenle yalnızca anahtar kelime değişikliği kısa sürede üst sıra garantilemez.

## Satın Alma Dönüşümü

Mevcut durumdaki en büyük bariyer, kullanıcının uygulamayı hiç denemeden ödeme yapması ve karar anında sosyal kanıt görememesidir. Düşük fiyat tek başına bu güven açığını kapatmıyor; aşırı düşük fiyat kalite algısını da zayıflatabilir.

### Kısa vadede — ücretli modeli koruyarak

1. İkonu ve ilk üç ekran görüntüsünü gerçek, güncel UI ile yenile.
2. 20–30 saniyelik, sessizde de anlaşılır bir preview video ekle: kural oluşturma → limit dolması → kilit → analiz/zincir.
3. Promo kodları hedef kullanıcı/test grubuna dağıtarak gerçek kullanım elde et; yorum karşılığında ödül veya olumlu yorum talebi verme.
4. Uygulama içinde değer görülen doğal bir anda Play In-App Review akışı göster: örneğin 3 başarılı gün veya ilk tamamlanan odak serisi sonrası.
5. Sürüm notlarını “Hata düzeltmeleri” yerine gerçek kullanıcı değerini anlatacak şekilde yaz: disiplin zinciri, daha güvenilir kilit, daha doğru kullanım takibi.

### Orta vadede — ücretsiz kurulum + tek seferlik ömür boyu kilit açma

Bu model, Limitra'nın “abonelik yok” farkını korurken kullanıcıya satın almadan önce güven kazanma fırsatı verir. Örnek ücretsiz katman: 1 uygulama sınırı, temel günlük limit ve 7 günlük analiz. Tek seferlik ürün: sınırsız uygulama, katı kilit, gelişmiş analiz, odak modu, temalar ve tam disiplin geçmişi.

**Kritik karar:** Google Play'de bir uygulama ücretliden ücretsize geçirildikten sonra aynı paket yeniden ücretli yapılamaz. Bu değişiklik ancak Play Billing ile tek seferlik ürün ve mevcut ücretli kullanıcıların hak aktarımı tasarlandıktan sonra yapılmalı.

## Önceliklendirilmiş Yol Haritası

### P0 — yayın kalitesi ve politika/keşif uygunluğu

1. Tüm yeni ekran görüntülerini gerçek uygulamadan `1080x1920` veya üstü üret.
2. “DOWNLOAD NOW” CTA'sını ve Google Play rozetlerini kaldır.
3. Güncel olmayan kilit ekranı ve kurmaca UI görsellerini değiştir.
4. `hi-IN` ve `th` görsel setlerini tamamla.
5. BOM karakterlerini 33 metadata dosyasından ve canlı listing alanlarından kaldır.

### P1 — güven ve dönüşüm

1. İkon/launcher/app içi logo/görsel sistemini tek marka kimliğinde birleştir.
2. İlk üç görseli “engelleme → katı kilit → kolay kurulum” hikâyesine çevir.
3. 11 dilde başlık ve açıklamaları yerel arama niyetine göre düzenle.
4. 20–30 saniyelik gerçek kullanım videosu ekle.
5. Organik, koşulsuz yorum isteme akışı ekle.

### P2 — büyüme optimizasyonu

1. Play Console'da Store listing visitors, install/open clicks, CTR, ülke, dil ve arama terimi kırılımlarını takip et.
2. Trafik yeterli olduğunda ikon ve ilk ekran görüntüsü için A/B testi çalıştır; düşük trafikte test anlamlı sonuç üretmez.
3. “app blocker”, “screen time” ve “digital detox” niyetleri için arama anahtar kelimesi hedefli özel mağaza sayfaları dene.
4. Ücretli uygulama ile ücretsiz kurulum + tek seferlik kilit açma modelini gelir, aktivasyon ve 7 günlük elde tutma verileriyle karşılaştıracak ürün planı hazırla.

## Ölçüm Planı

Değişikliklerden önce ve sonra 28 günlük dönemlerde şu metrikler izlenmeli:

- Store listing visitors
- Unique install/buy clicks ve CTR
- Ülke ve dil bazında CTR
- Play Search / Explore / Ads & referrals kırılımı
- Arama terimleri
- Satın alma, iade ve gelir
- İlk açılış, 1/7/28 günlük elde tutma
- Puan ve yorum hacmi

Bu denetimde Play Console dönüşüm ekranına erişilemedi: CLI mağaza içeriğini okuyabildi, ancak Chrome'daki Google hesabı geliştirici hesabına bağlı değildi; Cloud Storage edinme raporu da yapılandırılmamıştı. Bu nedenle ziyaret→satın alma hunisi hakkında sayısal hüküm verilmedi.

## Kaynaklar

- Google Play — Store listing best practices: https://support.google.com/googleplay/android-developer/answer/13393723
- Google Play — Preview asset requirements: https://support.google.com/googleplay/android-developer/answer/9866151
- Google Play — Search discoverability: https://support.google.com/googleplay/android-developer/answer/4448378
- Google Play — Store listing experiments: https://support.google.com/googleplay/android-developer/answer/12053285
- Google Play — Custom store listings: https://support.google.com/googleplay/android-developer/answer/9867158
- Google Play — User acquisition reports: https://support.google.com/googleplay/android-developer/answer/9859173
- Google Play — Paid/free pricing rule: https://support.google.com/googleplay/android-developer/answer/6334373
- AppBlock listing: https://play.google.com/store/apps/details?id=cz.mobilesoft.appblock
- Stay Focused listing: https://play.google.com/store/apps/details?id=com.stayfocused
