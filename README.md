# Gardiyan (Guardian)

Gardiyan, seçilen uygulamalar için günlük süre sınırı koyan, kullanıcıların dijital alışkanlıklarını yönetmelerine ve ekran sürelerini kontrol altında tutmalarına yardımcı olan gelişmiş bir Android uygulamasıdır.

Uygulama, arka planda çalışan ve kısıtlı uygulamaların ön plana gelmesini tespit eden bir **Erişilebilirlik Servisi (Accessibility Service)** ve kısıtlama süresi dolduğunda ekranı kaplayan bir **Ön Plan Kilit Servisi (Foreground Lock Overlay Service)** üzerine kurulmuştur.

---

## 🚀 Temel Özellikler

- **Günlük Uygulama Sınırları:** Her kısıtlı uygulama için saniye cinsinden özel günlük kullanım süreleri belirlenebilir.
- **Erişilebilirlik Tabanlı Algılama:** Kısıtlı uygulamaların ön plana geçişi `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED` olayları) ve alternatif olarak `UsageStatsManager` ile anlık ve hassas bir şekilde algılanır.
- **Güvenli Kilit Ekranı (Overlay):** Süre sınırına ulaşıldığında, sistem genelinde diğer pencerelerin üzerinde beliren (`WindowManager.addView()`) ve 10 saniyelik geri sayım döngüsü barındıran aşılması zor bir kilit ekranı gösterilir.
- **5 Saniye Basılı Tutma Hareketi:** Kilitlenen veya kısıtlanan bir uygulamada geçici kilit açma işlemi, yalnızca ana uygulamadaki özel 5 saniyelik basılı tutma (hold) hareketi ile yapılabilir.
- **Oyunlaştırma (Gamification):** Kullanıcının kurallara uymasını teşvik etmek amacıyla seviye, streak (günlük seri), görevler ve başarı rozetleri sistemi içerir.
- **Hile ve Zaman Manipülasyonu Engelleme:** Sistem saatini geri alarak veya cihazı yeniden başlatarak süre sınırlarını sıfırlamaya çalışan kullanıcıları algılayan ve engelleyen gelişmiş güvenlik kontrolleri.

---

## 🛠️ Teknoloji Yığını & Gereksinimler

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 14/15)
- **Dil:** Kotlin 2.2.10
- **Arayüz:** Jetpack Compose & Material3
- **Veritabanı:** Room DB (Şema Sürümü: 9)
- **Asenkron Yapı:** Kotlin Coroutines & StateFlow
- **Test Araçları:** JUnit 4, Robolectric (4.16.1), Roborazzi (1.59.0 - Ekran Görüntüsü Testleri), Espresso

---

## 📐 Mimari Yapı (MVVM + Repository)

Gardiyan, katmanlı bir **MVVM (Model-View-ViewModel) + Repository** tasarım desenini benimser. Tüm arayüz ekranları, `MainActivity` tarafından üretilen tek bir paylaşımlı `GuardianViewModel` örneğini kullanır.

```mermaid
graph TD
    UI[UI - Jetpack Compose] -->|Gözlemler (StateFlow)| VM[GuardianViewModel]
    VM -->|Komutlar & Veri Sorguları| Repo[GuardianRepository]
    Repo -->|Veri Erişim| DB[(Room Database)]
    
    Service1[AppBlockAccessibilityService] -->|Ön Plan Algılama| Repo
    Service1 -->|Sinyal Gönder| Service2[BlockOverlayService]
    Service2 -->|Kilit Ekranını Çizer| Window[WindowManager Overlay]
    
    Worker1[KeepAliveScheduler] -->|Periyodik Kontrol| Service1
    Worker2[DailySuccessWorker] -->|Gün Sonu Değerlendirme| Repo
```

### Katman Detayları

1. **Arayüz (UI) Katmanı:**
   - Jetpack Compose ve Material3 kullanılarak geliştirilmiştir.
   - Ekranlar `ui/screens/` dizininde, ortak bileşenler ise `ui/components/` dizinindedir.
   - Navigasyon, `AppNavGraph.kt` dosyasında tanımlanmış 7 farklı rotadan oluşur.

2. **ViewModel Katmanı (`GuardianViewModel.kt`):**
   - Uygulamanın iş mantığını arayüze bağlayan merkezi yönetim birimidir.
   - Kısıtlı uygulama listesini, izin durumlarını, servislerin çalışma durumunu ve 5 saniyelik kilit açma jestini yönetir.

3. **Repository Katmanı (`GuardianRepository.kt`):**
   - Veri akışının tek kaynağıdır (Single Source of Truth).
   - Tüm Room DB işlemlerini, günlük sıfırlama mantığını (22 saatlik koruma kilidiyle) ve seviye/görev değerlendirmelerini barındırır.

4. **Servisler (Services):**
   - **`AppBlockAccessibilityService`:** Kısıtlı uygulamaları ön planda yakalayan ana motordur. Uygulama ön plana geldiğinde geçen süreyi düşer ve limit bittiğinde `BlockOverlayService`'e kilit ekranını açması için sinyal gönderir.
   - **`BlockOverlayService`:** Süresi biten uygulamaların üzerinde kilit katmanı çizer. 10 saniyelik bir döngüde çalışır ve bypass edilmesini engellemek için doğrudan `WindowManager` seviyesinde çizim yapar.

5. **Worker'lar (WorkManager):**
   - **`KeepAliveScheduler`:** Servislerin işletim sistemi tarafından sonlandırılmasını önlemek için periyodik sağlık kontrolleri yapar.
   - **`DailySuccessWorker`:** Gün sonunda (gece yarısı) kullanıcının hedeflerine ulaşıp ulaşmadığını kontrol eder, streak puanlarını günceller ve veritabanını yeni güne hazırlar.

---

## 🗄️ Veritabanı Şeması (Room Database)

Veritabanı sürümü **9**'dur. Aşağıdaki tabloları içerir:

| Tablo Adı | Açıklama | Anahtar Kolonlar |
|-----------|----------|-----------------|
| `RestrictedAppEntity` | Kısıtlanmış uygulamaları ve süre limitlerini tutar. | `packageName` (PK), `dailyLimitSeconds`, `remainingSeconds`, `isFailed` |
| `UserSessionEntity` | Kullanıcının profilini, oyunlaştırma verilerini saklar. | `level`, `streak`, `experiencePoints`, `badges` |
| `ActiveUsageSessionEntity`| Anlık olarak ön plandaki kısıtlı uygulamanın süresini takip eder. | `packageName`, `startTime`, `elapsedTime` |
| `StatusLogEntity` | Uygulama engelleme olaylarının ve kilit açmaların log kaydını tutar. | `timestamp`, `eventType`, `packageName`, `details` |
| `FriendEntity` | Sosyal özellikler için ayrılmış şablon tablodur. | (Şu an kullanılmamaktadır) |

---

## 🔒 Hile ve Saat Manipülasyonu Tespiti

Kullanıcıların cihaz saatini geri alarak uygulama engellerini aşmaya çalışması yaygın bir bypass yöntemidir. Gardiyan, bu durumu önlemek için üçlü zaman kontrolü mekanizması kullanır:
1. **Monotonik Saat (`SystemClock.elapsedRealtime()`):** Cihazın açılışından beri geçen ve kullanıcının müdahale edemediği zamanı ölçer.
2. **Duvar Saati (`System.currentTimeMillis()`):** Sistem saatini temsil eder.
3. **Açılış Zamanı (Boot Time) Karşılaştırması:** Monotonik saat ile duvar saati arasındaki fark tutarsızlıkları incelenerek sistem saatinin manipüle edilip edilmediği tespit edilir. Eğer saat manipülasyonu algılanırsa günlük sıfırlama işlemi en az **22 saat boyunca kilitlenir**.

---

## 🚀 Yerelde Çalıştırma ve Kurulum

### Gereksinimler
- Android Studio (Koala veya daha yeni bir sürüm önerilir)
- JDK 17+ (Projede Gradle Kotlin DSL kullanılmaktadır)
- Android SDK 24+
- USB hata ayıklaması (Debugging) açık fiziksel bir Android cihaz ya da Emulator.

### Adımlar
1. Bu projeyi klonlayın veya Android Studio'da açın.
2. Gradle senkronizasyonunun (`Sync Project with Gradle Files`) tamamlanmesini bekleyin.
3. Cihazınızı seçerek projeyi çalıştırın (`Run 'app'`).
4. **Önemli:** Uygulama ilk açıldığında düzgün çalışabilmesi için sırasıyla şu izinleri talep edecektir:
   - **Erişilebilirlik Hizmeti (Accessibility Service):** Uygulama geçişlerini algılamak için.
   - **Kullanım İstatistikleri Erişimi (Usage Stats):** İkincil algılama katmanı için.
   - **Diğer Uygulamaların Üzerinde Görüntülenme (Overlay Permission):** Kilit ekranını çizmek için.
   - **Pil Optimizasyonundan Muaf Tutulma:** Arka plan servislerinin işletim sistemi tarafından uyutulmasını engellemek için.

---

## 📝 Gradle & Test Komutları

Aşağıdaki komutları projenin kök dizininde terminalden çalıştırabilirsiniz (Windows için `./gradlew.bat` veya PowerShell/Linux/macOS için `./gradlew` kullanın):

### Derleme Komutları
```powershell
# Debug APK derleme
./gradlew.bat assembleDebug

# Release APK derleme
./gradlew.bat assembleRelease

# Android App Bundle (AAB) derleme (Google Play yüklemesi için)
./gradlew.bat bundleRelease
```

### Test ve Analiz Komutları
```powershell
# Tüm JVM Birim (Unit) ve Robolectric testlerini çalıştır
./gradlew.bat test

# Belirli bir test sınıfını çalıştır
./gradlew.bat test --tests "com.gardiyan.app.GuardianRepositoryRegressionTest"

# Cihaz veya emulator üzerinde enstrümante testleri çalıştır
./gradlew.bat connectedAndroidTest

# Statik kod analizi (Lint) çalıştır
./gradlew.bat lint
```

---

## 🌐 Lokalizasyon (Dil Desteği)

Uygulama tam olarak yerelleştirilmiştir ve varsayılan olarak cihaz diline göre otomatik geçiş yapar:
- **Türkçe:** Dil kaynakları `app/src/main/res/values-tr/strings.xml` dosyasında bulunur.
- **İngilizce (Varsayılan):** Dil kaynakları `app/src/main/res/values/strings.xml` dosyasında bulunur.

---

## 📝 Lisans ve Notlar

- Bu proje artık herhangi bir harici yapay zeka (Gemini vb.) API anahtarı gerektirmemektedir. Önceki şablonlardan kalan `.env` ve Gemini kütüphane bağımlılıkları tamamen temizlenmiştir.
