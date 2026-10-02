"""Limitra Play Store kartları (v3) - tüm diller için.

Girdi : store_assets/<locale>-v3/source/*.png   (tools/capture_store_screens.py çıktısı)
        tools/store_copy_v3.json                (kart metinleri, dil başına)
Çıktı : store_assets/<locale>-v3/out/...        (ara PNG'ler)
        store_assets/<locale>-v3/play/...       (Play klasör yapısı: phoneScreenshots/1-8.png,
                                                  sevenInchScreenshots/1-4.png, tenInchScreenshots/1-4.png,
                                                  featureGraphic/feature.png)

Kullanım:
  python tools/generate_store_v3.py --locale tr-TR
  python tools/generate_store_v3.py --locale ar --only phone
  python tools/generate_store_v3.py --locale en-US --only feature

Metin taşarsa tarayıcı içinde başlık/alt metin otomatik küçülür (fit()); yine de çıktıyı gözle kontrol et.
"""
import argparse
import json
import math
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / "store_assets" / "store-v3-shared"
COPY = ROOT / "tools" / "store_copy_v3.json"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

BG = "#0F0E0C"
GOLD = "#D9A55B"
CREAM = "#F2EBDD"
MUTED = "#B4AC9B"

# Yazı tipi yığınları. NR=Newsreader (yalnız Latin), MR=Manrope (Latin+Kiril). Diğer yazılar sistem yazı tipine düşer.
SERIF = {
    "default": "NR, 'Times New Roman', serif",
    "ru": "'Times New Roman', Cambria, serif",
    "ar": "'Times New Roman', 'Segoe UI', serif",
    "hi": "'Nirmala UI', 'Segoe UI', serif",
    "th": "'Leelawadee UI', 'Segoe UI', serif",
}
SANS = {
    "default": "MR, 'Segoe UI', sans-serif",
    "ar": "'Segoe UI', Tahoma, sans-serif",
    "hi": "'Nirmala UI', 'Segoe UI', sans-serif",
    "th": "'Leelawadee UI', 'Segoe UI', sans-serif",
}
LINE_H = {"hi": 1.35, "th": 1.35, "ar": 1.25}   # Devanagari/Thai/Arap işaretleri için daha yüksek satır
NO_ITALIC = {"ar", "hi", "th"}   # bu yazılarda italik yok: vurgu yalnız altın renkle


def css(w, h, u, lang, rtl):
    serif = SERIF.get(lang, SERIF["default"])
    sans = SANS.get(lang, SANS["default"])
    em_style = "normal" if lang in NO_ITALIC else "italic"
    fonts = "../../store-v3-shared/fonts"
    return f"""
@font-face{{font-family:NR;src:url('{fonts}/newsreader_regular.ttf');font-weight:400}}
@font-face{{font-family:NR;src:url('{fonts}/newsreader_italic.ttf');font-style:italic;font-weight:400}}
@font-face{{font-family:MR;src:url('{fonts}/manrope_regular.ttf');font-weight:400}}
@font-face{{font-family:MR;src:url('{fonts}/manrope_semibold.ttf');font-weight:600}}
@font-face{{font-family:MR;src:url('{fonts}/manrope_bold.ttf');font-weight:700}}
*{{box-sizing:border-box;margin:0;padding:0}}
html,body{{width:{w}px;height:{h}px;overflow:hidden;background:{BG}}}
body{{position:relative;font-family:{sans};color:{CREAM};-webkit-font-smoothing:antialiased}}
.glow{{position:absolute;inset:0;background:
  radial-gradient({1200 * u}px {900 * u}px at var(--gx,50%) var(--gy,100%),rgba(217,165,91,.20),transparent 68%)}}
.copy{{position:absolute;z-index:5}}
h1{{font-family:{serif};font-weight:400;color:{CREAM};letter-spacing:{0 if lang in NO_ITALIC else '-.018em'};line-height:{LINE_H.get(lang, 1.0)}}}
h1 em{{font-style:{em_style};color:{GOLD}}}
p.sub{{font-family:{sans};font-weight:400;color:{MUTED};line-height:1.42}}
.phone{{position:absolute;overflow:hidden;background:#000;
  box-shadow:0 0 {280 * u}px rgba(217,165,91,.22),0 {40 * u}px {120 * u}px rgba(0,0,0,.85)}}
.phone::after{{content:'';position:absolute;inset:0;border-radius:inherit;pointer-events:none;
  border:{4 * u}px solid rgba(242,235,221,.26)}}
.phone img{{display:block}}
.wordmark{{position:absolute;top:62px;font-family:{serif};font-size:40px;color:{CREAM};letter-spacing:.01em}}
"""


FIT_JS = """
<script>
function fit(){
  const copy=document.querySelector('.copy'); if(!copy) return;
  const h1=copy.querySelector('h1'), sub=copy.querySelector('p.sub');
  const limit=parseFloat(document.body.dataset.limit||'99999');
  const maxLines=parseInt(document.body.dataset.maxlines||'3');
  let fs=parseFloat(getComputedStyle(h1).fontSize), sfs=parseFloat(getComputedStyle(sub).fontSize);
  const lh=parseFloat(document.body.dataset.lh||'1');
  const minfs=parseFloat(document.body.dataset.minfs||'64');
  const lines=()=>Math.round(h1.getBoundingClientRect().height/(parseFloat(getComputedStyle(h1).fontSize)*lh));
  const wide=()=>h1.scrollWidth>h1.clientWidth+1||sub.scrollWidth>sub.clientWidth+1;
  let guard=0;
  while((lines()>maxLines||wide()||copy.getBoundingClientRect().bottom>limit)&&guard++<40){
    if(fs>minfs){fs-=2;h1.style.fontSize=fs+'px';}
    else if(sfs>28){sfs-=2;sub.style.fontSize=sfs+'px';}
    else break;
  }
  document.body.dataset.fitted=Math.round(fs)+'/'+Math.round(sfs);
}
document.fonts.ready.then(()=>setTimeout(fit,50));
</script>
"""


def page(w, h, u, body, lang, rtl, gx="50%", gy="100%", limit=None, maxlines=3, minfs=64):
    d = " dir='rtl'" if rtl else ""
    return (f"<!doctype html><html lang='{lang}'{d}><head><meta charset='utf-8'>"
            f"<style>{css(w, h, u, lang, rtl)}</style></head>"
            f"<body style='--gx:{gx};--gy:{gy}' data-limit='{limit or 99999}' data-maxlines='{maxlines}' data-lh='{LINE_H.get(lang, 1.0)}' data-minfs='{minfs}'>"
            f"<div class='glow'></div>{body}{FIT_JS}</body></html>")


def auto_top(src_dir, src, margin=45):
    """Kilit ekranı içeriği dile göre dikey kayar: ilk içerik satırının margin px üstünden kırp."""
    im = Image.open(src_dir / src).convert("L")
    box = im.crop((0, 90, im.width, im.height)).point(lambda v: 255 if v > 60 else 0).getbbox()
    return max(0, 90 + (box[1] if box else 160) - margin)


def auto_top_grid(src_dir, src, margin=70):
    """Başarılar ekranında kaydırma dile göre ±birkaç satır oynar: ızgara başlığının margin px üstünden kırp."""
    blue = Image.open(src_dir / src).convert("RGB").split()[2]   # krem metin parlak, altın ilerleme çubuğu (B~90) değil
    box = blue.crop((0, 130, 700, 700)).point(lambda v: 255 if v > 170 else 0).getbbox()
    return max(0, 130 + (box[1] if box else 110) - margin)


def phone(src_dir, src, x, y, w, top=0, bottom=None, radius=60, h=None):
    if top == "auto":
        top = auto_top(src_dir, src)
    elif top == "auto_grid":
        top = auto_top_grid(src_dir, src)
    sw, sh = Image.open(src_dir / src).size
    sc = w / sw
    ch = ((bottom or sh) - top) * sc
    return (f"<div class='phone' style='left:{x}px;top:{y}px;width:{w}px;height:{h or ch}px;"
            f"border-radius:{radius * sc}px'>"
            f"<img src='../source/{src}' style='width:{w}px;margin-top:-{top * sc}px'></div>")


SUB_SCALE = {"th": 1.15, "hi": 1.05}


def copy_block(x, y, w, c, fs, subfs, rtl, gap=34, lang=None):
    side = f"right:{x}px" if rtl else f"left:{x}px"
    return (f"<div class='copy' style='{side};top:{y}px;width:{w}px;text-align:{'right' if rtl else 'left'}'>"
            f"<h1 style='font-size:{fs}px'>{c['h1']}</h1>"
            f"<p class='sub' style='font-size:{subfs}px;margin-top:{gap}px;max-width:{w * .94}px'>{c['sub']}</p></div>")


# ------------------------------------------------------------------ kart ailesi
def ledger_and_seal(L, W, H, u, rtl, lang):
    cards = {}
    X, CW = 84, 912
    c7 = L["cards"]["07"]
    rows = "".join(f"<div class='row'><span class='lbl'>{r}</span><span class='dots'></span>"
                   f"<span class='none'>{c7['none']}</span></div>" for r in c7["rows"])
    em = "normal" if lang in NO_ITALIC else "italic"
    serif = SERIF.get(lang, SERIF["default"])
    sans = SANS.get(lang, SANS["default"])
    side = "right" if rtl else "left"
    css7 = f"""<style>
.ledger{{position:absolute;{side}:84px;top:690px;width:912px;z-index:3}}
.row{{display:flex;align-items:baseline;gap:22px;padding:56px 0;border-bottom:2px solid rgba(242,235,221,.14)}}
.row:first-child{{border-top:2px solid rgba(242,235,221,.14)}}
.lbl{{font-family:{serif};font-size:80px;line-height:1.05;color:{CREAM};letter-spacing:-.01em}}
.dots{{flex:1;border-bottom:4px dotted rgba(242,235,221,.22);transform:translateY(-10px)}}
.none{{font-family:{serif};font-style:{em};font-size:80px;line-height:1.05;color:{GOLD}}}
.pill{{position:absolute;{side}:84px;top:1640px;z-index:3;display:flex;align-items:center;gap:24px;
  padding:30px 44px;border:3px solid rgba(217,165,91,.55);border-radius:999px;
  font-family:{sans};font-weight:600;font-size:38px;color:{CREAM}}}
.pill svg{{width:52px;height:52px}}
</style>"""
    shield = (f"<svg viewBox='0 0 24 24' fill='none' stroke='{GOLD}' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'>"
              "<path d='M12 3 4.5 6v5.5c0 4.3 3 7.7 7.5 9.5 4.5-1.8 7.5-5.2 7.5-9.5V6L12 3z'/><path d='m8.6 12.2 2.4 2.4 4.4-4.8'/></svg>")
    body = (copy_block(X, 118, CW, c7, 134, 40, rtl) + css7 + f"<div class='ledger'>{rows}</div>"
            f"<div class='pill'>{shield}{c7['pill']}</div>")
    cards["07-stays-on-device"] = page(W, H, u, body, lang, rtl, gy="78%", limit=670)

    c8 = L["cards"]["08"]
    teeth = 44
    pts = []
    for i in range(teeth * 2):
        r = 392 if i % 2 == 0 else 362
        a = math.pi * i / teeth
        pts.append(f"{r * math.cos(a):.1f},{r * math.sin(a):.1f}")
    ring_font = serif if lang in NO_ITALIC else sans
    # Arap/Hint/Tay yazılarında harf aralığı bozulmasın: tam halka yerine üst yay ortalanır.
    ring_attrs = "startOffset='25%' text-anchor='middle'" if lang in NO_ITALIC else "textLength='1790' lengthAdjust='spacing'"
    seal = f"""<svg class='seal' viewBox='-420 -420 840 840' xmlns='http://www.w3.org/2000/svg'>
<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
  <stop offset='0' stop-color='#F3D08F'/><stop offset='.5' stop-color='#D9A55B'/><stop offset='1' stop-color='#9A6B2B'/></linearGradient>
 <path id='ring' d='M -292,0 a 292,292 0 1,1 584,0 a 292,292 0 1,1 -584,0'/></defs>
<polygon points='{" ".join(pts)}' fill='url(#g)'/>
<circle r='340' fill='#12100D' stroke='url(#g)' stroke-width='6'/>
<circle r='246' fill='none' stroke='rgba(217,165,91,.35)' stroke-width='3'/>
<text font-family="{ring_font}" font-weight='700' font-size='{c8.get('ring_size', 34)}' fill='{GOLD}' letter-spacing='{0 if lang in NO_ITALIC else 6}'>
 <textPath href='#ring' {ring_attrs}>{c8['seal_ring']}</textPath></text>
<text x='0' y='80' text-anchor='middle' direction='ltr' unicode-bidi='bidi-override' font-family="{serif}" font-size='290' fill='{CREAM}' letter-spacing='-6'>{c8['seal_big']}</text>
<text x='0' y='160' text-anchor='middle' font-family="{sans}" font-weight='700' font-size='36' fill='{MUTED}' letter-spacing='{0 if lang in NO_ITALIC else 7}'>{c8['seal_small']}</text>
</svg>"""
    seal_css = (f"<style>.seal{{position:absolute;left:{(W - 960) // 2}px;top:740px;width:960px;height:960px;z-index:3;"
                f"filter:drop-shadow(0 0 120px rgba(217,165,91,.28))}}</style>")
    cards["08-pay-once"] = page(W, H, u, copy_block(X, 118, CW, c8, 134, 40, rtl) + seal_css + seal,
                                lang, rtl, gy="70%", limit=720)
    return cards


def phone_cards(L, src_dir, lang, rtl):
    W, H, u = 1080, 1920, 1.0
    X, CW = 84, 912
    C = L["cards"]
    cards = {}
    cards["01-five-more-minutes"] = page(W, H, u,
        copy_block(X, 118, CW, C["01"], 134, 40, rtl) + phone(src_dir, "lock.png", 100, 800, 880, top="auto"),
        lang, rtl, gy="96%", limit=770)
    spec = [("02-set-it-once", "02", "setup_limit.png", 96), ("03-clean-days", "03", "ach_grid.png", "auto_grid"),
            ("04-whats-left", "04", "trackers.png", 96), ("05-only-when-it-matters", "05", "setup_schedule.png", 396),
            ("06-on-record", "06", "timeline.png", 96)]
    for name, k, img, top in spec:
        cards[name] = page(W, H, u,
            copy_block(X, 118, CW, C[k], 134, 40, rtl) + phone(src_dir, img, 90, 660, 900, top=top),
            lang, rtl, gy="96%", limit=630)
    cards.update(ledger_and_seal(L, W, H, u, rtl, lang))
    return cards, W, H


def feature(L, src_dir, lang, rtl):
    W, H = 1024, 500
    u = W / 1080
    f = L["feature"]
    icon_x = "right:60px" if rtl else "left:60px"
    icon = (f"<img src='../../store-v3-shared/icon.png' style='position:absolute;{icon_x};top:52px;width:68px;height:68px;"
            f"border-radius:16px;box-shadow:0 0 0 2px rgba(242,235,221,.18)'>")
    wx = "right:144px" if rtl else "left:144px"
    word = f"<div class='wordmark' style='{wx}'>Limitra</div>"
    side = "right:60px" if rtl else "left:60px"
    al = "right" if rtl else "left"
    text = (f"<div class='copy' style='{side};top:170px;width:600px;text-align:{al}'>"
            f"<h1 style='font-size:62px'>{f['h1']}</h1>"
            f"<p class='sub' style='font-size:23px;margin-top:22px'>{f['sub']}</p></div>")
    px = 24 if rtl else 672
    ph = phone(src_dir, "lock.png", px, 56, 330, top="auto")
    return {"feature": page(W, H, u, icon + word + text + ph, lang, rtl, gx="78%", gy="100%", limit=470, maxlines=2, minfs=34)}, W, H


def tablet_cards(L, src_dir, lang, rtl, W, H):
    k = W / 1200
    u = W / 1080
    C = L["cards"]
    X, CW = 100 * k, 1000 * k
    fs, subfs = 138 * k, 42 * k
    pw = 940 * k
    px = (W - pw) / 2
    cards = {}
    cards["01-five-more-minutes"] = page(W, H, u,
        copy_block(X, 118 * k, CW, C["01"], fs, subfs, rtl) + phone(src_dir, "lock.png", px, 800 * k, pw, top="auto"),
        lang, rtl, gy="96%", limit=770 * k)
    for name, kk, img, top in [("02-set-it-once", "02", "setup_limit.png", 96), ("03-clean-days", "03", "ach_grid.png", "auto_grid"),
                               ("04-whats-left", "04", "trackers.png", 96)]:
        cards[name] = page(W, H, u,
            copy_block(X, 118 * k, CW, C[kk], fs, subfs, rtl) + phone(src_dir, img, px, 660 * k, pw, top=top),
            lang, rtl, gy="96%", limit=630 * k)
    return cards, W, H


# ------------------------------------------------------------------ çıktı
def render(name, html, w, h, work, outdir):
    work.mkdir(parents=True, exist_ok=True)
    outdir.mkdir(parents=True, exist_ok=True)
    f = work / f"{name}.html"
    f.write_text(html, encoding="utf-8")
    out = outdir / f"{name}.png"
    ud = Path(tempfile.gettempdir()) / "limitra_edge_ud"
    out.unlink(missing_ok=True)
    for _ in range(6):
        subprocess.run([EDGE, "--headless=new", "--disable-gpu", "--no-first-run", f"--user-data-dir={ud}",
                        "--hide-scrollbars", "--force-device-scale-factor=1", f"--window-size={w},{h}",
                        "--virtual-time-budget=6000", f"--screenshot={out}", f.as_uri()],
                       capture_output=True, timeout=120)
        if out.exists():
            break
        time.sleep(2)
    if not out.exists():
        raise RuntimeError(f"Edge ekran görüntüsü alamadı: {name}")
    im = Image.open(out).convert("RGB")
    if im.size != (w, h):
        im = im.crop((0, 0, w, h))
    im.save(out)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--locale", required=True)
    ap.add_argument("--only", default="all", help="phone|tablet7|tablet10|feature|all")
    ap.add_argument("--deck", default=str(COPY), help="metin dosyası (varsayılan tools/store_copy_v3.json)")
    ap.add_argument("--source-from", default=None, help="test için başka locale'in source klasörünü kullan")
    a = ap.parse_args()
    deck = json.loads(Path(a.deck).read_text(encoding="utf-8"))
    if a.locale not in deck:
        sys.exit(f"{COPY.name} içinde '{a.locale}' yok")
    L = deck[a.locale]
    if str(L.get("_status", "")).upper().startswith("TODO"):
        sys.exit(f"{a.locale}: metinler henüz çevrilmedi (_status={L['_status']!r}). Çeviriyi yapıp _status alanını sil.")
    lang, rtl = L["lang"], bool(L.get("rtl", False))
    base = ROOT / "store_assets" / f"{a.locale}-v3"
    src_dir, work, out = base / "source", base / "html", base / "out"
    if a.source_from:
        src_dir = ROOT / "store_assets" / f"{a.source_from}-v3" / "source"
    need = ["lock.png", "setup_limit.png", "setup_schedule.png", "trackers.png", "timeline.png", "ach_grid.png"]
    miss = [n for n in need if not (src_dir / n).exists()]
    if miss:
        sys.exit(f"source eksik: {miss} -> önce tools/capture_store_screens.py --locale {a.locale}")
    jobs = []
    if a.only in ("phone", "all"):
        jobs.append((phone_cards(L, src_dir, lang, rtl), "phone", "phoneScreenshots"))
    if a.only in ("feature", "all"):
        jobs.append((feature(L, src_dir, lang, rtl), "feature", "featureGraphic"))
    if a.only in ("tablet7", "all"):
        jobs.append((tablet_cards(L, src_dir, lang, rtl, 1200, 1920), "tablet7", "sevenInchScreenshots"))
    if a.only in ("tablet10", "all"):
        jobs.append((tablet_cards(L, src_dir, lang, rtl, 1600, 2560), "tablet10", "tenInchScreenshots"))
    play = base / "play"
    for (cards, W, H), folder, play_dir in jobs:
        (play / play_dir).mkdir(parents=True, exist_ok=True)
        for i, (n, h) in enumerate(cards.items(), 1):
            p = render(n, h, W, H, work, out / folder)
            target = play / play_dir / ("feature.png" if play_dir == "featureGraphic" else f"{i}.png")
            Image.open(p).save(target)
            print(target)


if __name__ == "__main__":
    main()
