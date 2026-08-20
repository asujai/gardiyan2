# Agent Çalışma Kuralları

Bu depo için Antigravity, Claude ve Codex birlikte çalışır.

## Model Görev Dağılımı
- **Claude:** Mimari, backend, servisler (Accessibility/Foreground Service), Room veritabanı, güvenlik, deployment ve karmaşık teknik işler.
- **Codex:** Teşhis, hata ayıklama, testler (Roborazzi/Robolectric), analiz ve ikinci göz.
- **Antigravity:** UI/UX, Jetpack Compose bileşenleri, stil/tema, Play Store mağaza varlıkları ve hacimli düzenlemeler.

## Proje Hafızası
Her oturum başında `SON_DURUM.md` ve `ISLEM_GECMISI.md` okunmalı, görev tamamlandığında güncellenmeli ve model önekli commit atılmalıdır (`[antigravity] ...`, `[claude] ...`, `[codex] ...`).
