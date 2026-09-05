# Play Store — Yarım Kalan İşler Listesi

**Tarih:** 2026-09-05
**Hazırlayan:** Claude (Opus 5)
**Hedef:** Antigravity
**Mevcut sürüm:** v1.2.0 / versionCode 17, production %100
**Ölçüm kaynağı:** Canlı Play Console API (`gpc`), yerel dosyalar, görsellerin gözle incelenmesi

Bu liste yalnızca **mağaza (store listing) tarafını** kapsar. Uygulama kodu ve servis kararlılığı ayrı konudur.

---

## ÖZET TABLO

| # | İş | Öncelik | Neden yarım |
|---|---|---|---|
| M1 | Ekran görüntülerindeki alt boşluk | P1 | "Giderildi" denildi, ölçümde %25-30 boşluk duruyor |
| M2 | Boş 3 ekran görüntüsü slotu | P1 | 8 slot var, 5 dolu |
| M3 | Tablet ekran görüntüleri | P2 | Canlıda 0 adet, hiç üretilmedi |
| M4 | Tanıtım videosu | P2 | 11/11 locale'de yok |
| M5 | 9 dilde telefon içi arayüz | P2 | Yalnız tr-TR lokalize edildi |
| M6 | Feature graphic dikey denge | P3 | Metin bloğu üstte toplanmış |
| M7 | İkon dizin tutarsızlığı | P3 | Yalnız `en-US/icon/` altında |

**Canlı doğrulama (tr-TR):** `phoneScreenshots: 5`, `sevenInchScreenshots: 0`, `tenInchScreenshots: 0`, `featureGraphic: 1`, `icon: 1`.

---

## M1 — Ekran görüntülerindeki alt boşluk (P1)

### Neden yarım
`28a6bab` raporunda *"Tüm kartlardaki alt boşluklar giderildi ve kart yoğunluğu dengelendi"* denildi. Boşluk azaldı ama gitmedi. tr-TR görsellerinde telefon çerçevesi içindeki ölçüm:

| Ekran | İçerik nerede bitiyor | Telefon çerçevesi nerede bitiyor | Boş alan |
|---|---|---|---|
| 1 (Limit Takibi) | ~1200 px (FAB 1715'te ayrı duruyor) | ~1870 px | **~%25** |
| 2 (Kısıtlama Ekle) | ~1340 px | ~1870 px | **~%25** |
| 4 (Zaman Akışı) | ~1240 px | ~1870 px | **~%30** |

Kart 4 en kötüsü: dört olay kartı ve gizlilik notundan sonra ekranın alt üçte biri tamamen boş beyaz.

### Yapılacak
İki yoldan biri:

**A. İçerik ekle (tercih edilen)**
- Kart 1: 4. bir uygulama satırı ekle (örn. YouTube Shorts veya X, "KORUNUYOR" durumunda). Alt boşluk kapanır, ürün daha dolu görünür.
- Kart 4: 2-3 olay kartı daha ekle (örn. "Seviye Atladı — Seviye 4 Stoacı", "Günlük Sıfırlama", "Başarı Serisi 18 Gün"). Zaman akışının gerçekten zengin bir geçmiş tuttuğu izlenimi güçlenir.
- Kart 2: "Korumayı Başlat" butonunun altına küçük bir bilgi satırı ("Koruma anında devreye girer, yeniden başlatma gerekmez") ekle.

**B. Çerçeveyi kırp**
Telefon mockup'ının yüksekliğini içeriğe göre kısalt ve 1080x1920 tuval içinde dikeyde ortala.

### Kabul kriteri
Her tr-TR ekran görüntüsünde telefon çerçevesi içindeki boş alan **%10'un altında** olmalı. Ölçüm: içeriğin bittiği piksel ile çerçevenin bittiği piksel arasındaki fark / çerçeve yüksekliği.

### Dosyalar
`tools/generate_all_store_locales.py`, `scratch/generate_all_cards.py`, `play_store_images/*/phoneScreenshots/`

---

## M2 — Boş 3 ekran görüntüsü slotu (P1)

### Neden yarım
Google Play telefon için **8 ekran görüntüsüne** izin veriyor. Canlıda **5** var. Üç slot boş duruyor ve bunlar ücretsiz reklam alanı.

### Yapılacak
Üç yeni kart üret. Önerilen içerik — hepsi full description'da zaten geçen ama görselleştirilmemiş satış argümanları:

**Kart 6 — Aboneliksiz tek ödeme**
- Eyebrow: `ABONELİK YOK`
- Başlık: `BİR KEZ ÖDE.\nÖMÜR BOYU KULLAN.`
- Alt başlık: `Aylık ücret yok, gizli ödeme yok.`
- Telefon içi: fiyatlandırma/bilgi ekranı — "Tek ödeme", "Abonelik yok", "Reklam yok" işaretli liste
- Gerekçe: Rakiplerin neredeyse tamamı abonelikli. En güçlü farklılaştırıcı ama hiçbir görselde yok.

**Kart 7 — %100 çevrimdışı ve gizli**
- Eyebrow: `VERİ CİHAZINDA KALIR`
- Başlık: `HESAP YOK.\nSUNUCU YOK.`
- Alt başlık: `Kullanım verin telefonundan hiç çıkmaz.`
- Telefon içi: gizlilik/ayarlar ekranı — "İnternet izni kullanılmıyor", "Veri yalnızca cihazda", "Hesap gerekmez"
- Gerekçe: Ekran süresi uygulamalarında gizlilik en büyük satın alma engeli.

**Kart 8 — Zamanlanmış koruma**
- Eyebrow: `AKTİF ZAMAN ARALIĞI`
- Başlık: `İŞ SAATLERİNDE\nOTOMATİK KİLİT.`
- Alt başlık: `Seçtiğin saatlerde ve günlerde kendiliğinden devreye girer.`
- Telefon içi: 09:00-18:00 Pzt-Cum aralığı aktifken bir uygulamanın otomatik kilitlendiği an
- Gerekçe: Kart 2'de ayar olarak görünüyor ama **sonucu** hiç gösterilmiyor. Ayrıca Brezilyalı kullanıcının şikayet ettiği özellik bu; öne çıkarmak hem satış hem güven.

### Kabul kriteri
11 dilin tamamında 8 telefon ekran görüntüsü, 1080x1920, canlıda doğrulanmış (`gpc images list --locale <l> --type phoneScreenshots` → 8 satır).

---

## M3 — Tablet ekran görüntüleri (P2)

### Neden yarım
Hiç üretilmedi. Canlı doğrulama: `sevenInchScreenshots: No images found`, `tenInchScreenshots: No images found`.

### Etki
- Play Console uygulamaya **"tabletler için optimize edilmemiş"** uyarısı basar.
- Tablet kullanıcılarının aramalarında ve "Tabletler için" bölümünde sıralama düşer.
- Play'in büyük ekran kalite katmanına hiç girilemez.

### Yapılacak
Mevcut kart tasarımlarını tablet tuvaline uyarla:
- **7 inç:** 1200x1920 (dikey) — en az 2, ideal 4 görsel
- **10 inç:** 1600x2560 (dikey) — en az 2, ideal 4 görsel

Not: Telefon görsellerini büyütmek yeterli değil; Play inceleme ekibi ve kullanıcı bunu fark eder. Tablet düzeninde telefon mockup'ı yerine geniş içerik yerleşimi kullanılmalı (iki sütun veya daha geniş kartlar).

Öncelik dil sırası: `en-US`, `tr-TR`, sonra kalan 9 dil.

### Kabul kriteri
En az `en-US` ve `tr-TR` için 4'er adet 7" ve 10" görsel canlıda; `gpc images list --type sevenInchScreenshots` boş dönmemeli.

---

## M4 — Tanıtım videosu (P2)

### Neden yarım
11/11 locale'de `video: false`. Hiç üretilmedi.

### Yapılacak
20-30 saniyelik ekran kaydı, YouTube'a yüklenip Play Console'a URL olarak bağlanır (Play videoyu dosya olarak kabul etmez, YouTube linki ister).

Önerilen akış:
1. (0-4 sn) Sonsuz kaydırma — telefon elde, TikTok akışı
2. (4-10 sn) Limitra'da kısıtlama ekleme — uygulama seç, 15 dakika, Korumayı Başlat
3. (10-16 sn) Süre dolar, Stoacı kilit ekranı belirir, Seneca alıntısı okunur
4. (16-22 sn) İlerleme ekranı — 18 günlük seri, Seviye 4 Stoacı
5. (22-28 sn) Kapanış kartı: `LIMITRA — Tek ödeme. Abonelik yok. %100 çevrimdışı.`

Teknik: dikey 1080x1920, sessiz izlenebilir olmalı (metin katmanlı), ilk 3 saniyede ürünün ne yaptığı anlaşılmalı.

### Kabul kriteri
En az `en-US` ve `tr-TR` locale'inde video URL'si tanımlı; `gpc listings get --locale tr-TR` çıktısında `video` alanı dolu.

---

## M5 — 9 dilde telefon içi arayüz (P2)

### Neden yarım
`28a6bab` ile yalnız `tr-TR` için 5/5 ekranın telefon içi arayüzü Türkçeleştirildi. Diğer 9 dilde (`de-DE`, `es-ES`, `fr-FR`, `pt-BR`, `ru-RU`, `hi-IN`, `id`, `th`, `ar`) üst başlıklar lokalize ama **telefon çerçevesi içindeki arayüz İngilizce**.

Raporda bu bilinçli tercih olarak belirtildi. Ancak tr-TR için yapılan yatırımın gerekçesi neyse (kullanıcı kendi dilinde arayüz görmezse uygulamanın o dili desteklemediğini sanır), aynısı bu 9 pazar için de geçerli.

### Yapılacak
`tr-TR` modelini tekrarla: her dil için kart 1, 2, 3, 4, 5'in telefon içi metinlerini o dile çevirip HTML/CSS kaynaklarını üret.

Öncelik sırası (pazar büyüklüğü ve indirme potansiyeline göre):
1. `pt-BR` — mevcut tek kullanıcı yorumu buradan geldi, aktif pazar
2. `es-ES` — geniş konuşur kitlesi
3. `de-DE` — yüksek ödeme gücü, tek ödeme modeline uygun
4. `fr-FR`
5. `id` — büyük Android pazarı
6. `ru-RU`, `hi-IN`, `th`, `ar`

Arapça için ek gereksinim: telefon içi arayüzün de RTL olması gerekir (üst başlıklarda RTL zaten doğru uygulanmış).

### Kabul kriteri
Her dil için 5 ekranın telefon içi metinleri o dilde; gözle doğrulama.

---

## M6 — Feature graphic dikey denge (P3)

### Neden yarım
1024x500 tuvalde metin bloğu üstte toplanmış, alt ~%25 boş. Logo solda dikeyde ortalı ama sağdaki metin grubu değil.

### Yapılacak
Sağdaki metin bloğunu (LIMITRA + başlık + meta satırı + alt başlık) dikeyde ortala. Gerekirse alt başlık satırının altına ince bir ayraç veya küçük bir rozet ("TEK ÖDEME") ekleyerek dengeyi kur.

### Kabul kriteri
Metin bloğunun üst ve alt boşluğu birbirine ±%5 içinde eşit.

---

## M7 — İkon dizin tutarsızlığı (P3)

### Neden yarım
`play_store_images/en-US/icon/icon.png` var, diğer 10 dilde `icon/` klasörü yok. İkon Play'de global bir varlık olduğu için işlevsel sorun yaratmıyor, ancak `gpc images sync` çalıştıran bir sonraki kişi için kafa karıştırıcı.

### Yapılacak
İki seçenekten biri: ikonu dil dizinlerinden çıkarıp `store_assets/icon/` altında tek yerde tut, veya senkron betiğine ikonun locale'den bağımsız olduğunu belirten bir yorum ekle.

### Kabul kriteri
Dizin yapısı kendini açıklıyor; yeni gelen biri ikonun neden tek dilde durduğunu sormuyor.

---

## SIRALAMA ÖNERİSİ

Etki/emek oranına göre:

1. **M2** (3 yeni ekran görüntüsü) — en yüksek dönüşüm etkisi, mevcut altyapıyla üretilebilir
2. **M1** (alt boşluk) — M2 ile aynı üretim turunda halledilebilir
3. **M5/pt-BR ve es-ES** — mevcut pazarlarda doğrudan etki
4. **M3** (tablet) — Play kalite rozetini açar
5. **M4** (video) — en yüksek emek, en yüksek dönüşüm; ayrı bir üretim işi
6. **M6, M7** — kozmetik, boş vakitte

---

## ÖNEMLİ NOT — kod dosyası çakışması

`app/src/main/java/com/gardiyan/app/service/AppBlockAccessibilityService.kt` dosyasında `d1acdc8` commit'iyle kilit tetikleme ve UsageStats yedek muhasebesi düzeltmeleri var. Bu listedeki işler mağaza tarafı olduğu için o dosyaya dokunulmamalı. Dokunulması gerekirse önce `git pull` / mevcut hal okunmalı, üzerine yazılmamalı.

---

## RAPORLAMA İSTEĞİ

Her madde için "yapıldı" derken lütfen **ölçümü** yaz:
- Ekran görüntüsü sayısı → `gpc images list --locale <l> --type phoneScreenshots` çıktısındaki adet
- Boşluk oranı → içeriğin bittiği piksel / çerçeve yüksekliği
- Video → `gpc listings get` çıktısındaki `video` alanı

"Giderildi" ifadesi tek başına yeterli değil; M1 tam olarak bu yüzden ikinci kez listeye girdi.
