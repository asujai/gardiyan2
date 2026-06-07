# Privacy Policy / Gizlilik Politikası

**Effective Date / Yürürlük Tarihi:** June 7, 2026

---

## English Version

This Privacy Policy describes how Gardiyan ("we", "our", or "us") handles user data and permissions in the Gardiyan mobile application (the "App").

### 1. Overview and Core Functionality
Gardiyan is a digital detox and productivity tool designed to help users limit their screen time and block distractive applications based on user-defined limits. 

### 2. Accessibility Service API Usage (CRITICAL DISCLOSURE)
Gardiyan uses the Android **Accessibility Service API** to provide its core functionality.
* **Why we use it:** We use this API solely to detect when a blocked or restricted application is opened by the user during their focus sessions. Once detected, Gardiyan overlays a lock screen (shield) to prevent usage and help you stay focused.
* **No Data Collection or Transmission:** The Accessibility Service API is used strictly in real-time. We **do not read, collect, store, or transmit** any personal or sensitive information (such as passwords, credit card numbers, messages, or keystrokes). All interactions are processed instantly on your local device.

### 3. System Alert Window (Overlay) Permission
The App requires the "Display over other apps" permission. This permission is used exclusively to display the lock/shield screen over restricted applications when the usage limit has been reached, ensuring the detox rules you set are enforced.

### 4. Local Data Storage (Room Database)
* **What we store:** Gardiyan stores your restricted application packages, daily limits, consecutive success days, and local application usage statistics (e.g., how much time is spent on specific apps).
* **Where it is stored:** All data is stored **locally** on your device using an encrypted/secured Room Database.
* **No Server Connections:** Gardiyan has **no remote servers**. Your data **never leaves your device** and is never shared, sold, or sent to any third party.

### 5. Third-Party Services
Gardiyan does not integrate with any third-party SDKs, analytics platforms, or ad networks that collect user data.

---

## Türkçe Versiyon

Bu Gizlilik Politikası, Gardiyan ("biz", "bizim" veya "bize") mobil uygulamasındaki ("Uygulama") kullanıcı verilerinin ve izinlerinin nasıl işlendiğini açıklamaktadır.

### 1. Genel Bakış ve Temel İşlevsellik
Gardiyan, kullanıcıların ekran sürelerini sınırlamalarına ve kendi belirledikleri limitler doğrultusunda dikkat dağıtıcı uygulamaları engellemelerine yardımcı olmak için tasarlanmış bir dijital detoks ve üretkenlik aracıdır.

### 2. Erişilebilirlik Hizmeti (Accessibility Service API) Kullanımı (ÖNEMLİ AÇIKLAMA)
Gardiyan, temel işlevlerini yerine getirebilmek için Android **Erişilebilirlik Hizmetleri API'sini (Accessibility Service API)** kullanır.
* **Neden kullanıyoruz:** Bu API'yi yalnızca, odaklanma seanslarınız sırasında engellenen veya sınırlandırılan bir uygulamanın açılıp açılmadığını algılamak amacıyla kullanırız. Algılandığında Gardiyan, kullanımı durdurmak ve odaklanmanıza yardımcı olmak için ekranda bir kilit/koruma ekranı gösterir.
* **Veri Toplanmaz veya Aktarılmaz:** Erişilebilirlik Hizmeti API'si kesinlikle gerçek zamanlı olarak çalışır. Şifreler, kredi kartı numaraları, kişisel mesajlar veya yazdığınız metinler gibi hiçbir kişisel veya hassas veriyi **okumayız, toplamayız, kaydetmeyiz ve dışarıya aktarmayız**. Tüm işlemler cihazınızda anlık ve yerel olarak gerçekleşir.

### 3. Diğer Uygulamaların Üzerinde Görüntüleme (System Alert Window / Overlay) İzni
Uygulama, "Diğer uygulamaların üzerinde görüntüleme" iznine ihtiyaç duyar. Bu izin, günlük sınırınız dolduğunda engellenen uygulamanın üzerinde kilit/koruma ekranını göstermek için kullanılır. Böylece belirlediğiniz detoks kurallarının uygulanması sağlanır.

### 4. Yerel Veri Depolama (Room Veritabanı)
* **Neleri kaydediyoruz:** Gardiyan; kısıtlanan uygulamalarınızın paket adlarını, günlük kullanım limitlerinizi, ardışık başarı günlerinizi ve yerel uygulama kullanım istatistiklerinizi (örn. hangi uygulamada ne kadar süre harcandığı) kaydeder.
* **Nerede depolanıyor:** Tüm veriler, cihazınızda güvenli bir şekilde **yerel (local) Room Veritabanı** içinde saklanır.
* **Sunucu Bağlantısı Yoktur:** Gardiyan'ın **hiçbir uzak sunucusu yoktur**. Verileriniz **asla cihazınızdan dışarı çıkmaz**, üçüncü taraflarla paylaşılmaz, satılmaz veya herhangi bir bulut sunucusuna gönderilmez.

### 5. Üçüncü Taraf Servisler
Gardiyan, kullanıcı verilerini toplayan hiçbir üçüncü taraf SDK, analitik platformu veya reklam ağı entegrasyonu içermez.
