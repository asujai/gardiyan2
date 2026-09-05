# v1.2.0 Denetim Raporu (2. Tur)

**Tarih:** 2026-09-05
**Denetleyen:** Claude (Opus 5)
**Denetlenen:** Antigravity `28a6bab` — v1.2.0 / versionCode 17
**Yöntem:** Canlı Play Console API (11 locale) + görsellerin gözle incelenmesi + kaynak kod okuma + cihaz veritabanı

---

## PUAN: 8 / 10

Önceki turda 6.5'ti. Ciddi ilerleme var: H1 ve H2 gerçekten çözülmüş. İki iddia doğrulanmadı, biri ciddi.

---

## 1. DOĞRULANAN İDDİALAR

| İddia | Ölçüm | Sonuç |
|---|---|---|
| 9 dilin full description 3,206-3,981 karaktere çıkarıldı | de 3974, es 3831, fr 3981, pt 3739, ru 3519, hi 3315, id 3935, th 3352, ar 3206 | ✅ Doğru |
| Kısa açıklamalar 70-80 banda çekildi | 70-80 arası, 11/11 | ✅ Doğru |
| 11 dil canlıya senkron | 11/11 locale yerel ↔ Play Console birebir | ✅ Doğru |
| tr-TR 5/5 ekran telefon içi Türkçe | Kart 1 "LİMİT TAKİBİ", Kart 2 "KISITLAMA EKLE", Kart 4 "İLERLEMEM / Zaman Akışı" gözle doğrulandı | ✅ Doğru |
| v1.2.0 / code 17 production %100 | `gpc tracks list` → `version_codes:[17], status:completed, rollout:100` | ✅ Doğru |
| `normalizeToCalendarDay` motoru | 7 dil + ISO indeks, çakışma taraması yapıldı, hatalı eşleşme yok | ✅ Kod doğru |
| Brezilya yorumu cevaplandı | `has_reply: true` | ✅ Doğru (kullanıcı yazdı) |

Görsel kalitesi belirgin şekilde arttı. Kart 1 artık üç uygulama ve üç farklı durum (KORUNUYOR / KILITLENDI / Süre Doldu) gösteriyor; Kart 2 çip seçimi, zaman aralığı ve gün seçiciyle dolu. Bu, önceki tek satırlık boş ekranlardan çok daha iyi bir satış argümanı.

---

## 2. DOĞRULANMAYAN İDDİA (ciddi)

### H4 düzeltmesi büyük ihtimalle kök nedeni çözmüyor

**İddia:** Brezilyalı kullanıcının "08:00-17:00 Pzt-Cum engellemiyor" hatası, Portekizce gün adlarının takvim günlerine eşlenememesinden kaynaklanıyordu ve `normalizeToCalendarDay` ile kökünden çözüldü.

**Ölçüm — bu senaryo mümkün değil:**

`app/src/main/java/com/gardiyan/app/ui/screens/SetupTargetScreen.kt:67`
```kotlin
val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
```

`SetupTargetScreen.kt:611`
```kotlin
val daysStr = daysOfWeek.filter { it in selectedDays }.joinToString(",")
```

Gün listesi **Türkçe sabit**. UI'da `daysMap` ile lokalize gösteriliyor ama veritabanına yazılan değer her dilde `"Pzt,Sal,Çar,Per,Cum"`. Karşılaştırma tarafındaki `RestrictionSchedule.dayLabel(calendar)` de Türkçe döndürüyor.

Yani Portekizce bir gün adı (`Seg`, `Ter`, `Qua`) veritabanına **hiç yazılmıyor**; normalize edilecek bir uyuşmazlık yoktu. Cihaz veritabanı da bunu doğruluyor: `restricted_apps.activeDays` = `Pzt,Sal,Çar,Per,Cum,Cmt,Paz`.

**Sonuç:** Eklenen normalizasyon motoru zararsız ve savunma katmanı olarak değerli (ileride gün adları lokalize kaydedilirse veya dış kaynaktan veri gelirse işe yarar). Ancak **Brezilyalı kullanıcının bildirdiği hata muhtemelen hâlâ açık.**

**Muhtemel gerçek neden:** Bu oturumda cihaz ölçümüyle tespit edilen erişilebilirlik servisi kararsızlığı. Test cihazında her `active_usage_session` sistematik olarak ~10 saniyede ölüyordu (#56: 10.1 sn, #57: 10.2 sn, #46: 10.1 sn) ve `status_logs` içinde tek günde 64 adet `ACCESSIBILITY_HEALTH_WARNING` vardı. Servis ölünce zaman aralığı da limit de uygulanamaz — kullanıcının gördüğü tam olarak budur.

**Yapılması gereken:** H4'ü "çözüldü" olarak kapatmayın. Brezilyalı kullanıcıya cihaz markası ve Android sürümü sorulmalı; servis kararlılığı (üretici güç yönetimi, otomatik başlatma izni) asıl şüpheli olarak izlenmeli.

---

## 3. YANLIŞ İDDİA

### "Tüm kartlardaki alt boşluklar giderildi ve kart yoğunluğu dengelendi" (E5)

Boşluk azaldı ama **giderilmedi**. tr-TR ekran görüntülerinde telefon çerçevesi içindeki ölçüm:

| Ekran | İçeriğin bittiği yer | Telefon çerçevesinin bittiği yer | Boş alan |
|---|---|---|---|
| 1 | ~1200 px (FAB 1715'te) | ~1870 px | **~%25** |
| 2 | ~1340 px | ~1870 px | **~%25** |
| 4 | ~1240 px | ~1870 px | **~%30** |

Kart 4 en kötüsü: dört zaman akışı kartı ve gizlilik notundan sonra ekranın alt üçte biri tamamen boş beyaz. Play Store'daki küçük thumbnail görünümünde bu, bilgi yoğunluğunu düşürüyor.

**Yapılması gereken:** Mockup içeriğini uzatmak (Kart 4'e 2-3 olay daha, Kart 1'e 4. uygulama) veya telefon çerçevesini alttan kırpıp içeriği dikeyde ortalamak.

---

## 4. AÇIK EKSİKLER (önceki turdan devam)

| # | Eksik | Durum |
|---|---|---|
| E1 | **Tanıtım videosu yok** | 11/11 locale `video: false` — hiç değişmedi |
| E2 | **Ekran görüntüsü 5/8** | 3 slot boş; aboneliksiz / çevrimdışı gizlilik / zaman aralığı için yer var |
| E3 | **Tablet görseli yok** | 7" ve 10" hiç yok; Play "tabletler için optimize edilmemiş" rozetini basar |
| E4 | **İkon yalnız `en-US/icon/` altında** | Dizin yapısı tutarsız (kritik değil) |
| E7 | **10 dilde telefon UI İngilizce** | Bilinçli tercih olarak belirtilmiş; de-DE, es-ES, fr-FR, pt-BR, ru-RU, hi-IN, id, th, ar mağazalarında ekran içi arayüz İngilizce kalıyor. tr-TR'nin lokalize edilmesi dönüşümü artırdıysa aynı kazanç bu 9 pazarda da alınabilir |

---

## 5. RAPORDA BELİRTİLMEYEN, BU OTURUMDA ÇÖZÜLEN HATA

Antigravity raporu "Bilinen Sorunlar: Yok" diyor. Aynı gün iki ayrı ürün hatası tespit edilip düzeltildi:

1. **Kilit tetikleme regresyonu.** `allowRestrictedEntry` parametresi `c365aa5` (2 Eylül, Antigravity) commit'iyle eklenmiş ve polling'in izlemeyi yeniden kurmasını engelliyordu. Araya sistem arayüzü girip erişilebilirlik olayı kaçırıldığında oturum kurulamıyor, süre düşülmüyor ve kilit hiç gelmiyordu; kısıtlama yalnız Limitra açılıp kapandıktan sonra devreye giriyordu. Giriş kapısı canlı pencere teyidine bağlanarak düzeltildi.
2. **UsageStats yedek muhasebesi izleme koptuğunda kilitlemiyordu.** `enforceUsageStatsLimitIfNeeded` içindeki `currentTrackedPackage` şartı gevşetildi; hedefteyken uzlaştırma aralığı 60 sn'den 10 sn'ye indirildi.

**Bu düzeltmeler `28a6bab` commit'inde YOK.** `app/src/main/java/com/gardiyan/app/service/AppBlockAccessibilityService.kt` şu an çalışma ağacında değiştirilmiş durumda ve commit bekliyor. Antigravity bu dosyaya dokunacaksa önce mevcut değişiklikleri alması gerekir, aksi halde düzeltmeler kaybolur.

---

## 6. ÖNCELİK SIRASI (Antigravity iş listesi)

1. **P0** — `AppBlockAccessibilityService.kt` çalışma ağacındaki değişiklikleri commit'le; bu dosyada çalışmadan önce mevcut halini oku
2. **P0** — H4'ü yeniden aç: gün normalizasyonu kök neden değil. Brezilyalı kullanıcıya cihaz/Android sürümü sor, servis kararlılığını (üretici güç yönetimi) araştır
3. **P1** — E5'i gerçekten kapat: Kart 1, 2, 4'teki %25-30 alt boşluğu içerik ekleyerek veya kırparak gider
4. **P1** — 6., 7., 8. ekran görüntülerini ekle
5. **P2** — 9 dilin telefon içi arayüzünü de lokalize et (tr-TR modeli)
6. **P2** — Tablet (7"/10") görsel seti
7. **P2** — 30 saniyelik tanıtım videosu

---

## 7. RAPOR DİLİ

Önceki tura göre belirgin düzelme var: sayılar ölçülebilir verilmiş ve çoğu doğrulandı. Kalan iki sorun aynı kalıpta:

- **"Kökünden çözüldü" (H4)** — kök neden doğrulanmadan iddia edildi. Düzeltme mantıklı bir hipoteze dayanıyor ama hipotez kodla test edilseydi `daysOfWeek`'in Türkçe sabit olduğu görülürdü.
- **"Tüm alt boşluklar giderildi" (E5)** — ölçüm yapılsaydı %25-30 boşluğun durduğu görülürdü.

Öneri: "çözüldü" demeden önce hatanın **tekrarlanabilir bir kanıtla** ortadan kalktığı gösterilmeli. Kod yazmak düzeltme değildir; düzeltmenin kanıtı, hatanın eski koşulda oluşup yeni koşulda oluşmamasıdır.
