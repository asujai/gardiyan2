# Privacy Policy / Gizlilik Politikası

**Effective Date / Yürürlük Tarihi:** August 13, 2026

---

## English Version

This Privacy Policy describes how Limitra ("we", "our", or "us") handles user data and permissions in the Limitra mobile application (the "App").

### 1. Overview and Core Functionality
Limitra is a digital wellness and productivity tool designed to help users limit their screen time and block distractive applications based on user-defined limits.

### 2. Accessibility Service API Usage (CRITICAL DISCLOSURE)
Limitra uses the Android **Accessibility Service API** to provide its core functionality.
* **Why we use it:** We use this API solely to detect in real time when a blocked or restricted application is opened by the user during their focus sessions. Once detected, Limitra overlays a lock screen (shield) to prevent usage and help you stay focused.
* **No Data Collection or Transmission:** The Accessibility Service API is used strictly in real-time. We **do not read, collect, store, or transmit** any personal or sensitive information (such as passwords, financial details, messages, or keystrokes). All interactions are processed instantly and locally on your device.

### 3. System Alert Window (Overlay) Permission
The App requires the "Display over other apps" permission. This permission is used exclusively to display the lock/shield screen over restricted applications when the usage limit has been reached, ensuring your digital boundaries are enforced.

### 4. Local Data Storage (Room Database)
* **What we store:** Limitra stores your selected restricted application packages, daily limits, consecutive success days, and local application usage statistics (e.g. time spent on specific apps).
* **Where it is stored:** All Limitra core data is stored **strictly locally** on your device using local Room Database storage, protected by the Android app sandbox with backups disabled (`android:allowBackup="false"`).
* **No Network Capability:** Limitra does not request Android's Internet or network-state permissions. Your productivity and detox data **never leaves your device** and is never shared or sold.

### 5. Third-Party Services and Advertising
Limitra contains **no advertising SDK, analytics SDK, tracker, cloud telemetry, or remote account service**. It does not display ads and does not access the Google Advertising ID. Links to the privacy policy, terms, or email support are opened by another app chosen by the user, such as their browser or email client; Limitra itself does not download those pages.

---

## Türkçe Versiyon

Bu Gizlilik Politikası, Limitra ("biz", "bizim" veya "bize") mobil uygulamasındaki ("Uygulama") kullanıcı verilerinin ve izinlerin nasıl işlendiğini açıklamaktadır.

### 1. Genel Bakış ve Temel İşlevsellik
Limitra, kullanıcıların ekran sürelerini sınırlandırmalarına ve kendi belirledikleri limitler doğrultusunda dikkat dağıtıcı uygulamaları engellemelerine yardımcı olmak için tasarlanmış bir dijital detoks ve üretkenlik aracıdır.

### 2. Erişilebilirlik Hizmeti (Accessibility Service API) Kullanımı (ÖNEMLİ AÇIKLAMA)
Limitra, temel işlevlerini yerine getirebilmek için Android **Erişilebilirlik Hizmetleri API'sini (Accessibility Service API)** kullanır.
* **Neden kullanıyoruz:** Bu API'yi yalnızca, odaklanma seanslarınız sırasında engellenen veya sınırlandırılan bir uygulamanın açılıp açılmadığını anlık olarak algılamak amacıyla kullanırız. Algılandığında Limitra, kullanımı durdurmak ve odaklanmanıza yardımcı olmak için ekranda bir kilit/koruma ekranı gösterir.
* **Veri Toplanmaz veya Aktarılmaz:** Erişilebilirlik Hizmeti API'si kesinlikle gerçek zamanlı olarak çalışır. Şifreler, kredi kartı numaraları, kişisel mesajlar veya yazdığınız metinler gibi hiçbir kişisel veya hassas veriyi **okumayız, toplamayız, kaydetmeyiz ve dışarıya aktarmayız**. Tüm işlemler cihazınızda anlık ve yerel olarak gerçekleşir.

### 3. Diğer Uygulamaların Üzerinde Görüntüleme (System Alert Window / Overlay) İzni
Uygulama, "Diğer uygulamaların üzerinde görüntüleme" iznine ihtiyaç duyar. Bu izin, günlük sınırınız dolduğunda engellenen uygulamanın üzerinde kilit/koruma ekranını göstermek için kullanılır. Böylece belirlediğiniz detoks kurallarının uygulanması sağlanır.

### 4. Yerel Veri Depolama (Room Veritabanı)
* **Neleri kaydediyoruz:** Limitra; kısıtlanan uygulamalarınızın paket adlarını, günlük kullanım limitlerinizi, ardışık başarı günlerinizi ve yerel uygulama kullanım istatistiklerinizi (örn. hangi uygulamada ne kadar süre harcandığı) kaydeder.
* **Nerede depolanıyor:** Limitra'nın tüm çekirdek verileri, Android uygulama korumalı alanı (app sandbox) ile korunan ve yedeklemeleri devre dışı bırakılmış (`android:allowBackup="false"`) **yerel (local) Room Veritabanı** içinde saklanır.
* **Ağ Yeteneği Yoktur:** Limitra, Android'in İnternet veya ağ durumu izinlerini istemez. Verileriniz **asla cihazınızdan dışarı çıkmaz**, üçüncü taraflarla paylaşılmaz veya satılmaz.

### 5. Üçüncü Taraf Servisler ve Reklamlar
Limitra **reklam SDK'sı, analitik SDK'sı, izleyici, bulut telemetrisi veya uzak hesap servisi içermez**. Reklam göstermez ve Google Reklam Kimliği'ne erişmez. Gizlilik politikası, kullanım şartları veya e-posta destek bağlantıları; yalnızca kullanıcı dokunduğunda tarayıcı ya da e-posta uygulaması gibi kullanıcının seçtiği başka bir uygulamada açılır. Limitra bu sayfaları kendisi indirmez.
