# Agent Çalışma Kuralları

Bu depo için Antigravity, Claude ve Codex birlikte çalışır.

## Model Görev Dağılımı
- **Claude:** Mimari, backend, servisler (Accessibility/Foreground Service), Room veritabanı, güvenlik, deployment ve karmaşık teknik işler.
- **Codex:** Teşhis, hata ayıklama, testler (Roborazzi/Robolectric), analiz ve ikinci göz.
- **Antigravity:** UI/UX, Jetpack Compose bileşenleri, stil/tema, Play Store mağaza varlıkları ve hacimli düzenlemeler.

## Kaynak Dosyalarinda Kodlama Kurali (ZORUNLU)

`app/src/main/res/values*/strings.xml` ve `metadata/**/*.txt` dosyalari **UTF-8**'dir.
Bu dosyalara toplu duzenleme yapan her arac UTF-8 okuyup UTF-8 yazmak zorundadir.

**Gecmis olay:** `c28d3a1` (21 Agustos 2026) commit'i 11 dilin `strings.xml` dosyasini
Windows-1254 (Turkce ANSI) olarak okuyup UTF-8 yazdi. Sonuc: tum diller cift kodlandi
(`Bugunluk` -> `BugÃ¼nlÃ¼k` gorunumu). 25 Agustos 2026'da onarildi.

**Kurallar:**
- PowerShell'de asla ciplak `Set-Content` / `Out-File` / `>` kullanma. Her zaman
  `-Encoding utf8` ver.
- Python kullanirken `io.open(..., encoding="utf-8")` ile ac ve yaz.
- `sed -i` gibi arac zincirlerinde locale'in UTF-8 oldugundan emin ol.
- Toplu duzenlemeden SONRA dogrula:

```bash
grep -c "Ã" app/src/main/res/values-tr/strings.xml   # 0 dondurmeli
```

- Supheliysen derlenmis APK'dan kontrol et:

```bash
aapt2 dump strings app/build/outputs/apk/debug/app-debug.apk | grep "limitin doldu"
```

## Proje Hafızası
Her oturum başında `SON_DURUM.md` ve `ISLEM_GECMISI.md` okunmalı, görev tamamlandığında güncellenmeli ve model önekli commit atılmalıdır (`[antigravity] ...`, `[claude] ...`, `[codex] ...`).
