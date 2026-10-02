"""Mağaza v3 çıktı denetimi (Antigravity kendi kontrolü için, Claude teslim denetimi için kullanır).

  python tools/validate_store_v3.py                 # 11 dilin tamamı
  python tools/validate_store_v3.py de-DE tr-TR     # seçili diller

Kontroller (dil başına):
  1. play/ klasörü: phone 8, sevenInch 4, tenInch 4, featureGraphic 1; boyutlar, RGB (alfa yok), < 8 MB
  2. store_copy_v3.json: _status TODO değil; en-US dışında metin İngilizceyle aynı değil;
     Latin olmayan dillerde (ru, ar, hi, th) başlıklarda İngilizce harf yok ("Limitra" hariç)
  3. source/ ekranları var ve play/ kartlarından yeni (kaynak değişmişse kartlar yeniden üretilmeli)
  4. metadata/<locale>: title <= 30, short <= 80, full <= 4000; mojibake yok; "Limitra" geçiyor
  5. release_notes: store_assets/<locale>-v3/release_notes.txt <= 500 karakter
Çıkış kodu: hata varsa 1.
"""
import json
import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "store_assets"
LOCALES = ["en-US", "tr-TR", "de-DE", "es-ES", "fr-FR", "pt-BR", "id", "ru-RU", "hi-IN", "th", "ar"]
EXPECT = {"phoneScreenshots": (8, (1080, 1920)), "sevenInchScreenshots": (4, (1200, 1920)),
          "tenInchScreenshots": (4, (1600, 2560)), "featureGraphic": (1, (1024, 500))}
NON_LATIN = {"ru-RU", "ar", "hi-IN", "th"}
BAD_MOJIBAKE = ["Ã", "â€", "Ä±", "Å\u009f", "�"]


def strip_tags(s):
    return re.sub(r"<[^>]+>", " ", s)


def check_locale(loc, deck, en):
    errs, warns = [], []
    base = ASSETS / f"{loc}-v3"
    # 1 görseller
    for folder, (count, size) in EXPECT.items():
        d = base / "play" / folder
        files = sorted(d.glob("*.png")) if d.exists() else []
        if len(files) != count:
            errs.append(f"play/{folder}: {len(files)} dosya (beklenen {count})")
            continue
        for f in files:
            im = Image.open(f)
            if im.size != size:
                errs.append(f"{f.name} boyut {im.size} != {size}")
            if im.mode != "RGB":
                errs.append(f"{folder}/{f.name} mod {im.mode} (RGB olmalı, alfa yok)")
            if f.stat().st_size > 8_000_000:
                errs.append(f"{folder}/{f.name} 8 MB'ı aşıyor")
    # 2 metin destesi
    L = deck.get(loc)
    if not L:
        errs.append("store_copy_v3.json'da yok")
    else:
        if str(L.get("_status", "")).upper().startswith("TODO"):
            errs.append(f"_status={L['_status']!r}")
        if loc != "en-US":
            same = []
            for k, c in L["cards"].items():
                for f_, v in c.items():
                    ev = en["cards"][k].get(f_)
                    if isinstance(v, str) and v == ev and f_ not in ("seal_big",):
                        same.append(f"{k}.{f_}")
                    if isinstance(v, list) and v == ev:
                        same.append(f"{k}.{f_}")
            if L["feature"] == en["feature"]:
                same.append("feature")
            if same:
                errs.append(f"İngilizceyle aynı metin: {same}")
        if loc in NON_LATIN:
            for k, c in L["cards"].items():
                for f_ in ("h1", "sub", "pill", "seal_ring", "seal_small"):
                    t = strip_tags(c.get(f_, ""))
                    if re.search(r"[A-Za-z]{2,}", t.replace("Limitra", "")):
                        errs.append(f"Latin harf kalmış: {k}.{f_}: {t[:50]!r}")
    # 3 kaynak ekran tazeliği
    src = base / "source"
    newest_src = max((p.stat().st_mtime for p in src.glob("*.png")), default=0)
    oldest_play = min((p.stat().st_mtime for p in (base / "play").rglob("*.png")), default=0)
    if not newest_src:
        errs.append("source/ boş")
    elif oldest_play < newest_src - 1:
        warns.append("source ekranları play kartlarından yeni: kartları yeniden üretin")
    # 4 listeleme metinleri
    meta = ROOT / "metadata" / loc
    lim = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}
    for fn, mx in lim.items():
        p = meta / fn
        if not p.exists():
            errs.append(f"metadata/{loc}/{fn} yok")
            continue
        t = p.read_text(encoding="utf-8").strip()
        if len(t) > mx:
            errs.append(f"{fn}: {len(t)} > {mx}")
        if any(m in t for m in BAD_MOJIBAKE):
            errs.append(f"{fn}: bozuk karakter (mojibake)")
        if fn == "full_description.txt" and "Limitra" not in t:
            warns.append(f"{fn}: 'Limitra' geçmiyor")
    # 5 sürüm notu
    rn = base / "release_notes.txt"
    if not rn.exists():
        errs.append("release_notes.txt yok")
    else:
        t = rn.read_text(encoding="utf-8").strip()
        if len(t) > 500:
            errs.append(f"release_notes {len(t)} > 500")
        if any(m in t for m in BAD_MOJIBAKE):
            errs.append("release_notes: mojibake")
    return errs, warns


def main():
    locs = sys.argv[1:] or LOCALES
    deck = json.loads((ROOT / "tools" / "store_copy_v3.json").read_text(encoding="utf-8"))
    en = deck["en-US"]
    bad = False
    for loc in locs:
        errs, warns = check_locale(loc, deck, en)
        status = "OK " if not errs else "HATA"
        print(f"[{status}] {loc}")
        for e in errs:
            print("   -", e)
        for w in warns:
            print("   ~", w)
        bad |= bool(errs)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
