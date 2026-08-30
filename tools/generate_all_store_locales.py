import os
import subprocess
import json
import base64
from pathlib import Path
from PIL import Image

PROJECT_ROOT = Path(r"c:\Users\abdul\gardiyan2")
EDGE_PATH = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

ICON_PATH = PROJECT_ROOT / "store_assets" / "icon" / "play-icon-512.png"
FEATURE_ICON_PATH = PROJECT_ROOT / "store_assets" / "icon" / "limitra-mark-transparent-v2.png"

# Read icons as base64 for reliable HTML embedding
with open(ICON_PATH, "rb") as f:
    ICON_B64 = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")

with open(FEATURE_ICON_PATH, "rb") as f:
    FEATURE_ICON_B64 = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")

CARDS_CONFIG = [
    {
        "id": "1",
        "file": "01-block-distractions.png",
        "source": PROJECT_ROOT / "test_render" / "limitra-protected3.png",
    },
    {
        "id": "2",
        "file": "02-set-limits.png",
        "source": PROJECT_ROOT / "test_render" / "limitra-add.png",
    },
    {
        "id": "3",
        "file": "03-build-a-streak.png",
        "source": PROJECT_ROOT / "test_render" / "limitra-progress.png",
    },
    {
        "id": "4",
        "file": "04-private-history.png",
        "source": PROJECT_ROOT / "test_render" / "limitra-timeline.png",
    },
    {
        "id": "5",
        "file": "05-offline-private.png",
        "source": PROJECT_ROOT / "test_render" / "limitra-permissions-1080.png",
    }
]

# Read source phone screenshots as base64
SOURCE_B64 = {}
for card in CARDS_CONFIG:
    with open(card["source"], "rb") as f:
        SOURCE_B64[card["id"]] = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")

LOCALES = {
    "tr-TR": {
        "name": "Turkish",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "KESİNTİSİZ UYGULAMA ENGELLEME",
                "headline": "DİKKAT DAĞITANLARI ENGELLE.\nZAMANIN SANA KALSIN.",
                "subtitle": "Sonsuz kaydırmayı durduran günlük limitler.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "ESNEK GÜNLÜK LİMİTLER",
                "headline": "LİMİTLERİ KENDİNE\nGÖRE AYARLA.",
                "subtitle": "Uygulamaları, süre sınırlarını ve koruma günlerini seç.",
                "headline_size": 74,
                "subtitle_size": 30,
            },
            {
                "id": "3",
                "eyebrow": "MOTİVE EDEN İLERLEME",
                "headline": "ZİNCİRİ KUR.\nSEVİYE ATLA.",
                "subtitle": "Ekran alışkanlıklarını görünür bir ilerlemeye dönüştür.",
                "headline_size": 76,
                "subtitle_size": 31,
            },
            {
                "id": "4",
                "eyebrow": "GİZLİ VE YEREL GEÇMİŞ",
                "headline": "HER HAREKET\nŞEFFAFÇA KAYITTA.",
                "subtitle": "Hesap açmadan koruma geçmişini incele.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "TEK ÖDEME - ABONELİK YOK",
                "headline": "VERİLERİN SENİNLE\nGÜVENDE KALSIN.",
                "subtitle": "Reklamsız, takipçisiz ve internet izinsiz.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Dikkat dağıtanları engelle.\nZamanın sana kalsın.",
            "meta": "TEK ÖDEME  |  ABONELİK YOK  |  %100 ÇEVRİMDIŞI",
            "subtitle": "Cihazında işlenen gizli ve güvenli uygulama engelleme.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "de-DE": {
        "name": "German",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "STRIKTE APP-BLOCKIERUNG",
                "headline": "ABLENKUNGEN STOPPEN.\nZEIT GEWINNEN.",
                "subtitle": "Tägliche Limits gegen endloses Scrollen.",
                "headline_size": 66,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "FLEXIBLE TAGESLIMITS",
                "headline": "LIMITS NACH DEINEN\nWÜNSCHEN SETZEN.",
                "subtitle": "Wähle Apps, Zeitlimits und Schutztage.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "FORTSCHRITT DER MOTIVIERT",
                "headline": "SERIE AUFBAUEN.\nLEVEL AUFSTEIGEN.",
                "subtitle": "Mache bessere Bildschirmgewohnheiten sichtbar.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "4",
                "eyebrow": "LOKALER PRIVATER VERLAUF",
                "headline": "JEDE AKTION\nKLAR IM BLICK.",
                "subtitle": "Schutzverlauf ganz ohne Konto einsehen.",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "EINMALZAHLUNG - KEIN ABO",
                "headline": "DEINE DATEN\nBLEIBEN BEI DIR.",
                "subtitle": "Keine Werbung, kein Tracking, keine Internetberechtigung.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Ablenkungen stoppen.\nZeit gewinnen.",
            "meta": "EINMALZAHLUNG  |  KEIN ABO  |  100% OFFLINE",
            "subtitle": "Private App-Blockierung, lokal auf deinem Gerät.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "es-ES": {
        "name": "Spanish",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "BLOQUEO ESTRICTO DE APPS",
                "headline": "BLOQUEA DISTRACCIONES.\nRECUPERA TU TIEMPO.",
                "subtitle": "Límites diarios para frenar el scroll infinito.",
                "headline_size": 64,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "LÍMITES DIARIOS FLEXIBLES",
                "headline": "DEFINE LÍMITES\nA TU MANERA.",
                "subtitle": "Elige aplicaciones, límites de tiempo y días.",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "PROGRESO QUE MOTIVA",
                "headline": "CREA UNA RACHA.\nSUBE DE NIVEL.",
                "subtitle": "Convierte mejores hábitos en progreso visible.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "4",
                "eyebrow": "HISTORIAL PRIVADO EN DISPOSITIVO",
                "headline": "CADA ACCIÓN,\nBIEN REGISTRADA.",
                "subtitle": "Revisa tu historial de protection sin crear cuenta.",
                "headline_size": 70,
                "subtitle_size": 30,
            },
            {
                "id": "5",
                "eyebrow": "PAGO ÚNICO - SIN SUSCRIPCIONES",
                "headline": "TUS DATOS SE\nQUEDAN CONTIGO.",
                "subtitle": "Sin anuncios, sin rastreadores y sin permiso de Internet.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Bloquea distracciones.\nRecupera tu tiempo.",
            "meta": "PAGO ÚNICO  |  SIN SUSCRIPCIÓN  |  100% OFFLINE",
            "subtitle": "Bloqueo privado de apps procesado en tu dispositivo.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "fr-FR": {
        "name": "French",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "BLOCAGE STRICT D'APPLICATIONS",
                "headline": "BLOQUEZ LES DISTRACTIONS.\nGARDEZ VOTRE TEMPS.",
                "subtitle": "Des limites quotidiennes contre le défilement infini.",
                "headline_size": 60,
                "subtitle_size": 30,
            },
            {
                "id": "2",
                "eyebrow": "LIMITES QUOTIDIENNES FLEXIBLES",
                "headline": "DÉFINISSEZ VOS LIMITES\nÀ VOTRE FAÇON.",
                "subtitle": "Choisissez les apps, les durées et les jours.",
                "headline_size": 66,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "UN PROGRÈS MOTIVANT",
                "headline": "BÂTISSEZ UNE SÉRIE.\nPASSEZ AU NIVEAU SUPÉRIEUR.",
                "subtitle": "Transformez vos habitudes d'écran en progrès visible.",
                "headline_size": 58,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "HISTORIQUE PRIVÉ SUR L'APPAREIL",
                "headline": "CHAQUE ACTION\nCLAIREMENT SUIVIE.",
                "subtitle": "Consultez votre historique sans aucun compte.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "PAIEMENT UNIQUE - SANS ABONNEMENT",
                "headline": "VOS DONNÉES\nVOUS APPARTIENNENT.",
                "subtitle": "Sans pub, sans traceur et sans autorisation Internet.",
                "headline_size": 64,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Bloquez les distractions.\nGardez votre temps.",
            "meta": "PAIEMENT UNIQUE  |  SANS ABONNEMENT  |  100% HORS LIGNE",
            "subtitle": "Blocage d'applications privé, traité sur votre appareil.",
            "headline_size": 34,
            "meta_size": 17,
            "subtitle_size": 18,
        }
    },
    "id": {
        "name": "Indonesian",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "PEMBLOKIRAN APLIKASI KETAT",
                "headline": "BLOKIR DISTRAKSI.\nJAGA WAKTU ANDA.",
                "subtitle": "Batas harian untuk menghentikan scrolling tanpa henti.",
                "headline_size": 70,
                "subtitle_size": 30,
            },
            {
                "id": "2",
                "eyebrow": "BATAS HARIAN FLEKSIBEL",
                "headline": "ATUR BATAS SESUAI\nKEINGINAN ANDA.",
                "subtitle": "Pilih aplikasi, batas waktu, dan hari perlindungan.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "3",
                "eyebrow": "KEMAJUAN YANG MEMOTIVASI",
                "headline": "BANGUN REKOR.\nTINGKATKAN LEVEL.",
                "subtitle": "Ubah kebiasaan layar menjadi kemajuan nyata.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "4",
                "eyebrow": "RIWAYAT PRIBADI DI PERANGKAT",
                "headline": "SETIAP TINDAKAN\nTERCATAT JELAS.",
                "subtitle": "Tinjau riwayat perlindungan tanpa perlu akun.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "SEKALI BAYAR - TANPA LANGGANAN",
                "headline": "DATA ANDA TETAP\nAMAN DI PERANGKAT.",
                "subtitle": "Tanpa iklan, tanpa pelacak, tanpa izin Internet.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Blokir distraksi.\nJaga waktu Anda.",
            "meta": "SEKALI BAYAR  |  TANPA LANGGANAN  |  100% OFFLINE",
            "subtitle": "Pemblokiran aplikasi privat yang diproses di perangkat Anda.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "pt-BR": {
        "name": "Portuguese",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "BLOQUEIO ESTRITO DE APPS",
                "headline": "BLOQUEIE DISTRAÇÕES.\nRECUPERE SEU TEMPO.",
                "subtitle": "Limites diários para parar a rolagem infinita.",
                "headline_size": 64,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "LIMITES DIÁRIOS FLEXÍVEIS",
                "headline": "DEFINA LIMITES DO\nSEU JEITO.",
                "subtitle": "Escolha aplicativos, limites de tempo e dias.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "PROGRESSO QUE MOTIVA",
                "headline": "CRIE UMA SEQUÊNCIA.\nSUBA DE NÍVEL.",
                "subtitle": "Transforme seus hábitos de tela em progresso visível.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "HISTÓRICO PRIVADO NO DISPOSITIVO",
                "headline": "CADA AÇÃO\nBEM REGISTRADA.",
                "subtitle": "Revise seu histórico de proteção sem criar conta.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "5",
                "eyebrow": "PAGAMENTO ÚNICO - SEM ASSINATURAS",
                "headline": "SEUS DADOS\nFICAM COM VOCÊ.",
                "subtitle": "Sem anúncios, sem rastreadores e sem permissão de Internet.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Bloqueie distrações.\nRecupere seu tempo.",
            "meta": "PAGAMENTO ÚNICO  |  SEM ASSINATURA  |  100% OFFLINE",
            "subtitle": "Bloqueio privado de apps processado no seu dispositivo.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "ru-RU": {
        "name": "Russian",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "СТРОГАЯ БЛОКИРОВКА ПРИЛОЖЕНИЙ",
                "headline": "БЛОКИРУЙТЕ ОТВЛЕЧЕНИЯ.\nБЕРЕГИТЕ ВРЕМЯ.",
                "subtitle": "Дневные лимиты против бесконечной ленты.",
                "headline_size": 60,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "ГИБКИЕ ДНЕВНЫЕ ЛИМИТЫ",
                "headline": "НАСТРАИВАЙТЕ ЛИМИТЫ\nПОД СЕБЯ.",
                "subtitle": "Выбирайте приложения, время и дни защиты.",
                "headline_size": 64,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "ПРОГРЕСС, КОТОРЫЙ МОТИВИРУЕТ",
                "headline": "ДЕРЖИТЕ СЕРИЮ.\nПОВЫШАЙТЕ УРОВЕНЬ.",
                "subtitle": "Превратите контроль экрана в наглядный результат.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "ПРИВАТНАЯ ИСТОРИЯ НА УСТРОЙСТВЕ",
                "headline": "КАЖДОЕ ДЕЙСТВИЕ\nПОД КОНТРОЛЕМ.",
                "subtitle": "Просматривайте историю без создания аккаунта.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "РАЗОВАЯ ПОКУПКА - БЕЗ ПОДПИСОК",
                "headline": "ВАШИ ДАННЫЕ\nОСТАЮТСЯ У ВАС.",
                "subtitle": "Без рекламы, без трекеров и без доступа в Интернет.",
                "headline_size": 70,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Блокируйте отвлечения.\nБерегите время.",
            "meta": "РАЗОВАЯ ПОКУПКА  |  БЕЗ ПОДПИСКИ  |  100% ОФЛАЙН",
            "subtitle": "Приватная блокировка приложений на вашем устройстве.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "hi-IN": {
        "name": "Hindi",
        "is_rtl": False,
        "font_family": "'Nirmala UI', 'Segoe UI', sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "सख्त ऐप ब्लॉकिंग",
                "headline": "भटकाव को रोकें।\nसमय बचाएं।",
                "subtitle": "अनंत स्क्रॉलिंग रोकने के लिए दैनिक सीमाएं।",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "लचीली दैनिक सीमाएं",
                "headline": "अपनी पसंद से\nसीमाएं तय करें।",
                "subtitle": "ऐप्स, समय सीमा और सुरक्षा के दिन चुनें।",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "प्रेरित करने वाली प्रगति",
                "headline": "लगातार स्ट्रीक बनाएं।\nलेवल बढ़ाएं।",
                "subtitle": "स्क्रीन की बेहतर आदतों को दिखाई देने वाली प्रगति में बदलें।",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "डिवाइस पर निजी इतिहास",
                "headline": "हर गतिविधि का\nस्पष्ट रिकॉर्ड।",
                "subtitle": "बिना किसी अकाउंट के अपना सुरक्षा इतिहास देखें।",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "एक बार भुगतान - कोई सब्सक्रिप्शन नहीं",
                "headline": "आपका डेटा\nआपके पास सुरक्षित।",
                "subtitle": "कोई विज्ञापन नहीं, कोई ट्रैकर नहीं और कोई इंटरनेट अनुमति नहीं।",
                "headline_size": 68,
                "subtitle_size": 29,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "भटकाव को रोकें।\nसमय बचाएं।",
            "meta": "एक बार भुगतान  |  कोई सब्सक्रिप्शन नहीं  |  100% ऑफलाइन",
            "subtitle": "निजी ऐप ब्लॉकिंग, पूरी तरह आपके डिवाइस पर संसाधित।",
            "headline_size": 34,
            "meta_size": 17,
            "subtitle_size": 17,
        }
    },
    "th": {
        "name": "Thai",
        "is_rtl": False,
        "font_family": "'Leelawadee UI', 'Segoe UI', Tahoma, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "การบล็อกแอปที่เข้มงวด",
                "headline": "บล็อกสิ่งรบกวน\nรักษาเวลาของคุณ",
                "subtitle": "กำหนดขีดจำกัดรายวันเพื่อหยุดการไถหน้าจอไม่รู้จบ",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "ขีดจำกัดรายวันที่ยืดหยุ่น",
                "headline": "ตั้งขีดจำกัด\nในแบบของคุณ",
                "subtitle": "เลือกแอป ขีดจำกัดเวลา และวันที่ต้องการป้องกัน",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "ความคืบหน้าที่สร้างแรงบันดาลใจ",
                "headline": "สร้างสถิติต่อเนื่อง\nเลเวลอัป",
                "subtitle": "เปลี่ยนพฤติกรรมการใช้หน้าจอเป็นความสำเร็จที่จับต้องได้",
                "headline_size": 70,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "ประวัติส่วนตัวบนอุปกรณ์",
                "headline": "ทุกกิจกรรม\nบันทึกชัดเจน",
                "subtitle": "ตรวจสอบประวัติการป้องกันได้โดยไม่ต้องสร้างบัญชี",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "จ่ายครั้งเดียว - ไม่มีระบบสมาชิก",
                "headline": "ข้อมูลของคุณ\nอยู่กับคุณเสมอ",
                "subtitle": "ไม่มีโฆษณา ไม่มีการติดตาม และไม่ขอสิทธิ์อินเทอร์เน็ต",
                "headline_size": 72,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "บล็อกสิ่งรบกวน\nรักษาเวลาของคุณ",
            "meta": "จ่ายครั้งเดียว  |  ไม่มีระบบสมาชิก  |  ออฟไลน์ 100%",
            "subtitle": "การบล็อกแอปแบบส่วนตัว ประมวลผลบนอุปกรณ์ของคุณ",
            "headline_size": 34,
            "meta_size": 17,
            "subtitle_size": 17,
        }
    },
    "ar": {
        "name": "Arabic",
        "is_rtl": True,
        "font_family": "'Segoe UI', Tahoma, 'Arial', sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "حظر تطبيقات صارم",
                "headline": "احظر المشتتات.\nحافظ على وقتك.",
                "subtitle": "حدود يومية لإيقاف التمرير اللانهائي.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "حدود يومية مرنة",
                "headline": "اضبط الحدود\nبطريقتك الخاصة.",
                "subtitle": "اختر التطبيقات، وحدود الوقت، وأيام الحماية.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "تقدم يحفزك للوصول",
                "headline": "ابنِ سلسلة نجاح.\nوارتقِ بمستواك.",
                "subtitle": "حوّل عادات الشاشة إلى تقدم حقيقي وملموس.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "سجل نشاط خاص على جهازك",
                "headline": "كل إجراء\nمسجل بوضوح.",
                "subtitle": "راجع سجل الحماية بدون الحاجة إلى إنشاء حساب.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "5",
                "eyebrow": "دفع لمرة واحدة - بدون اشتراكات",
                "headline": "بياناتك تظل\nفي أمان معك.",
                "subtitle": "بدون إعلانات، بدون متتبعات، وبدون إذن إنترنت.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "احظر المشتتات.\nحافظ على وقتك.",
            "meta": "دفع لمرة واحدة  |  بدون اشتراكات  |  %100 بدون إنترنت",
            "subtitle": "حظر تطبيقات خاص وآمن يُعالج بالكامل على جهازك.",
            "headline_size": 34,
            "meta_size": 17,
            "subtitle_size": 17,
        }
    }
}

def generate_html_screenshot(card, locale_key, locale_cfg):
    is_rtl = locale_cfg.get("is_rtl", False)
    font_fam = locale_cfg.get("font_family", "'Segoe UI', sans-serif")
    source_b64 = SOURCE_B64[card["id"]]
    
    if is_rtl:
        header_style = "position: absolute; top: 48px; left: 58px; right: 58px; display: flex; flex-direction: row-reverse; align-items: center; justify-content: flex-start; gap: 18px;"
        text_container_style = "position: absolute; top: 155px; left: 58px; right: 58px; direction: rtl; text-align: right;"
    else:
        header_style = "position: absolute; top: 48px; left: 58px; display: flex; align-items: center; gap: 18px;"
        text_container_style = "position: absolute; top: 155px; left: 58px; right: 58px; text-align: left;"

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    width: 1080px;
    height: 1920px;
    background: linear-gradient(180deg, #07142E 0%, #0C2A50 100%);
    overflow: hidden;
    position: relative;
    font-family: {font_fam};
    -webkit-font-smoothing: antialiased;
  }}
  .glow1 {{
    position: absolute;
    width: 720px; height: 720px;
    left: -220px; top: -180px;
    border-radius: 50%;
    background: #1EE6C5;
    opacity: 0.094;
    filter: blur(40px);
  }}
  .glow2 {{
    position: absolute;
    width: 620px; height: 620px;
    left: 720px; top: 1250px;
    border-radius: 50%;
    background: #1EE6C5;
    opacity: 0.094;
    filter: blur(40px);
  }}
  .header {{
    {header_style}
  }}
  .icon {{
    width: 82px;
    height: 82px;
    display: block;
  }}
  .brand {{
    font-size: 26px;
    font-weight: 700;
    color: #FFFFFF;
    letter-spacing: 2px;
  }}
  .text-container {{
    {text_container_style}
  }}
  .eyebrow {{
    font-size: 25px;
    font-weight: 700;
    color: #22E6C5;
    letter-spacing: 1.2px;
    margin-bottom: 12px;
    text-transform: uppercase;
  }}
  .headline {{
    font-size: {card.get('headline_size', 72)}px;
    font-weight: 800;
    color: #FFFFFF;
    line-height: 1.15;
    margin-bottom: 16px;
    white-space: pre-line;
    letter-spacing: -0.5px;
  }}
  .subtitle {{
    font-size: {card.get('subtitle_size', 31)}px;
    font-weight: 400;
    color: #B7C7DC;
    line-height: 1.35;
    white-space: pre-line;
  }}
  .phone-frame {{
    position: absolute;
    left: 144px;
    top: 510px;
    width: 792px;
    height: 1368px;
    border-radius: 62px;
    border: 8px solid rgba(255, 255, 255, 0.51);
    box-shadow: 14px 18px 40px rgba(0, 0, 0, 0.4);
    overflow: hidden;
    background: #000;
  }}
  .phone-frame img {{
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }}
</style>
</head>
<body>
  <div class="glow1"></div>
  <div class="glow2"></div>
  <div class="header">
    <img class="icon" src="{ICON_B64}">
    <div class="brand">LIMITRA</div>
  </div>
  <div class="text-container">
    <div class="eyebrow">{card['eyebrow']}</div>
    <div class="headline">{card['headline']}</div>
    <div class="subtitle">{card['subtitle']}</div>
  </div>
  <div class="phone-frame">
    <img src="{source_b64}">
  </div>
</body>
</html>"""
    return html

def generate_html_feature(locale_key, locale_cfg):
    is_rtl = locale_cfg.get("is_rtl", False)
    font_fam = locale_cfg.get("font_family", "'Segoe UI', sans-serif")
    feat = locale_cfg["feature"]
    
    if is_rtl:
        # RTL layout: Icon on right, text on left right-aligned
        icon_style = "position: absolute; width: 360px; height: 360px; right: 54px; top: 70px;"
        text_style = "position: absolute; top: 90px; left: 54px; right: 440px; direction: rtl; text-align: right; display: flex; flex-direction: column; justify-content: center;"
    else:
        icon_style = "position: absolute; width: 360px; height: 360px; left: 54px; top: 70px;"
        text_style = "position: absolute; top: 90px; left: 430px; right: 54px; text-align: left; display: flex; flex-direction: column; justify-content: center;"

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    width: 1024px;
    height: 500px;
    background: linear-gradient(90deg, #06142F 0%, #0B3354 100%);
    overflow: hidden;
    position: relative;
    font-family: {font_fam};
    -webkit-font-smoothing: antialiased;
  }}
  .icon {{
    {icon_style}
  }}
  .text-container {{
    {text_style}
  }}
  .brand {{
    font-size: 66px;
    font-weight: 800;
    color: #FFFFFF;
    letter-spacing: 2px;
    margin-bottom: 8px;
  }}
  .headline {{
    font-size: {feat.get('headline_size', 34)}px;
    font-weight: 700;
    color: #FFFFFF;
    line-height: 1.25;
    margin-bottom: 22px;
    white-space: pre-line;
  }}
  .meta {{
    font-size: {feat.get('meta_size', 18)}px;
    font-weight: 700;
    color: #22E6C5;
    letter-spacing: 1px;
    margin-bottom: 8px;
    text-transform: uppercase;
  }}
  .subtitle {{
    font-size: {feat.get('subtitle_size', 18)}px;
    font-weight: 700;
    color: #B7C7DC;
    line-height: 1.35;
  }}
</style>
</head>
<body>
  <img class="icon" src="{FEATURE_ICON_B64}">
  <div class="text-container">
    <div class="brand">{feat['brand']}</div>
    <div class="headline">{feat['headline']}</div>
    <div class="meta">{feat['meta']}</div>
    <div class="subtitle">{feat['subtitle']}</div>
  </div>
</body>
</html>"""
    return html

def render_html_to_png(html_str, output_png_path, width, height, temp_id="temp"):
    temp_html = PROJECT_ROOT / "scratch" / f"render_{temp_id}.html"
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(html_str)
    
    cmd = [
        EDGE_PATH,
        "--headless=new",
        "--disable-gpu",
        "--hide-scrollbars",
        "--force-device-scale-factor=1",
        f"--window-size={width},{height}",
        f"--screenshot={str(output_png_path)}",
        f"file:///{temp_html.as_posix()}"
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    try:
        if temp_html.exists():
            temp_html.unlink()
    except Exception:
        pass
    if res.returncode != 0 or not output_png_path.exists():
        raise RuntimeError(f"Edge render failed: {res.stderr}")

def main():
    print("Starting generation of 10 localized Play Store asset sets...")
    
    total_locales = len(LOCALES)
    generated_counts = 0
    
    for loc_key, loc_cfg in LOCALES.items():
        print(f"\n--- Generating for {loc_key} ({loc_cfg['name']}) ---")
        
        # 1. Output directory in store_assets/<locale>-v2/
        store_assets_dir = PROJECT_ROOT / "store_assets" / f"{loc_key}-v2"
        store_assets_dir.mkdir(parents=True, exist_ok=True)
        
        # 2. Output directory in store_assets/play-sync-v2/<locale>/
        play_sync_phone = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "phoneScreenshots"
        play_sync_feature = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "featureGraphic"
        play_sync_phone.mkdir(parents=True, exist_ok=True)
        play_sync_feature.mkdir(parents=True, exist_ok=True)
        
        # Render 5 Screenshots
        for idx, card in enumerate(loc_cfg["cards"], 1):
            file_name = CARDS_CONFIG[idx - 1]["file"]
            dest1 = store_assets_dir / file_name
            dest2 = play_sync_phone / f"{idx}.png"
            
            html = generate_html_screenshot(card, loc_key, loc_cfg)
            render_html_to_png(html, dest1, 1080, 1920, f"{loc_key}_{idx}")
            
            # Verify and copy to play_sync
            img = Image.open(dest1)
            assert img.size == (1080, 1920), f"Invalid size {img.size} for {dest1}"
            img.save(dest2, "PNG")
            print(f"  [OK] Screenshot {idx}: {file_name} -> {dest1.stat().st_size} bytes")
            generated_counts += 1

        # Render Feature Graphic
        feat_dest1 = store_assets_dir / "feature-graphic-1024x500.png"
        feat_dest2 = play_sync_feature / "feature.png"
        feat_html = generate_html_feature(loc_key, loc_cfg)
        render_html_to_png(feat_html, feat_dest1, 1024, 500, f"{loc_key}_feat")
        
        feat_img = Image.open(feat_dest1)
        assert feat_img.size == (1024, 500), f"Invalid size {feat_img.size} for {feat_dest1}"
        feat_img.save(feat_dest2, "PNG")
        print(f"  [OK] Feature Graphic -> {feat_dest1.stat().st_size} bytes")
        generated_counts += 1

    print(f"\nSuccessfully generated all {generated_counts} assets across {total_locales} locales!")

if __name__ == "__main__":
    main()
