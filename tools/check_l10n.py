"""Uygulama dizelerinin çeviri eksiklerini denetler (values/strings.xml  vs  values-<dil>/strings.xml).

  python tools/check_l10n.py              # 10 dilin özeti
  python tools/check_l10n.py de           # tek dil, ayrıntılı liste
  python tools/check_l10n.py --json out.json

Raporlananlar (dil başına):
  MISSING   : varsayılan dosyada var, dil dosyasında yok  -> Android İngilizceye düşer.
  UNTRANSLATED: dil dosyasında var ama metni İngilizceyle aynı (izin verilenler hariç).
Çıkış kodu: eksik veya çevrilmemiş varsa 1, temizse 0.
"""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
LOCALES = {"ar": "values-ar", "de": "values-de", "es": "values-es", "fr": "values-fr", "hi": "values-hi",
           "id": "values-in", "pt": "values-pt", "ru": "values-ru", "th": "values-th", "tr": "values-tr"}
NON_LATIN = {"ar", "hi", "ru", "th"}      # bu dillerde yazar adları da yazıya çevrilmeli (Seneca -> Сенека)

# Çeviri gerektirmeyen (marka/biçim) anahtarlar
ALLOW_ALWAYS = {"app_name", "dashboard_title", "profile_version_format"}
ALLOW_RE = [re.compile(r"^quote_author_\d+$")]   # Latin yazılı dillerde isimler aynı kalabilir


def load(path):
    out = {}
    for e in ET.parse(path).getroot():
        name = e.get("name")
        if e.get("translatable") == "false" or not name:
            continue
        if e.tag == "string":
            out[name] = "".join(e.itertext()).strip()
        elif e.tag in ("plurals", "string-array"):
            out[name] = "|".join("".join(i.itertext()).strip() for i in e)
    return out


def has_words(text):
    return bool(re.search(r"[A-Za-z]{3,}", text))


def analyse(lang):
    base = load(RES / "values" / "strings.xml")
    loc = load(RES / LOCALES[lang] / "strings.xml")
    missing = [k for k in base if k not in loc]
    untranslated = []
    for k, v in base.items():
        if k not in loc or loc[k] != v or not has_words(v) or k in ALLOW_ALWAYS:
            continue
        if any(r.match(k) for r in ALLOW_RE) and lang not in NON_LATIN:
            continue
        untranslated.append(k)
    return base, missing, untranslated


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    js = sys.argv[sys.argv.index("--json") + 1] if "--json" in sys.argv else None
    if js:
        args = [a for a in args if a != js]
    langs = args or list(LOCALES)
    report, bad = {}, False
    for lang in langs:
        base, missing, untr = analyse(lang)
        report[lang] = {"missing": missing, "untranslated": untr}
        bad |= bool(missing or untr)
        q = [k for k in untr if k.startswith("quote_text_")]
        print(f"{lang}: eksik={len(missing)} çevrilmemiş={len(untr)} (söz={len(q)}, diğer={len(untr) - len(q)})")
        if len(langs) == 1:
            for k in missing:
                print(f"  MISSING      {k} = {base[k][:70]!r}")
            for k in untr:
                print(f"  UNTRANSLATED {k} = {base[k][:70]!r}")
    if js:
        Path(js).write_text(json.dumps(report, ensure_ascii=False, indent=1), encoding="utf-8")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
