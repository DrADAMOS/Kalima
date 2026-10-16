#!/usr/bin/env python3
"""Clean existing app/src/main/assets/arabic-guess-words.txt in-place using High Precision Base Word filtering and frequency ranking.

Usage:
  python tools/clean_existing_guess_list.py [--max-words 45000] [--freq tools/freq_arabic_words.txt]
"""
from __future__ import annotations

import argparse
import json
import random
import re
import unicodedata
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
GUESS_FILE = ASSET_DIR / "arabic-guess-words.txt"
DICTIONARY_KT = ROOT / "app" / "src" / "main" / "java" / "com" / "adamos" / "kalima" / "data" / "Dictionary.kt"
REMOTE_JSON = ROOT / "dictionary" / "remote-dictionary-v1.json"
FREQ_FILE_DEFAULT = ROOT / "tools" / "freq_arabic_words.txt"

ARABIC_RE = re.compile(r"^[\u0621-\u064A]+$")
DIACRITICS_RE = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")
REQUIRED_REGRESSION_WORDS = {"جحش", "سلب", "رقى", "رقي", "سماء", "مدرسة", "كتاب", "دفء", "ماء"}

UNDOUBLEABLE_LETTERS = set("اىةهءؤئوي")
ALLOWED_HAMZA_ENDS = {"اء", "وء", "يء", "فء", "بء", "زء", "دء", "كء", "رء", "نء", "لء", "طء", "ثء"}

INFLECTED_SUFFIXES = (
    "تكما", "تهم", "تكم", "تها", "تكن", "تما", "تن",
    "هما", "كما", "هم", "هن", "كم", "كن", "نا", "ني", "ها", "ك", "ه", "ي", "وا"
)

FUNCTION_WORDS_BLOCKLIST = {
    "لهم", "بهم", "منهم", "عنهم", "إليهم", "عليهم", "فيها", "فيه", "فيهم",
    "منها", "عنها", "إليها", "عليها", "لكم", "بكم", "منكم", "عنكم", "إليكم", "عليكم",
    "لنع", "اهل", "كلهم", "بعضهم", "غيرهم"
}

NATIVE_PREFIX_EXCEPTIONS = {
    "ورد", "ولد", "وزن", "وفى", "وعد", "وقد", "وجد", "وجه", "وطن", "وقت",
    "فيل", "فأر", "فهد", "فصل", "فكر", "فرح", "فوز", "فجر", "فن", "فرن",
    "باب", "بيت", "بحر", "برق", "بنت", "برج", "بئر", "بومة", "بذور",
    "ليل", "لحم", "لبن", "لون", "لعبة", "لسان", "لؤلؤ", "لطيف",
    "سمك", "سماء", "سحاب", "سيف", "سوق", "سفينة", "سيارة", "ساعة"
}


def normalize_for_matching(word: str) -> str:
    """Normalize Arabic word for dictionary comparison and matching."""
    word = unicodedata.normalize("NFC", word).replace("ـ", "").strip()
    word = DIACRITICS_RE.sub("", word)
    return word.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ة": "ه"}))


def clean_display_spelling(word: str) -> str:
    """Clean word while preserving standard display spelling (e.g. ta marbuta ة)."""
    word = unicodedata.normalize("NFC", word).replace("ـ", "").strip()
    return DIACRITICS_RE.sub("", word)


def load_freq_file(path: Path | None) -> dict[str, int]:
    """Load word frequency list if available."""
    freqs: dict[str, int] = {}
    if path is None or not path.exists():
        return freqs
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) >= 2 and parts[1].isdigit():
                freqs[clean_display_spelling(parts[0])] = int(parts[1])
            elif len(parts) == 1:
                freqs[clean_display_spelling(parts[0])] = freqs.get(clean_display_spelling(parts[0]), 0) + 1
    return freqs


def load_curated_data() -> tuple[set[str], set[str], Counter, Counter]:
    """Extract curated words, bigram frequencies, and trigram frequencies."""
    curated_norm: set[str] = set()
    curated_raw: set[str] = set()

    if DICTIONARY_KT.exists():
        text = DICTIONARY_KT.read_text(encoding="utf-8")
        matches = re.findall(r'"([\u0600-\u06FF]+)"', text)
        for m in matches:
            disp = clean_display_spelling(m)
            norm = normalize_for_matching(m)
            if 3 <= len(norm) <= 8:
                curated_raw.add(disp)
                curated_norm.add(norm)

    for path in (REMOTE_JSON, ASSET_DIR / "remote-dictionary-v1.json"):
        if path.exists():
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
                for key in ("answers", "guesses"):
                    for w in data.get(key, []):
                        disp = clean_display_spelling(str(w))
                        norm = normalize_for_matching(str(w))
                        if 3 <= len(norm) <= 8:
                            curated_raw.add(disp)
                            curated_norm.add(norm)
            except Exception:
                pass

    for reg in REQUIRED_REGRESSION_WORDS:
        curated_raw.add(clean_display_spelling(reg))
        curated_norm.add(normalize_for_matching(reg))

    bigram_freq: Counter = Counter()
    trigram_freq: Counter = Counter()

    for w in curated_norm:
        for i in range(len(w) - 1):
            bigram_freq[w[i : i + 2]] += 1
        for i in range(len(w) - 2):
            trigram_freq[w[i : i + 3]] += 1

    return curated_norm, curated_raw, bigram_freq, trigram_freq


def is_high_precision_base_word(
    raw: str,
    curated_norm: set[str],
    bigram_freq: Counter,
    trigram_freq: Counter,
    freq_dict: dict[str, int] | None = None,
    min_bg_freq: int = 3,
    min_tg_freq: int = 1,
) -> bool:
    """High-precision validator for Arabic base guess entries."""
    display = clean_display_spelling(raw)
    norm = normalize_for_matching(raw)

    if not (3 <= len(norm) <= 8 and bool(ARABIC_RE.fullmatch(norm))):
        return False

    if norm in curated_norm:
        return True

    if norm in FUNCTION_WORDS_BLOCKLIST or display in FUNCTION_WORDS_BLOCKLIST:
        return False

    # 1. Reject possessive/pronoun suffixes on non-curated words
    if len(norm) >= 4:
        for suf in INFLECTED_SUFFIXES:
            if norm.endswith(suf) and len(norm) - len(suf) >= 3:
                stem = norm[: len(norm) - len(suf)]
                if stem not in curated_norm:
                    return False

    # 2. Reject proclitic prefixes
    if len(norm) >= 5 and (norm.startswith("بال") or norm.startswith("فال") or norm.startswith("وال") or norm.startswith("كال")):
        return False
    if len(norm) >= 4 and norm.startswith("لل"):
        return False
    if len(norm) >= 5 and norm.startswith("ال"):
        return False
    if len(norm) >= 5 and (norm[0] in "وف") and norm not in NATIVE_PREFIX_EXCEPTIONS:
        return False

    # 3. Hamza check
    hamza_idx = norm.find("ء")
    if hamza_idx >= 0:
        if hamza_idx < len(norm) - 1:
            return False
        end2 = norm[-2:] if len(norm) >= 2 else ""
        if end2 not in ALLOWED_HAMZA_ENDS:
            return False

    # 4. Repeated letters check
    for i in range(len(norm) - 1):
        if i < len(norm) - 2 and norm[i] == norm[i + 1] == norm[i + 2]:
            return False
        if norm[i] == norm[i + 1] and norm[i] in UNDOUBLEABLE_LETTERS:
            return False

    # 5. Bigram frequency check
    for i in range(len(norm) - 1):
        bg = norm[i : i + 2]
        if bigram_freq.get(bg, 0) < min_bg_freq:
            return False

    # 6. Trigram frequency check for length >= 4
    if len(norm) >= 4:
        valid_tg_count = sum(1 for i in range(len(norm) - 2) if trigram_freq.get(norm[i : i + 3], 0) >= min_tg_freq)
        if valid_tg_count < (len(norm) - 3) // 2 + 1:
            return False

    # 7. Optional local frequency/ranking threshold
    if freq_dict:
        if display not in freq_dict and norm not in freq_dict:
            return False

    return True


def filter_and_sample(
    candidates: list[str],
    curated_norm: set[str],
    curated_raw: set[str],
    bigram_freq: Counter,
    trigram_freq: Counter,
    freq_dict: dict[str, int] | None = None,
    max_words: int = 45000,
) -> tuple[list[str], list[str]]:
    kept_set: set[str] = set()
    rejected: list[str] = []

    for raw in candidates:
        disp = clean_display_spelling(raw)
        if is_high_precision_base_word(raw, curated_norm, bigram_freq, trigram_freq, freq_dict):
            kept_set.add(disp)
        else:
            rejected.append(raw)

    for c in curated_raw:
        disp = clean_display_spelling(c)
        if 3 <= len(normalize_for_matching(disp)) <= 8:
            kept_set.add(disp)

    sorted_kept = sorted(kept_set)

    if max_words > 0 and len(sorted_kept) > max_words:
        non_curated = [w for w in sorted_kept if normalize_for_matching(w) not in curated_norm]
        budget_remaining = max(0, max_words - len(curated_norm))
        if budget_remaining < len(non_curated):
            step = len(non_curated) / budget_remaining
            sampled = [non_curated[int(i * step)] for i in range(budget_remaining)]
            final_kept = sorted(set(curated_raw).union(sampled))
        else:
            final_kept = sorted_kept
    else:
        final_kept = sorted_kept

    return final_kept, rejected


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--max-words", type=int, default=45000, help="Target word count (30000..60000)")
    parser.add_argument("--freq", type=Path, default=FREQ_FILE_DEFAULT, help="Path to local word frequency text file")
    args = parser.parse_args()

    if not GUESS_FILE.exists():
        print(f"Error: {GUESS_FILE} does not exist.", file=sys.stderr)
        return 1

    curated_norm, curated_raw, bigram_freq, trigram_freq = load_curated_data()
    freq_dict = load_freq_file(args.freq)

    with GUESS_FILE.open("r", encoding="utf-8") as f:
        raw_words = [line.strip() for line in f if line.strip()]

    initial_count = len(raw_words)
    kept, rejected = filter_and_sample(raw_words, curated_norm, curated_raw, bigram_freq, trigram_freq, freq_dict, args.max_words)

    GUESS_FILE.write_text("".join(w + "\n" for w in kept), encoding="utf-8", newline="\n")

    print(f"=== Cleaned {GUESS_FILE} ===")
    print(f"Before count: {initial_count:,}")
    print(f"After count:  {len(kept):,}")
    print(f"Rejected:     {len(rejected):,}\n")

    random.seed(1234)
    sample_kept = random.sample(kept, min(30, len(kept)))
    sample_rejected = random.sample(rejected, min(30, len(rejected)))

    print("=== 30 Random Kept Samples ===")
    print(", ".join(sample_kept))
    print("\n=== 30 Random Rejected Samples ===")
    print(", ".join(sample_rejected))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
