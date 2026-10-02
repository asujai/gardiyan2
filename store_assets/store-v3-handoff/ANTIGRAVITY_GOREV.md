# Antigravity görevi: Limitra mağaza kartlarını 9 dile çıkar (v3)

Hazırlayan: Claude, 2 Ekim 2026. Denetleyecek: Claude (iş bitince kullanıcı haber verecek).

## 0. Özet (önce bunu oku)

**Hedef.** İngilizce (en-US) mağaza seti Google Play'de canlı. Aynı seti **9 dile** çıkaracaksın:
`de-DE, es-ES, fr-FR, pt-BR, id, ru-RU, hi-IN, th, ar`.
Türkçe (tr-TR) pilot olarak Claude tarafından hazırlandı, dokunma (yalnız aşağıdaki §9 denetimine bak).

**Neden "yalnız dili değiştir" yetmiyor.** Kartlardaki telefon ekranları gerçek uygulama ekranlarıdır
ve o dilin arayüzüyle çekilmelidir. Ölçtük: bu 9 dilde uygulamanın kendi çevirileri eksik.
Çevirisi yapılmadan çekilen ekranlarda İngilizce parçalar görünür (ör. "Active", "All day · Every day",
"left 01:11", kilit ekranındaki söz). Bu yüzden iş sırası:

```
A. Uygulama çevirilerini tamamla  ->  B. Derle  ->  C. Dil başına ekranları çek
  ->  D. Kart metinlerini çevir  ->  E. Kartları üret  ->  F. Mağaza metinleri  ->  G. Doğrula + kayıt + commit
```

**Dokunmayacakların.** Kotlin kaynakları, `app/build.gradle.kts` (versionCode/versionName),
imza dosyaları, `play-service-account.json`, gerçek telefon (`X4XKPFXWVSO7TKEE`).
**Yapmayacakların.** Play Console'a yükleme (`gpc ... upload/sync/update`), `git push`, sürüm yükseltme.
Yükleme, sürüm yükseltme ve push işlerini denetimden sonra Claude yapacak.

**Bitti sayılır.** §10'daki kontrol listesi: `python tools/validate_store_v3.py` 9 dilde `[OK ]`,
`python tools/check_l10n.py` çıkış kodu 0, `testDebugUnitTest` yeşil, commit atılmış, kayıtlar yazılmış.

---

## 1. Mevcut durum

| Locale | Uygulama dili | `res` klasörü | Durum |
|---|---|---|---|
| en-US | en | `values` | Canlı (Play'de). `store_assets/en-US-v3/` |
| tr-TR | tr | `values-tr` | Görseller + metinler hazır, **Play'e yüklenmedi** (Claude yükleyecek). `store_assets/tr-TR-v3/` |
| de-DE | de | `values-de` | **Yapılacak** |
| es-ES | es | `values-es` | **Yapılacak** |
| fr-FR | fr | `values-fr` | **Yapılacak** |
| pt-BR | pt | `values-pt` | **Yapılacak** |
| id | id | **`values-in`** (Android'de Endonezce `in`) | **Yapılacak** |
| ru-RU | ru | `values-ru` | **Yapılacak** |
| hi-IN | hi | `values-hi` | **Yapılacak** |
| th | th | `values-th` | **Yapılacak** |
| ar | ar | `values-ar` (**sağdan sola**) | **Yapılacak** |

**Hazır araçlar (hepsi `tools/` altında, Python 3.13 + Pillow kurulu):**

| Araç | Ne yapar |
|---|---|
| `check_l10n.py` | Uygulama dizelerindeki eksik/çevrilmemiş anahtarları listeler. Çıkış 0 = temiz. |
| `capture_store_screens.py` | Emülatörde bir dil için 6 gerçek ekranı çeker (`store_assets/<locale>-v3/source/`). |
| `generate_store_v3.py` | `store_copy_v3.json` + `source/` ekranlarından kartları üretir (`.../play/`). |
| `validate_store_v3.py` | Teslim denetimi (boyut, format, metin, mojibake, TODO, İngilizce kalıntı). |
| `store_copy_v3.json` | **Kart metinleri.** en-US ve tr-TR dolu; diğer 9 dil `_status: "TODO..."` ile İngilizce kopya. |

Tasarım sabit: koyu zemin, altın vurgu, Newsreader (başlık) + Manrope (alt metin). Tasarımı değiştirme;
yalnız dile bağlı alanlar (metin, font yığını, RTL) değişir. Üretici bunları kendisi yönetir.

---

## 2. Ortam kuralları

* Windows 11. Komutlar PowerShell'de çalışır; yol örnekleri Git Bash'te de geçerli.
* Proje: `C:\Users\abdul\gardiyan2`. JDK 21 şart:
  `-Dorg.gradle.java.home="C:/Program Files/Android/Android Studio/jbr"`.
* **adb'de her zaman `-s emulator-5554`.** Bilgisayara kullanıcının gerçek telefonu da bağlı
  (`X4XKPFXWVSO7TKEE`). Ona hiçbir komut gönderme. `capture_store_screens.py` zaten yalnız `emulator-*` kabul eder.
* Emülatör kapalıysa başlat (arka planda), açılışı bekle:
  ```powershell
  Start-Process "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -ArgumentList "-avd","Pixel_API_34","-no-snapshot-save"
  adb -s emulator-5554 shell getprop sys.boot_completed   # 1 dönene kadar bekle
  ```
  Emülatör eski bir sürümle açılabilir; `capture_store_screens.py --install` bunu çözer (güncel debug APK'yı kurar).
* Emülatörde başka projelerin uygulamaları var (CleanScan vb.). Silme/kaldırma yapma.
  Betiğin ihtiyaç duyduğu paketler: `com.google.android.youtube`, `com.android.chrome`,
  `com.limitra.socialprototype` (ekranlardaki "Social" satırı). Yoksa betik yine çalışır ama o satırın simgesi farklı olur;
  rapor et.
* Edge headless kullanılır (`C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`). Kullanıcının Edge'i açık olabilir;
  üretici ayrı profil dizini kullanır, sorun yok. Üretim sırasında Edge'i elle kapatma.
* Dosya kodlaması: her şey **UTF-8, BOM'suz**. Düzenlediğin her `strings.xml`/`.txt`/`.json` için
  sonunda `grep -c "Ã"` (veya `Select-String "Ã|â€|Ä±"`) 0 olmalı.

---

## 3. ADIM A: Uygulama çevirilerini tamamla (kod değil, kaynak metin)

Düzenlenecek dosyalar (9 adet):
`app/src/main/res/values-de/strings.xml`, `values-es`, `values-fr`, `values-pt`, `values-in`, `values-ru`,
`values-hi`, `values-th`, `values-ar` (hepsinde `strings.xml`).
Kaynak (İngilizce): `app/src/main/res/values/strings.xml`. Referans (eksiksiz): `values-tr/strings.xml`.

Eksikleri görmek için:
```powershell
python tools/check_l10n.py          # 10 dilin özeti
python tools/check_l10n.py de       # tek dil: tam liste (MISSING / UNTRANSLATED)
```
Bugünkü ölçüm (çıkış kodu 1):

| Dil | Eksik anahtar | Çevrilmemiş | Çevrilmemişin içindeki söz (quote_text) | Diğer |
|---|---|---|---|---|
| de | 21 | 75 | 54 | 21 |
| es | 21 | 71 | 54 | 17 |
| fr | 21 | 76 | 54 | 22 |
| pt | 21 | 72 | 54 | 18 |
| id | 21 | 72 | 54 | 18 |
| ru | 21 | 122 | 54 | 68 (54'ü yazar adı) |
| hi | 21 | 122 | 54 | 68 (54'ü yazar adı) |
| ar | 21 | 122 | 54 | 68 (54'ü yazar adı) |
| th | 21 | 55 | 1 | 54 (hepsi yazar adı) |

### A1. 21 eksik anahtar (9 dilin hepsinde aynı)
Bu anahtarlar `values/strings.xml` ve `values-tr/strings.xml` içinde var, 9 dilde **hiç yok**
(Android İngilizceye düşüyor). Her dosyaya ekle (ilgili bölümün yanına; sıra önemli değil):

```
setup_target_restriction_name, setup_target_restriction_name_optional, setup_target_restriction_name_placeholder,
setup_target_error_no_name, protected_apps_time_left, protected_apps_usage_progress,
setup_target_active_window, setup_target_active_window_desc, setup_target_active_window_toggle,
setup_target_start_time, setup_target_end_time,
protected_group_apps, protected_group_app_count, protected_group_active, protected_group_scheduled,
protected_group_all_day, protected_group_every_day,
perm_state_failsafe, perm_accessibility_failsafe_desc, accessibility_failsafe_warning,
profile_privacy_choices
```
(`profile_privacy_choices` `values-tr`'de de yok: onu da ekle.)

Bunlardan kartlarda görünenler: `setup_target_restriction_name*`, `setup_target_active_window*`,
`setup_target_start_time/end_time`, `protected_group_*`, `protected_apps_time_left`. Yani eksik bırakılırsa
5 numaralı kartta (saat aralığı) ve 4 numaralı kartta (kalan süre) İngilizce görünür.

### A2. Çevrilmemiş (İngilizceyle aynı kalmış) dizeler
`check_l10n.py <dil>` `UNTRANSLATED` olarak listeler. Çevir. İstisnalar (çevirme):
`app_name`, `dashboard_title`, `profile_version_format` ("Limitra" marka adı).
Yazar adları (`quote_author_*`): **Latin yazılı dillerde (de, es, fr, pt, id) aynı kalabilir**
(Seneca, Marcus Aurelius...). **ru, hi, th, ar'da yazıya çevir** (Сенека, सेनेका, เซเนกา, سينيكا...).

Kartlarda doğrudan görünenler (öncelik **P0**, atlama):
`setup_target_hour_label`, `setup_target_minute_label` ("HOUR/MINUTE"), `quote_text_20` + `quote_author_20`
(1 numaralı kartın kilit ekranı sözü), `discipline_summary_*`, `btn_save`, `btn_delete`.

### A3. Stoacı sözler (`quote_text_1` ... `quote_text_54`)
8 dilde (th hariç) 54 sözün **hepsi İngilizce**. Kilit ekranı kullanıcıya her gün bir sözü gösterir; yani bu
yalnız mağaza değil **ürün kusuru**. Hepsini çevir (en azından #20; hepsi önerilir, çıkış kodu 0 hepsini ister).
Kural: sözün anlamını koru; mümkünse o dilde yerleşik çeviriyi kullan; dilin tırnak/noktalama kuralına uy.
`values-tr/strings.xml` içindeki karşılıklara bakarak ton ve uzunluk için fikir edin. Uydurma alıntı üretme:
metni değiştirme, yalnız çevir.

### A4. XML kuralları (derleme kırılmasın)
* Kesme işareti `\'`, tırnak `\"`, `&` → `&amp;`, `<` → `&lt;`. Başında `@` ya da `?` olan metni `\@` ile yaz.
* Yer tutucuları aynen koru: `%1$s`, `%1$d`, `%%`. Sırayı değiştirebilirsin ama numarayı değiştirme.
* `%1$d%% remaining` gibi biçimlerde `%%` kalmalı.
* `plurals`/`string-array` varsa (ör. başarılar) öğe sayısı ve `quantity` değerleri dilin kuralına uysun
  (ar: zero/one/two/few/many/other, ru: one/few/many/other, diğerleri one/other).
* Uzunluk: kısa etiketler (HOUR, START, END, ACTIVE) ekranda sığmalı; çok uzatma, büyük harf kullanımı İngilizcedeki gibi.
* Sözlük: kartlardaki ekranlarla çelişmemek için aynı kavramı hep aynı kelimeyle çevir
  ("restriction/kısıtlama" = uygulamanın "limit"i; "protected apps" = korunan uygulamalar). Diller arası tutarlılık için
  `values-tr/strings.xml`'e bak.

### A5. Doğrulama (Adım A bitişi)
```powershell
python tools/check_l10n.py          # çıkış kodu 0 olmalı (9 dilde eksik=0, çevrilmemiş=0)
```
`values-tr`'de kalan 2 "çevrilmemiş" (`profile_level_label`, `usage_limit_prefix`) Claude'un konusu; dokunma.

---

## 4. ADIM B: Derle ve test et

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest "-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr"
```
Beklenen: `BUILD SUCCESSFUL`, 155 test, 0 hata. Hata çıkarsa (ör. `AAPT: error: ... apostrophe`) ilgili
`strings.xml` satırını düzelt. APK: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 5. ADIM C: Her dil için ekranları çek

Her dil için **bir komut** (yaklaşık 3-4 dk):
```powershell
python tools/capture_store_screens.py --locale de-DE --install
```
`--install` ilk dilde yeterli; sonrakilerde çıkarabilirsin. Çıktı: `store_assets/de-DE-v3/source/` içinde
`setup_limit.png, setup_schedule.png, trackers.png, timeline.png, ach_grid.png, lock.png` (1080x1920).
Betik: Premium Koyu tema, 9:41 saat, tam pil, demo veri (64 günlük seri, 4 korunan uygulama), uygulama dilini
`cmd locale set-app-locales` ile değiştirir, kilit ekranı sözünü #20'ye sabitler (sözler günlük döner; sabitlemezsek
her çekim farklı olur). Tek adımı yeniden çekmek için: `--only lock` (`setup,trackers,timeline,achievements,lock`).

**Her dil için çıktıyı GÖZLE kontrol et** (6 PNG'yi aç):
1. Ekranlarda **hiç İngilizce metin yok** (özellikle "Active", "All day", "left", "HOUR/MINUTE", kilit sözü).
   Varsa Adım A'da o anahtar eksik demektir: düzelt, derle, `--install` ile yeniden çek.
2. `setup_limit`: saat **00:30**, iki uygulama çipi (YouTube, Chrome), "Aktif zaman aralığı" kapalı.
3. `setup_schedule`: anahtar **açık**, **09:00 – 18:00**, günlerden **yalnız hafta içi 5 gün** (Pazartesi–Cuma karşılıkları) seçili, hafta sonu 2 gün kapalı.
4. `trackers`: 4 satır (Chrome, Social, YouTube, YT Music), hiç kırmızı "limit doldu" yok.
5. `timeline`: "Bugün" altında 07:37 bir kilit, "Dün" altında "23:55 gün başarıyla tamamlandı".
6. `ach_grid`: "6/9 kazanıldı" başlığı ve 3x3 çerçeve ızgarası; en üstte kesik çerçeve/çubuk **yok**.
7. `lock`: YouTube simgesi, çevrilmiş "limit doldu" metni, çevrilmiş **Epiktetos sözü**, düğme.
8. Üstte bildirim balonu, klavye, sistem diyaloğu **yok**. Durum çubuğunda 9:41.

**ar (sağdan sola):** betik x koordinatlarını yansıtır. Ekranlar aynalanmış görünür (başlık sağda, gezinme çubuğunda
Ana Sayfa sağda). Beklenen. Sayılar Arap-Hint rakamı çıkabilir ("٦ من ٩"); kabul.

**Sık sorunlar**

| Belirti | Sebep / çözüm |
|---|---|
| `device 'emulator-5554' not found` | Emülatör kapalı → §2'deki komutla aç, boot bitince tekrar dene. |
| Ekranlarda eski (yeni olmayan) arayüz | Emülatör eski APK ile açıldı → `--install` ekle. |
| Başka bir uygulama (CleanScan) ekranda | Betik başlatmayı beceremedi → emülatörü yeniden başlat, tekrar çalıştır. |
| Kilit ekranında İngilizce | Betik 4 kez dener. Hâlâ İngilizceyse `overlay_*` anahtarı o dilde eksik/çevrilmemiş. |
| `UYARI: ... quote_text_20 çevrilmemiş` | A3'ü tamamla, yeniden çek (`--only lock`). |
| Saat seçicide yanlış değer | Çekimi yeniden çalıştır (`--only setup`). Hâlâ yanlışsa sonucu raporla, elle düzeltme. |
| Üstte "Limitra koruması aktif değil" balonu | Betik bildirim iznini kapatır; çıktıysa `--only setup` ile tekrar çek. |

---

## 6. ADIM D: Kart metinlerini çevir (`tools/store_copy_v3.json`)

Düzenlenecek **tek dosya**: `tools/store_copy_v3.json`. Her dil kendi bloğunda. Referans: `"en-US"` (kaynak) ve
`"tr-TR"` (çevrilmiş örnek). 9 dilin bloğunda şu an İngilizce metin ve `"_status": "TODO: ..."` var:
çeviriyi bitirince **`_status` satırını sil** (üretici ve doğrulayıcı TODO görürse çalışmaz).

Alanlar (her dilde aynı yapı):

| Alan | Anlamı | Kural |
|---|---|---|
| `lang`, `rtl` | Dil kodu, yön | **Değiştirme.** (`ar` için `rtl: true`.) |
| `cards.01 ... 06` → `h1`, `sub` | Başlık, alt metin | `h1` içinde `<br>` satır kırar, `<em>...</em>` **altın vurgu**dur. Her başlıkta vurgu olmalı. |
| `cards.07` → `h1`, `sub`, `rows` (4 öğe), `none`, `pill` | Gizlilik kartı | `rows` sırası: Hesap, Reklam, İzleyici/takip aracı, İnternet izni. `none` = "Yok/Hiçbiri". |
| `cards.08` → `h1`, `sub`, `seal_ring`, `seal_big`, `seal_small` | Tek ödeme kartı | `seal_big` hep `1×`. `seal_ring` mühür çevresindeki yazı (aynı 4 ifade, "·" ayraçlı, sonda "·"). `seal_small` = "ÖDEME". |
| `feature` → `h1`, `sub` | 1024x500 öne çıkan grafik | `h1` kısa olsun (2 satıra sığar). `sub` iki cümle, `<br>` ile. |

**Çevirirken kurallar**

1. **Anlamı çevir, kelimeyi değil.** Kart 1: "Just five more minutes. / Not today." = o dilde "bir dakika daha" tüketen,
   insanın kendi kendine verdiği bahane + kesin ama sakin bir "bugün olmaz". Dilin yerleşik deyimini kullan.
2. **Değişmez iddialar** (hepsi doğrulanmış; ekleme/çıkarma yok): günlük limit koyarsın ve süre dolunca Limitra
   uygulamayı **kilitler**; hesap yok, reklam yok, takip aracı yok, **internet izni yok**, tamamen çevrimdışı;
   **tek ödeme**, abonelik yok, ek satış yok; **9 animasyonlu çerçeve, 1. günden 365. güne**.
   Yasak: fiyat, rakip adı, "kırılamaz/mutlak engel", "bağımlılığı tedavi eder", pil/tasarruf/yüzde rakamı, iade garantisi.
3. **Uygulama diliyle tutarlı ol.** Kartta görünen ekranlardaki kelimeler (kısıtlama, korunan uygulamalar, zaman tüneli,
   başarılar, kilit) başlıklarda da aynı kelimeyle geçsin. Bakmak için: `values-<dil>/strings.xml`.
4. **Marka adı "Limitra" her yerde Latin harfle kalır.**
5. **Tırnak işareti dilin kuralı:** en/tr/pt/id/hi/th `“ ”`; de `„ “`; es ve fr `« »` (fr'de boşluklu: `« … »`);
   ru ve ar `« »`.
6. **Uzunluk.** Başlık satırı ~14-18 harf (Latin) civarı sığar; üretici taşarsa yazıyı otomatik küçültür ama çok küçülürse
   okunmaz. Başlık en çok 3 satır, `sub` en çok 2 satır olsun. Almanca gibi uzun dillerde kısalt.
7. **Latin olmayan yazılar (ru, ar, hi, th):** başlıkta İngilizce harf kalmasın (doğrulayıcı kontrol eder; "Limitra" hariç).
8. **Alt metin biçimi:** noktalama ve büyük harf dilin kuralına göre (hi/th/ar'da büyük harf yok; `seal_ring` ve
   `seal_small` bu dillerde olduğu gibi yazılır).

**Dil notları**

| Dil | Not |
|---|---|
| de-DE | Uzun bileşik kelimeler. Başlıkları kısa tut. "Du" hitabı (uygulama da "Dein Limit" kullanıyor). |
| es-ES / fr-FR / pt-BR | Aksanlı harfler Newsreader'da var; yine de her başlığı gözle kontrol et (boş kutu/yedek yazı tipi var mı). |
| id | `id` kodu; uygulama klasörü `values-in`. Metin Endonezce. |
| ru-RU | Kiril için üretici Times New Roman kullanır (Newsreader yalnız Latin). Normaldir. |
| hi-IN | Nirmala UI kullanılır, italik yok (vurgu yalnız altın renk). Satır yüksekliği otomatik artar. |
| th | Leelawadee UI, italik yok. Boşluksuz yazı: `<br>` ile satırı **sen** böl (kelime ortasından bölünmesin). |
| ar | `rtl: true`. Düzen aynalanır. Mühür yazısı (`seal_ring`) üst yayda ortalanır; kısa tut (~40 karakter). Tırnak `« »`. |

Çeviri bitince hemen test et (Adım E). Çeviriyi baştan sona bir kez de yüksek sesle oku: doğal mı, reklam gibi mi?

---

## 7. ADIM E: Kartları üret

Her dil için (Adım C ve D bittikten sonra):
```powershell
python tools/generate_store_v3.py --locale de-DE
```
Çıktı `store_assets/de-DE-v3/play/` altında, Play'in beklediği yapıda:
`phoneScreenshots/1..8.png` (1080x1920), `sevenInchScreenshots/1..4.png` (1200x1920),
`tenInchScreenshots/1..4.png` (1600x2560), `featureGraphic/feature.png` (1024x500).
(Ara dosyalar `out/` ve `html/` altında; git'e girmez.)

Sıra: 1 kilit, 2 limit kurma, 3 başarılar, 4 kalan süre, 5 saat aralığı, 6 zaman tüneli, 7 gizlilik, 8 tek ödeme.
Üretici `source/` eksikse ve `_status` TODO ise durur ve nedenini yazar.

**Her dilde 8 telefon kartını + öne çıkan grafiği + 1 tablet kartını GÖZLE kontrol et:**
* Başlık ve alt metin kutudan taşmıyor, telefon çerçevesine **binmiyor**; en fazla 3 satır başlık.
* Boş kutu (tofu ▯), yanlış glif, birbirine geçmiş satır yok (özellikle hi/th/ar).
* Vurgu (`<em>`) altın renkte ve anlamlı kelimede.
* Kilit kartında (1) telefonun üstü "Limitra: AppBlock" satırını kesmiyor.
* Başarılar kartında (3) üstte ızgara başlığı görünüyor, kesik çubuk yok.
* Kart 7'de 4 satırın hepsi tek satırda; kart 8'de mühür yazısı okunuyor.
* ar: tüm düzen aynalı (başlık sağda), kart 7'de etiketler sağda "لا يوجد" solda.
* Her kartta yalnız o dilin yazısı var; İngilizce kalıntı yok (çevrilmemiş kopya).

Referans için: `store_assets/en-US-v3/play/` (İngilizce) ve `store_assets/tr-TR-v3/play/` (Türkçe).

---

## 8. ADIM F: Mağaza metinleri (kartlardan ayrı, aynı iş)

Kartlar yeniyken eski, dile göre değişen mağaza açıklamaları kalmıştır. Yenile.
**Master metin:** `metadata/en-US/short_description.txt` ve `full_description.txt` (canlı).
Çevrilmiş örnek: `metadata/tr-TR/` (aynı yapıda).

Her dil için `metadata/<locale>/` içinde:

| Dosya | Sınır | Not |
|---|---|---|
| `title.txt` | 30 | **Değiştirme.** |
| `short_description.txt` | **80 karakter** | en-US: "Daily app limits that lock when time is up. Pay once. No ads, no subscription." |
| `full_description.txt` | **4000 karakter** | en-US yapısını koru: açılış cümlesi, NASIL ÇALIŞIR, NELER VAR, TASARIMI GEREĞİ ÖZEL, TEK ÖDEME, KİMLER İÇİN, İZİNLER, SSS, web sitesi satırı. |
| `store_assets/<locale>-v3/release_notes.txt` | **500 karakter** | v1.3.0 sürüm notu (aşağıda). |

Kurallar:
* İlk paragrafta **"Limitra App Block"** adı bir kez geçsin (kullanıcı kararı; arama motorları ve yapay zekâ için ürün adı).
  Son satırda `https://limitra.online` web sitesi satırı kalsın.
* **Eski metinden düzenleme yapma; en-US'u baştan çevir.** Eski dosyalarda doğrulanmamış/eski ifadeler var
  ("tavizsiz", "bağımlılığı kırar", eski seviye adları "Stoic/Zen Master" gibi). Yeni metin yalnız §6'daki doğrulanmış
  iddiaları içerir.
* Seviye adları uygulamadaki `level_name_1..6` ile aynı olsun (ilk: Rookie karşılığı, son: Full Control karşılığı):
  `values-<dil>/strings.xml`'den al.
* Uzunluğu kontrol et (`len`), mojibake yok.

**v1.3.0 sürüm notu (master, en-US) ve çevirilecek ifade:**
```
• Completely redesigned look: refined typography, four colour themes in light and dark, smoother animations.
• New Achievements: keep your streak to earn 9 animated frames, from day 1 to day 365.
• The lock screen now matches your theme.
• The daily usage list now resets correctly at midnight, and limit-edit warnings appear instantly.
```
Her dil için aynı 4 madde, `•` ile, `store_assets/<locale>-v3/release_notes.txt` (UTF-8). Claude bunları Play'in toplu
biçimine (`<de-DE>...</de-DE>`) çevirip kullanıcıya verecek; sen yalnız dosyaları yaz.

---

## 9. Kapsam dışı ama bil

* **tr-TR**: Claude yaptı (kartlar, metin, sürüm notu). Çevirileri gözden geçirirsen, sorun bulduğun satırları rapora yaz,
  doğrudan değiştirme.
* **en-US**: canlı; dokunma. En-US'ta `tools/generate_store_v3.py --locale en-US` yeniden üretirse birkaç pikselde canlıdan
  farklı çıkar (üretici eski bir betikten taşındı); bu normal, **yeniden yükleme yapılmayacak**.
* Uygulama sürümü: yeni çeviriler uygulamayı değiştirir. **Yeni sürüm kodunu (versionCode 21) Claude verecek**; sen
  sürüm numarasına dokunma.

---

## 10. ADIM G: Doğrulama, kayıt, commit

Aşağıdakilerin hepsi geçmeden bitti deme:

```powershell
python tools/check_l10n.py                                   # çıkış 0
.\gradlew.bat :app:testDebugUnitTest "-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr"   # yeşil
python tools/validate_store_v3.py de-DE es-ES fr-FR pt-BR id ru-RU hi-IN th ar      # hepsi [OK ]
```
`validate_store_v3.py` şunları denetler: görsel sayısı/boyutu/RGB/8 MB; `_status` TODO yok; İngilizce kopya yok;
Latin olmayan dillerde başlıkta İngilizce harf yok; `source` kartlardan yeni; başlık ≤30, kısa ≤80, uzun ≤4000;
mojibake yok; `release_notes.txt` ≤500.

**Kayıt (kullanıcının projedeki kuralı):**
1. `ISLEM_GECMISI.md` dosyasının **en üstüne** kayıt ekle (başka modelin kaydına dokunma):
   ```
   ## [YYYY-MM-DD HH:mm] - Mağaza kartları 9 dile çıkarıldı (v3)

   * **Model:** Antigravity
   * **Etkilenen Dosyalar:** `[GÜNCELLENDİ]` app/src/main/res/values-*/strings.xml, tools/store_copy_v3.json, metadata/*; `[YENİ]` store_assets/<locale>-v3/**
   * **Yapılan İşlem:** ...
   * **Doğrulama:** (çalıştırdığın komutlar ve sonuçları)
   * **Bilinen Sorunlar:** ...
   * **Sonraki Öneri:** Claude denetimi, sürüm 21, Play yükleme
   ```
2. `SON_DURUM.md` içindeki "Son İşlem" bölümünü güncelle.
3. Commit (push **yok**): `git add app/src/main/res/values-* tools/store_copy_v3.json metadata store_assets ISLEM_GECMISI.md SON_DURUM.md`
   ardından `git commit -m "[antigravity] feat: mağaza kartları ve uygulama çevirileri (9 dil)"`.
   `.idea/` ve `*.jks`, `play-service-account.json` hiçbir koşulda eklenmez.

---

## 11. Teslimden sonra Claude'un yapacağı denetim (kendi kendine test et)

1. `check_l10n.py`, `validate_store_v3.py`, `testDebugUnitTest` komutlarını kendisi çalıştırır.
2. 9 dilin 8 kartını + öne çıkan grafiği tek tek açar; taşma, tofu, İngilizce kalıntı, RTL, kırpma, boş alan bakar.
3. Her dilde `source/` ekran görüntülerini açar: İngilizce parça, balon/klavye, yanlış değer (00:30, 09:00–18:00, Pzt–Cum).
4. Çevirilerin kalitesini örnekler (başlıklar, kart 7/8, sürüm notu, 10'ar uygulama dizesi ve 5'er söz).
5. `strings.xml` değişikliklerinin yalnız ekleme/çeviri olduğunu `git diff` ile kontrol eder (yer tutucular, `\'`, plural).
6. Doğruysa: versionCode 21, imzalı AAB, Play taslağı, görsel/metin yükleme, push.

## 12. Dosya haritası

```
tools/capture_store_screens.py     ekran çekimi (emülatör)
tools/generate_store_v3.py         kart üretici
tools/validate_store_v3.py         teslim denetimi
tools/check_l10n.py                uygulama çevirisi denetimi
tools/store_copy_v3.json           kart metinleri (düzenlenir)
store_assets/store-v3-shared/      ortak yazı tipleri ve ikon (dokunma)
store_assets/<locale>-v3/source/   o dilin 6 gerçek ekranı (capture çıktısı)
store_assets/<locale>-v3/play/     Play'e hazır görseller (generate çıktısı)
store_assets/<locale>-v3/release_notes.txt   v1.3.0 notu
metadata/<locale>/                 title / short / full açıklama
app/src/main/res/values-*/strings.xml        uygulama çevirileri (düzenlenir)
```
