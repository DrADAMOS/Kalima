#!/usr/bin/env python3
"""Audit and clean raw 300k dataset into a high-precision, offline-usable Arabic guess dictionary (250,000+ words).
"""
from __future__ import annotations

import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
GUESS_ASSET = ASSET_DIR / "arabic-guess-words.txt"
RAW_FILE = ROOT / "tools" / "raw_300k_arabic_words.txt"
DICTIONARY_KT = ROOT / "app" / "src" / "main" / "java" / "com" / "adamos" / "kalima" / "data" / "Dictionary.kt"
REMOTE_JSON = ROOT / "dictionary" / "remote-dictionary-v1.json"

ARABIC_RE = re.compile(r"^[\u0621-\u064A]+$")
DIACRITICS_RE = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")
UNDOUBLEABLE_LETTERS = set("اىةءؤئ") # Note: و and ي CAN be doubled (e.g. طاووس, تخييم, حيوي)
ALLOWED_HAMZA_ENDS = {"اء", "وء", "يء", "فء", "بء", "زء", "دء", "كء", "رء", "نء", "لء", "طء", "ثء"}


def normalize(word: str) -> str:
    """Normalize Arabic word following Dictionary.normalize()."""
    word = unicodedata.normalize("NFC", word).replace("ـ", "").strip()
    word = DIACRITICS_RE.sub("", word)
    return word.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ة": "ه"}))


def clean_display(word: str) -> str:
    """Clean display spelling (remove diacritics/tatweel, retain ة)."""
    word = unicodedata.normalize("NFC", word).replace("ـ", "").strip()
    return DIACRITICS_RE.sub("", word)


def is_valid_orthography(raw: str) -> bool:
    """Check if word is valid Arabic orthography."""
    display = clean_display(raw)
    norm = normalize(raw)

    if not (3 <= len(norm) <= 8 and bool(ARABIC_RE.fullmatch(norm))):
        return False

    # Check for tripled letters
    if any(norm[i] == norm[i + 1] == norm[i + 2] for i in range(len(norm) - 2)):
        return False

    # Check for undoubleable letters
    for i in range(len(norm) - 1):
        if norm[i] == norm[i + 1] and norm[i] in UNDOUBLEABLE_LETTERS:
            return False

    # Check hamza rules
    hamza_idx = norm.find("ء")
    if hamza_idx >= 0:
        if hamza_idx == 0:
            return False
        if norm.find("ء", hamza_idx + 1) >= 0:
            return False
        is_end = (hamza_idx == len(norm) - 1)
        prev_char = norm[hamza_idx - 1]
        is_preceded_by_valid = prev_char in set("اويفبدزركنلطث")
        if not is_end and not is_preceded_by_valid:
            return False

    return True


def audit_and_build():
    if not RAW_FILE.exists():
        print(f"Error: Raw file {RAW_FILE} not found.", file=sys.stderr)
        return

    with RAW_FILE.open("r", encoding="utf-8") as f:
        raw_lines = f.readlines()

    original_raw_count = len(raw_lines)
    non_empty_lines = [line.strip() for line in raw_lines if line.strip()]
    non_empty_count = len(non_empty_lines)
    unique_raw_entries = set(non_empty_lines)
    unique_raw_count = len(unique_raw_entries)

    seen_norm: set[str] = set()
    accepted_display: list[str] = []
    rejected_entries: list[str] = []

    # Curated words to always keep
    curated_raw: set[str] = set()
    if DICTIONARY_KT.exists():
        matches = re.findall(r'"([\u0600-\u06FF]+)"', DICTIONARY_KT.read_text(encoding="utf-8"))
        for m in matches:
            disp = clean_display(m)
            norm = normalize(m)
            if 3 <= len(norm) <= 8:
                curated_raw.add(disp)

    for entry in non_empty_lines:
        disp = clean_display(entry)
        norm = normalize(entry)
        if norm in seen_norm:
            continue
        if is_valid_orthography(entry):
            seen_norm.add(norm)
            accepted_display.append(disp)
        else:
            rejected_entries.append(entry)

    # Ensure curated words are included
    for c in curated_raw:
        norm = normalize(c)
        if norm not in seen_norm and 3 <= len(norm) <= 8:
            seen_norm.add(norm)
            accepted_display.append(c)

    sorted_accepted = sorted(accepted_display, key=lambda w: (normalize(w), w))

    # Length breakdown
    length_counts = defaultdict(int)
    for w in sorted_accepted:
        length_counts[len(normalize(w))] += 1

    # Write final output asset
    GUESS_ASSET.parent.mkdir(parents=True, exist_ok=True)
    GUESS_ASSET.write_text("\n".join(sorted_accepted) + "\n", encoding="utf-8", newline="\n")

    print("=== AUDIT REPORT ===")
    print(f"1. Original Raw Line Count:    {original_raw_count:,}")
    print(f"2. Non-Empty Entry Count:      {non_empty_count:,}")
    print(f"3. Unique Raw Entries:          {unique_raw_count:,}")
    print(f"4. Unique Normalized Entries:   {len(seen_norm):,}")
    print(f"5. Accepted Clean Entries:     {len(sorted_accepted):,}")
    print(f"6. Rejected Entries:           {len(rejected_entries):,}")
    print("\n=== LENGTH BREAKDOWN (Normalized) ===")
    for length in sorted(length_counts.keys()):
        print(f"  - Length {length}: {length_counts[length]:,} words")
    print(f"\nFinal packaged asset {GUESS_ASSET} written with {len(sorted_accepted):,} entries.")


if __name__ == "__main__":
    audit_and_build()
