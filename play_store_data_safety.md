# Google Play Console Data Safety Guide / Veri Güvenliği Rehberi

This guide provides accurate, policy-compliant instructions for the **Google Play Console Data Safety Form** and **Advertising ID Declaration** for Limitra.

Bu kılavuz, Limitra için **Google Play Console Veri Güvenliği Formu** ve **Reklam Kimliği Beyanı** için politika uyumlu ve doğru yanıtları sağlar.

---

## 1. Current Build Status (Mevcut Derleme Durumu: Çevrimdışı ve Reklamsız)

* **Build Configuration / Derleme Yapılandırması:** No advertising or analytics SDK; no `INTERNET`, `ACCESS_NETWORK_STATE`, Advertising ID, or AdServices permission in the merged release manifest.
* **Core Limitra Data / Çekirdek Uygulama Verisi:** 100% on-device (Room Database). No remote servers, cloud telemetry, advertising, or trackers; data does not leave the user's device.
* **Play Console Data Safety Selection:**
  * "Does your app collect or share any of the required user data types?" -> **NO / HAYIR**
  * Core on-device processing does not constitute data collection under Google Play policies.
  * "Does your app use Advertising ID?" -> **NO / HAYIR** (Advertising ID and Android AdServices permissions are explicitly removed from the merged manifest; see Section 3).

---

## 2. Limitra Core Database & On-Device Data Analysis (Yerel Tablo Analizi)

Limitra processes all detox and digital wellness data strictly on the local device across five Room entities:

1. **UserSessionEntity (`user_sessions`):**
   * *Data stored / Saklanan veri:* Local profile username, user level, streak info, consecutive success days, active target app details, and local shame message/image path.
   * *Scope / Kapsam:* Device-local profile only. No remote accounts, passwords, or cloud sync.
2. **RestrictedAppEntity (`restricted_apps`):**
   * *Data stored / Saklanan veri:* Package names of restricted apps, daily usage limits, remaining seconds, violation status, and active days.
   * *Purpose / Amaç:* Local app blocking and boundary enforcement.
3. **ActiveUsageSessionEntity (`active_usage_session`):**
   * *Data stored / Saklanan veri:* App package name, session start time, and last seen timestamp for usage calculation.
   * *Purpose / Amaç:* Local screen time tracking and limit enforcement.
4. **StatusLogEntity (`status_logs`):**
   * *Data stored / Saklanan veri:* Event history (e.g. RESTRICTION_ADDED, LIMIT_CHANGED, SUCCESS, VIOLATION, DATA_CLEARED).
   * *Purpose / Amaç:* Local timeline display for the user.
5. **FriendEntity (`friends_list`):**
   * *Data stored / Saklanan veri:* Relational schema structure for future local features. Dormant in MVP UI.

---

## 3. Advertising ID & Network Contract (Reklam Kimliği ve Ağ Sözleşmesi)

* **Advertising ID declaration / Reklam Kimliği beyanı:** Select **NO / HAYIR**.
* The source manifest uses `tools:node="remove"` for Google Advertising ID, Android AdServices, Internet, and network-state permissions. The final merged release manifest must be checked before every release.
* `OfflinePrivacyContractTest` prevents advertising dependencies and network runtime switches from being reintroduced unnoticed.
* Any future decision to add networking, analytics, advertising, accounts, or cloud sync requires a new privacy review and updated Privacy Policy, Data Safety form, store text, screenshots, and user disclosures **before** that build is published.

---

## 4. User Data Deletion & Clear My Data Compliance (Veri Silme Uyumluluğu)

* **In-App Deletion:** The user can tap **"Clear My Data"** in Profile > Settings at any time. This invokes `GuardianViewModel.clearAllUserData()`, completely wiping:
  * All Room database tables (`restricted_apps`, `active_usage_session`, `status_logs`, `user_sessions`, `friends_list`)
  * All local SharedPreferences: `gardiyan_settings`, `gardiyan_eval_prefs`, `gardiyan_notifications`, `gardiyan_theme_prefs`, and `gardiyan_secure_reset_prefs`.
  * Does not alter or revoke system-level Android permissions granted by the user.
* **System-Level Deletion:** The user can clear app storage via Android Settings > Apps > Limitra > Storage > Clear Data.
