#!/usr/bin/env python3
"""Build Kalima's offline Arabic guess dictionary from the licensed word-list source.

Usage:
  python tools/build_arabic_dictionary.py
  python tools/build_arabic_dictionary.py --source C:/data/arabic-words.txt

The script downloads the upstream 46.9 MB text file if --source is omitted, filters
entries to Arabic-only words of 3..8 normalized letters, deduplicates, merges the
existing snapshot's guesses, and writes app/src/main/assets/arabic-guess-words.txt.
It intentionally does NOT change the secret answer list.
"""
from __future__ import annotations
import argparse
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
SNAPSHOT = ASSET_DIR / "remote-dictionary-v1.json"
OUTPUT = ASSET_DIR / "arabic-guess-words.txt"
SOURCE_URL = "https://raw.githubusercontent.com/MustafaLinux/arabic-words-list/main/arabic-words.txt"
ARABIC_RE = re.compile(r"^[\u0621-\u064A\u0671\u067E\u0686\u0698\u06A9\u06AF\u06CC\u0649\u0629]+$")
DIACRITICS_RE = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")


def normalize(word: str) -> str:
    word = DIACRITICS_RE.sub("", word.replace("ـ", "").strip())
    return word.translate(str.maketrans({"أ":"ا", "إ":"ا", "آ":"ا", "ٱ":"ا", "ة":"ه"}))


def is_game_word(word: str) -> bool:
    return 3 <= len(word) <= 8 and bool(ARABIC_RE.fullmatch(word))


def iter_source_words(path: Path):
    # Upstream is a plain-text word list; split whitespace defensively in case
    # a mirror or a future revision wraps entries into multiple columns.
    with path.open("r", encoding="utf-8-sig", errors="replace") as stream:
        for line in stream:
            for token in line.split():
                yield token


def build(source: Path, output: Path = OUTPUT, snapshot: Path = SNAPSHOT) -> int:
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

    # Regression words make the build fail loudly if source filtering changes.
    required = {"سلب", "رقى", "رقي"}
    missing = required - words
    if missing:
        raise ValueError("Required regression words missing after build: " + ", ".join(sorted(missing)))

    output.parent.mkdir(parents=True, exist_ok=True)
    temp = output.with_suffix(output.suffix + ".tmp")
    temp.write_text("".join(word + "\n" for word in sorted(words)), encoding="utf-8", newline="\n")
    temp.replace(output)
    return len(words)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Path to a downloaded arabic-words.txt")
    parser.add_argument("--keep-download", action="store_true", help="Keep downloaded source in tools/cache")
    args = parser.parse_args()
    source = args.source
    if source is None:
        cache = ROOT / "tools" / "cache"
        cache.mkdir(parents=True, exist_ok=True)
        source = cache / "arabic-words.txt"
        if not source.exists():
            request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "KalimaDictionaryBuilder/1.0"})
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
    count = build(source)
    print(f"Generated {OUTPUT} with {count:,} unique normalized guesses (3–8 letters).")
    print("Secret answers were not changed.")
    if source.parent.name == "cache" and not args.keep_download:
        source.unlink(missing_ok=True)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
