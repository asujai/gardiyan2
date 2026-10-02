# -*- coding: utf-8 -*-
"""Applies translations to strings.xml for all 9 target languages.
Ensures UTF-8 encoding without BOM, preserves XML formatting.
"""
import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

from check_l10n import LOCALES, NON_LATIN
from translations_data import MISSING_21, AUTHORS_TRANSLATION, OTHER_UNTRANSLATED, QUOTES_54

RES = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
BASE_FILE = RES / "values" / "strings.xml"

# Parse base to know author for each quote
base_tree = ET.parse(BASE_FILE)
quote_authors = {}
for i in range(1, 55):
    elem = base_tree.find(f".//string[@name='quote_author_{i}']")
    if elem is not None:
        quote_authors[i] = elem.text.strip()

print("Base quote authors loaded:", len(quote_authors))

def update_xml_content(content, key, new_val):
    # Regex to replace existing <string name="KEY">...</string>
    pattern = rf'(<string\s+name="{re.escape(key)}">)(.*?)(</string>)'
    if re.search(pattern, content):
        return re.sub(pattern, rf'\g<1>{new_val}\g<3>', content)
    return content

def insert_after_tag(content, anchor_key, new_xml_lines):
    # Find closing tag </string> for anchor_key
    pattern = rf'(<string\s+name="{re.escape(anchor_key)}">.*?</string>)'
    m = re.search(pattern, content)
    if not m:
        raise ValueError(f"Anchor key '{anchor_key}' not found in content!")
    insert_pos = m.end()
    insertion = "\n" + new_xml_lines.rstrip()
    return content[:insert_pos] + insertion + content[insert_pos:]

for lang, folder in LOCALES.items():
    if lang == "tr":
        continue  # tr already handled
    xml_path = RES / folder / "strings.xml"
    content = xml_path.read_text(encoding="utf-8")
    orig_content = content
    print(f"Processing {lang} ({folder})...")

    # 1. Insert 21 missing keys if not present
    m21 = MISSING_21[lang]
    # Cluster 1: after setup_target_add
    c1_keys = [
        "setup_target_restriction_name",
        "setup_target_restriction_name_optional",
        "setup_target_restriction_name_placeholder",
        "setup_target_error_no_name",
        "protected_apps_time_left",
        "protected_apps_usage_progress",
    ]
    c1_lines = ""
    for k in c1_keys:
        if f'name="{k}"' not in content:
            c1_lines += f'    <string name="{k}">{m21[k]}</string>\n'
    if c1_lines:
        content = insert_after_tag(content, "setup_target_add", c1_lines)

    # Cluster 2: after setup_target_days
    c2_keys = [
        "setup_target_active_window",
        "setup_target_active_window_desc",
        "setup_target_active_window_toggle",
        "setup_target_start_time",
        "setup_target_end_time",
        "protected_group_apps",
        "protected_group_app_count",
        "protected_group_active",
        "protected_group_scheduled",
        "protected_group_all_day",
        "protected_group_every_day",
    ]
    c2_lines = ""
    for k in c2_keys:
        if f'name="{k}"' not in content:
            c2_lines += f'    <string name="{k}">{m21[k]}</string>\n'
    if c2_lines:
        content = insert_after_tag(content, "setup_target_days", c2_lines)

    # Cluster 3: failsafe keys
    if 'name="perm_state_failsafe"' not in content:
        line = f'    <string name="perm_state_failsafe">{m21["perm_state_failsafe"]}</string>\n'
        content = insert_after_tag(content, "perm_state_reenable", line)

    if 'name="perm_accessibility_failsafe_desc"' not in content:
        line = f'    <string name="perm_accessibility_failsafe_desc">{m21["perm_accessibility_failsafe_desc"]}</string>\n'
        content = insert_after_tag(content, "perm_accessibility_reenable_desc", line)

    if 'name="accessibility_failsafe_warning"' not in content:
        line = f'    <string name="accessibility_failsafe_warning">{m21["accessibility_failsafe_warning"]}</string>\n'
        content = insert_after_tag(content, "accessibility_reenable_warning", line)

    # Cluster 4: profile_privacy_choices
    if 'name="profile_privacy_choices"' not in content:
        line = f'    <string name="profile_privacy_choices">{m21["profile_privacy_choices"]}</string>\n'
        content = insert_after_tag(content, "profile_notifications_desc", line)

    # 2. Update OTHER_UNTRANSLATED
    if lang in OTHER_UNTRANSLATED:
        for k, v in OTHER_UNTRANSLATED[lang].items():
            content = update_xml_content(content, k, v)

    # 3. Update Quotes 1..54 (for de, es, fr, pt, id, ru, hi, ar)
    for q_idx in range(1, 55):
        if q_idx in QUOTES_54 and lang in QUOTES_54[q_idx]:
            k = f"quote_text_{q_idx}"
            v = QUOTES_54[q_idx][lang]
            content = update_xml_content(content, k, v)

    # 4. Update Authors for NON_LATIN (ru, ar, hi, th)
    if lang in NON_LATIN and lang in AUTHORS_TRANSLATION:
        auth_map = AUTHORS_TRANSLATION[lang]
        for q_idx in range(1, 55):
            ak = f"quote_author_{q_idx}"
            orig_author = quote_authors.get(q_idx)
            if orig_author and orig_author in auth_map:
                new_author = auth_map[orig_author]
                content = update_xml_content(content, ak, new_author)

    # Write back if changed
    if content != orig_content:
        xml_path.write_text(content, encoding="utf-8")
        print(f"Updated {xml_path}")
    else:
        print(f"No changes for {xml_path}")

print("All translations applied.")
