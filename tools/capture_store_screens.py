"""Limitra mağaza ekran görüntüsü çekimi (emülatör, Premium Koyu tema, demo veri).

Kullanım:
  python tools/capture_store_screens.py --locale tr-TR
  python tools/capture_store_screens.py --locale ar --only lock,trackers

Çıktı: store_assets/<locale>-v3/source/{setup_limit,setup_schedule,trackers,timeline,ach_grid,lock}.png
Yalnız emülatöre (varsayılan emulator-5554) dokunur; gerçek telefona asla.
Gereksinim: emülatörde com.gardiyan.app (debug), com.google.android.youtube, com.android.chrome kurulu.
"""
import argparse
import json
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
        sys.stderr.reconfigure(encoding="utf-8", errors="backslashreplace")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parent.parent
PKG = "com.gardiyan.app"
A11Y = f"{PKG}/{PKG}.service.AppBlockAccessibilityService"
YT = "com.google.android.youtube"

# Play locale -> uygulama içi dil kodu (ProfileScreen.kt dil listesiyle aynı)
APP_LANG = {"en-US": "en", "tr-TR": "tr", "de-DE": "de", "es-ES": "es", "fr-FR": "fr",
            "hi-IN": "hi", "id": "id", "pt-BR": "pt", "ru-RU": "ru", "th": "th", "ar": "ar"}
RES_DIR = {"id": "values-in"}   # diğer diller values-<kod>; en -> values
LOCK_QUOTE = "20"               # kilit kartında gösterilecek söz (Epictetus: "If you do not wish to do something…")
ALL_STEPS = ["setup", "trackers", "timeline", "achievements", "lock"]

SERIAL = "emulator-5554"
RTL = False
LANG = "en"
TMP = Path(tempfile.gettempdir()) / "limitra_capture"
TMP.mkdir(exist_ok=True)


def adb(*args, check=True, text=True):
    r = subprocess.run(["adb", "-s", SERIAL, *args], capture_output=True, text=text,
                       env={**__import__("os").environ, "MSYS_NO_PATHCONV": "1"})
    if check and r.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} -> {r.stderr or r.stdout}")
    return r.stdout


def sh(cmd):
    return adb("shell", cmd)


def mx(x):
    """RTL dillerde (ar) arayüz aynalanır; x koordinatını yansıt."""
    return 1080 - x if RTL else x


def tap(x, y, mirror=True, wait=0.6):
    sh(f"input tap {mx(x) if mirror else x} {y}")
    time.sleep(wait)


def swipe(x1, y1, x2, y2, ms=300, wait=1.0):
    sh(f"input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(wait)


def dump_nodes():
    sh("uiautomator dump /sdcard/ui.xml >/dev/null")
    out = TMP / "ui.xml"
    adb("pull", "/sdcard/ui.xml", str(out))
    return list(ET.parse(out).iter("node"))


def bounds_center(node):
    m = re.findall(r"\d+", node.get("bounds"))
    l, t, r, b = map(int, m)
    return (l + r) // 2, (t + b) // 2


def tap_id(res_id):
    for n in dump_nodes():
        if n.get("resource-id") == res_id:
            x, y = bounds_center(n)
            sh(f"input tap {x} {y}")
            time.sleep(0.8)
            return
    raise RuntimeError(f"düğüm yok: {res_id}")


def screenshot(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
                          capture_output=True).stdout
    path.write_bytes(data)
    print("  ->", path)


def run_as_sql(sql_text):
    f = TMP / "q.sql"
    f.write_text(sql_text, encoding="utf-8")
    adb("push", str(f), "/data/local/tmp/q.sql")
    sh(f"run-as {PKG} sh -c 'sqlite3 databases/guardian_db < /data/local/tmp/q.sql'")


def push_prefs(name, xml):
    f = TMP / f"{name}.xml"
    f.write_text(xml, encoding="utf-8")
    adb("push", str(f), f"/data/local/tmp/{name}.xml")
    sh(f"run-as {PKG} sh -c 'mkdir -p shared_prefs && cp /data/local/tmp/{name}.xml shared_prefs/{name}.xml'")


def restart_app():
    sh("am force-stop com.limitra.cleanscan")
    sh("input keyevent 3")
    sh(f"am force-stop {PKG}")
    sh(f"settings put secure enabled_accessibility_services {A11Y}")  # force-stop izni düşürür
    sh("settings put secure accessibility_enabled 1")
    sh(f"am start -W -n {PKG}/.MainActivity")
    time.sleep(4)


# ---------------------------------------------------------------- hazırlık
def lock_quote_prefs(lang):
    """Kilit ekranı sözü günün tarihine göre döner; kartın her çekimde aynı olması için seçili sözü
    'özel söz' olarak (yalnız benimkiler) yazarız. Metin, dilin strings.xml'indeki çeviridir."""
    folder = "values" if lang == "en" else RES_DIR.get(lang, f"values-{lang}")
    root = ET.parse(ROOT / "app" / "src" / "main" / "res" / folder / "strings.xml").getroot()
    vals = {e.get("name"): "".join(e.itertext()) for e in root if e.tag == "string"}
    bs = chr(92)   # strings.xml'de kaçışlı yazılır: \' \" \n
    text = (vals[f"quote_text_{LOCK_QUOTE}"].replace(bs + "'", "'").replace(bs + '"', '"').replace(bs + "n", " "))
    author = vals[f"quote_author_{LOCK_QUOTE}"]
    base = ET.parse(ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml").getroot()
    en_text = next("".join(e.itertext()) for e in base if e.get("name") == f"quote_text_{LOCK_QUOTE}")
    if lang != "en" and vals[f"quote_text_{LOCK_QUOTE}"] == en_text:
        print(f"  UYARI: {folder} içinde quote_text_{LOCK_QUOTE} çevrilmemiş (İngilizce) -> kilit kartı İngilizce çıkar")
    js = json.dumps([{"text": text, "author": author, "isSelected": True}], ensure_ascii=False)
    return escape(js)


def prepare(lang):
    sh("settings put global sysui_demo_allowed 1")
    for c in ["-e command enter", "-e command clock -e hhmm 0941",
              "-e command battery -e level 100 -e plugged false",
              "-e command network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4",
              "-e command notifications -e visible false"]:
        sh(f"am broadcast -a com.android.systemui.demo {c}")
    sh(f"appops set {PKG} GET_USAGE_STATS allow")
    sh(f"appops set {PKG} SYSTEM_ALERT_WINDOW allow")
    sh(f"dumpsys deviceidle whitelist +{PKG}")
    sh(f"pm revoke {PKG} android.permission.POST_NOTIFICATIONS")   # 'koruma aktif değil' bildirimi ekrana düşmesin
    sh("pm disable-user --user 0 com.google.android.googlequicksearchbox 2>/dev/null || true")
    sh(f"am force-stop {PKG}")
    push_prefs("gardiyan_settings", f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="initial_permission_gate_completed" value="true" />
    <boolean name="initial_permission_gate_legacy_migrated" value="true" />
    <string name="theme_mode">DARK</string>
    <string name="theme_palette">PREMIUM_DARK</string>
    <boolean name="show_only_my_quotes" value="true" />
    <string name="custom_quotes_json">{lock_quote_prefs(lang)}</string>
</map>""")
    push_prefs("limitra_achievements", """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="best_streak" value="64" />
    <string name="equipped_frame">OBSIDIAN_CROWN</string>
    <set name="celebrated_frames"><string>SPARK</string><string>BRONZE</string><string>SILVER_LAUREL</string>
    <string>GOLD_SEAL</string><string>MOONSTONE</string><string>OBSIDIAN_CROWN</string></set>
</map>""")
    sh(f"cmd locale set-app-locales {PKG} --locales {lang}")
    sh(f"am start -n {PKG}/.MainActivity")   # DB yoksa oluşsun (taze kurulum)
    time.sleep(4)
    sh(f"am force-stop {PKG}")


def rows_sql(yt_seconds):
    today = time.strftime("%Y-%m-%d")
    now = int(time.time() * 1000)
    created = now - 70 * 86400000
    days_all = "Pzt,Sal,Çar,Per,Cum,Cmt,Paz"  # iç değerler yerelden bağımsız, Türkçe kalır

    def row(pkg, name, lim, rem_s, win=0, s=0, e=0, days=days_all):
        return ("INSERT INTO restricted_apps (packageName,appName,dailyLimitMinutes,remainingMinutesToday,"
                "remainingSecondsToday,isActive,isFailed,restrictionGroupId,restrictionName,activeWindowEnabled,"
                "activeStartMinutes,activeEndMinutes,activeDays,lastResetDate,createdAtMillis,nextDayLimitMinutes,"
                "nextDayActiveDays,lastLimitUpdateDate,todayMinLimitMinutes,usageStatsBaselineMillisToday,"
                "lastUsageStatsObservedMillisToday,lastUsageStatsReconciledAtMillis) VALUES "
                f"('{pkg}','{name}',{lim},{rem_s // 60},{rem_s},1,0,'g_{pkg}','',{win},{s},{e},'{days}',"
                f"'{today}',{created},{lim},'{days}','{today}',{lim},-1,0,0);")
    return "\n".join([
        "DELETE FROM restricted_apps;",
        row(YT, "YouTube", 60, yt_seconds),
        row("com.limitra.socialprototype", "Social", 45, 860),
        row("com.android.chrome", "Chrome", 90, 4260, 1, 540, 1080, "Pzt,Sal,Çar,Per,Cum"),
        row("com.google.android.apps.youtube.music", "YT Music", 30, 420),
    ])


def device_midnight_ms():
    """Cihazın yerel gün başlangıcı (ms); olay saatleri durum çubuğundaki 9:41 ile tutarlı kalsın."""
    now = int(sh("date +%s").strip())
    hms = sh("date +%H:%M:%S").strip().split(":")
    sod = int(hms[0]) * 3600 + int(hms[1]) * 60 + int(hms[2])
    return (now - sod) * 1000


def logs_sql():
    mid = device_midnight_ms()
    D = 86400000
    out = ["DELETE FROM status_logs;", "UPDATE user_sessions SET consecutiveSuccessDays=64, level=6, isActive=1, hasRedBadge=0;"]

    def log(t, ev, app, pkg, det):
        out.append(f"INSERT INTO status_logs (eventType,timestamp,appName,packageName,details) VALUES ('{ev}',{t},'{app}','{pkg}','{det}');")
    for d in range(1, 11):
        log(mid - (d - 1) * D - 300000, "SUCCESS_DAY", "", "", "Daily goal completed")   # her günün 23:55'i
    log(mid + 7 * 3600000 + 37 * 60000, "OVERLAY_SHOWN", "YouTube", YT, "YouTube limit reached - lock screen shown")      # bugün 07:37
    log(mid - D + 6 * 3600000 + 37 * 60000, "OVERLAY_SHOWN", "Social", "com.limitra.socialprototype", "Social limit reached - lock screen shown")  # dün 06:37
    log(mid - 2 * D + 7 * 3600000 + 37 * 60000, "OVERLAY_SHOWN", "YouTube", YT, "YouTube limit reached - lock screen shown")
    return "\n".join(out)


# ---------------------------------------------------------------- adımlar
def step_setup(out):
    """Kısıtlama yokken: Home -> Yeni kısıtlama -> uygulama seç -> 00:30 -> aktif saat 09-18 Pzt-Cum."""
    run_as_sql("DELETE FROM restricted_apps;\n" + logs_sql())
    restart_app()
    tap(200, 1855)                  # Home sekmesi
    tap(540, 1010, wait=2)          # Yeni kısıtlama
    tap(892, 633, wait=2)           # + (uygulama seç)
    for q in ("youtube", "chrome"):
        tap(540, 540, mirror=False)
        sh(f"input text {q}")
        time.sleep(2)
        tap(967, 737)               # ilk sonucu seç
        tap(964, 540, wait=1)       # aramayı temizle
    sh("input keyevent 4")          # klavyeyi kapat
    time.sleep(1)
    tap(540, 1815, mirror=False, wait=2)   # seçimi onayla
    tap(370, 1253, wait=0.5)        # saat -1 (01 -> 00)
    for _ in range(30):
        sh(f"input tap {mx(712)} 1022")   # dakika +1 (00 -> 30)
    time.sleep(1)
    screenshot(out / "setup_limit.png")
    tap(905, 1525, wait=1.5)        # aktif zaman aralığı anahtarı
    swipe(540, 1500, 540, 700)
    tap(300, 780, wait=2)           # başlangıç saati -> sistem saat seçici
    sh("input tap 303 1069"); time.sleep(1.2)   # kadranda 9 (kadran aynalanmaz)
    tap_id("android:id/button1")    # Tamam
    time.sleep(1.2)
    tap(780, 780, wait=2)           # bitiş saati
    sh("input tap 540 1211"); time.sleep(1.2)   # kadran iç halka 18
    tap_id("android:id/button1")
    time.sleep(1.2)
    tap(788, 1140, wait=0.5)        # Cmt kapat
    tap(912, 1140, wait=1)          # Paz kapat
    screenshot(out / "setup_schedule.png")


def step_trackers(out):
    run_as_sql(rows_sql(2280) + "\n" + logs_sql())
    restart_app()
    tap(540, 1855, wait=2)          # Korunanlar sekmesi
    screenshot(out / "trackers.png")


def step_timeline(out):
    run_as_sql(rows_sql(2280) + "\n" + logs_sql())
    restart_app()
    tap(880, 1855, wait=1.5)        # İlerleme
    tap(540, 330, wait=2)           # Zaman tüneli sekmesi
    screenshot(out / "timeline.png")


def step_achievements(out):
    run_as_sql(rows_sql(2280) + "\n" + logs_sql())
    restart_app()
    tap(880, 1855, wait=2)          # İlerleme > Özet
    tap(540, 1465, wait=4)          # Başarılar kartı (giriş animasyonu bitsin)
    swipe(540, 1750, 540, 350, ms=1800, wait=1.5)   # yavaş = fling yok, kaydırma ~1400 px ve deterministik
    screenshot(out / "ach_grid.png")


def string_value(lang, key):
    folder = "values" if lang == "en" else RES_DIR.get(lang, f"values-{lang}")
    for f in (folder, "values"):
        root = ET.parse(ROOT / "app" / "src" / "main" / "res" / f / "strings.xml").getroot()
        for e in root:
            if e.tag == "string" and e.get("name") == key:
                bs = chr(92)
                return "".join(e.itertext()).replace(bs + "'", "'").replace(bs + '"', '"')
    return ""


def step_lock(out):
    """Kilit ekranı bazen ilk denemede önceki/varsayılan dille açılabiliyor: üstteki metni beklenenle
    karşılaştır, uyuşmazsa uygulamayı yeniden başlatıp en çok 4 kez dene."""
    expected = string_value(LANG, "overlay_limit_over")
    for attempt in range(4):
        sh(f"cmd locale set-app-locales {PKG} --locales {LANG}")
        run_as_sql(rows_sql(0) + "\n" + logs_sql())
        restart_app()
        sh("input keyevent 3")
        time.sleep(1)
        sh(f"monkey -p {YT} 1")
        time.sleep(5)
        texts = [n.get("text") for n in dump_nodes() if n.get("text")]
        if expected in texts:
            break
        print(f"  kilit metni beklenenle uyuşmadı (deneme {attempt + 1}); yeniden deniyorum. beklenen={expected!r}")
    screenshot(out / "lock.png")
    sh("input keyevent 3")


def main():
    global SERIAL, RTL, LANG
    ap = argparse.ArgumentParser()
    ap.add_argument("--locale", required=True, choices=sorted(APP_LANG))
    ap.add_argument("--serial", default=SERIAL)
    ap.add_argument("--only", default=",".join(ALL_STEPS), help="virgülle: " + ",".join(ALL_STEPS))
    ap.add_argument("--out", default=None)
    ap.add_argument("--install", action="store_true", help="debug APK'yı önce kur (app/build/outputs/apk/debug)")
    a = ap.parse_args()
    SERIAL = a.serial
    if not SERIAL.startswith("emulator-"):
        sys.exit("Güvenlik: yalnız emülatör seri numarası kabul edilir (emulator-XXXX).")
    lang = APP_LANG[a.locale]
    LANG = lang
    RTL = lang == "ar"
    out = Path(a.out) if a.out else ROOT / "store_assets" / f"{a.locale}-v3" / "source"
    print(f"locale={a.locale} lang={lang} rtl={RTL} out={out}")
    if a.install:
        apk = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
        adb("install", "-r", str(apk))
    prepare(lang)
    steps = a.only.split(",")
    fn = {"setup": step_setup, "trackers": step_trackers, "timeline": step_timeline,
          "achievements": step_achievements, "lock": step_lock}
    for s in steps:
        print("adım:", s)
        fn[s](out)
    sh("am broadcast -a com.android.systemui.demo -e command exit")
    print("bitti")


if __name__ == "__main__":
    main()
