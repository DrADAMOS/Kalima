import json
import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_arabic_dictionary import build, normalize, is_game_word, select_words


class DictionaryBuilderTests(unittest.TestCase):
    def test_normalization_and_length_filter(self):
        self.assertEqual(normalize("رُقَى"), "رقى")
        self.assertEqual(normalize("مــدرسَة"), "مدرسه")
        self.assertTrue(is_game_word("سلب"))
        self.assertTrue(is_game_word("جحش"))
        self.assertFalse(is_game_word("رق"))
        self.assertFalse(is_game_word("abc"))
        self.assertFalse(is_game_word("رقى123"))

    def test_selection_keeps_all_short_words_before_sampling_long_words(self):
        short_words = {"جحش", "سلب", "رقى", "رقي"}
        long_words = {
            "ابتثج", "ابتثح", "ابتثخ", "ابتثد", "ابتثذ", "ابتثر",
            "ابتثز", "ابتثس", "ابتثش", "ابتثص",
        }

        selected = select_words(short_words | long_words, max_words=6)

        self.assertTrue(short_words.issubset(selected))
        self.assertEqual(len([word for word in selected if len(word) > 4]), 2)
        self.assertEqual(selected, sorted(set(selected)))

    def test_selection_rejects_invalid_budget(self):
        with self.assertRaises(ValueError):
            select_words({"جحش"}, max_words=0)

    def test_build_deduplicates_preserves_regressions_and_excludes_answers(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source.txt"
            source.write_text("سلب\nرقي\nرُقَى\nسلب\nabc\nرق\n", encoding="utf-8")
            snapshot = root / "remote-dictionary-v1.json"
            snapshot.write_text(
                json.dumps(
                    {"schemaVersion": 1, "answers": ["قمر"], "guesses": ["نور"]},
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )
            output = root / "assets" / "arabic-guess-words.txt"

            count = build(source, output, snapshot, max_words=10)
            words = output.read_text(encoding="utf-8").splitlines()

            self.assertEqual(count, len(set(words)))
            self.assertIn("جحش", words)
            self.assertIn("سلب", words)
            self.assertIn("رقي", words)
            self.assertIn("رقى", words)
            self.assertIn("نور", words)
            self.assertNotIn("abc", words)
            self.assertNotIn("قمر", words)  # answers are not copied from the snapshot


if __name__ == "__main__":
    unittest.main()
