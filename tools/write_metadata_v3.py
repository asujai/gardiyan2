"""Limitra v3 mağaza açıklamaları ve sürüm notları üretici (9 dil).
Master: metadata/en-US/{short_description,full_description}.txt ve store_assets/en-US-v3/release_notes.txt
Referans: metadata/tr-TR/ ve store_assets/tr-TR-v3/
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

SHORT_DESCS = {
    "de-DE": "Tägliche App-Limits, die sperren wenn Zeit um ist. Einmal zahlen, kein Abo.",
    "es-ES": "Límites diarios que bloquean al agotarse el tiempo. Pago único, sin suscripción.",
    "fr-FR": "Limites d'applis qui bloquent à temps écoulé. Achat unique, sans abonnement.",
    "pt-BR": "Limites diários que bloqueiam ao acabar o tempo. Pague uma vez, sem assinatura.",
    "id": "Batas harian yang mengunci saat waktu habis. Sekali bayar, tanpa langganan.",
    "ru-RU": "Дневные лимиты блокируют приложения при истечении времени. Без подписок.",
    "hi-IN": "समय समाप्त होने पर लॉक होने वाली ऐप सीमाएं। एक बार भुगतान, कोई सदस्यता नहीं।",
    "th": "จำกัดเวลาแอปรายวัน ล็อกทันทีเมื่อหมดเวลา จ่ายครั้งเดียว ไม่ต้องสมัครสมาชิก",
    "ar": "حدود يومية تقفل التطبيق عند انتهاء الوقت. ادفع مرة واحدة وبلا اشتراك.",
}

RELEASE_NOTES = {
    "de-DE": """• Komplett überarbeitetes Design: verfeinerte Typografie, vier Farbthemen in Hell und Dunkel, flüssigere Animationen.
• Neue Erfolge: Halte deine Serie, um 9 animierte Rahmen von Tag 1 bis Tag 365 freizuschalten.
• Der Sperrbildschirm passt sich jetzt deinem Design an.
• Die tägliche Nutzungsliste wird um Mitternacht zuverlässig zurückgesetzt; Limit-Warnungen erscheinen sofort.""",

    "es-ES": """• Diseño completamente renovado: tipografía refinada, cuatro temas de color en claro y oscuro, animaciones más fluidas.
• Nuevos logros: mantén tu racha para ganar 9 marcos animados, del día 1 al 365.
• La pantalla de bloqueo ahora coincide con tu tema.
• La lista de uso diario se reinicia a medianoche y los avisos de edición de límites aparecen al instante.""",

    "fr-FR": """• Design entièrement repensé : typographie soignée, quatre thèmes en mode clair et sombre, animations plus fluides.
• Nouveaux succès : gardez votre série pour débloquer 9 cadres animés, du 1er au 365e jour.
• L'écran de verrouillage s'adapte désormais à votre thème.
• Réinitialisation précise de l'utilisation à minuit et alertes instantanées lors des modifications.""",

    "pt-BR": """• Visual totalmente reformulado: tipografia refinada, quatro temas de cores no claro e escuro, animações mais fluidas.
• Novas conquistas: mantenha sua sequência para desbloquear 9 molduras animadas, do dia 1 ao 365.
• A tela de bloqueio agora acompanha o seu tema.
• O uso diário é zerado pontualmente à meia-noite e os avisos de alteração de limites aparecem na hora.""",

    "id": """• Tampilan baru sepenuhnya: tipografi lebih halus, empat tema warna terang dan gelap, animasi lebih mulus.
• Pencapaian Baru: pertahankan streak untuk meraih 9 bingkai animasi, dari hari ke-1 hingga ke-365.
• Layar kunci kini menyesuaikan dengan tema pilihanmu.
• Penggunaan harian kini tereset tepat tengah malam dan peringatan ubah limit muncul seketika.""",

    "ru-RU": """• Полностью обновлённый дизайн: утончённая типографика, 4 цветовые темы (светлая и тёмная), плавные анимации.
• Новые достижения: удерживайте серию и открывайте 9 анимированных рамок от 1 до 365 дня.
• Экран блокировки теперь оформлен в выбранной теме.
• Список использования точно сбрасывается в полночь; предупреждения об изменении лимитов появляются мгновенно.""",

    "hi-IN": """• पूरी तरह से नया डिज़ाइन: परिष्कृत टाइपोग्राफी, लाइट और डार्क में चार रंगीन थीम, सहज एनिमेशन।
• नई उपलब्धियां: दिन 1 से 365 तक 9 एनिमेटेड फ्रेम अनलॉक करने के लिए अपनी स्ट्रीक बनाए रखें।
• लॉक स्क्रीन अब आपकी थीम से मेल खाती है।
• दैनिक उपयोग सूची आधी रात को सही ढंग से रीसेट होती है; सीमा संपादन चेतावनियां तुरंत दिखाई देती हैं।""",

    "th": """• ดีไซน์ใหม่ทั้งหมด: ตัวอักษรประณีตยิ่งขึ้น 4 ธีมสีทั้งโหมดสว่างและมืด แอนิเมชันลื่นไหลกว่าเดิม
• ความสำเร็จใหม่: รักษาความต่อเนื่องเพื่อรับ 9 กรอบภาพเคลื่อนไหว ตั้งแต่วันที่ 1 ถึง 365
• หน้าจอล็อกเปลี่ยนสีและสไตล์ตามธีมของคุณแล้ว
• รีเซ็ตสถิติรายวันอย่างถูกต้องตอนเที่ยงคืน พร้อมการแจ้งเตือนแก้ไขลิมิตทันที""",

    "ar": """• مظهر جديد كلياً: خطوط مصقولة، 4 سمات ألوان بالوضعين الفاتح والداكن، حركات أكثر سلاسة.
• إنجازات جديدة: حافظ على سلسلتك لتحصل على 9 إطارات متحركة من اليوم 1 حتى 365.
• شاشة القفل الآن تتطابق تماماً مع السمة المختارة.
• يُعاد ضبط قائمة الاستخدام بدقة عند منتصف الليل، وتظهر تنبيهات تعديل الحدود فوراً.""",
}

FULL_DESCS = {
    "de-DE": """„Nur noch fünf Minuten“ — und schon ist eine Stunde vorbei. Limitra App Block durchbricht diese Schleife.

Wähle die Apps, die dich ablenken, lege ein tägliches Zeitlimit fest, und Limitra sperrt sie in der Sekunde, in der deine Zeit abgelaufen ist. Kein Konto, keine Werbung, kein Abonnement. Du zahlst einmal und die App funktioniert komplett offline.

SO FUNKTIONIERT ES
1. Wähle die Apps, die du einschränken möchtest: soziale Medien, Kurzvideos, Spiele oder beliebige andere Apps.
2. Bestimme das Tageslimit, die gültigen Tage und auf Wunsch aktive Zeitfenster wie Arbeits- oder Lernzeiten.
3. Ist die Zeit um, tritt ein ruhiger Sperrbildschirm anstelle der App, begleitet von einem kurzen Zitat, das zum Innehalten einlädt.

WAS DU BEKOMMST
• Tägliche Limits für jede App mit optionalen Zeitfenstern und Schutztagen
• Sperrbildschirm mit stoischen Zitaten (Seneca, Marcus Aurelius, Epiktet und mehr) oder eigenen Texten
• Serien-Zähler und 9 animierte Erfolgsrahmen vom 1. bis zum 365. Tag
• Sechs Disziplinstufen, von Anfänger bis Volle Kontrolle
• Private Zeitleiste für jede Sperre und jeden erfolgreich abgeschlossenen Tag
• Vier Farbthemen im hellen und dunklen Modus in 11 Sprachen

DATENSCHUTZ DURCH DESIGN
• Kein Konto, keine Werbung, keine Tracker
• Limitra verlangt keine Internetberechtigung; deine Nutzungsdaten verlassen niemals dein Gerät
• Alles funktioniert vollständig offline

EINMALIGE ZAHLUNG
Ein einziger Kauf schaltet alle Funktionen frei. Kein Abonnement, keine automatische Verlängerung, keine Zusatzverkäufe.

FÜR WEN ES GEMACHT IST
• Schüler und Studierende in Prüfungsphasen, die ungestörte Lernzeit brauchen
• Entwickler, Schreibende und alle, die tiefe Konzentration suchen
• Jeder, der endloses Scrollen besonders spät abends reduzieren möchte

BERECHTIGUNGEN: WARUM LIMITRA SIE BRAUCHT
• Nutzungsdaten-Zugriff: Berechnet direkt auf deinem Gerät die Nutzungszeit der gewählten Apps zur Limit-Einhaltung.
• Bedienungshilfen-Dienst: Erkennt das Öffnen gesperrter Apps, um den Sperrbildschirm anzuzeigen. Limitra liest, speichert oder überträgt weder Nachrichten noch Passwörter oder private Daten.
• Über anderen Apps anzeigen: Legt den Sperrbildschirm über die eingeschränkte Anwendung.
• Akku-Optimierung ignorieren: Gewährleistet den zuverlässigen Schutzbetrieb im Hintergrund.

HÄUFIG GESTELLTE FRAGEN
Funktioniert Limitra ohne Internet?
Ja. Die App hat keine Internetberechtigung und läuft vollständig lokal auf deinem Smartphone.

Muss ich später erneut bezahlen?
Nein. Einmaliger Kauf, keine wiederkehrenden Kosten.

Kann ich eigene Zitate auf dem Sperrbildschirm verwenden?
Ja. Trage dein eigenes Zitat in den Einstellungen ein und lasse es dauerhaft oder abwechselnd anzeigen.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "es-ES": """«Solo cinco minutos más» — y se pasa una hora. Limitra App Block detiene ese bucle.

Elige las apps que más te distraen, fija un límite diario y Limitra las bloqueará en el instante en que tu tiempo se termine. Sin cuentas, sin publicidad y sin suscripciones. Pagas una sola vez y funciona por completo sin conexión.

CÓMO FUNCIONA
1. Selecciona las aplicaciones que quieres limitar: redes sociales, vídeos cortos, juegos o cualquier app.
2. Define el límite diario, los días activos y, si lo deseas, un horario determinado como horas de trabajo o estudio.
3. Al agotarse el tiempo, una pantalla de bloqueo serena sustituye a la app, con una cita breve para ayudarte a pausar.

QUÉ INCLUYE
• Límites diarios individuales por app, con intervalo horario y días de protección opcionales
• Pantalla de bloqueo con citas estoicas (Séneca, Marco Aurelio, Epicteto y más) o tus propias frases
• Registro de racha y 9 marcos animados de logros, desde el día 1 hasta el día 365
• Seis niveles de disciplina, desde Principiante hasta Control Total
• Cronología privada de cada bloqueo y cada día completado con éxito
• Cuatro temas de color en modos claro y oscuro, disponibles en 11 idiomas

PRIVACIDAD POR DISEÑO
• Sin cuenta de usuario, sin anuncios, sin rastreadores
• Limitra no solicita permiso de internet: tus datos de uso jamás salen de tu teléfono
• Todo funciona 100% offline

PAGO ÚNICO
Una compra única desbloquea todas las funciones. Sin suscripciones recurrentes ni costes añadidos.

PENSADO PARA
• Estudiantes que preparan exámenes y requieren sesiones de estudio sin distracciones
• Desarrolladores, escritores y cualquier profesional que busque concentración profunda
• Quienes deseen reducir el desplazamiento infinito por la pantalla, sobre todo por la noche

PERMISOS: POR QUÉ LOS PIDE LIMITRA
• Acceso de uso: calcula en tu propio dispositivo el tiempo que usas cada app para aplicar el límite.
• Servicio de accesibilidad: detecta la apertura de una aplicación limitada para mostrar la pantalla de bloqueo. Limitra no lee, no guarda ni envía mensajes, contraseñas ni datos personales.
• Mostrar sobre otras apps: permite desplegar la pantalla de bloqueo encima de la app limitada.
• Exención de optimización de batería: ayuda a mantener la protección activa en segundo plano con fiabilidad.

PREGUNTAS FRECUENTES
¿Funciona Limitra sin internet?
Sí. No tiene permiso de acceso a internet y se ejecuta por completo en tu teléfono.

¿Tendré que volver a pagar?
No. Es una única compra de por vida, sin renovaciones.

¿Puedo poner mis propias citas en la pantalla de bloqueo?
Sí. Puedes añadir tus citas en Ajustes y mostrarlas siempre o combinadas con las predeterminadas.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "fr-FR": """« Juste cinq minutes de plus » — et une heure s'est envolée. Limitra App Block brise cette boucle.

Choisissez les applications qui captent votre attention, fixez une limite quotidienne, et Limitra les bloque dès que votre temps est écoulé. Sans compte, sans publicité, sans abonnement. Vous payez une seule fois et l'application fonctionne totalement hors ligne.

COMMENT ÇA MARCHE
1. Choisissez les applications à restreindre : réseaux sociaux, vidéos courtes, jeux ou tout autre contenu.
2. Définissez la limite journalière, les jours d'application et, si besoin, une plage horaire active (travail, révisions).
3. Une fois le temps écoulé, un écran de verrouillage apaisant remplace l'application, affichant une citation pour vous inviter à souffler.

CE QUI EST INCLUS
• Limites quotidiennes par application avec plages horaires et jours de protection optionnels
• Écran de verrouillage avec citations stoïciennes (Sénèque, Marc Aurèle, Épictète, etc.) ou vos propres phrases
• Suivi de série et 9 cadres animés de succès, du 1er au 365e jour
• Six niveaux de discipline, de Débutant à Contrôle Total
• Chronologie privée répertoriant chaque verrouillage et chaque journée réussie
• Quatre thèmes de couleurs en modes clair et sombre, traduits en 11 langues

CONFIDENTIALITÉ DÈS LA CONCEPTION
• Aucun compte, aucune publicité, aucun traceur
• Limitra ne demande pas l'autorisation internet : vos données d'utilisation ne quittent jamais votre appareil
• Fonctionnement intégralement hors ligne

PAIEMENT UNIQUE
Un achat unique donne accès à toutes les fonctionnalités actuelles et futures. Zéro abonnement, zéro vente incitative.

CONÇU POUR
• Les étudiants préparant des examens nécessitant une concentration ininterrompue
• Les développeurs, auteurs et créateurs en quête de travail en profondeur
• Toute personne voulant réduire le défilement infini sur écran, en particulier tard le soir

AUTORISATIONS : POURQUOI LIMITRA EN A BESOIN
• Accès aux données d'utilisation : mesure localement le temps passé sur chaque app choisie afin d'appliquer la limite.
• Service d'accessibilité : détecte l'ouverture d'une application restreinte pour afficher l'écran de verrouillage. Limitra ne lit, n'enregistre ni ne transmet aucun message, mot de passe ou donnée privée.
• Superposition sur d'autres applications : affiche l'écran de verrouillage par-dessus l'app restreinte.
• Exclusion d'optimisation de batterie : garantit le maintien du service de protection en arrière-plan.

FOIRE AUX QUESTIONS
Limitra fonctionne-t-il sans connexion internet ?
Oui. L'application ne possède aucune autorisation internet et tourne exclusivement sur votre téléphone.

Devrai-je payer à nouveau ?
Non. Un achat unique et définitif, sans renouvellement.

Puis-je afficher mes propres citations sur l'écran de verrouillage ?
Oui. Ajoutez vos citations personnelles dans les Réglages pour les afficher seules ou en alternance.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "pt-BR": """“Só mais cinco minutinhos” — e uma hora inteira já se foi. O Limitra App Block interrompe esse ciclo.

Escolha os aplicativos que mais tomam seu tempo, defina um limite diário e o Limitra os bloqueia no instante em que seu tempo acaba. Sem conta, sem anúncios e sem assinatura. Você paga apenas uma vez e tudo funciona 100% offline.

COMO FUNCIONA
1. Selecione os aplicativos que deseja limitar: redes sociais, vídeos curtos, jogos ou o que preferir.
2. Defina o limite de tempo diário, os dias da semana aplicáveis e, se quiser, um intervalo de horário ativo para focar no trabalho ou estudos.
3. Quando o tempo expirar, uma tela de bloqueio serena substitui o app, trazendo uma breve citação reflexiva para ajudá-lo a fazer uma pausa.

O QUE VOCÊ RECEBE
• Limites diários para cada app, com intervalo de horários e dias de proteção personalizados
• Tela de bloqueio com citações estoicas (Sêneca, Marco Aurélio, Epicteto e outros) ou frases personalizadas
• Acompanhamento de sequência e 9 molduras animadas de conquistas, do dia 1 ao 365
• Seis níveis de disciplina, do Iniciante ao Controle Total
• Linha do tempo privada registrando cada bloqueio e dia de meta cumprida
• Quatro temas visuais nos modos claro e escuro, em 11 idiomas

PRIVACIDADE POR PRINCÍPIO
• Sem cadastro, sem anúncios, sem rastreadores
• O Limitra não solicita permissão de internet: seus dados de uso jamais saem do seu aparelho
• Funcionamento totalmente offline

PAGAMENTO ÚNICO
Uma única compra inclui todas as funcionalidades. Sem assinaturas, sem cobranças recorrentes nem vendas casadas.

FEITO PARA
• Estudantes se preparando para provas e vestibulares que precisam de foco contínuo
• Programadores, escritores e profissionais que demandam concentração profunda
• Quem deseja diminuir a rolagem infinita nas telas, especialmente à noite

PERMISSÕES: POR QUE O LIMITRA SOLICITA
• Acesso ao uso: calcula no próprio aparelho o tempo de uso de cada app escolhido para garantir o cumprimento do limite.
• Serviço de acessibilidade: identifica quando um app restrito é aberto para acionar a tela de bloqueio. O Limitra não lê, não armazena e não transmite mensagens, senhas nem informações pessoais.
• Sobrepor a outros apps: permite exibir a tela de bloqueio sobre o app restrito.
• Isenção da otimização de bateria: mantém a proteção funcionando de modo contínuo em segundo plano.

PERGUNTAS FREQUENTES
O Limitra funciona sem conexão à internet?
Sim. O aplicativo não possui permissão de internet e opera estritamente no seu dispositivo.

Terei que pagar novamente depois?
Não. Pagamento único com acesso completo e vitalício, sem renovações.

Posso exibir minhas próprias frases na tela de bloqueio?
Sim. Você pode cadastrar frases próprias nas Configurações e exibi-las com exclusividade ou junto às citações padrão.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "id": """“Lima menit lagi” — tahu-tahu satu jam telah berlalu. Limitra App Block menghentikan lingkaran itu.

Pilih aplikasi yang menyita perhatianmu, tentukan batas waktu harian, dan Limitra langsung menguncinya begitu waktumu habis. Tanpa akun, tanpa iklan, tanpa langganan. Cukup sekali bayar dan aplikasi berfungsi sepenuhnya secara offline.

CARA KERJA
1. Pilih aplikasi yang ingin dibatasi: media sosial, video pendek, game, atau apa pun.
2. Tentukan batas harian, hari aktif, dan jika mau, rentang jam aktif seperti jam kerja atau jam belajar.
3. Saat waktu habis, layar kunci yang tenang akan menggantikan aplikasi, disertai kutipan singkat untuk membantumu berhenti sejenak.

FITUR UTAMA
• Batas waktu harian per aplikasi, dengan rentang jam dan hari perlindungan opsional
• Layar kunci dengan kutipan Stoik (Seneca, Marcus Aurelius, Epictetus, dll.) atau kutipan buatanmu sendiri
• Pelacak streak dan 9 bingkai pencapaian animasi, dari hari ke-1 hingga hari ke-365
• Enam tingkat disiplin, mulai dari Pemula hingga Kontrol Penuh
• Linimasa pribadi untuk setiap penguncian dan hari sukses yang diselesaikan
• Empat tema warna dalam mode terang dan gelap, tersedia dalam 11 bahasa

PRIVASI TINGKAT TINGGI
• Tanpa akun, tanpa iklan, tanpa pelacak
• Limitra tidak meminta izin akses internet: data penggunaan tidak pernah keluar dari ponselmu
• Semua fitur berjalan offline

SEKALI BAYAR
Satu kali pembelian mencakup semua fitur. Tanpa langganan bulanan, tanpa biaya perpanjangan, tanpa biaya tersembunyi.

DIRANCANG UNTUK
• Pelajar dan mahasiswa yang mempersiapkan ujian dan butuh waktu belajar tanpa gangguan
• Programmer, penulis, dan profesional yang membutuhkan fokus mendalam
• Siapa saja yang ingin mengurangi scrolling tiada henti di media sosial, terutama larut malam

IZIN APLIKASI: MENGAPA LIMITRA MEMINTANYA
• Akses penggunaan: menghitung langsung di perangkat durasi pemakaian setiap aplikasi untuk menerapkan batas waktu.
• Layanan aksesibilitas: mendeteksi saat aplikasi yang dibatasi dibuka agar layar kunci bisa muncul. Limitra tidak membaca, tidak menyimpan, dan tidak mengirimkan pesan, kata sandi, atau data pribadi apa pun.
• Tampilkan di atas aplikasi lain: menampilkan layar kunci di atas aplikasi yang dibatasi.
• Pengecualian optimasi baterai: menjaga perlindungan tetap aktif secara andal di latar belakang.

PERTANYAAN UMUM
Apakah Limitra berfungsi tanpa internet?
Ya. Aplikasi ini tidak memiliki izin internet dan beroperasi sepenuhnya di dalam perangkatmu.

Apakah saya akan ditagih lagi nanti?
Tidak. Sekali bayar untuk seterusnya, tanpa perpanjangan langganan.

Bisakah saya memasang kutipan sendiri di layar kunci?
Bisa. Tambahkan kutipan pribadimu di Pengaturan untuk ditampilkan terus-menerus atau bergantian dengan kutipan bawaan.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "ru-RU": """«Ещё пять минут» — и прошёл целый час. Limitra App Block останавливает этот замкнутый круг.

Выберите приложения, которые отнимают ваше время, задайте дневной лимит, и Limitra заблокирует их сразу по истечении времени. Без аккаунтов, без рекламы и без подписок. Вы платите один раз, и приложение работает полностью офлайн.

КАК ЭТО РАБОТАЕТ
1. Выберите нужные приложения: соцсети, короткие видеоролики, игры или любые другие программы.
2. Установите дневной лимит времени, дни недели и при необходимости интервал активных часов (например, время учёбы или работы).
3. Когда лимит исчерпан, поверх приложения открывается спокойный экран блокировки с короткой цитатой, помогающей сделать паузу.

ЧТО ВЫ ПОЛУЧАЕТЕ
• Дневные лимиты для каждого приложения с настройкой активных часов и дней недели
• Экран блокировки с цитатами стоиков (Сенека, Марк Аврелий, Эпиктет и др.) или вашими собственными мыслями
• Учёт непрерывной серии и 9 анимированных рамок достижений с 1 по 365 день
• Шесть уровней дисциплины: от Новичок до Полный контроль
• Приватная хроника каждого факта блокировки и каждого успешно завершённого дня
• Четыре цветовые темы в светлом и тёмном оформлении на 11 языках

КОНФИДЕНЦИАЛЬНОСТЬ ПО УМОЛЧАНИЮ
• Без учётных записей, без рекламы, без сторонних трекеров
• Limitra не запрашивает разрешение на доступ к интернету: данные об использовании никогда не покидают ваше устройство
• Полная работа в автономном режиме

ЕДИНОВРЕМЕННАЯ ОПЛАТА
Одна покупка открывает все возможности. Никаких подписок, скрытых платежей и регулярных списаний.

ДЛЯ КОГО СОЗДАНО
• Студентов и школьников, которым требуется непрерывная концентрация при подготовке к экзаменам
• Разработчиков, авторов и специалистов, нуждающихся в глубоком фокусе
• Всех, кто хочет избавиться от бесконечного листания ленты, особенно перед сном

РАЗРЕШЕНИЯ: ЗАЧЕМ ОНИ НУЖНЫ LIMITRA
• Доступ к истории использования: локально на устройстве вычисляет время работы приложений для соблюдения лимита.
• Служба специальных возможностей (Accessibility): определяет запуск ограниченного приложения для отображения экрана блокировки. Limitra не читает, не сохраняет и не передаёт ваши сообщения, пароли или личные данные.
• Отображение поверх других окон: выводит экран блокировки поверх заблокированного приложения.
• Игнорирование оптимизации батареи: обеспечивает стабильную работу защиты в фоновом режиме.

ЧАСТО ЗАДАВАЕМЫЕ ВОПРОСЫ
Работает ли Limitra без подключения к интернету?
Да. У приложения нет разрешения на выход в интернет, оно работает локально на вашем смартфоне.

Будут ли повторные списания?
Нет. Это разовая покупка с бессрочным доступом.

Можно ли выводить свои цитаты на экран блокировки?
Да. Вы можете добавить собственные фразы в Настройках и показывать только их или чередовать с базовыми.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "hi-IN": """“बस पांच मिनट और” — और एक घंटा बीत जाता है। Limitra App Block इस चक्र को रोकता है।

उन ऐप्स को चुनें जो आपका समय खींचते हैं, एक दैनिक समय सीमा निर्धारित करें, और समय समाप्त होते ही Limitra उन्हें लॉक कर देता है। कोई खाता नहीं, कोई विज्ञापन नहीं, कोई सदस्यता नहीं। आप केवल एक बार भुगतान करते हैं और यह पूरी तरह से ऑफ़लाइन काम करता है।

यह कैसे काम करता है
1. वे ऐप्स चुनें जिन्हें आप सीमित करना चाहते हैं: सोशल मीडिया, लघु वीडियो, गेम या कुछ भी।
2. दैनिक सीमा, लागू होने वाले दिन और काम या अध्ययन के लिए सक्रिय घंटे तय करें।
3. समय समाप्त होने पर, ऐप की जगह एक शांत लॉक स्क्रीन आ जाती है, जो आपको रुकने में मदद करने के लिए एक विचार दिखाती है।

आपको क्या मिलता है
• प्रत्येक ऐप के लिए दैनिक सीमाएं, वैकल्पिक सक्रिय घंटे और सुरक्षा दिवस
• स्टोइक विचारकों (सेनेका, मार्कस ऑरेलियस, एपिक्टेटस आदि) या आपके अपने विचारों के साथ लॉक स्क्रीन
• स्ट्रीक ट्रैकिंग और दिन 1 से 365 तक 9 एनिमेटेड उपलब्धि फ्रेम
• नौसिखिया से पूर्ण नियंत्रण तक छह अनुशासन स्तर
• प्रत्येक लॉक और प्रत्येक पूर्ण दिन की एक निजी समयरेखा
• 11 भाषाओं में लाइट और डार्क मोड में चार रंगीन थीम

गोपनीयता पहले
• कोई खाता नहीं, कोई विज्ञापन नहीं, कोई ट्रैकर नहीं
• Limitra इंटरनेट अनुमति नहीं मांगता; आपका उपयोग डेटा कभी आपके फोन से बाहर नहीं जाता
• सब कुछ ऑफ़लाइन काम करता है

एकमुश्त भुगतान
एक ही खरीद में हर सुविधा शामिल है। कोई सदस्यता नहीं, कोई स्वतः नवीनीकरण नहीं।

किसके लिए उपयोगी है
• परीक्षा की तैयारी करने वाले छात्र जिन्हें बिना रुकावट अध्ययन समय की आवश्यकता है
• डेवलपर्स, लेखक और गहरे फोकस की जरूरत वाले पेशेवर
• वे सभी जो विशेष रूप से देर रात स्क्रीन स्क्रॉलिंग कम करना चाहते हैं

अनुमतियां: LIMITRA इन्हें क्यों मांगता है
• उपयोग एक्सेस: सीमा लागू करने के लिए डिवाइस पर ऐप उपयोग समय की गणना करता है।
• एक्सेसिबिलिटी सेवा: सीमित ऐप खुलने पर तुरंत पहचान कर लॉक स्क्रीन प्रदर्शित करती है। Limitra आपके संदेश, पासवर्ड या व्यक्तिगत डेटा को कभी नहीं पढ़ता, सहेजता या भेजता है।
• अन्य ऐप्स के ऊपर प्रदर्शित करें: सीमित ऐप के ऊपर लॉक स्क्रीन दिखाता है।
• बैटरी ऑप्टिमाइज़ेशन छूट: सुरक्षा को बैकग्राउंड में लगातार चालू रखने में मदद करती है।

अक्सर पूछे जाने वाले प्रश्न
क्या Limitra बिना इंटरनेट के काम करता है?
हाँ। इसमें इंटरनेट की कोई अनुमति नहीं है और यह पूरी तरह से आपके फोन पर चलता है।

क्या मुझसे दोबारा शुल्क लिया जाएगा?
नहीं। एक बार का भुगतान, कोई नवीनीकरण नहीं।

क्या मैं लॉक स्क्रीन पर अपना विचार दिखा सकता हूँ?
हाँ। सेटिंग्स में अपना विचार जोड़ें और इसे हमेशा या डिफ़ॉल्ट के साथ मिलाकर दिखाएं।

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "th": """“ขออีกแค่ห้านาที” — รู้ตัวอีกทีก็ผ่านไปเป็นชั่วโมง Limitra App Block หยุดวงจรนี้ให้คุณ

เลือกแอปที่ดึงดูดเวลาของคุณ ตั้งลิมิตเวลาใช้งานในแต่ละวัน แล้ว Limitra จะล็อกแอปนั้นทันทีที่หมดเวลา ไม่ต้องมีบัญชีผู้ใช้ ไม่มีโฆษณา ไม่ต้องสมัครสมาชิกรายเดือน จ่ายเพียงครั้งเดียวและทำงานแบบออฟไลน์ได้อย่างสมบูรณ์

วิธีใช้งาน
1. เลือกแอปที่คุณต้องการจำกัด: โซเชียลมีเดีย, วิดีโอสั้น, เกม หรือแอปใดก็ได้
2. กำหนดเวลาใช้งานรายวัน วันที่ต้องการใช้งาน และช่วงเวลาที่ต้องการเปิดการป้องกัน (เช่น เวลางานหรือเวลาอ่านหนังสือ)
3. เมื่อหมดเวลา หน้าจอล็อกที่เรียบสงบจะขึ้นมาแทนที่แอป พร้อมข้อความเตือนสติสั้นๆ ให้คุณหยุดพัก

สิ่งที่คุณจะได้รับ
• จำกัดเวลาใช้งานรายวันแยกตามแอป พร้อมตั้งช่วงเวลาและวันป้องกันได้ตามใจ
• หน้าจอล็อกพร้อมคำคมสโตอิก (เซเนกา, มาร์คุส ออเรลิอุส, เอพิคเทตัส และอื่นๆ) หรือข้อความของคุณเอง
• การนับสถิติต่อเนื่อง (Streak) และ 9 กรอบความสำเร็จแบบเคลื่อนไหว ตั้งแต่วันที่ 1 ถึง 365
• หกระดับวินัย ตั้งแต่ มือใหม่ ถึง ควบคุมเต็มรูปแบบ
• ไทม์ไลน์ส่วนตัวบันทึกประวัติการล็อกและวันที่ทำสำเร็จครบถ้วน
• ธีมสี 4 แบบในโหมดสว่างและมืด รองรับ 11 ภาษา

ออกแบบเพื่อความเป็นส่วนตัว
• ไม่มีระบบสมาชิก ไม่มีโฆษณา ไม่มีการติดตามข้อมูล
• Limitra ไม่ขอสิทธิ์การเข้าถึงอินเทอร์เน็ต ข้อมูลการใช้งานของคุณจึงไม่เคยหลุดออกจากเครื่อง
• ทุกอย่างทำงานแบบออฟไลน์

จ่ายครั้งเดียวจบ
การซื้อเพียงครั้งเดียวปลดล็อกทุกฟีเจอร์ ไม่มีค่าบริการรายเดือน ไม่มีการต่ออายุอัตโนมัติ

เหมาะสำหรับ
• นักเรียน นักศึกษาที่เตรียมสอบและต้องการเวลาอ่านหนังสือโดยไม่มีสิ่งรบกวน
• นักพัฒนา นักเขียน และทุกคนที่ต้องการสมาธิขั้นสูงในการทำงาน
• ผู้ที่ต้องการลดการไถฟีดโซเชียล โดยเฉพาะช่วงดึกก่อนนอน

สิทธิ์การเข้าถึง: ทำไม LIMITRA ถึงต้องขอ
• สิทธิ์การเข้าถึงข้อมูลการใช้งาน: คำนวณระยะเวลาใช้งานแอปภายในเครื่องเพื่อบังคับใช้ลิมิต
• บริการการเข้าถึง (Accessibility Service): ตรวจจับการเปิดแอปที่ถูกจำกัดเพื่อแสดงหน้าจอล็อก โดย Limitra จะไม่เข้าถึง ไม่อ่าน ไม่บันทึก และไม่ส่งต่อข้อความ รหัสผ่าน หรือข้อมูลส่วนตัวใดๆ ทั้งสิ้น
• แสดงทับแอปอื่น: ใช้เพื่อแสดงหน้าจอล็อกทับบนแอปที่ถูกจำกัดเวลา
• ละเว้นการเพิ่มประสิทธิภาพแบตเตอรี่: เพื่อให้ระบบการป้องกันทำงานต่อเนื่องได้อย่างเสถียรในพื้นหลัง

คำถามที่พบบ่อย
Limitra ทำงานโดยไม่มีอินเทอร์เน็ตได้หรือไม่?
ได้ แอปไม่มีสิทธิ์เชื่อมต่ออินเทอร์เน็ตและทำงานทั้งหมดบนอุปกรณ์ของคุณ

จะต้องจ่ายเงินเพิ่มอีกในภายหลังหรือไม่?
ไม่ต้อง จ่ายเพียงครั้งเดียว ใช้ได้ตลอดชีพ ไม่มีการต่ออายุ

สามารถใช้คำคมของตัวเองบนหน้าจอล็อกได้หรือไม่?
ได้ คุณสามารถเพิ่มข้อความของตัวเองได้ในการตั้งค่า เพื่อแสดงตลอดเวลาหรือสลับกับคำคมตั้งต้น

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",

    "ar": """«خمس دقائق فقط» — وفجأة تمضي ساعة كاملة. Limitra App Block يضع حداً لهذه الدوامة.

اختر التطبيقات التي تسرق وقتك، وحدد لها حداً يومياً، وسيقوم Limitra بقفلها فور نفاد الوقت المخصص. بلا حساب، بلا إعلانات، وبلا اشتراكات. تدفع مرة واحدة ويعمل التطبيق بالكامل بدون إنترنت.

كيف يعمل التطبيق
1. حدد التطبيقات التي ترغب في تقييدها: شبكات التواصل، مقاطع الفيديو القصيرة، الألعاب، أو أي تطبيق آخر.
2. عيّن الحد الزمني اليومي، وأيام الحماية، ويمكنك أيضاً تحديد نافذة زمنية نشطة كأوقات العمل أو الدراسة.
3. عند انتهاء الوقت، تحل شاشة قفل هادئة محل التطبيق، مصحوبة باقتباس ملهم يعينك على التوقف وأخذ استراحة.

ما ستحصل عليه
• حدود يومية مخصصة لكل تطبيق، مع فترات نشطة وأيام حماية اختيارية
• شاشة قفل تضم اقتباسات رواقية (سينيكا، ماركوس أوريليوس، إبكتيتوس وغيرهم) أو عباراتك الشخصية
• متابعة سلسلة الأيام المتواصلة و9 إطارات إنجازات متحركة، من اليوم 1 حتى اليوم 365
• ستة مستويات انضباط، تبدأ من مبتدئ وتصل إلى تحكم كامل
• جدول زمني خاص يوثق كل عملية قفل وكل يوم تم إنجازه بنجاح
• أربع سمات ألوان بالوضعين الفاتح والداكن، متوفرة بـ 11 لغة

الخصوصية أولويتنا القصوى
• بلا حسابات، بلا إعلانات، وبلا أي أدوات تتبع
• لا يطلب Limitra إذن الوصول للإنترنت على الإطلاق؛ بيانات استخدامك لا تغادر هاتفك أبداً
• جميع الميزات تعمل محلياً دون الحاجة لاتصال بالشبكة

دفع لمرة واحدة
شراء واحد يمنحك الوصول الدائم لجميع الميزات. لا اشتراكات، لا تجديدات، ولا تكاليف إضافية.

مصمم خصيصاً لأجل
• الطلاب الذين يستعدون للامتحانات ويحتاجون إلى ساعات دراسة هادئة دون تشتيت
• المطورين والكُتّاب وكل من يتطلب عمله تركيزاً عميقاً
• كل من يسعى لتقليل التصفح اللانهائي للشاشات، لا سيما في أوقات النوم

الأذونات: لماذا يطلبها LIMITRA
• الوصول لبيانات الاستخدام: لقياس الوقت المستغرق داخل كل تطبيق محدد محلياً لتطبيق الحد.
• خدمة إمكانية الوصول: للتعرف على فتح التطبيق المقيد وعرض شاشة القفل فوراً. لا يقوم Limitra بقراءة رسائلك أو كلمات مرورك أو أي بيانات خاصة، ولا يخزنها ولا ينقلها إطلاقاً.
• الظهور فوق التطبيقات الأخرى: لإظهار شاشة القفل الهادئة أعلى التطبيق المقيد.
• استثناء تحسين أداء البطارية: لضمان استمرار الحماية ومراقبة الوقت في الخلفية بموثوقية.

الأسئلة الشائعة
هل يعمل Limitra بدون اتصال بالإنترنت؟
نعم. لا يمتلك التطبيق إذن الإنترنت إطلاقاً ويعمل محلياً بالكامل على هاتفك.

هل سأدفع أي رسوم مرة أخرى مستقبلاً؟
لا. شراء واحد لمرة واحدة فقط وبدون أي اشتراكات أو تجديد.

هل يمكنني إضافة عباراتي الخاصة لشاشة القفل؟
نعم. يمكنك كتابة اقتباساتك الخاصة في الإعدادات واختيار ظهورها دائماً أو بالتناوب مع الاقتباسات الافتراضية.

Website: https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest).""",
}

def main():
    errors = []
    for loc, short in SHORT_DESCS.items():
        if len(short) > 80:
            errors.append(f"{loc} short_description too long: {len(short)} > 80")
        meta_dir = ROOT / "metadata" / loc
        meta_dir.mkdir(parents=True, exist_ok=True)
        (meta_dir / "short_description.txt").write_text(short, encoding="utf-8")
        print(f"[OK] {loc} short_description: {len(short)} chars")

    for loc, full in FULL_DESCS.items():
        if len(full) > 4000:
            errors.append(f"{loc} full_description too long: {len(full)} > 4000")
        if "Limitra App Block" not in full[:200]:
            errors.append(f"{loc} full_description missing 'Limitra App Block' in first paragraph")
        if not full.strip().endswith("https://limitra.online (FAQ, pricing, changelog and comparisons with StayFree, AppBlock, YourHour, Digital Wellbeing and Forest)."):
            errors.append(f"{loc} full_description missing website ending")
        meta_dir = ROOT / "metadata" / loc
        meta_dir.mkdir(parents=True, exist_ok=True)
        (meta_dir / "full_description.txt").write_text(full.strip() + "\n", encoding="utf-8")
        print(f"[OK] {loc} full_description: {len(full)} chars")

    for loc, rel in RELEASE_NOTES.items():
        if len(rel) > 500:
            errors.append(f"{loc} release_notes too long: {len(rel)} > 500")
        store_dir = ROOT / "store_assets" / f"{loc}-v3"
        store_dir.mkdir(parents=True, exist_ok=True)
        (store_dir / "release_notes.txt").write_text(rel.strip() + "\n", encoding="utf-8")
        print(f"[OK] {loc} release_notes: {len(rel)} chars")

    if errors:
        print("\nERRORS:")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print("\nAll metadata and release notes written successfully!")

if __name__ == "__main__":
    main()
