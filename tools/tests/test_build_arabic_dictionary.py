import json
import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_arabic_dictionary import build, normalize, is_game_word

class DictionaryBuilderTests(unittest.TestCase):
    def test_normalization_and_length_filter(self):
        self.assertEqual(normalize("رُقَى"), "رقى")
        self.assertEqual(normalize("مــدرسَة"), "مدرسه")
        self.assertTrue(is_game_word("سلب"))
        self.assertFalse(is_game_word("رق"))
        self.assertFalse(is_game_word("abc"))
        self.assertFalse(is_game_word("رقى123"))

    def test_build_deduplicates_and_preserves_answers(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source.txt"
            source.write_text("سلب\nرقي\nرُقَى\nسلب\nabc\nرق\n", encoding="utf-8")
            snapshot = root / "remote-dictionary-v1.json"
            snapshot.write_text(json.dumps({"schemaVersion": 1, "answers": ["قمر"], "guesses": ["نور"]}, ensure_ascii=False), encoding="utf-8")
            output = root / "assets" / "arabic-guess-words.txt"
            count = build(source, output, snapshot)
            words = output.read_text(encoding="utf-8").splitlines()
            self.assertEqual(count, len(set(words)))
            self.assertIn("سلب", words)
            self.assertIn("رقي", words)
            self.assertIn("رقى", words)
            self.assertIn("نور", words)
            self.assertNotIn("abc", words)
            self.assertNotIn("قمر", words)  # answers are not copied into guess list from answers array

if __name__ == "__main__":
    unittest.main()
