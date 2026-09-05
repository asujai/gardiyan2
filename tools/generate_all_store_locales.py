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

# Full 8 Phone Cards Configuration (M2)
CARDS_CONFIG = [
    {"id": "1", "file": "01-block-distractions.png"},
    {"id": "2", "file": "02-set-limits.png"},
    {"id": "3", "file": "03-build-a-streak.png"},
    {"id": "4", "file": "04-private-history.png"},
    {"id": "5", "file": "05-stoic-lock.png", "legacy_file": "05-offline-private.png"},
    {"id": "6", "file": "06-pay-once.png"},
    {"id": "7", "file": "07-offline-privacy.png"},
    {"id": "8", "file": "08-scheduled-lock.png"},
]

# 4 Tablet Cards Configuration (M3)
TABLET_CARDS_CONFIG = [
    {"id": "1", "name": "block-distractions"},
    {"id": "2", "name": "set-limits"},
    {"id": "3", "name": "build-a-streak"},
    {"id": "4", "name": "pay-once"},
]

# Helper to load phone mockups as base64 with highest density
SOURCE_MAP = {
    "1_tr": PROJECT_ROOT / "test_render" / "limitra-protected3-tr.png",
    "1_en": PROJECT_ROOT / "test_render" / "limitra-protected3-en.png",
    "1_pt": PROJECT_ROOT / "test_render" / "limitra-c1-pt.png",
    "1_es": PROJECT_ROOT / "test_render" / "limitra-c1-es.png",
    
    "2_tr": PROJECT_ROOT / "test_render" / "limitra-add-tr.png",
    "2_en": PROJECT_ROOT / "test_render" / "limitra-add-en.png",
    "2_pt": PROJECT_ROOT / "test_render" / "limitra-c2-pt.png",
    "2_es": PROJECT_ROOT / "test_render" / "limitra-c2-es.png",
    
    "3_tr": PROJECT_ROOT / "test_render" / "limitra-progress-tr.png",
    "3_en": PROJECT_ROOT / "test_render" / "limitra-progress-en.png",
    "3_pt": PROJECT_ROOT / "test_render" / "limitra-c3-pt.png",
    "3_es": PROJECT_ROOT / "test_render" / "limitra-c3-es.png",
    
    "4_tr": PROJECT_ROOT / "test_render" / "limitra-timeline-tr.png",
    "4_en": PROJECT_ROOT / "test_render" / "limitra-timeline-en.png",
    "4_pt": PROJECT_ROOT / "test_render" / "limitra-c4-pt.png",
    "4_es": PROJECT_ROOT / "test_render" / "limitra-c4-es.png",
    
    "5_tr": PROJECT_ROOT / "test_render" / "limitra-stoic-tr.png",
    "5_en": PROJECT_ROOT / "test_render" / "limitra-stoic-en.png",
    "5_pt": PROJECT_ROOT / "test_render" / "limitra-c5-pt.png",
    "5_es": PROJECT_ROOT / "test_render" / "limitra-c5-es.png",
    
    "6_tr": PROJECT_ROOT / "test_render" / "limitra-c6-tr.png",
    "6_en": PROJECT_ROOT / "test_render" / "limitra-c6-en.png",
    "6_pt": PROJECT_ROOT / "test_render" / "limitra-c6-pt.png",
    "6_es": PROJECT_ROOT / "test_render" / "limitra-c6-es.png",
    
    "7_tr": PROJECT_ROOT / "test_render" / "limitra-c7-tr.png",
    "7_en": PROJECT_ROOT / "test_render" / "limitra-c7-en.png",
    "7_pt": PROJECT_ROOT / "test_render" / "limitra-c7-pt.png",
    "7_es": PROJECT_ROOT / "test_render" / "limitra-c7-es.png",
    
    "8_tr": PROJECT_ROOT / "test_render" / "limitra-c8-tr.png",
    "8_en": PROJECT_ROOT / "test_render" / "limitra-c8-en.png",
    "8_pt": PROJECT_ROOT / "test_render" / "limitra-c8-pt.png",
    "8_es": PROJECT_ROOT / "test_render" / "limitra-c8-es.png",
}

def get_source_b64(card_id, locale_key):
    lang = "en"
    if locale_key == "tr-TR":
        lang = "tr"
    elif locale_key == "pt-BR":
        lang = "pt"
    elif locale_key == "es-ES":
        lang = "es"
    
    key = f"{card_id}_{lang}"
    img_path = SOURCE_MAP.get(key)
    if not img_path or not img_path.exists():
        img_path = SOURCE_MAP.get(f"{card_id}_en")
    if not img_path or not img_path.exists():
        img_path = SOURCE_MAP.get(f"{card_id}_tr")

    with open(img_path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")

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
                "headline": "HER HAREKET\nAÇIKÇA KAYDEDİLİR.",
                "subtitle": "Hesap açmadan koruma geçmişini incele.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "STOACI KİLİT EKRANI",
                "headline": "DÜRTÜSEL KULLANIMI\nBİLGECE DURDUR.",
                "subtitle": "Süre dolduğunda tavizsiz kilit ve ilham veren alıntılar.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "ABONELİK YOK",
                "headline": "BİR KEZ ÖDE.\nÖMÜR BOYU KULLAN.",
                "subtitle": "Aylık ücret yok, gizli ödeme yok. Tüm özellikler sonsuza dek senin.",
                "headline_size": 70,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "VERİ CİHAZINDA KALIR",
                "headline": "HESAP YOK.\nSUNUCU YOK.",
                "subtitle": "Kullanım verin ve ekran alışkanlıkların telefonundan hiç çıkmaz.",
                "headline_size": 74,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "AKTİF ZAMAN ARALIĞI",
                "headline": "İŞ SAATLERİNDE\nOTOMATİK KİLİT.",
                "subtitle": "Seçtiğin saatlerde ve günlerde kendiliğinden devreye girer.",
                "headline_size": 72,
                "subtitle_size": 30,
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
    "en-US": {
        "name": "English",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "STRICT APP BLOCKING",
                "headline": "BLOCK DISTRACTIONS.\nKEEP YOUR TIME.",
                "subtitle": "Daily limits that stop the endless scroll.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "FLEXIBLE DAILY LIMITS",
                "headline": "SET LIMITS\nYOUR WAY.",
                "subtitle": "Choose apps, time limits and protection days.",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "PROGRESS THAT MOTIVATES",
                "headline": "BUILD A STREAK.\nLEVEL UP.",
                "subtitle": "Turn better screen habits into visible progress.",
                "headline_size": 74,
                "subtitle_size": 31,
            },
            {
                "id": "4",
                "eyebrow": "PRIVATE ON-DEVICE HISTORY",
                "headline": "EVERY ACTION.\nCLEARLY TRACKED.",
                "subtitle": "Review your protection history without an account.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "STOIC LOCK SCREEN",
                "headline": "BREAK THE IMPULSE.\nPAUSE WITH WISDOM.",
                "subtitle": "Strict limits and timeless philosophy when your time is up.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "NO SUBSCRIPTIONS",
                "headline": "PAY ONCE.\nUSE FOREVER.",
                "subtitle": "No monthly fees, no hidden charges. All features unlocked forever.",
                "headline_size": 74,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% ON-DEVICE PRIVACY",
                "headline": "NO ACCOUNT.\nNO SERVERS.",
                "subtitle": "Your habits and screen time logs never leave your physical phone.",
                "headline_size": 74,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "SCHEDULED PROTECTION",
                "headline": "AUTOMATIC LOCK\nDURING WORK HOURS.",
                "subtitle": "Activates seamlessly on designated days and scheduled hours.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Block distractions.\nKeep your time.",
            "meta": "PAY ONCE  |  NO SUBSCRIPTIONS  |  100% OFFLINE",
            "subtitle": "Private app blocking, processed on your device.",
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
                "headline": "SERIE AUFBAUEN.\nLEVEL STEIGERN.",
                "subtitle": "Mache bewusstere Bildschirmgewohnheiten sichtbar.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "LOKALER PRIVATER VERLAUF",
                "headline": "JEDE AKTION\nKLAR PROTOKOLLIERT.",
                "subtitle": "Schutzverlauf ohne Benutzerkonto einsehen.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "STÖISCHER SPERRBILDSCHIRM",
                "headline": "IMPULS STOPPEN.\nWEISE INNEHALTEN.",
                "subtitle": "Strenge Limits und zeitlose Philosophie bei Zeitablauf.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "KEIN ABONNEMENT",
                "headline": "EINMAL ZAHLEN.\nLEBENSLANG NUTZEN.",
                "subtitle": "Keine Monatsgebühren, keine Abofallen. Für immer freigeschaltet.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% OFFLINE-DATENSCHUTZ",
                "headline": "KEIN KONTO.\nKEINE SERVER.",
                "subtitle": "Deine Nutzungsdaten verlassen niemals dein Smartphone.",
                "headline_size": 74,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "GEPLANTER SCHUTZ",
                "headline": "AUTOMATISCHE SPERRE\nBEI DER ARBEIT.",
                "subtitle": "Aktiviert sich verlässlich zu festgelegten Tagen und Uhrzeiten.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Ablenkungen stoppen.\nZeit gewinnen.",
            "meta": "EINMALZAHLUNG  |  KEIN ABO  |  100% OFFLINE",
            "subtitle": "Privates App-Blockieren direkt auf deinem Gerät.",
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
                "headline": "DEFINE LÍMITES A\nTU MANERA.",
                "subtitle": "Elige aplicaciones, límites de tiempo y días.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "PROGRESO QUE MOTIVA",
                "headline": "CREA UNA RACHA.\nSUBE DE NIVEL.",
                "subtitle": "Transforma hábitos de pantalla en progreso real.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "HISTORIAL PRIVADO EN DISPOSITIVO",
                "headline": "CADA ACCIÓN\nBIEN REGISTRADA.",
                "subtitle": "Revisa tu historial de protección sin crear cuenta.",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "PANTALLA DE BLOQUEO ESTOICA",
                "headline": "FRENA EL IMPULSO.\nPAUSA CON SABIDURÍA.",
                "subtitle": "Límites estrictos y filosofía atemporal cuando el tiempo acaba.",
                "headline_size": 64,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "SIN SUSCRIPCIÓN",
                "headline": "PAGA UNA VEZ.\nÚSALO SIEMPRE.",
                "subtitle": "Sin pagos mensuales ni suscripciones ocultas. Tuyo para siempre.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% PRIVACIDAD LOCAL",
                "headline": "SIN CUENTAS.\nSIN SERVIDORES.",
                "subtitle": "Tus datos de pantalla nunca salen de tu dispositivo físico.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "PROTECCIÓN PROGRAMADA",
                "headline": "BLOQUEO AUTOMÁTICO\nEN EL TRABAJO.",
                "subtitle": "Se activa solo en los días y horarios que tú elijas.",
                "headline_size": 68,
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
                "headline_size": 62,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "LIMITES QUOTIDIENNES FLEXIBLES",
                "headline": "FIXEZ VOS LIMITES\nÀ VOTRE FAÇON.",
                "subtitle": "Choisissez les apps, la durée et les jours actifs.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "UN PROGRÈS QUI MOTIVE",
                "headline": "CRÉEZ UNE SÉRIE.\nPASSEZ AU NIVEAU.",
                "subtitle": "Transformez vos habitudes d'écran en progrès visible.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "HISTORIQUE PRIVÉ SUR L'APPAREIL",
                "headline": "CHAQUE ACTION\nCLAIREMENT SUIVIE.",
                "subtitle": "Consultez votre historique sans créer de compte.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "ÉCRAN DE BLOCAGE STOÏCIEN",
                "headline": "STOPPEZ L'IMPULSION.\nPAUSE AVEC SAGESSE.",
                "subtitle": "Limites strictes et philosophie intemporelle à l'échéance.",
                "headline_size": 64,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "SANS ABONNEMENT",
                "headline": "PAYEZ UNE FOIS.\nUTILISEZ POUR TOUJOURS.",
                "subtitle": "Aucun abonnement mensuel ni frais cachés. Débloqué à vie.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% HORS-LIGNE & PRIVÉ",
                "headline": "AUCUN COMPTE.\nAUCUN SERVEUR.",
                "subtitle": "Vos données d'utilisation ne quittent jamais votre téléphone.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "PROTECTION PROGRAMMÉE",
                "headline": "BLOCAGE AUTOMATIQUE\nAU TRAVAIL.",
                "subtitle": "S'active automatiquement aux heures et jours sélectionnés.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Bloquez les distractions.\nGardez votre temps.",
            "meta": "PAIEMENT UNIQUE  |  SANS ABONNEMENT  |  100% HORS-LIGNE",
            "subtitle": "Blocage d'applications privé traité sur votre appareil.",
            "headline_size": 34,
            "meta_size": 17,
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
                "eyebrow": "TELA DE BLOQUEIO ESTÓICA",
                "headline": "PARE O IMPULSO.\nREFLITA COM SABEDORIA.",
                "subtitle": "Limites estritos e filosofia atemporal ao esgotar seu tempo.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "SEM ASSINATURA",
                "headline": "PAGUE UMA VEZ.\nUSE PARA SEMPRE.",
                "subtitle": "Sem taxas mensais nem renovações automáticas. Acesso vitalício.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "DADOS FICAM NO CELULAR",
                "headline": "SEM CONTAS.\nSEM SERVIDORES.",
                "subtitle": "Seus hábitos de tela e histórico nunca saem do seu celular.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "FOCO AGENDADO",
                "headline": "BLOQUEIO AUTOMÁTICO\nNO TRABALHO.",
                "subtitle": "Ativação pontual nos dias e horários selecionados por você.",
                "headline_size": 68,
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
                "eyebrow": "СТОИЧЕСКИЙ ЭКРАН БЛОКИРОВКИ",
                "headline": "ОСТАНОВИ ИМПУЛЬС.\nМУДРАЯ ПАУЗА.",
                "subtitle": "Строгие лимиты и мудрые цитаты, когда время истекло.",
                "headline_size": 64,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "БЕЗ ПОДПИСКИ",
                "headline": "ОДИН ПЛАТЕЖ.\nНАВСЕГДА.",
                "subtitle": "Без ежемесячных списаний и скрытых платежей. Вечный доступ.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "ДАННЫЕ НА УСТРОЙСТВЕ",
                "headline": "БЕЗ АККАУНТА.\nБЕЗ СЕРВЕРОВ.",
                "subtitle": "Ваша статистика экрана никогда не покидает ваш телефон.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "ЗАЩИТА ПО РАСПИСАНИЮ",
                "headline": "АВТОБЛОКИРОВКА В\nРАБОЧИЕ ЧАСЫ.",
                "subtitle": "Автоматически включается в выбранные вами дни и часы.",
                "headline_size": 66,
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
                "eyebrow": "स्टोइक लॉक स्क्रीन",
                "headline": "आदत पर विराम लगाएं।\nसमझदारी से रुकें।",
                "subtitle": "समय पूरा होने पर सख्त सीमाएं और प्रेरणादायक विचार।",
                "headline_size": 62,
                "subtitle_size": 29,
            },
            {
                "id": "6",
                "eyebrow": "कोई सब्सक्रिप्शन नहीं",
                "headline": "एक बार भुगतान।\nहमेशा के लिए।",
                "subtitle": "कोई मासिक शुल्क नहीं। हमेशा के लिए सभी सुविधाएं अनलॉक।",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% डिवाइस पर गोपनीयता",
                "headline": "कोई खाता नहीं।\nकोई सर्वर नहीं।",
                "subtitle": "आपका डेटा कभी भी आपके फोन से बाहर नहीं जाता।",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "शेड्यूल सुरक्षा",
                "headline": "काम के घंटों में\nऑटो लॉक।",
                "subtitle": "निर्धारित दिनों और समय पर अपने आप सक्रिय हो जाता है।",
                "headline_size": 68,
                "subtitle_size": 30,
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
    "id": {
        "name": "Indonesian",
        "is_rtl": False,
        "font_family": "'Segoe UI', -apple-system, Roboto, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "BLOKIR APLIKASI KETAT",
                "headline": "HENTIKAN DISTRAKSI.\nHEMAT WAKTUMU.",
                "subtitle": "Batas harian untuk menghentikan scrolling tiada henti.",
                "headline_size": 68,
                "subtitle_size": 31,
            },
            {
                "id": "2",
                "eyebrow": "BATAS HARIAN FLEKSIBEL",
                "headline": "ATUR BATAS SESUAI\nKEINGINANMU.",
                "subtitle": "Pilih aplikasi, batas durasi, dan hari perlindungan.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "3",
                "eyebrow": "PROGRES YANG MEMOTIVASI",
                "headline": "BANGUN STREAK.\nNAIKKAN LEVEL.",
                "subtitle": "Ubah kebiasaan layar menjadi progres yang terlihat nyata.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "4",
                "eyebrow": "RIWAYAT PRIBADI DI PERANGKAT",
                "headline": "SETIAP TINDAKAN\nTERCATAT JELAS.",
                "subtitle": "Tinjau riwayat perlindungan tanpa perlu membuat akun.",
                "headline_size": 70,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "LAYAR KUNCI STOIK",
                "headline": "HENTIKAN IMPULS.\nJEDA DENGAN BIJAK.",
                "subtitle": "Batas tegas dan kutipan filosofis saat waktu penggunaan habis.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "TANPA LANGGANAN",
                "headline": "BAYAR SEKALI.\nPAKAI SELAMANYA.",
                "subtitle": "Tanpa biaya bulanan atau jebakan langganan. Milikmu selamanya.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "100% PRIVASI DI PERANGKAT",
                "headline": "TANPA AKUN.\nTANPA SERVER.",
                "subtitle": "Data pemakaian layar Anda tidak pernah meninggalkan ponsel Anda.",
                "headline_size": 72,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "PERLINDUNGAN TERJADWAL",
                "headline": "KUNCI OTOMATIS\nDI JAM KERJA.",
                "subtitle": "Aktif otomatis pada hari dan jam yang telah Anda tentukan.",
                "headline_size": 70,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "Hentikan distraksi.\nHemat waktumu.",
            "meta": "SEKALI BAYAR  |  TANPA LANGGANAN  |  100% OFFLINE",
            "subtitle": "Pemblokir aplikasi privat yang diproses di perangkat Anda.",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
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
                "headline": "ทุกการกระทำ\nบันทึกอย่างชัดเจน",
                "subtitle": "ดูประวัติการป้องกันโดยไม่ต้องลงทะเบียนบัญชี",
                "headline_size": 72,
                "subtitle_size": 31,
            },
            {
                "id": "5",
                "eyebrow": "หน้าจอล็อกสไตล์สโตอิก",
                "headline": "หยุดแรงกระตุ้น\nพักอย่างชาญฉลาด",
                "subtitle": "ขีดจำกัดที่เข้มงวดและปรัชญาเหนือกาลเวลาเมื่อหมดเวลาใช้งาน",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "ไม่มีค่าบริการรายเดือน",
                "headline": "จ่ายครั้งเดียว\nใช้ได้ตลอดชีพ",
                "subtitle": "ไม่มีค่าบริการรายเดือน ไม่มีการตัดเงินซ้ำซ้อน ปลดล็อกถาวร",
                "headline_size": 70,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "ความเป็นส่วนตัว 100% บนเครื่อง",
                "headline": "ไม่ต้องมีบัญชี\nไม่มีเซิร์ฟเวอร์",
                "subtitle": "ข้อมูลการใช้งานหน้าจอของคุณจะไม่รั่วไหลออกจากเครื่องอย่างแน่นอน",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "การป้องกันตามเวลาที่กำหนด",
                "headline": "ล็อกอัตโนมัติ\nในชั่วโมงทำงาน",
                "subtitle": "เปิดใช้งานอย่างราบรื่นตามวันและเวลาที่คุณกำหนดไว้ล่วงหน้า",
                "headline_size": 68,
                "subtitle_size": 30,
            },
        ],
        "feature": {
            "brand": "LIMITRA",
            "headline": "บล็อกสิ่งรบกวน\nรักษาเวลาของคุณ",
            "meta": "จ่ายครั้งเดียว  |  ไม่มีรายเดือน  |  ออฟไลน์ 100%",
            "subtitle": "การบล็อกแอปที่เน้นความเป็นส่วนตัว ประมวลผลบนเครื่องของคุณ",
            "headline_size": 34,
            "meta_size": 18,
            "subtitle_size": 18,
        }
    },
    "ar": {
        "name": "Arabic",
        "is_rtl": True,
        "font_family": "'Segoe UI', Tahoma, Arial, sans-serif",
        "cards": [
            {
                "id": "1",
                "eyebrow": "حظر تطبيقات صارم وفعال",
                "headline": "احظر المشتتات.\nحافظ على وقتك.",
                "subtitle": "حدود يومية ذكية توقف التمرير اللانهائي فوراً.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "2",
                "eyebrow": "حدود يومية مرنة تماماً",
                "headline": "اضبط الحدود\nعلى طريقتك الخاصة.",
                "subtitle": "اختر التطبيقات، والحدود الزمنية، وأيام الحماية.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "3",
                "eyebrow": "تقدم يحفزك على الالتزام",
                "headline": "ابنِ سلسلتك.\nوارتقِ بمستواك.",
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
                "eyebrow": "شاشة قفل رِواقية",
                "headline": "أوقف الاندفاع.\nتوقف بحكمة.",
                "subtitle": "حدود صارمة واقتباسات فلسفية ملهمة عند انتهاء الوقت.",
                "headline_size": 66,
                "subtitle_size": 30,
            },
            {
                "id": "6",
                "eyebrow": "بدون اشتراكات إطلاقاً",
                "headline": "ادفع مرة واحدة.\nواستخدم للأبد.",
                "subtitle": "لا رسوم شهرية ولا مصاريف متكررة. وصول دائم وكامل.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "7",
                "eyebrow": "خصوصية محلية 100%",
                "headline": "بدون حسابات.\nبدون خوادم.",
                "subtitle": "سجل استخدامك وعادات شاشتك لا تغادر هاتفك المحمول أبداً.",
                "headline_size": 68,
                "subtitle_size": 30,
            },
            {
                "id": "8",
                "eyebrow": "حماية مجدولة بذكاء",
                "headline": "قفل تلقائي خلال\nساعات العمل.",
                "subtitle": "تعمل تلقائياً بدقة وفق الأيام والساعات المحددة مسبقاً.",
                "headline_size": 66,
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
    
    card_id = card["id"]
    source_b64 = get_source_b64(card_id, locale_key)
    
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

# M6 Feature Graphic with perfect vertical balance (top: 109px)
def generate_html_feature(locale_key, locale_cfg):
    is_rtl = locale_cfg.get("is_rtl", False)
    font_fam = locale_cfg.get("font_family", "'Segoe UI', sans-serif")
    feat = locale_cfg["feature"]
    
    # top: 109px perfectly balances vertical margins (134px top, 133px bottom -> 1px deviation)
    if is_rtl:
        icon_style = "position: absolute; width: 360px; height: 360px; right: 54px; top: 70px;"
        text_style = "position: absolute; top: 109px; left: 54px; right: 440px; direction: rtl; text-align: right; display: flex; flex-direction: column; justify-content: center;"
    else:
        icon_style = "position: absolute; width: 360px; height: 360px; left: 54px; top: 70px;"
        text_style = "position: absolute; top: 109px; left: 430px; right: 54px; text-align: left; display: flex; flex-direction: column; justify-content: center;"

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

# M3 Tablet Screenshots Generator (7-inch: 1200x1920, 10-inch: 1600x2560)
def generate_html_tablet(card_idx, locale_key, locale_cfg, width, height):
    is_rtl = locale_cfg.get("is_rtl", False)
    font_fam = locale_cfg.get("font_family", "'Segoe UI', sans-serif")
    card = locale_cfg["cards"][card_idx]
    
    # Scale factor relative to 1200x1920 base
    scale = width / 1200.0
    
    card_id = card["id"]
    source_b64 = get_source_b64(card_id, locale_key)

    # For tablet, we show a spacious, elevated tablet frame with the rich mockups
    frame_w = int(880 * scale)
    frame_h = int(1280 * scale)
    frame_left = (width - frame_w) // 2
    frame_top = int(520 * scale)
    
    header_top = int(60 * scale)
    header_left = int(70 * scale)
    icon_sz = int(90 * scale)
    brand_sz = int(32 * scale)
    
    text_top = int(180 * scale)
    eyebrow_sz = int(28 * scale)
    headline_sz = int(card.get('headline_size', 70) * scale * 0.95)
    subtitle_sz = int(card.get('subtitle_size', 30) * scale * 1.05)
    
    align_dir = "rtl" if is_rtl else "ltr"
    align_text = "right" if is_rtl else "left"

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    width: {width}px;
    height: {height}px;
    background: linear-gradient(180deg, #07142E 0%, #0C2A50 100%);
    overflow: hidden;
    position: relative;
    font-family: {font_fam};
    -webkit-font-smoothing: antialiased;
    direction: {align_dir};
  }}
  .glow1 {{
    position: absolute;
    width: {int(900*scale)}px; height: {int(900*scale)}px;
    left: -{int(200*scale)}px; top: -{int(200*scale)}px;
    border-radius: 50%;
    background: #1EE6C5;
    opacity: 0.09;
    filter: blur({int(50*scale)}px);
  }}
  .glow2 {{
    position: absolute;
    width: {int(800*scale)}px; height: {int(800*scale)}px;
    right: -{int(200*scale)}px; bottom: -{int(200*scale)}px;
    border-radius: 50%;
    background: #1EE6C5;
    opacity: 0.09;
    filter: blur({int(50*scale)}px);
  }}
  .header {{
    position: absolute;
    top: {header_top}px;
    left: {header_left}px;
    right: {header_left}px;
    display: flex;
    align-items: center;
    gap: {int(20*scale)}px;
  }}
  .icon {{
    width: {icon_sz}px;
    height: {icon_sz}px;
  }}
  .brand {{
    font-size: {brand_sz}px;
    font-weight: 800;
    color: #FFFFFF;
    letter-spacing: 2px;
  }}
  .text-container {{
    position: absolute;
    top: {text_top}px;
    left: {header_left}px;
    right: {header_left}px;
    text-align: {align_text};
  }}
  .eyebrow {{
    font-size: {eyebrow_sz}px;
    font-weight: 700;
    color: #22E6C5;
    letter-spacing: 1.5px;
    margin-bottom: {int(12*scale)}px;
    text-transform: uppercase;
  }}
  .headline {{
    font-size: {headline_sz}px;
    font-weight: 800;
    color: #FFFFFF;
    line-height: 1.15;
    margin-bottom: {int(16*scale)}px;
    white-space: pre-line;
    letter-spacing: -0.5px;
  }}
  .subtitle {{
    font-size: {subtitle_sz}px;
    font-weight: 400;
    color: #B7C7DC;
    line-height: 1.35;
    white-space: pre-line;
  }}
  .tablet-frame {{
    position: absolute;
    left: {frame_left}px;
    top: {frame_top}px;
    width: {frame_w}px;
    height: {frame_h}px;
    border-radius: {int(54*scale)}px;
    border: {int(10*scale)}px solid rgba(255, 255, 255, 0.55);
    box-shadow: 0 {int(24*scale)}px {int(60*scale)}px rgba(0, 0, 0, 0.5);
    overflow: hidden;
    background: #000;
  }}
  .tablet-frame img {{
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
    <div class="brand">LIMITRA TABLET</div>
  </div>
  <div class="text-container">
    <div class="eyebrow">{card['eyebrow']}</div>
    <div class="headline">{card['headline']}</div>
    <div class="subtitle">{card['subtitle']}</div>
  </div>
  <div class="tablet-frame">
    <img src="{source_b64}">
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
    print("==================================================================")
    print("STARTING FULL STORE ASSETS GENERATION FOR ALL 11 LOCALES")
    print("==================================================================")
    print("Includes:")
    print("  - M1: 0% empty space on Card 2 (and all cards)")
    print("  - M2: Full 8 Phone Screenshots (Cards 1 to 8) across ALL 11 locales")
    print("  - M3: 7-inch & 10-inch Tablet Screenshots (4 cards each)")
    print("  - M5: Localized phone mockups for pt-BR (Portuguese) and es-ES (Spanish)")
    print("  - M6: Feature Graphic with pixel-perfect vertical balance")
    print("  - M7: Standardized icon directory structure across all locales")
    print("==================================================================")
    
    total_locales = len(LOCALES)
    total_phone_imgs = 0
    total_tablet_imgs = 0
    
    for loc_key, loc_cfg in LOCALES.items():
        print(f"\n>>> Processing Locale: {loc_key} ({loc_cfg['name']}) <<<")
        
        # Directories
        store_assets_dir = PROJECT_ROOT / "store_assets" / f"{loc_key}-v2"
        store_assets_dir.mkdir(parents=True, exist_ok=True)
        
        play_sync_phone = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "phoneScreenshots"
        play_sync_feature = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "featureGraphic"
        play_sync_seven = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "sevenInchScreenshots"
        play_sync_ten = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "tenInchScreenshots"
        play_sync_icon = PROJECT_ROOT / "store_assets" / "play-sync-v2" / loc_key / "icon"
        
        play_sync_phone.mkdir(parents=True, exist_ok=True)
        play_sync_feature.mkdir(parents=True, exist_ok=True)
        play_sync_seven.mkdir(parents=True, exist_ok=True)
        play_sync_ten.mkdir(parents=True, exist_ok=True)
        play_sync_icon.mkdir(parents=True, exist_ok=True)
        
        legacy_phone = PROJECT_ROOT / "play_store_images" / loc_key / "phoneScreenshots"
        legacy_feature = PROJECT_ROOT / "play_store_images" / loc_key / "featureGraphic"
        legacy_seven = PROJECT_ROOT / "play_store_images" / loc_key / "sevenInchScreenshots"
        legacy_ten = PROJECT_ROOT / "play_store_images" / loc_key / "tenInchScreenshots"
        legacy_icon = PROJECT_ROOT / "play_store_images" / loc_key / "icon"
        
        legacy_phone.mkdir(parents=True, exist_ok=True)
        legacy_feature.mkdir(parents=True, exist_ok=True)
        legacy_seven.mkdir(parents=True, exist_ok=True)
        legacy_ten.mkdir(parents=True, exist_ok=True)
        legacy_icon.mkdir(parents=True, exist_ok=True)

        # 1. Standardize Icon (M7)
        icon_img = Image.open(ICON_PATH)
        icon_img.save(play_sync_icon / "icon.png", "PNG")
        icon_img.save(legacy_icon / "icon.png", "PNG")

        # 2. Render 8 Phone Screenshots (M1 & M2 & M5)
        for idx, card in enumerate(loc_cfg["cards"], 1):
            file_name = CARDS_CONFIG[idx - 1]["file"]
            dest1 = store_assets_dir / file_name
            dest2 = play_sync_phone / f"{idx}.png"
            dest3 = legacy_phone / f"{idx}.png"
            
            html = generate_html_screenshot(card, loc_key, loc_cfg)
            render_html_to_png(html, dest1, 1080, 1920, f"{loc_key}_p{idx}")
            
            img = Image.open(dest1)
            assert img.size == (1080, 1920), f"Invalid size {img.size} for {dest1}"
            img.save(dest2, "PNG")
            img.save(dest3, "PNG")
            
            if "legacy_file" in CARDS_CONFIG[idx - 1]:
                legacy_card_dest = store_assets_dir / CARDS_CONFIG[idx - 1]["legacy_file"]
                img.save(legacy_card_dest, "PNG")
                
            print(f"  [Phone {idx}/8] {file_name} -> OK")
            total_phone_imgs += 1

        # 3. Render Feature Graphic (M6: Perfect 1px Vertical Balance)
        feat_dest1 = store_assets_dir / "feature-graphic-1024x500.png"
        feat_dest2 = play_sync_feature / "feature.png"
        feat_dest3 = legacy_feature / "feature.png"
        feat_html = generate_html_feature(loc_key, loc_cfg)
        render_html_to_png(feat_html, feat_dest1, 1024, 500, f"{loc_key}_feat")
        
        feat_img = Image.open(feat_dest1)
        assert feat_img.size == (1024, 500), f"Invalid size {feat_img.size} for {feat_dest1}"
        feat_img.save(feat_dest2, "PNG")
        feat_img.save(feat_dest3, "PNG")
        print("  [Feature Graphic] -> OK")

        # 4. Render 7-inch & 10-inch Tablet Screenshots (M3: 4 cards each)
        # We generate tablet assets for all locales to completely eliminate tablet penalty
        for t_idx in range(4):
            card_num = t_idx + 1
            # 7-inch: 1200 x 1920
            seven_html = generate_html_tablet(t_idx, loc_key, loc_cfg, 1200, 1920)
            seven_dest_sync = play_sync_seven / f"{card_num}.png"
            seven_dest_leg = legacy_seven / f"{card_num}.png"
            render_html_to_png(seven_html, seven_dest_sync, 1200, 1920, f"{loc_key}_7in_{card_num}")
            Image.open(seven_dest_sync).save(seven_dest_leg, "PNG")

            # 10-inch: 1600 x 2560
            ten_html = generate_html_tablet(t_idx, loc_key, loc_cfg, 1600, 2560)
            ten_dest_sync = play_sync_ten / f"{card_num}.png"
            ten_dest_leg = legacy_ten / f"{card_num}.png"
            render_html_to_png(ten_html, ten_dest_sync, 1600, 2560, f"{loc_key}_10in_{card_num}")
            Image.open(ten_dest_sync).save(ten_dest_leg, "PNG")
            
            print(f"  [Tablet {card_num}/4] 7-inch & 10-inch -> OK")
            total_tablet_imgs += 2

    print("\n==================================================================")
    print(f"SUCCESS: Generated {total_phone_imgs} Phone Screenshots across {total_locales} locales")
    print(f"SUCCESS: Generated {total_tablet_imgs} Tablet Screenshots (7\" and 10\")")
    print("Ready for Google Play Console Sync (`gpc images sync`)")
    print("==================================================================")

if __name__ == "__main__":
    main()
