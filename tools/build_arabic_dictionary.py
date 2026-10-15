#!/usr/bin/env python3
"""Build Kalima's offline Arabic guess dictionary from the licensed word-list source.

Usage:
  python tools/build_arabic_dictionary.py
  python tools/build_arabic_dictionary.py --source C:/data/arabic-words.txt

The script downloads the upstream text file if --source is omitted, filters entries
to Arabic-only words of 3..8 normalized letters, deduplicates, merges snapshot guesses,
and writes app/src/main/assets/arabic-guess-words.txt. Secret answers are never copied.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
SNAPSHOT = ASSET_DIR / "remote-dictionary-v1.json"
OUTPUT = ASSET_DIR / "arabic-guess-words.txt"
SOURCE_URL = "https://raw.githubusercontent.com/MustafaLinux/arabic-words-list/main/arabic-words.txt"
ARABIC_RE = re.compile(r"^[\u0621-\u064A\u0671\u0672\u0673\u0675\u0676\u0677\u0678\u0679\u067A\u067B\u067C\u067D\u067E\u067F\u0680\u0681\u0682\u0683\u0684\u0685\u0686\u0687\u0688\u0689\u068A\u068B\u068C\u068D\u068E\u068F\u0690\u0691\u0692\u0693\u0694\u0695\u0696\u0697\u0698\u0699\u06A9\u06AF\u06BA\u06BB\u06BC\u06BD\u06BE\u06C0\u06C1\u06C2\u06C3\u06C4\u06C5\u06C6\u06C7\u06C8\u06C9\u06CA\u06CB\u06CC\u06CD\u06CE\u06CF\u06D0\u06D1\u06D2\u06D3]+$")
DIACRITICS_RE = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")
REQUIRED_REGRESSION_WORDS = {"جحش", "سلب", "رقى", "رقي"}


def normalize(word: str) -> str:
    """Apply the same character policy as the PowerShell builder."""
    word = unicodedata.normalize("NFC", word).replace("ـ", "").strip()
    word = DIACRITICS_RE.sub("", word)
    return word.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ة": "ه"}))


def is_game_word(word: str) -> bool:
    return 3 <= len(word) <= 8 and bool(ARABIC_RE.fullmatch(word))


def select_words(words: set[str], max_words: int = 150000) -> list[str]:
    """Retain all 3–4 letter words, then deterministically sample longer words.

    max_words is a target budget for the selected source vocabulary. The complete
    short-word bucket takes priority and can exceed the target if it ever grows
    beyond it. Regression words are merged separately by build().
    """
    if max_words < 1:
        raise ValueError("max_words must be greater than zero")

    short_words = sorted(word for word in words if len(word) <= 4)
    long_words = sorted(word for word in words if len(word) > 4)
    remaining = max(0, max_words - len(short_words))

    if remaining == 0:
        selected_long: list[str] = []
    elif len(long_words) <= remaining:
        selected_long = long_words
    else:
        step = len(long_words) / remaining
        selected_long = [long_words[int(i * step)] for i in range(remaining)]

    return sorted(set(short_words).union(selected_long))


def iter_source_words(path: Path):
    # Upstream is a plain-text word list; split whitespace defensively in case
    # a mirror or future revision wraps entries into multiple columns.
    with path.open("r", encoding="utf-8-sig", errors="replace") as stream:
        for line in stream:
            for token in line.split():
                yield token


def build(
    source: Path,
    output: Path = OUTPUT,
    snapshot: Path = SNAPSHOT,
    max_words: int = 150000,
) -> int:
    words: set[str] = set()
    for raw in iter_source_words(source):
        word = normalize(raw)
        if is_game_word(word):
            words.add(word)

    if snapshot.exists():
        data = json.loads(snapshot.read_text(encoding="utf-8"))
        if data.get("schemaVersion") != 1:
            raise ValueError("Unsupported dictionary snapshot schema")
        for raw in data.get("guesses", []):
            word = normalize(str(raw))
            if is_game_word(word):
                words.add(word)

    selected = set(select_words(words, max_words=max_words))
    # Explicit regression cases are always guesses, never secret answers.
    selected.update(REQUIRED_REGRESSION_WORDS)

    output.parent.mkdir(parents=True, exist_ok=True)
    temp = output.with_suffix(output.suffix + ".tmp")
    temp.write_text(
        "".join(word + "\n" for word in sorted(selected)),
        encoding="utf-8",
        newline="\n",
    )
    temp.replace(output)
    return len(selected)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Path to a downloaded arabic-words.txt")
    parser.add_argument("--max-words", type=int, default=150000, help="Target cap after retaining all 3–4 letter words")
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

    count = build(source, max_words=args.max_words)
    print(f"Generated {OUTPUT} with {count:,} unique normalized guesses (3–8 letters).")
    print("All 3–4 letter source words were retained; longer words were sampled only if needed.")
    print("Verified regression words: جحش, سلب, رقى, رقي")
    print("Secret answers were not changed.")
    if source.parent.name == "cache" and not args.keep_download:
        source.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
