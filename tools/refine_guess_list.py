#!/usr/bin/env python3
"""High-precision refinement of app/src/main/assets/arabic-guess-words.txt.

The upstream word list contains many non-words.  A candidate is kept only if it is
(a) in the trusted vocabulary (curated answers/guesses in Dictionary.kt, the JSON
snapshot and tools/freq_arabic_words.txt), or (b) a plain Arabic inflection of a
trusted word (clitic prefix/suffix, plural/dual/possessive ending, verb prefix).
Run from the project root:  py -3 tools/refine_guess_list.py
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSET = ROOT / "app/src/main/assets/arabic-guess-words.txt"
DICT_KT = ROOT / "app/src/main/java/com/adamos/kalima/data/Dictionary.kt"
JSON_SNAPSHOT = ROOT / "dictionary/remote-dictionary-v1.json"
FREQ = ROOT / "tools/freq_arabic_words.txt"
REGRESSION = ["جحش", "سلب", "رقى", "رقي"]   # must always be guessable

DIAC = re.compile("[\u0640\u064B-\u065F\u0670\u06D6-\u06ED]")
AR = re.compile("^[\u0621-\u064A]+$")

def normalize(t: str) -> str:
    t = DIAC.sub("", t)
    for a in "أإآٱ":
        t = t.replace(a, "ا")
    return t.replace("ة", "ه")

def ok_shape(w: str) -> bool:
    if not (3 <= len(w) <= 8) or not AR.match(w):
        return False
    i = w.find("ء")
    if i >= 0 and not (i == len(w) - 1 or (i == len(w) - 2 and w[-1] == "ه")):
        return False
    if re.search(r"(.)\1\1", w):
        return False
    if any(w[i] == w[i + 1] and w[i] in "اىءؤئ" for i in range(len(w) - 1)):
        return False
    return True

def trusted_vocab() -> set:
    vocab = set()
    kt = DICT_KT.read_text(encoding="utf-8-sig")
    for m in re.finditer(r'"([\u0621-\u064A\u064B-\u065F]+)"', kt):
        vocab.add(normalize(m.group(1)))
    if JSON_SNAPSHOT.exists():
        j = json.loads(JSON_SNAPSHOT.read_text(encoding="utf-8"))
        for k in ("answers", "guesses"):
            vocab.update(normalize(x) for x in j.get(k, []))
    if FREQ.exists():
        for line in FREQ.read_text(encoding="utf-8-sig").splitlines():
            parts = line.split()
            if parts:
                vocab.add(normalize(parts[0]))
    return {w for w in vocab if AR.match(w) and len(w) >= 2}

NOUN_PREFIX = ["", "ال", "و", "ف", "ب", "ل", "وال", "فال", "بال", "كال", "لل", "ول", "ولل", "وب", "فب", "وك", "فل"]
NOUN_SUFFIX = ["", "ه", "ها", "هم", "هن", "هما", "كم", "كن", "نا", "ني", "ات", "ون", "ين", "ان", "ته", "تي", "تك", "تها", "تنا", "اتها", "اتهم", "اته", "اتنا", "اتي", "اتك"]
VERB_PREFIX = ["", "ي", "ت", "ن", "ا", "و", "ف", "س", "سي", "ست", "سن", "ول", "لي", "لت", "ولي"]
VERB_SUFFIX = ["", "ت", "وا", "ا", "ن", "نا", "ه", "ها", "هم", "ون", "ين", "ان", "تم", "تن", "تا", "ني", "كم", "وه", "وها", "وهم"]

def stems(w: str):
    for pre_list, suf_list in ((NOUN_PREFIX, NOUN_SUFFIX), (VERB_PREFIX, VERB_SUFFIX)):
        for p in pre_list:
            if p and not w.startswith(p):
                continue
            for s in suf_list:
                if s and not w.endswith(s):
                    continue
                core = w[len(p):len(w) - len(s)]
                if len(core) >= 2:
                    yield core
                    if s in ("ات", "ه", "ته", "تي", "تك", "تها", "تنا", "اتها", "اتهم", "اته", "اتنا", "اتي", "اتك") and core:
                        yield core + "ه"   # تاء مربوطة ثم جمع/ضمير
                        yield core + "ى"
                    if s in ("", "ه", "ها", "هم", "ك", "ي", "نا", "ون", "ين", "ان") and core.endswith("ي"):
                        yield core[:-1] + "ى"

def keep(w: str, vocab: set) -> bool:
    if w in vocab:
        return True
    return any(c in vocab for c in stems(w))

def main(argv):
    vocab = trusted_vocab() | {normalize(w) for w in REGRESSION}
    src = Path(next((a.split("=", 1)[1] for a in argv if a.startswith("--source=")), ASSET))
    raw = [normalize(l.strip()) for l in src.read_text(encoding="utf-8-sig").splitlines() if l.strip()]
    kept = {w for w in raw if ok_shape(w) and keep(w, vocab)}
    kept |= {w for w in vocab if ok_shape(w)}          # trusted words are always guessable
    out = sorted(kept, key=lambda x: (len(x), x))
    if "--dry" not in argv:
        ASSET.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
    print(f"trusted vocab: {len(vocab)} | input: {len(raw)} | output: {len(out)}")
    return out

if __name__ == "__main__":
    main(sys.argv[1:])
