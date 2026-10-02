# -*- coding: utf-8 -*-
"""Updates store_copy_v3.json for all 9 languages.
Removes _status field, preserves exact formatting and UTF-8 encoding.
"""
import json
from pathlib import Path

COPY_PATH = Path("tools/store_copy_v3.json")
data = json.loads(COPY_PATH.read_text(encoding="utf-8"))

# Translations for 9 locales
DE = {
    "lang": "de",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "„Nur noch fünf<br>Minuten.“<br><em>Heute nicht.</em>",
            "sub": "Setze ein Tageslimit. Ist die Zeit abgelaufen, sperrt Limitra die App."
        },
        "02": {
            "h1": "Einmal festlegen.<br><em>Wort halten.</em>",
            "sub": "Wähle Apps, ein Tageslimit und die relevanten Tage aus."
        },
        "03": {
            "h1": "Jeder saubere Tag<br><em>zählt.</em>",
            "sub": "Halte deine Serie und verdiene neun animierte Rahmen, von Tag 1 bis Tag 365."
        },
        "04": {
            "h1": "Restzeit auf<br><em>einen Blick.</em>",
            "sub": "Die verbleibende Zeit jeder geschützten App an einem Ort."
        },
        "05": {
            "h1": "Nur wenn es<br><em>drauf ankommt.</em>",
            "sub": "Apps nur während der Stunden und Tage einschränken, die du bestimmst."
        },
        "06": {
            "h1": "Dein Fortschritt,<br><em>dokumentiert.</em>",
            "sub": "Eine private Zeitachse jeder Sperre und jedes geschafften Tages."
        },
        "07": {
            "h1": "Nichts verlässt<br><em>dein Smartphone.</em>",
            "sub": "Limitra funktioniert völlig offline. Deine Nutzungsdaten bleiben auf diesem Gerät.",
            "rows": ["Konten", "Werbung", "Tracker", "Internetzugriff"],
            "none": "Keine",
            "pill": "Funktioniert komplett ohne Internet"
        },
        "08": {
            "h1": "Einmal zahlen.<br><em>Nicht jeden Monat.</em>",
            "sub": "Ein Kauf, alle Funktionen. Kein Abo, das gekündigt werden muss.",
            "seal_ring": "EINMALIGER KAUF · KEIN ABO · KEINE WERBUNG · KEINE ZUSATZKOSTEN ·",
            "seal_big": "1×",
            "seal_small": "ZAHLUNG"
        }
    },
    "feature": {
        "h1": "„Nur noch fünf<br>Minuten.“ <em>Heute nicht.</em>",
        "sub": "Tägliche App-Limits, die wirklich sperren.<br>Einmaliger Kauf. Kein Abo."
    }
}

ES = {
    "lang": "es",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "«Solo cinco<br>minutos más.»<br><em>Hoy no.</em>",
            "sub": "Define un límite diario. Cuando se acaba el tiempo, Limitra bloquea la app."
        },
        "02": {
            "h1": "Configúralo una vez.<br><em>Cumple tu palabra.</em>",
            "sub": "Elige las apps, un límite diario y los días que cuentan."
        },
        "03": {
            "h1": "Haz que cada día<br>limpio <em>cuente.</em>",
            "sub": "Mantén tu racha y gana nueve marcos animados, del día 1 al 365."
        },
        "04": {
            "h1": "Mira lo que queda<br><em>de un vistazo.</em>",
            "sub": "El tiempo restante de cada app protegida, en un solo lugar."
        },
        "05": {
            "h1": "Solo cuando<br><em>importa.</em>",
            "sub": "Limita las apps en las horas y días que elijas."
        },
        "06": {
            "h1": "Tu progreso,<br><em>registrado.</em>",
            "sub": "Una línea de tiempo privada de cada bloqueo y cada día completado."
        },
        "07": {
            "h1": "Nada sale de<br><em>tu teléfono.</em>",
            "sub": "Limitra funciona completamente sin conexión. Tus datos se quedan en este dispositivo.",
            "rows": ["Cuentas", "Publicidad", "Rastreadores", "Permiso de internet"],
            "none": "Ninguno",
            "pill": "Funciona totalmente sin internet"
        },
        "08": {
            "h1": "Paga una vez.<br><em>No cada mes.</em>",
            "sub": "Una sola compra, todas las funciones. Sin suscripción que cancelar.",
            "seal_ring": "PAGO ÚNICO · SIN SUSCRIPCIÓN · SIN ANUNCIOS · SIN COSTES EXTRA ·",
            "seal_big": "1×",
            "seal_small": "PAGO"
        }
    },
    "feature": {
        "h1": "«Solo cinco minutos<br>más.» <em>Hoy no.</em>",
        "sub": "Límites diarios de apps que bloquean de verdad.<br>Un solo pago. Sin suscripción."
    }
}

FR = {
    "lang": "fr",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "« Juste cinq<br>minutes de plus. »<br><em>Pas aujourd'hui.</em>",
            "sub": "Fixez une limite quotidienne. Le temps écoulé, Limitra verrouille l'application."
        },
        "02": {
            "h1": "Réglez-le une fois.<br><em>Tenez parole.</em>",
            "sub": "Choisissez les applications, une limite quotidienne et les jours actifs."
        },
        "03": {
            "h1": "Que chaque jour<br>propre <em>compte.</em>",
            "sub": "Gardez votre série pour débloquer neuf cadres animés, du 1er au 365e jour."
        },
        "04": {
            "h1": "Voyez le temps restant<br><em>d'un coup d'œil.</em>",
            "sub": "Le temps restant pour chaque application protégée, en un seul endroit."
        },
        "05": {
            "h1": "Seulement quand<br><em>c'est nécessaire.</em>",
            "sub": "Restreignez les applications aux heures et jours de votre choix."
        },
        "06": {
            "h1": "Vos progrès,<br><em>enregistrés.</em>",
            "sub": "Un journal privé de chaque verrouillage et de chaque journée réussie."
        },
        "07": {
            "h1": "Rien ne quitte<br><em>votre téléphone.</em>",
            "sub": "Limitra fonctionne entièrement hors ligne. Vos données restent sur cet appareil.",
            "rows": ["Comptes", "Publicités", "Traqueurs", "Accès à Internet"],
            "none": "Aucun",
            "pill": "Fonctionne sans aucun accès à Internet"
        },
        "08": {
            "h1": "Payez une fois.<br><em>Pas chaque mois.</em>",
            "sub": "Un seul achat, toutes les fonctionnalités. Aucun abonnement à résilier.",
            "seal_ring": "ACHAT UNIQUE · SANS ABONNEMENT · SANS PUB · SANS FRAIS CACHÉS ·",
            "seal_big": "1×",
            "seal_small": "PAIEMENT"
        }
    },
    "feature": {
        "h1": "« Cinq minutes de<br>plus. » <em>Pas aujourd'hui.</em>",
        "sub": "Des limites d'applications qui bloquent vraiment.<br>Paiement unique. Sans abonnement."
    }
}

PT = {
    "lang": "pt",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "“Só mais<br>cinco minutinhos.”<br><em>Hoje não.</em>",
            "sub": "Defina um limite diário. Quando o tempo acaba, o Limitra bloqueia o app."
        },
        "02": {
            "h1": "Configure uma vez.<br><em>Cumpra sua palavra.</em>",
            "sub": "Escolha os apps, um limite diário e os dias que importam."
        },
        "03": {
            "h1": "Faça cada dia<br>limpo <em>valer a pena.</em>",
            "sub": "Mantenha sua sequência e conquiste nove molduras animadas, do dia 1 ao 365."
        },
        "04": {
            "h1": "Veja o tempo restante<br><em>em um instante.</em>",
            "sub": "O tempo restante de cada app protegido, em um só lugar."
        },
        "05": {
            "h1": "Apenas quando<br><em>importa.</em>",
            "sub": "Limite os apps durante os horários e dias que você escolher."
        },
        "06": {
            "h1": "Seu progresso,<br><em>registrado.</em>",
            "sub": "Uma linha do tempo privada de cada bloqueio e dia concluído."
        },
        "07": {
            "h1": "Nada sai do<br><em>seu celular.</em>",
            "sub": "O Limitra funciona 100% offline. Seus dados de uso ficam neste aparelho.",
            "rows": ["Contas", "Anúncios", "Rastreadores", "Permissão de internet"],
            "none": "Nenhum",
            "pill": "Funciona totalmente sem internet"
        },
        "08": {
            "h1": "Pague uma vez.<br><em>Não todo mês.</em>",
            "sub": "Uma compra, todos os recursos. Sem assinatura para cancelar.",
            "seal_ring": "PAGAMENTO ÚNICO · SEM ASSINATURA · SEM ANÚNCIOS · SEM UPSELLS ·",
            "seal_big": "1×",
            "seal_small": "PAGAMENTO"
        }
    },
    "feature": {
        "h1": "“Só mais cinco<br>minutos.” <em>Hoje não.</em>",
        "sub": "Limites diários de apps que bloqueiam de verdade.<br>Compra única. Sem assinatura."
    }
}

ID = {
    "lang": "id",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "“Cuma lima<br>menit lagi.”<br><em>Tidak hari ini.</em>",
            "sub": "Pasang batas harian. Begitu waktu habis, Limitra mengunci aplikasi."
        },
        "02": {
            "h1": "Atur sekali.<br><em>Tepati janjimu.</em>",
            "sub": "Pilih aplikasi, batas harian, dan hari-hari yang berlaku."
        },
        "03": {
            "h1": "Jadikan setiap hari<br>bersih <em>bermakna.</em>",
            "sub": "Jaga rentetanmu dan raih sembilan bingkai animasi, dari hari 1 hingga 365."
        },
        "04": {
            "h1": "Pantau sisa waktu<br><em>dalam sekejap.</em>",
            "sub": "Sisa waktu setiap aplikasi terlindungi, terpusat di satu tempat."
        },
        "05": {
            "h1": "Hanya saat<br><em>dibutuhkan.</em>",
            "sub": "Batasi aplikasi pada jam dan hari yang Anda tentukan."
        },
        "06": {
            "h1": "Kemajuanmu,<br><em>tercatat rapi.</em>",
            "sub": "Garis waktu privat untuk setiap penguncian dan hari yang berhasil."
        },
        "07": {
            "h1": "Tidak ada data keluar<br><em>dari ponselmu.</em>",
            "sub": "Limitra bekerja sepenuhnya offline. Data penggunaanmu tetap di perangkat ini.",
            "rows": ["Akun", "Iklan", "Pelacak", "Izin internet"],
            "none": "Nol",
            "pill": "Bekerja sepenuhnya tanpa internet"
        },
        "08": {
            "h1": "Bayar sekali.<br><em>Bukan tiap bulan.</em>",
            "sub": "Sekali beli, semua fitur terbuka. Tidak ada langganan untuk dibatalkan.",
            "seal_ring": "BAYAR SEKALI · TANPA LANGGANAN · TANPA IKLAN · TANPA BIAYA LAIN ·",
            "seal_big": "1×",
            "seal_small": "BAYAR"
        }
    },
    "feature": {
        "h1": "“Cuma lima menit<br>lagi.” <em>Tidak hari ini.</em>",
        "sub": "Batas aplikasi harian yang benar-benar mengunci.<br>Sekali beli. Tanpa langganan."
    }
}

RU = {
    "lang": "ru",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "«Еще всего<br>пять минут.»<br><em>Не сегодня.</em>",
            "sub": "Задайте дневной лимит. Когда время выйдет, Limitra заблокирует приложение."
        },
        "02": {
            "h1": "Настройте раз.<br><em>Держите слово.</em>",
            "sub": "Выберите приложения, дневной лимит и нужные дни недели."
        },
        "03": {
            "h1": "Каждый чистый день<br><em>имеет значение.</em>",
            "sub": "Держите серию и открывайте девять анимированных рамок с 1-го по 365-й день."
        },
        "04": {
            "h1": "Сколько осталось —<br><em>как на ладони.</em>",
            "sub": "Оставшееся время по каждому защищенному приложению в одном месте."
        },
        "05": {
            "h1": "Только когда<br><em>это нужно.</em>",
            "sub": "Ограничивайте приложения именно в те часы и дни, которые выбрали сами."
        },
        "06": {
            "h1": "Ваш путь,<br><em>без лишних глаз.</em>",
            "sub": "Приватная хроника каждой блокировки и каждого успешного дня."
        },
        "07": {
            "h1": "Никаких утечек<br><em>с телефона.</em>",
            "sub": "Limitra работает полностью офлайн. Данные об использовании остаются на устройстве.",
            "rows": ["Аккаунты", "Реклама", "Трекеры", "Доступ в интернет"],
            "none": "Нет",
            "pill": "Работает вообще без интернета"
        },
        "08": {
            "h1": "Платите раз.<br><em>А не каждый месяц.</em>",
            "sub": "Одна покупка — все функции навсегда. Никаких подписок.",
            "seal_ring": "ОДНА ПОКУПКА · БЕЗ ПОДПИСКИ · БЕЗ РЕКЛАМЫ · БЕЗ ДОПЛАТ ·",
            "seal_big": "1×",
            "seal_small": "ОПЛАТА"
        }
    },
    "feature": {
        "h1": "«Еще пять минут.»<br><em>Не сегодня.</em>",
        "sub": "Дневные лимиты, которые реально блокируют.<br>Разовая покупка. Без подписки."
    }
}

HI = {
    "lang": "hi",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "“बस पांच<br>मिनट और।”<br><em>आज नहीं।</em>",
            "sub": "दैनिक सीमा तय करें। समय समाप्त होने पर Limitra ऐप को लॉक कर देता है।"
        },
        "02": {
            "h1": "एक बार सेट करें।<br><em>अपना वादा निभाएं।</em>",
            "sub": "ऐप्स, दैनिक समय सीमा और सुरक्षा के दिन चुनें।"
        },
        "03": {
            "h1": "हर सफल दिन को<br><em>खास बनाएं।</em>",
            "sub": "अपनी लड़ी बनाए रखें और दिन 1 से 365 तक नौ एनिमेटेड फ्रेम अर्जित करें।"
        },
        "04": {
            "h1": "बचा हुआ समय<br><em>एक नज़र में देखें।</em>",
            "sub": "हर सुरक्षित ऐप का शेष समय, एक ही स्थान पर।"
        },
        "05": {
            "h1": "केवल जब<br><em>ज़रूरी हो।</em>",
            "sub": "केवल अपने चुने हुए घंटों और दिनों में ही ऐप्स को प्रतिबंधित करें।"
        },
        "06": {
            "h1": "आपकी प्रगति,<br><em>सुरक्षित दर्ज।</em>",
            "sub": "हर लॉक और पूरे हुए दिन की एक निजी समयरेखा।"
        },
        "07": {
            "h1": "कुछ भी फोन से<br><em>बाहर नहीं जाता।</em>",
            "sub": "Limitra पूरी तरह ऑफलाइन काम करता है। आपका उपयोग डेटा इसी डिवाइस पर रहता है।",
            "rows": ["खाता", "विज्ञापन", "ट्रैकर", "इंटरनेट अनुमति"],
            "none": "कोई नहीं",
            "pill": "बिना इंटरनेट के पूरी तरह काम करता है"
        },
        "08": {
            "h1": "एक बार भुगतान करें।<br><em>हर महीने नहीं।</em>",
            "sub": "एक बार की खरीद, हर सुविधा आपकी। रद्द करने का कोई झंझट नहीं।",
            "seal_ring": "एकमुश्त खरीद · कोई सदस्यता नहीं · कोई विज्ञापन नहीं · कोई अतिरिक्त शुल्क नहीं ·",
            "seal_big": "1×",
            "seal_small": "भुगतान"
        }
    },
    "feature": {
        "h1": "“बस पांच मिनट और।”<br><em>आज नहीं।</em>",
        "sub": "दैनिक ऐप सीमाएं जो वास्तव में लॉक करती हैं।<br>एकमुश्त खरीद। कोई सदस्यता नहीं।"
    }
}

TH = {
    "lang": "th",
    "rtl": False,
    "cards": {
        "01": {
            "h1": "“ขออีกแค่<br>ห้านาที”<br><em>ไม่ใช่วันนี้</em>",
            "sub": "กำหนดขีดจำกัดรายวัน เมื่อหมดเวลา Limitra จะล็อกแอปทันที"
        },
        "02": {
            "h1": "ตั้งค่าครั้งเดียว<br><em>รักษาสัญญา</em>",
            "sub": "เลือกแอป ขีดจำกัดเวลา และวันที่ต้องการควบคุม"
        },
        "03": {
            "h1": "ให้ทุกวันที่สำเร็จ<br><em>มีความหมาย</em>",
            "sub": "รักษาสถิติต่อเนื่องเพื่อรับกรอบอนิเมชัน 9 แบบ ตั้งแต่วันที่ 1 ถึง 365"
        },
        "04": {
            "h1": "ดูเวลาที่เหลือ<br><em>ได้ในพริบตา</em>",
            "sub": "เวลาคงเหลือของทุกแอปที่ได้รับการปกป้อง รวมไว้ในที่เดียว"
        },
        "05": {
            "h1": "เฉพาะช่วงเวลาที่<br><em>สำคัญจริงๆ</em>",
            "sub": "จำกัดแอปเฉพาะชั่วโมงและวันที่คุณเลือกกำหนดเอง"
        },
        "06": {
            "h1": "บันทึกความก้าวหน้า<br><em>อย่างเป็นส่วนตัว</em>",
            "sub": "ไทม์ไลน์ส่วนตัวที่บันทึกทุกการล็อกและทุกวันที่ทำสำเร็จ"
        },
        "07": {
            "h1": "ไม่มีข้อมูลใด<br><em>หลุดออกจากเครื่อง</em>",
            "sub": "Limitra ทำงานแบบออฟไลน์เต็มรูปแบบ ข้อมูลการใช้งานอยู่บนอุปกรณ์นี้เท่านั้น",
            "rows": ["บัญชีผู้ใช้", "โฆษณา", "ตัวติดตาม", "การอนุญาตอินเทอร์เน็ต"],
            "none": "ไม่มี",
            "pill": "ใช้งานได้โดยไม่ต้องใช้อินเทอร์เน็ตเลย"
        },
        "08": {
            "h1": "จ่ายครั้งเดียว<br><em>ไม่ใช่รายเดือน</em>",
            "sub": "ซื้อครั้งเดียวได้ทุกฟีเจอร์ ไม่มีค่าบริการรายเดือนให้ต้องกดยกเลิก",
            "seal_ring": "ซื้อครั้งเดียว · ไม่ต้องสมัครสมาชิก · ไม่มีโฆษณา · ไม่มีการขายเพิ่ม ·",
            "seal_big": "1×",
            "seal_small": "ชำระเงิน"
        }
    },
    "feature": {
        "h1": "“ขออีกแค่ห้านาที”<br><em>ไม่ใช่วันนี้</em>",
        "sub": "ขีดจำกัดแอปรายวันที่ล็อกได้จริง<br>จ่ายครั้งเดียว ไม่มีค่าบริการรายเดือน"
    }
}

AR = {
    "lang": "ar",
    "rtl": True,
    "cards": {
        "01": {
            "h1": "«خمس دقائق<br>أخرى فقط.»<br><em>ليس اليوم.</em>",
            "sub": "ضع حداً يومياً. وعند نفاد الوقت، يقفل Limitra التطبيق فوراً."
        },
        "02": {
            "h1": "اضبطه مرة واحدة.<br><em>وأوفِ بوعدك.</em>",
            "sub": "اختر التطبيقات، والحد اليومي، والأيام المحددة."
        },
        "03": {
            "h1": "اجعل كل يوم<br>نظيف <em>يُحسب لك.</em>",
            "sub": "حافظ على استمراريتك واكسب تسعة إطارات متحركة من اليوم 1 إلى 365."
        },
        "04": {
            "h1": "اعرف الوقت المتبقي<br><em>بنظرة واحدة.</em>",
            "sub": "الوقت المتبقي لكل تطبيق محمي، في مكان واحد."
        },
        "05": {
            "h1": "فقط عندما<br><em>يكون مهماً.</em>",
            "sub": "قيّد التطبيقات خلال الساعات والأيام التي تختارها أنت."
        },
        "06": {
            "h1": "تقدّمك،<br><em>موثّق بالكامل.</em>",
            "sub": "سجل زمني خاص لكل قفل وكل يوم نجحت في إكماله."
        },
        "07": {
            "h1": "لا شيء يغادر<br><em>هاتفك أبداً.</em>",
            "sub": "يعمل Limitra دون اتصال بالإنترنت تماماً. تظل بياناتك على جهازك فقط.",
            "rows": ["حسابات", "إعلانات", "أدوات تتبع", "إذن الإنترنت"],
            "none": "لا يوجد",
            "pill": "يعمل بالكامل دون أي اتصال بالإنترنت"
        },
        "08": {
            "h1": "ادفع مرة واحدة.<br><em>وليس كل شهر.</em>",
            "sub": "عملية شراء واحدة لجميع المزايا. لا يوجد اشتراك تحتاج لإلغائه.",
            "seal_ring": "شراء لمرة واحدة · بلا اشتراك · بلا إعلانات · بلا رسوم إضافية ·",
            "seal_big": "1×",
            "seal_small": "دفع"
        }
    },
    "feature": {
        "h1": "«خمس دقائق أخرى.»<br><em>ليس اليوم.</em>",
        "sub": "حدود يومية للتطبيقات تقفل بالفعل.<br>دفع لمرة واحدة. بلا اشتراك."
    }
}

data["de-DE"] = DE
data["es-ES"] = ES
data["fr-FR"] = FR
data["pt-BR"] = PT
data["id"] = ID
data["ru-RU"] = RU
data["hi-IN"] = HI
data["th"] = TH
data["ar"] = AR

COPY_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
print("store_copy_v3.json updated for all 9 locales!")
