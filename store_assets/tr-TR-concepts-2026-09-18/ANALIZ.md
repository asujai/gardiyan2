# Limitra — reklam öncesi mağaza değerlendirmesi

18 Eylül 2026 — Codex

## Karar

Mevcut mağaza görsel olarak tutarlı, ancak soğuk reklam trafiğini satın almaya ikna etmek için zayıf noktaları var. Öznel uzman değerlendirmem: **6/10**. Bu puan ölçülmüş dönüşüm oranı değildir. Ürünü zaten arayan kullanıcı satın alabilir; reklamla ilk kez tanışan kullanıcı için gerçek çalışma biçimi ve güven daha görünür olmalı. Büyük reklam bütçesinden önce güveni zedeleyen iddiaları düzeltmek ve ilk ekranları sadeleştirmek önerilir.

## İnceleme kapsamı ve sınırları

- Türkçe/Türkiye canlı sayfa tarayıcıda incelendi: başlık, ₺29,99 satın alma düğmesi, 10+ indirme, görünür puan bulunmaması, ekran görüntüsü şeridi ve kısa açıklama. Bu gözlemler 18 Eylül oturumuna aittir; ülke ve zamana göre değişebilir.
- Canlı şeritle görsel olarak eşleşen yerel `play_store_images/tr-TR/phoneScreenshots/1.png`–`8.png` dosyaları tam boy incelendi. Birebir sunucu dosyası hash karşılaştırması yapılmadı.
- limitra.online ana sayfasının metni incelendi. Canlı uygulama cihazda çalıştırılmadı; sınırlı kaynak kod ve proje durum karşılaştırması yapıldı.
- Play Console satış, trafik, iade ve dönüşüm verileri alınmadı. Satın alma yüzdesi veya beklenen reklam getirisi tahmin edilmedi.

## Güçlü yanlar

- Lacivert/turkuaz renkler, başlıklar ve ikon tutarlı bir ürün kimliği oluşturuyor.
- İlk kart ürün kategorisini anlatıyor; temel sorunla bağlantısı var.
- Günlük limit, kilit ekranı, ilerleme ve gizlilik ayrı ayrı gösteriliyor.
- Aboneliksiz model ve cihazda işleme, karar vermeyi kolaylaştırabilecek gerçek farklılıklar.

## Kart bazında değerlendirme

| Kart | Bulgu | Öneri |
|---|---|---|
| 1 — Dikkat dağıtanları engelle | Başlık anlaşılır; sekiz uygulamalı liste küçük önizlemede ayrıntı kalabalığına dönüşüyor. | Tek sonuç ve büyük ürün detayı; sınıra ulaşınca ne olduğunu göster. |
| 2 — Limitleri ayarla | Fazla küçük yazı, çok sayıda iddia ve kontrol. Sıkı kilit açıklamasıyla “istediğin an düzenlenebilir” metni bağlamsız çelişiyor. | Süre ve gün seçimini öne çıkar; modların farkını açıklamada anlat. |
| 3 — Zinciri kur | Tutarlı fakat satın almadan önce mekanizmayı anlaması gereken kullanıcı için erken. | İlerleme kartını 4–5. sıraya taşı. |
| 4 — Her hareket kaydedilir | Gözetlenme çağrışımı gizlilik mesajını zedeliyor. | “İlerlemeni gör.” / “Limit geçmişini cihazında takip et.” |
| 5 — Stoacı kilit | Ürünün somut sonucu burada; ilk kez gelen kişi bunu geç görüyor. | İlk iki karta taşı. “Dürtüsel kullanımı bilgece durdur” yerine günlük dil kullan. |
| 6 — Tek ödeme | Ücretli üründe “Pro’ya geç” ikinci ödeme kuşkusu yaratıyor. İade garantisi ve sonsuz özellik taahhüdü gereksiz risk. | Gerçek olmayan ödeme ekranını kaldır; aboneliksizliği açıklamada net anlat. |
| 7 — Gizlilik | Avantaj güçlü; “yerel Room DB” kullanıcı için teknik. Şifreli saklama iddiası ayrıca kanıt gerektiriyor. | “Verilerin sende kalır.” İnternet izni yok/hesap yok yeterli. |
| 8 — Saat aralığı | Kullanım senaryosu iyi; bağımsız “odak modu” sayfasının gerçek arayüzle eşleşmesi doğrulanmalı. | Gerçek ayar ekranı üzerinden seçili saatleri göster; otomatik tam engel ile limitin etkin olduğu aralığı karıştırma. |

## Reklamdan önce giderilecek güven sorunları

1. **Pro ekranı:** İncelenen kaynakta BillingClient veya Pro yükseltme akışı bulunmadı; proje durumu uygulama içi satın alımı ertelenmiş olarak kaydediyor. Kart 6 gerçek ürün ekranı olarak sunulmamalı.
2. **48 saat iade garantisi:** Google politikası koşula bağlı geri ödeme olasılığını anlatır; herkese koşulsuz garanti vermez. Bu ifadeyi kaldırın.
3. **Kanıtsız sayılar:** Kart 2’deki “<%0.8” pil tüketimi ve “haftada 14 saat 30 dakika” tasarruf için ölçüm/metodoloji yok. Gösterim verilerini beklenen kullanıcı sonucu gibi sunmayın.
4. **Şifreleme:** GuardianDatabase standart Room builder kullanıyor; incelenen konfigürasyonda uygulama düzeyinde şifreleme sağlayıcısı görülmedi. Android cihaz şifrelemesi ile uygulamaya özgü veritabanı şifrelemesini eşitlemeyin. “Cihazında saklanır” yeterli.
5. **Kesin aşılmazlık:** “Bypass edilemez”, “kesintisiz” gibi mutlak vaatleri cihaz izinleri/arka plan davranışıyla sınırlayın. Kullanıcıya etkin korumanın gerektirdiği izinleri açıklayın.

Manifest, INTERNET iznini `tools:node="remove"` ile kaldırıyor; yalnızca kelimenin dosyada bulunması internet yetkisi olduğu anlamına gelmez. Çevrimdışı konumlandırma kaynakla uyumlu.

## Önerilen anlatı sırası

1. **Sonuç:** “Biraz daha değil. Bugünlük bu kadar.” — gerçek kilit ekranı.
2. **Kontrol:** “Sınırını sen belirle.” — gerçek günlük süre/gün ayarı.
3. **Günlük kullanım:** “Kalan süreni bir bakışta gör.” — gerçek korunan uygulamalar ekranı.
4. **Alışkanlık:** “İlerlemeni gör.” — gerçek disiplin/seri ekranı.
5. **Gizlilik:** “Verilerin sende kalır.” — kısa açıklama, gerçek ürünle bağlantı.
6. **Program:** “Gününe uygun sınırlar.” — gerçek saat aralığı ayarı ve doğru davranış açıklaması.

Sekiz yuvanın tamamını doldurmak kendi başına hedef değildir. Boşluğu sıfırlamak da kalite ölçütü değildir: okunabilirlik, tek mesaj, ürün doğruluğu ve satın alma tereddüdünü azaltma daha önemlidir. İlk üç kartta ürün arayüzü öncelikli olmalı.

## Üretilen Türkçe görseller

- `01-bugunluk-bu-kadar.png`: temel problem/sonuç; mevcut kilit kartından referansla üretildi.
- `02-sinirini-belirle.png`: süre ve gün seçiminin sade sunumu.
- `03-verilerin-sende.png`: gizlilik avantajı için tanıtım illüstrasyonu; önerilen mağaza dizisinde 5. sıra, üçüncü ekran değil.

**Teslim niteliği:** Bunlar üç bitmiş raster **tasarım konseptidir**, doğrulanmış uygulama ekran görüntüsü veya yayına hazır tam mağaza seti değildir. ImageGen çıktıları 941×1672 piksel; istenen 1080×1920 çözünürlük gerçekleşmedi. Metinler görsel olarak okunup Türkçe kontrol edildi. İkinci kartta metin ve kontrol ölçeği belirgin iyileşti; üçüncü kart arayüz göstermediğinden ilk üç ürün ekranı yerine kullanılmamalı.

Yayın üretiminde orijinal logo dosyasını ve güncel cihazdan alınan ekranları kullanın; AI ile yeniden çizilen UI parçalarını gerçek ekran kırpımlarıyla değiştirin. Başlık/metin katmanlarını düzenlenebilir tutup en az dört gerçek ekranı 1080×1920 olarak dışa aktarın. Slogan alanını Google'ın önerdiği yaklaşık %20 sınırına indirin. AI konseptte logo ve arayüzün yeniden çizilmiş olması özellikle doğrulanmalıdır. Dosyalar canlıya yüklenmedi; mevcut varlıklar korunuyor.

### Yerelleştirme metinleri

| Dosya | Başlık | Destek metni |
|---|---|---|
| 01 | Biraz daha değil. Bugünlük bu kadar. | Günlük limit koy. Süre dolunca uygulama engellensin. |
| 02 | Sınırını sen belirle. | Uygulamayı, günlük süreyi ve günleri seç. |
| 03 | Verilerin sende kalır. | Hesap açmadan. İnternete bağlanmadan. |

## Açıklama önerisi

Kısa açıklama adayı: **Günlük limit koy, dikkat dağıtan uygulamaları engelle. Çevrimdışı, aboneliksiz.**

Uzun açıklama açılışı: “Biraz bakıp çıkacağım derken zaman mı geçiyor? Limitra ile seçtiğin uygulamalara günlük süre sınırı koy. Süre dolduğunda engelleme ekranı devreye girsin. Tek seferlik satın alım; abonelik ve reklam yok. Kullanım verilerin cihazında kalır.”

Ardından üç kurulum adımı, sıkı modun gerçek sınırları, izinlerin nedenleri ve cihaz/pil ayarları gelsin. “Bağımlılığı kırar” gibi kesin sonuçlar yerine davranışı destekleyen somut özellikler anlatılsın.

## Web sitesiyle uyum

Ana sayfanın sakin ve kullanıcı odaklı tonu güçlü. Fakat üst bölüm Android 7.0+, alt bölüm Android 8.0+ diyor; kaynak `minSdk=24`, yani 7.0. Uyumluluk ifadesi tutarlı olmalı. Site dolar, Türkiye mağazası TL gösteriyor; tek bir evrensel fiyat yerine yerel güncel fiyat için mağazaya yönlendirmek daha net. Sitedeki temsili arayüz etiketlenmiş olsa da gerçek ürün görüntüsü satın alma güvenini artırabilir. “Dünya basını” içeriği ürüne verilmiş basın onayı olarak sunulmamalı.

## Reklam testi

- İlk aşamada tek hedef kitle/ülke/dil seçin; reklamda verilen söz ilk mağaza görselinde devam etsin.
- Önce doğruluk sorunlarını giderin; sonra ilk görsel için iki varyantı test edin. Aynı testte fiyat, ikon, metin ve tüm ekranları değiştirmeyin.
- Play Console deneyinde yeterli veri ve güven aralığı oluşmadan kazanan ilan etmeyin. 10+ indirme toplamı, mevcut trafik hızı veya dönüşüm oranını göstermez.
- Mağaza düğmesi tıklaması, tamamlanan satın alma ve net gelir ayrı ölçümlerdir. Reklam tıklamasını satın alma sanmayın. Ülke/dil/kampanya kırılımında mağaza etkileşimi, gerçekleşen satış, iade ve kullanıcı başına edinme maliyetini birlikte değerlendirin.
- Reklamı ölçeklendirme ölçütü: net satış katkısı edinme maliyetini karşılıyor mu? Görseller tek başına kârlılık garantisi değildir.
- Gerçek uygulama akışından kısa bir demo hazırlayın: limit belirleme → süre dolması → engelleme. İlk saniyelerde ürünün ne yaptığını gösterin.

## Kaynaklar

- [Canlı Türkçe mağaza](https://play.google.com/store/apps/details?id=com.gardiyan.app&hl=tr&gl=TR)
- [Limitra web sitesi](https://limitra.online/)
- [Google görsel rehberi](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en): gerçek ürün deneyimi, ilk üç ekranda UI, küçük yazıdan kaçınma, önerilen slogan oranı ve tavsiye yüzeyleri için çözünürlük. Zorunlu koşullar ile “highly recommended” önerileri farklıdır; her öneri ihlali mağazadan kaldırma anlamına gelmez.
- [Google geri ödeme koşulları](https://support.google.com/googleplay/answer/2479637?hl=tr)
- [Mağaza A/B deneyleri](https://support.google.com/googleplay/android-developer/answer/12053285?hl=en)
- [Mağaza performans ölçümü](https://support.google.com/googleplay/android-developer/answer/9859173?hl=en)

— Codex, 2026-09-18
