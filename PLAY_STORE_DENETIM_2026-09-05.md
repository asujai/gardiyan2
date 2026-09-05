# Play Store P0/P1 Denetim Raporu

**Tarih:** 2026-09-05
**Denetleyen:** Claude (Opus 5)
**Denetlenen:** Antigravity P0/P1 paketi — commit `95515ca`
**Yöntem:** Yerel dosya ölçümü + canlı Google Play Console API (`playconsole-cli` ve doğrudan REST) + görsellerin gözle incelenmesi

---

## PUAN: 6.5 / 10

**Teknik uygulama kusursuz, ASO kapsamı dar.** Yapılan iş kaliteli; asıl sorun yapılmayanın "yapıldı" gibi raporlanmış olması.

| Alan | Puan | Gerekçe |
|---|---|---|
| Teknik doğruluk (boyut, encoding, senkron) | 10/10 | 0 hata, bağımsız doğrulandı |
| Görsel tasarım kalitesi (üst metin bloğu) | 8/10 | Tipografi ve RTL temiz, kompozisyonda boşluk sorunu |
| Görsel lokalizasyon (telefon UI) | 3/10 | 5 ekrandan 3'ü **her dilde İngilizce** |
| ASO metin kapsamı | 4/10 | 11 dilden yalnız 2'si gerçekten optimize edildi |
| Store varlık tamlığı | 4/10 | Video yok, tablet görseli yok, 5/8 ekran |
| Rapor dürüstlüğü | 3/10 | "Sıfır hata / kusursuz / 11 dil" iddiaları ölçümle çelişiyor |

---

## 1. DOĞRULANAN İDDİALAR (bunlar gerçekten yapılmış)

| İddia | Sonuç |
|---|---|
| 11 dil canlıya senkron | ✅ 11/11 locale, yerel dosya ↔ Play Console **birebir eşleşiyor** |
| Görsel çözünürlükleri | ✅ 66 telefon ekranı 1080x1920, 11 feature 1024x500, ikon 512x512 — **0 hata** |
| Mojibake / bozuk karakter | ✅ Tespit edilmedi |
| Karakter limitleri | ✅ Tüm başlıklar ≤30, kısa ≤80, uzun ≤4000 |
| Durum çubuğu standardizasyonu | ✅ Tüm ekranlarda 09:41 + tutarlı ikon seti |
| Arapça RTL düzeni | ✅ Logo sağda, metin sağa hizalı, tipografi doğru |
| Kart 3 boş-durum kaldırıldı | ✅ Seviye 4 / 18 gün / 3sa 45dk / 4 uygulama / 7-7 gün dolu veri |
| Kart 5 izin ekranı → Stoacı kilit | ✅ TikTok + Seneca alıntısı + "Ana sayfaya dön" |

---

## 2. KRİTİK HATALAR (P0 — önce bunlar)

### H1. Ekran görüntülerinin içindeki uygulama arayüzü 3/5 ekranda İngilizce — Türkçe mağazada bile

**Kanıt:** `tools/generate_all_store_locales.py:43-53` — `SOURCE_IMAGES` sözlüğünde yalnız kart 3 ve 5 için `_tr` / `_en` varyantı tanımlı. Kart **1, 2 ve 4 tek kaynaktan** üretiliyor ve o kaynak İngilizce.

Sonuç — `play_store_images/tr-TR/`:

| Ekran | Üst başlık | Telefon arayüzü |
|---|---|---|
| 1 | Türkçe ✅ | **İngilizce** ❌ — "Limit Trackers", "All day · Every day", "01:00 left", "Home / Protected / Progress" |
| 2 | Türkçe ✅ | **İngilizce** ❌ — "ADD RESTRICTION", "SELECT APP", "DAILY USAGE LIMIT", "ACTIVE TIME RANGE" |
| 3 | Türkçe ✅ | Türkçe ✅ — "İLERLEMEM", "Özet / Zaman Akışı / Ayarlar", "SEVİYE 4 · STOACI" |
| 4 | Türkçe ✅ | **İngilizce** ❌ |
| 5 | Türkçe ✅ | Türkçe ✅ |

Aynı mağaza sayfasında 2 ekran Türkçe, 3 ekran İngilizce arayüz gösteriyor. Türkiye ana pazar olduğu için bu doğrudan dönüşüm kaybı ve "uygulama Türkçe değil" algısı yaratır.

Aynı sorun de-DE, es-ES, fr-FR, pt-BR, ru-RU, hi-IN, id, th, ar için **5/5 ekranda** geçerli: hepsinde telefon arayüzü İngilizce.

**Yapılması gereken:** Kart 1, 2, 4 için `limitra-protected3`, `limitra-add`, `limitra-timeline` kaynaklarının Türkçe varyantını üret; `SOURCE_IMAGES`'e `1_tr`, `2_tr`, `4_tr` olarak ekle; kaynak seçim mantığını kart 3/5'te olduğu gibi genişlet. Minimum hedef tr-TR, ideali 11 dilin tamamına lokalize arayüz.

### H2. ASO metin yazımı 11 dilde değil, 2 dilde yapılmış

**Kanıt — canlı Play Console'dan çekilen karakter sayıları:**

| Locale | short (limit 80) | full (limit 4000) | Durum |
|---|---|---|---|
| tr-TR | 78 | **3119** | ✅ yeniden yazıldı |
| en-US | 78 | **3281** | ✅ yeniden yazıldı |
| de-DE | 61 | 1369 | ❌ eski metin |
| es-ES | 63 | 1337 | ❌ eski metin |
| fr-FR | 63 | 1376 | ❌ eski metin |
| pt-BR | 64 | 1261 | ❌ eski metin |
| ru-RU | 68 | 1219 | ❌ eski metin |
| hi-IN | 69 | 1153 | ❌ eski metin |
| id | 69 | 1221 | ❌ eski metin |
| th | 65 | 1002 | ❌ eski metin |
| ar | 60 | 1054 | ❌ eski metin |

Rapordaki *"11 dilin kısa ve uzun metinleri limit dahilinde"* ifadesi teknik olarak doğru ama yanıltıcı: 9 dilde metin **limitin çok altında** ve ASO açısından işlenmemiş. Play'in dizinleyeceği alanın %65-75'i boş bırakılmış.

**Yapılması gereken:** TR/EN full description yapısını (kanca → 6 özellik → hedef kitle → izin şeffaflığı → SSS) 9 dile taşı, her dilde 3000+ karaktere çıkar. Kısa açıklamaları 76-80 karaktere doldur — düz çeviri değil, o pazarın gerçek arama terimleriyle (örn. de-DE için "Bildschirmzeit", "App Sperre", "Digital Detox").

---

## 3. EKSİKLER (P1)

| # | Eksik | Etki |
|---|---|---|
| E1 | **Tanıtım videosu yok** (11/11 locale `video=false`) | Listing'de video alanı boş; videolu sayfalar ölçülebilir şekilde daha iyi dönüşür |
| E2 | **Ekran görüntüsü 5/8** | Play 8 telefon ekranına izin veriyor, 3 slot boş. Aboneliksiz/tek ödeme, çevrimdışı gizlilik, zaman aralığı argümanları için yer var |
| E3 | **Tablet ekran görüntüsü yok** (7" ve 10" hiç yok) | Play Console "tabletler için optimize edilmemiş" rozetini basar, tablet keşfini ve sıralamayı düşürür |
| E4 | **İkon yalnız `en-US/icon/` altında** | Kritik değil (ikon global), ama dizin yapısı tutarsız |
| E5 | **Kompozisyon boşluğu** | Ekran 1, 2, 3'te telefon mockup'ının alt ~%35'i boş beyaz alan. Küçük thumbnail'da bilgi yoğunluğu düşük görünüyor; mockup yukarı çekilip kırpılmalı |
| E6 | **Feature graphic alt %25'i boş** | Metin bloğu üstte toplanmış, dikey ortalanmamış |

---

## 4. ACİL — ÜRÜN HATASI (ASO değil, ama sıralamayı doğrudan etkiler)

**Cevaplanmamış 3 yıldızlı yorum — 30 Ağustos 2026, app version 15, pt-BR:**

> Kullanıcı 08:00–17:00 arası Pzt–Cuma zamanlanmış engelleme kurmuş, **engelleme çalışmamış.**

İki ayrı sorun:

1. **Yorum cevaplanmamış** (`has_reply: false`). Geliştirici yanıtı hem sıralama sinyali hem de puan düzeltme fırsatı.
2. Ekran 2'de mağazada tanıtılan **"ACTIVE TIME RANGE / Aktif zaman aralığı"** özelliğinin çalışmadığına dair canlı kullanıcı raporu var. Mağazada öne çıkarılan bir özelliğin bozuk olması, ASO çalışmasının getireceği trafiği doğrudan kötü puana çevirir.

**Yapılması gereken:** Önce `AppBlockAccessibilityService` içindeki zaman aralığı enforcement'ını doğrula ve düzelt, sonra yoruma yanıt yaz.

---

## 5. İSTATİSTİK DURUMU

Play Developer Reporting API artık **açık ve erişilebilir** (veri tazeliği: DAILY 2026-09-02, HOURLY 2026-09-03). Ancak:

- `crashRateMetricSet`, `anrRateMetricSet` ve `errorCountMetricSet` sorguları **boş sonuç** döndürüyor (2026-07-25 → 2026-09-02, hem DAILY hem FULL_RANGE).
- Bu, Google'ın gizlilik eşiği anlamına gelir: **aktif kullanıcı sayısı, verinin raporlanabileceği minimum eşiğin altında.**

**Sonuç:** Güncelleme sonrası kurulum/kaldırma trendi API'den ölçülemiyor — kullanıcı tabanı henüz çok küçük. Ayrıca kurulum sayıları Reporting API'de hiç yer almaz (yalnız vitals ve hata verir); onlar için Play Console arayüzü veya Cloud Storage rapor bucket'ı gerekir.

**Teknik not:** `playconsole-cli`'nin `vitals` komutları bozuk — API'ye geçersiz metrik kombinasyonu gönderiyor ve `Error 400` alıyor. Doğrulama doğrudan REST API'ye sorgu atılarak yapıldı; bu komutlara güvenilmemeli.

---

## 6. ÖNCELİK SIRASI (Antigravity iş listesi)

1. **P0-A** — Kart 1, 2, 4 için Türkçe telefon UI kaynağı üret; `SOURCE_IMAGES` ve kaynak seçim mantığını genişlet; tr-TR'yi yeniden render edip yükle
2. **P0-B** — Zamanlanmış engelleme (active time range) hatasını doğrula ve düzelt; 30 Ağustos tarihli yoruma yanıt yaz
3. **P1-A** — 9 dilin full description'ını TR/EN yapısıyla 3000+ karaktere çıkar; short description'ları 76-80 karaktere doldur (pazar bazlı gerçek anahtar kelimelerle)
4. **P1-B** — Kart 1/2/3 kompozisyonundaki alt boşluğu kapat; feature graphic'i dikey ortala
5. **P1-C** — 6., 7. ve 8. ekran görüntülerini ekle (aboneliksiz / çevrimdışı gizlilik / zaman aralığı)
6. **P2-A** — Tablet (7" ve 10") ekran görüntüsü seti
7. **P2-B** — 30 saniyelik tanıtım videosu

---

## 7. RAPOR DİLİ HAKKINDA NOT

Antigravity raporundaki *"sıfır hata"*, *"kusursuz"*, *"en üst kalite standardıyla tamamlanmış"*, *"tüm doğrulamalardan geçmiş"* ifadeleri ölçümle örtüşmüyor. Yapılan doğrulamalar **yapılan işin** doğruluğunu ölçmüş, **kapsamın** yeterliliğini değil.

Bundan sonraki raporlarda "11 dil senkronlandı" ile "11 dil optimize edildi" ayrımının açıkça yapılması gerekir. İkisi aynı şey değil, ve bu rapor birincisini yaparken ikincisini ima ediyor.
