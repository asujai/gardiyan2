# Google Play Console Data Safety Guide / Veri Güvenliği Rehberi

This guide is prepared based on the analysis of Room Database entities in the Gardiyan application. It provides exact answers and configurations for the **Google Play Console Data Safety Form**.

Bu kılavuz, Gardiyan uygulamasındaki Room Veritabanı tablolarının (Entity) analizine dayanarak hazırlanmıştır. **Google Play Console Veri Güvenliği Formu** için kesin yanıtları ve yapılandırma yönergelerini içerir.

---

## 1. Database Analysis (Veritabanı Tablo Analizi)

The application stores all user and usage data locally on the device using five main Room entities:
Uygulama, tüm kullanıcı ve kullanım verilerini cihaz üzerinde yerel olarak beş ana Room tablosunda saklar:

1. **UserSessionEntity (`user_sessions`):**
   * *Data stored / Saklanan veri:* Local profile username, user level (Rookie, Master), streak info, consecutive success days, active target app details, and local custom shame message/image path.
   * *Purpose / Amaç:* Local profile configuration and gamification.
2. **RestrictedAppEntity (`restricted_apps`):**
   * *Data stored / Saklanan veri:* Package name and name of restricted apps, usage limits, remaining seconds, fail status, and active days.
   * *Purpose / Amaç:* App blocking functionality.
3. **ActiveUsageSessionEntity (`active_usage_session`):**
   * *Data stored / Saklanan veri:* App package name, entry time, and last seen time for screen time tracking.
   * *Purpose / Amaç:* Local screen time tracking and limit calculation.
4. **StatusLogEntity (`status_logs`):**
   * *Data stored / Saklanan veri:* Logs of events (e.g., FAILURE, SUCCESS, LEVEL_UP, LIMIT_CHANGED) with timestamps and descriptions.
   * *Purpose / Amaç:* Displaying history and logs to the user locally.
5. **FriendEntity (`friends_list`):**
   * *Data stored / Saklanan veri:* Relational schema structure for future v2 social feature (friend name, level, sync times).
   * *Note / Not:* Not active in the MVP UI. Even if used in v2, it does not collect telemetry.

---

## 2. Play Console Data Safety Questionnaire Answers (Form Yanıtları)

### Section A: Data Collection and Security (Veri Toplama ve Güvenlik)

1. **Does your app collect or share any of the required user data types? / Uygulamanız gerekli kullanıcı verisi türlerinden herhangi birini topluyor mu veya paylaşıyor mu?**
   * **Answer / Yanıt:** **NO / HAYIR**
   * *Reasoning / Gerekçe:* All data is processed and stored strictly on the local device. The application does not collect data on a server, nor does it share any data with third parties.
   * *Important Play Store Policy Note / Önemli Not:* Under Play Console guidelines, data that is processed solely on the device locally (on-device processing) does not need to be declared as "Collected" in the Data Safety form, provided it never leaves the device. Therefore, you can safely select **No**.

2. **Is all of the user data collected by your app encrypted in transit? / Uygulamanız tarafından toplanan tüm kullanıcı verileri aktarım sırasında şifreleniyor mu?**
   * **Answer / Yanıt:** **Not Applicable / Geçersiz** (Since no data is transmitted or collected off-device / Cihaz dışına veri aktarılmadığı için).

3. **Do you provide a way for users to request that their data be deleted? / Kullanıcılara verilerinin silinmesini talep etme yöntemi sunuyor musunuz?**
   * **Answer / Yanıt:** **YES / EVET**
   * *How is it handled? / Nasıl sağlanıyor?:* Users can delete all their data at any time in two ways:
     1. In-app: By using the "Clear All Data" button in settings (which triggers the `clearAllUserData` function in ViewModel, wiping the database).
     2. Android System: By going to Android Settings > Apps > Gardiyan > Storage > "Clear Data / Storage". This completely and permanently deletes the local SQLite/Room database.

---

## 3. Detailed Data Declarations (If Google requests explicit listing)
*(Eğer Google beyan edilmesini isterse seçilecek veri kategorileri)*

If you choose to declare local data processing voluntarily to be ultra-transparent, use the following declarations:
*(Eğer ultra-şeffaf olmak amacıyla yerel verileri beyan etmeyi seçerseniz, aşağıdaki tanımları kullanın):*

### Data Type: App Activity (Uygulama Etkinliği)
* **Specific Data / Alt Veri Türü:** App interactions (Uygulama etkileşimleri)
* **Collected / Toplanıyor mu?:** Yes (Processed locally on-device / Evet, yerel olarak cihazda işlenir)
* **Shared / Paylaşılıyor mu?:** No (Asla paylaşılmaz)
* **Ephemeral / Geçici mi?:** No (Stored in Room DB / Hayır, Room DB'de saklanır)
* **Purpose / Amaç:** App functionality, screen time limits (Uygulama işlevselliği, ekran süresi sınırları)

### Data Type: Personal Info (Kişisel Bilgiler)
* **Specific Data / Alt Veri Türü:** User name or Nickname (Kullanıcı adı veya Takma ad)
* **Collected / Toplanıyor mu?:** Yes (Processed locally on-device / Evet, yerel olarak cihazda işlenir)
* **Shared / Paylaşılıyor mu?:** No (Asla paylaşılmaz)
* **Ephemeral / Geçici mi?:** No (Stored in Room DB / Hayır, Room DB'de saklanır)
* **Purpose / Amaç:** Account management, personalization (Hesap yönetimi, kişiselleştirme)

### Data Type: Diagnostics (Teşhis ve Analizler)
* **Specific Data / Alt Veri Türü:** Crash logs / Diagnostics (Çökme günlükleri / Teşhis verileri)
* **Collected / Toplanıyor mu?:** Yes (Processed locally on-device / Evet, yerel olarak cihazda işlenir)
* **Shared / Paylaşılıyor mu?:** No (Asla paylaşılmaz)
* **Ephemeral / Geçici mi?:** No (Stored in Room DB / Hayır, Room DB'de saklanır)
* **Purpose / Amaç:** App functionality, debugging (Uygulama işlevselliği, hata ayıklama)
