"""Limitra en-US Play Store kartlari (v3): gercek uygulama ekran goruntuleri + tipografi.

Kaynaklar store_assets/en-US-v3/source altindaki emulator goruntuleridir
(Premium Dark tema, demo verisi). Cikti: store_assets/en-US-v3/out/.
Calistirma: python tools/generate_store_en_v3.py [phone|tablet7|tablet10|feature|all]
"""
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
D = ROOT / "store_assets" / "en-US-v3"
SRC = D / "source"
HTML = D / "html"
OUT = D / "out"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

BG = "#0F0E0C"
CARD = "#181613"
GOLD = "#D9A55B"
CREAM = "#F2EBDD"
MUTED = "#B4AC9B"
FAINT = "#6F6859"


def css(w, h, u):
    """u: olcek birimi (1080 genislikte 1.0)."""
    return f"""
@font-face{{font-family:NR;src:url('../fonts/newsreader_regular.ttf');font-weight:400}}
@font-face{{font-family:NR;src:url('../fonts/newsreader_medium.ttf');font-weight:500}}
@font-face{{font-family:NR;src:url('../fonts/newsreader_italic.ttf');font-style:italic;font-weight:400}}
@font-face{{font-family:MR;src:url('../fonts/manrope_regular.ttf');font-weight:400}}
@font-face{{font-family:MR;src:url('../fonts/manrope_semibold.ttf');font-weight:600}}
@font-face{{font-family:MR;src:url('../fonts/manrope_bold.ttf');font-weight:700}}
@font-face{{font-family:MR;src:url('../fonts/manrope_extrabold.ttf');font-weight:800}}
*{{box-sizing:border-box;margin:0;padding:0}}
html,body{{width:{w}px;height:{h}px;overflow:hidden;background:{BG}}}
body{{position:relative;font-family:MR,sans-serif;color:{CREAM};-webkit-font-smoothing:antialiased}}
.glow{{position:absolute;inset:0;background:
  radial-gradient({1200*u}px {900*u}px at var(--gx,50%) var(--gy,100%),rgba(217,165,91,.20),transparent 68%)}}
.copy{{position:absolute;z-index:5}}
h1{{font-family:NR;font-weight:400;color:{CREAM};letter-spacing:-.018em;line-height:1.0}}
h1 em{{font-style:italic;color:{GOLD}}}
p.sub{{font-family:MR;font-weight:400;color:{MUTED};line-height:1.42}}
.phone{{position:absolute;overflow:hidden;background:#000;
  box-shadow:0 0 {280*u}px rgba(217,165,91,.22),0 {40*u}px {120*u}px rgba(0,0,0,.85)}}
.phone::after{{content:'';position:absolute;inset:0;border-radius:inherit;pointer-events:none;
  border:{4*u}px solid rgba(242,235,221,.26)}}
.phone img{{display:block}}
"""


def page(w, h, u, body, gx="50%", gy="100%"):
    return (f"<!doctype html><html lang='en'><head><meta charset='utf-8'>"
            f"<style>{css(w, h, u)}</style></head>"
            f"<body style='--gx:{gx};--gy:{gy}'><div class='glow'></div>{body}</body></html>")


def phone(src, x, y, w, top=0, bottom=None, radius=None, u=1.0, z=1, h=None):
    """Kaynagin [top, bottom) satirlarini w genislikte cihaz ekrani olarak koyar."""
    sw, sh = Image.open(SRC / src).size
    sc = w / sw
    ch = ((bottom or sh) - top) * sc
    r = (radius if radius is not None else 60) * sc
    return (f"<div class='phone' style='left:{x}px;top:{y}px;width:{w}px;height:{h or ch}px;"
            f"border-radius:{r}px;z-index:{z}'>"
            f"<img src='../source/{src}' style='width:{w}px;margin-top:-{top * sc}px'></div>")


def copy(x, y, w, h1, sub, fs, subfs, gap=34):
    return (f"<div class='copy' style='left:{x}px;top:{y}px;width:{w}px'>"
            f"<h1 style='font-size:{fs}px'>{h1}</h1>"
            f"<p class='sub' style='font-size:{subfs}px;margin-top:{gap}px;max-width:{w * .94}px'>{sub}</p></div>")


# ---------------------------------------------------------------- telefon kartlari
def phone_cards():
    W, H = 1080, 1920
    u = 1.0
    X, CW = 84, 912
    cards = {}

    cards["01-five-more-minutes"] = page(W, H, u,
        copy(X, 118, CW, "“Just five<br>more minutes.”<br><em>Not today.</em>",
             "Set a daily limit. When time is up, Limitra locks the app.", 134, 40)
        + phone("lock.png", 100, 800, 880, top=250, radius=62), gy="96%")

    cards["02-set-it-once"] = page(W, H, u,
        copy(X, 118, CW, "Set it once.<br><em>Keep your word.</em>",
             "Pick the apps, a daily limit and the days that count.", 134, 40)
        + phone("setup_limit.png", 90, 660, 900, top=96, radius=62), gy="96%")

    cards["03-clean-days"] = page(W, H, u,
        copy(X, 118, CW, "Make every<br>clean day <em>count.</em>",
             "Keep your streak and earn nine animated frames, from day 1 to day 365.", 134, 40)
        + phone("ach_grid.png", 90, 660, 900, top=170, radius=62), gy="92%")

    cards["04-whats-left"] = page(W, H, u,
        copy(X, 118, CW, "See what is left<br><em>at a glance.</em>",
             "Time left for every protected app, in one place.", 134, 40)
        + phone("trackers.png", 90, 660, 900, top=96, radius=62), gy="96%")

    cards["05-only-when-it-matters"] = page(W, H, u,
        copy(X, 118, CW, "Only when<br><em>it matters.</em>",
             "Limit apps during the hours and days you choose.", 134, 40)
        + phone("setup_schedule.png", 90, 660, 900, top=396, radius=62), gy="96%")

    cards["06-on-record"] = page(W, H, u,
        copy(X, 118, CW, "Your progress,<br><em>on record.</em>",
             "A private timeline of every lock and every completed day.", 134, 40)
        + phone("timeline.png", 90, 660, 900, top=96, radius=62), gy="96%")

    # ---- 07 gizlilik: defter satirlari
    rows = ["Accounts", "Ads", "Trackers", "Internet permission"]
    ledger = "".join(
        f"<div class='row'><span class='lbl'>{r}</span><span class='dots'></span><span class='none'>None</span></div>"
        for r in rows)
    ledger_css = f"""<style>
.ledger{{position:absolute;left:84px;top:690px;width:912px;z-index:3}}
.row{{display:flex;align-items:baseline;gap:22px;padding:56px 0;border-bottom:2px solid rgba(242,235,221,.14)}}
.row:first-child{{border-top:2px solid rgba(242,235,221,.14)}}
.lbl{{font-family:NR;font-size:80px;line-height:1;color:{CREAM};letter-spacing:-.01em}}
.dots{{flex:1;border-bottom:4px dotted rgba(242,235,221,.22);transform:translateY(-10px)}}
.none{{font-family:NR;font-style:italic;font-size:80px;line-height:1;color:{GOLD}}}
.pill{{position:absolute;left:84px;top:1640px;z-index:3;display:flex;align-items:center;gap:24px;
  padding:30px 44px;border:3px solid rgba(217,165,91,.55);border-radius:999px;
  font-family:MR;font-weight:600;font-size:38px;color:{CREAM}}}
.pill svg{{width:52px;height:52px}}
</style>"""
    shield = (f"<svg viewBox='0 0 24 24' fill='none' stroke='{GOLD}' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'>"
              "<path d='M12 3 4.5 6v5.5c0 4.3 3 7.7 7.5 9.5 4.5-1.8 7.5-5.2 7.5-9.5V6L12 3z'/><path d='m8.6 12.2 2.4 2.4 4.4-4.8'/></svg>")
    cards["07-stays-on-device"] = page(W, H, u,
        copy(X, 118, CW, "Nothing leaves<br><em>your phone.</em>",
             "Limitra works fully offline. Your usage data stays on this device.", 134, 40)
        + ledger_css + f"<div class='ledger'>{ledger}</div>"
        + f"<div class='pill'>{shield}Works with no internet at all</div>", gy="78%")

    # ---- 08 tek odeme: muhur
    import math
    teeth = 44
    pts = []
    for i in range(teeth * 2):
        r = 392 if i % 2 == 0 else 362
        a = math.pi * i / teeth
        pts.append(f"{r * math.cos(a):.1f},{r * math.sin(a):.1f}")
    seal = f"""<svg class='seal' viewBox='-420 -420 840 840' xmlns='http://www.w3.org/2000/svg'>
<defs>
 <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
  <stop offset='0' stop-color='#F3D08F'/><stop offset='.5' stop-color='#D9A55B'/><stop offset='1' stop-color='#9A6B2B'/></linearGradient>
 <path id='ring' d='M -292,0 a 292,292 0 1,1 584,0 a 292,292 0 1,1 -584,0'/>
</defs>
<polygon points='{" ".join(pts)}' fill='url(#g)'/>
<circle r='340' fill='#12100D' stroke='url(#g)' stroke-width='6'/>
<circle r='246' fill='none' stroke='rgba(217,165,91,.35)' stroke-width='3'/>
<text font-family='MR' font-weight='700' font-size='34' fill='{GOLD}' letter-spacing='6'>
 <textPath href='#ring' textLength='1790' lengthAdjust='spacing'>ONE PURCHASE · NO SUBSCRIPTION · NO ADS · NO UPSELLS ·</textPath></text>
<text x='0' y='80' text-anchor='middle' font-family='NR' font-size='290' fill='{CREAM}' letter-spacing='-6'>1×</text>
<text x='0' y='160' text-anchor='middle' font-family='MR' font-weight='700' font-size='36' fill='{MUTED}' letter-spacing='7'>PAYMENT</text>
</svg>"""
    seal_css = f"<style>.seal{{position:absolute;left:{(W-960)//2}px;top:740px;width:960px;height:960px;z-index:3;filter:drop-shadow(0 0 120px rgba(217,165,91,.28))}}</style>"
    cards["08-pay-once"] = page(W, H, u,
        copy(X, 118, CW, "Pay once.<br><em>Not every month.</em>",
             "One purchase, every feature. No subscription to cancel.", 134, 40)
        + seal_css + seal, gy="70%")
    return cards, W, H



# ---------------------------------------------------------------- one cikan grafik 1024x500
def feature():
    W, H = 1024, 500
    u = W / 1080
    icon = ("<img src='../source/icon.png' style='position:absolute;left:60px;top:52px;width:68px;height:68px;"
            "border-radius:16px;box-shadow:0 0 0 2px rgba(242,235,221,.18)'>")
    word = (f"<div style='position:absolute;left:144px;top:62px;font-family:NR;font-size:40px;color:{CREAM};"
            f"letter-spacing:.01em'>Limitra</div>")
    text = (f"<div class='copy' style='left:60px;top:170px;width:600px'>"
            f"<h1 style='font-size:62px'>“Just five more<br>minutes.” <em>Not today.</em></h1>"
            f"<p class='sub' style='font-size:23px;margin-top:22px'>Daily app limits that really lock.<br>"
            f"One purchase. No subscription.</p></div>")
    ph = phone("lock.png", 672, 56, 330, top=250, radius=60)
    return {"feature": page(W, H, u, icon + word + text + ph, gx="78%", gy="100%")}, W, H


# ---------------------------------------------------------------- tablet kartlari (5:8)
def tablet_cards(W, H):
    k = W / 1200
    u = W / 1080
    X, CW = 100 * k, 1000 * k
    fs, subfs = 138 * k, 42 * k
    pw = 940 * k
    px = (W - pw) / 2
    py = 660 * k
    cards = {}
    cards["01-five-more-minutes"] = page(W, H, u,
        copy(X, 118 * k, CW, "“Just five<br>more minutes.”<br><em>Not today.</em>",
             "Set a daily limit. When time is up, Limitra locks the app.", fs, subfs)
        + phone("lock.png", px, 800 * k, pw, top=250, radius=62), gy="96%")
    cards["02-set-it-once"] = page(W, H, u,
        copy(X, 118 * k, CW, "Set it once.<br><em>Keep your word.</em>",
             "Pick the apps, a daily limit and the days that count.", fs, subfs)
        + phone("setup_limit.png", px, py, pw, top=96, radius=62), gy="96%")
    cards["03-clean-days"] = page(W, H, u,
        copy(X, 118 * k, CW, "Make every<br>clean day <em>count.</em>",
             "Keep your streak and earn nine animated frames, from day 1 to day 365.", fs, subfs)
        + phone("ach_grid.png", px, py, pw, top=170, radius=62), gy="96%")
    cards["04-whats-left"] = page(W, H, u,
        copy(X, 118 * k, CW, "See what is left<br><em>at a glance.</em>",
             "Time left for every protected app, in one place.", fs, subfs)
        + phone("trackers.png", px, py, pw, top=96, radius=62), gy="96%")
    return cards, W, H


def write_render(name, html, w, h, folder):
    HTML.mkdir(parents=True, exist_ok=True)
    folder.mkdir(parents=True, exist_ok=True)
    f = HTML / f"{name}.html"
    f.write_text(html, encoding="utf-8")
    out = folder / f"{name}.png"
    import time
    ud = Path(__import__("tempfile").gettempdir()) / "limitra_edge_ud"
    out.unlink(missing_ok=True)
    for attempt in range(6):
        subprocess.run([EDGE, "--headless=new", "--disable-gpu", "--no-first-run",
                        f"--user-data-dir={ud}", "--hide-scrollbars",
                        "--force-device-scale-factor=1", f"--window-size={w},{h}",
                        "--virtual-time-budget=4000", f"--screenshot={out}", f.as_uri()],
                       capture_output=True, timeout=90)
        if out.exists():
            break
        time.sleep(2)
    if not out.exists():
        raise RuntimeError(f"Edge screenshot alinamadi: {out}")
    im = Image.open(out).convert("RGB")
    if im.size != (w, h):
        im = im.crop((0, 0, w, h))
    im.save(out)
    return out


if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "phone"
    jobs = []
    if which in ("phone", "all"):
        jobs.append((phone_cards(), "phone"))
    if which in ("feature", "all"):
        jobs.append((feature(), "feature"))
    if which in ("tablet7", "all"):
        jobs.append((tablet_cards(1200, 1920), "tablet7"))
    if which in ("tablet10", "all"):
        jobs.append((tablet_cards(1600, 2560), "tablet10"))
    for (cards, W, H), folder in jobs:
        for n, h in cards.items():
            print(write_render(n, h, W, H, OUT / folder))
