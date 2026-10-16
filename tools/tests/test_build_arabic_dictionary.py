import json
import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_arabic_dictionary import (
    build,
    normalize_for_matching,
    clean_display_spelling,
    is_game_word,
    select_words,
    load_curated_data,
)


class DictionaryBuilderTests(unittest.TestCase):
    def setUp(self):
        self.curated_norm, self.curated_raw, self.bigram_freq, self.trigram_freq = load_curated_data()

    def test_normalization_and_length_filter(self):
        self.assertEqual(normalize_for_matching("رُقَى"), "رقى")
        self.assertEqual(normalize_for_matching("مــدرسَة"), "مدرسه")
        self.assertEqual(clean_display_spelling("مــدرسَة"), "مدرسة")
        self.assertTrue(is_game_word("سلب", self.curated_norm, self.bigram_freq, self.trigram_freq))
        self.assertTrue(is_game_word("جحش", self.curated_norm, self.bigram_freq, self.trigram_freq))
        self.assertFalse(is_game_word("رق", self.curated_norm, self.bigram_freq, self.trigram_freq))
        self.assertFalse(is_game_word("abc", self.curated_norm, self.bigram_freq, self.trigram_freq))
        self.assertFalse(is_game_word("رقى123", self.curated_norm, self.bigram_freq, self.trigram_freq))

    def test_rejection_of_garbage_words(self):
        garbage = ["ءءء", "ءءل", "ءؤخ", "اناموظف", "تكذي", "هازيو", "بلعس", "وحظربه"]
        for g in garbage:
            self.assertFalse(
                is_game_word(g, self.curated_norm, self.bigram_freq, self.trigram_freq),
                f"Garbage word '{g}' should be rejected",
            )

    def test_retention_of_real_words(self):
        real_words = ["سماء", "مدرسة", "كتاب", "جحش", "سلب", "رقى", "رقي", "دفء", "ماء"]
        for w in real_words:
            self.assertTrue(
                is_game_word(w, self.curated_norm, self.bigram_freq, self.trigram_freq),
                f"Real word '{w}' should be kept",
            )

    def test_selection_respects_max_words_and_curated_protection(self):
        short_words = {"جحش", "سلب", "رقى", "رقي"}
        long_words = {
            "ابتثج", "ابتثح", "ابتثخ", "ابتثد", "ابتثذ", "ابتثر",
            "ابتثز", "ابتثس", "ابتثش", "ابتثص",
        }

        selected = select_words(short_words | long_words, self.curated_norm, self.curated_raw, max_words=6)

        self.assertTrue(short_words.issubset(selected))
        self.assertLessEqual(len(selected), 6 + len(self.curated_raw))
        self.assertEqual(selected, sorted(set(selected)))

    def test_selection_rejects_invalid_budget(self):
        with self.assertRaises(ValueError):
            select_words({"جحش"}, self.curated_norm, self.curated_raw, max_words=0)

    def test_build_deduplicates_preserves_regressions_and_excludes_answers(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source.txt"
            source.write_text("سلب\nرقي\nرُقَى\nسلب\nabc\nرق\nسماء\nمدرسة\nكتاب\n", encoding="utf-8")
            snapshot = root / "remote-dictionary-v1.json"
            snapshot.write_text(
                json.dumps(
                    {"schemaVersion": 1, "answers": ["دفء"], "guesses": ["نور"]},
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            output = root / "assets" / "arabic-guess-words.txt"

            count = build(source, output, snapshot, max_words=20)
            words = output.read_text(encoding="utf-8").splitlines()

            self.assertEqual(count, len(set(words)))
            self.assertIn("جحش", words)
            self.assertIn("سلب", words)
            self.assertIn("رقي", words)
            self.assertIn("رقى", words)
            self.assertIn("سماء", words)
            self.assertIn("مدرسة", words)
            self.assertIn("كتاب", words)
            self.assertIn("دفء", words)
            self.assertNotIn("abc", words)


if __name__ == "__main__":
    unittest.main()
