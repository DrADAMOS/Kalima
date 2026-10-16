#!/usr/bin/env python3
"""Build Kalima's offline Arabic guess dictionary from the licensed word-list source.

Usage:
  python tools/build_arabic_dictionary.py [--source PATH] [--max-words 45000] [--freq PATH]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
import urllib.request
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from refine_guess_list import keep as derives_from_vocab, REGRESSION as _REGRESSION  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
SNAPSHOT = ASSET_DIR / "remote-dictionary-v1.json"
REMOTE_JSON = ROOT / "dictionary" / "remote-dictionary-v1.json"
DICTIONARY_KT = ROOT / "app" / "src" / "main" / "java" / "com" / "adamos" / "kalima" / "data" / "Dictionary.kt"
OUTPUT = ASSET_DIR / "arabic-guess-words.txt"
SOURCE_URL = "https://raw.githubusercontent.com/MustafaLinux/arabic-words-list/main/arabic-words.txt"
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
    """Build curated words set, bigram frequencies, and trigram frequencies."""
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

    for path in (REMOTE_JSON, SNAPSHOT):
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


def is_game_word(
    raw: str,
    curated_norm: set[str] | None = None,
    bigram_freq: Counter | None = None,
    trigram_freq: Counter | None = None,
    freq_dict: dict[str, int] | None = None,
) -> bool:
    """Quality validator for Arabic base guess entries."""
    display = clean_display_spelling(raw)
    norm = normalize_for_matching(raw)

    if not (3 <= len(norm) <= 8 and bool(ARABIC_RE.fullmatch(norm))):
        return False

    if curated_norm is not None and norm in curated_norm:
        return True

    if norm in FUNCTION_WORDS_BLOCKLIST or display in FUNCTION_WORDS_BLOCKLIST:
        return False

    # 0. Precision gate: a non-curated entry must be a plain inflection of a trusted word
    #    (clitic/plural/possessive/verb prefix). This removes the non-words that dominate
    #    the upstream list; see refine_guess_list.py for the exact rules.
    if curated_norm is not None and not derives_from_vocab(norm, curated_norm):
        return False

    # 1. Reject possessive/pronoun suffixes on non-curated words
    if len(norm) >= 4:
        for suf in INFLECTED_SUFFIXES:
            if norm.endswith(suf) and len(norm) - len(suf) >= 3:
                stem = norm[: len(norm) - len(suf)]
                if curated_norm is not None and stem not in curated_norm:
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
    if bigram_freq is not None:
        for i in range(len(norm) - 1):
            bg = norm[i : i + 2]
            if bigram_freq.get(bg, 0) < 3:
                return False

    # 6. Trigram frequency check for length >= 4
    if trigram_freq is not None and len(norm) >= 4:
        valid_tg_count = sum(1 for i in range(len(norm) - 2) if trigram_freq.get(norm[i : i + 3], 0) >= 1)
        if valid_tg_count < (len(norm) - 3) // 2 + 1:
            return False

    # 7. Optional local frequency/ranking threshold
    if freq_dict:
        if display not in freq_dict and norm not in freq_dict:
            return False

    return True


def select_words(
    words: set[str],
    curated_norm: set[str],
    curated_raw: set[str],
    max_words: int = 45000,
) -> list[str]:
    """Sample entries deterministically to fit target budget (30,000–60,000 words)."""
    if max_words < 1:
        raise ValueError("max_words must be greater than zero")

    combined = set(words).union(curated_raw)
    sorted_all = sorted(combined)

    if len(sorted_all) <= max_words:
        return sorted_all

    protected_norm = set(curated_norm) | {normalize_for_matching(w) for w in _REGRESSION}
    protected_words = {w for w in sorted_all if normalize_for_matching(w) in protected_norm}
    non_curated = [w for w in sorted_all if normalize_for_matching(w) not in protected_norm]
    budget_remaining = max(0, max_words - len(protected_words))

    if budget_remaining == 0:
        return sorted(set(curated_raw).union(protected_words))

    if budget_remaining < len(non_curated):
        step = len(non_curated) / budget_remaining
        sampled = [non_curated[int(i * step)] for i in range(budget_remaining)]
        return sorted(set(curated_raw).union(sampled))

    return sorted_all


def iter_source_words(path: Path):
    with path.open("r", encoding="utf-8-sig", errors="replace") as stream:
        for line in stream:
            for token in line.split():
                yield token


def build(
    source: Path,
    output: Path = OUTPUT,
    snapshot: Path = SNAPSHOT,
    max_words: int = 45000,
    freq_path: Path | None = FREQ_FILE_DEFAULT,
) -> int:
    curated_norm, curated_raw, bigram_freq, trigram_freq = load_curated_data()
    freq_dict = load_freq_file(freq_path)

    accepted: set[str] = set()
    for raw in iter_source_words(source):
        disp = clean_display_spelling(raw)
        if is_game_word(raw, curated_norm, bigram_freq, trigram_freq, freq_dict):
            accepted.add(disp)

    selected = select_words(accepted, curated_norm, curated_raw, max_words=max_words)

    output.parent.mkdir(parents=True, exist_ok=True)
    temp = output.with_suffix(output.suffix + ".tmp")
    temp.write_text(
        "".join(word + "\n" for word in selected),
        encoding="utf-8",
        newline="\n",
    )
    temp.replace(output)
    return len(selected)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Path to a downloaded arabic-words.txt")
    parser.add_argument("--max-words", type=int, default=45000, help="Target word count (30000..60000)")
    parser.add_argument("--freq", type=Path, default=FREQ_FILE_DEFAULT, help="Path to local word frequency text file")
    parser.add_argument("--keep-download", action="store_true", help="Keep downloaded source in tools/cache")
    args = parser.parse_args()

    source = args.source
    if source is None:
        cache = ROOT / "tools" / "cache"
        cache.mkdir(parents=True, exist_ok=True)
        source = cache / "arabic-words.txt"
        if not source.exists():
            request = urllib.request.Request(
                SOURCE_URL, headers={"User-Agent": "KalimaDictionaryBuilder/1.0"}
            )
            try:
                with urllib.request.urlopen(request, timeout=60) as response, source.open("wb") as out:
                    while True:
                        chunk = response.read(1024 * 1024)
                        if not chunk:
                            break
                        out.write(chunk)
            except Exception as exc:
                source.unlink(missing_ok=True)
                print(f"Could not download source: {exc}", file=sys.stderr)
                print("Download arabic-words.txt manually and rerun with --source PATH", file=sys.stderr)
                return 2

    count = build(source, max_words=args.max_words, freq_path=args.freq)
    print(f"Generated {OUTPUT} with {count:,} filtered base guesses (3–8 letters).")
    if source.parent.name == "cache" and not args.keep_download:
        source.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
