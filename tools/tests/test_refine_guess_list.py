import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import refine_guess_list as r


class RefineGuessListTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vocab = r.trusted_vocab() | {r.normalize(w) for w in r.REGRESSION}

    def kept(self, w):
        n = r.normalize(w)
        return r.ok_shape(n) and r.keep(n, self.vocab)

    def test_garbage_is_rejected(self):
        for g in ["ءءء", "ءءل", "ءؤخ", "اناموظف", "تكذي", "بلعس", "وحظربه"]:
            self.assertFalse(self.kept(g), g)

    def test_real_words_and_regressions_are_kept(self):
        for w in ["سماء", "مدرسة", "كتاب", "جحش", "سلب", "رقى", "رقي", "فاكهة", "قراءة", "طاووس", "تخييم"]:
            self.assertTrue(self.kept(w), w)

    def test_simple_inflections_are_kept(self):
        for w in ["كتبت", "مدارس", "سيارات"]:
            self.assertTrue(self.kept(w), w)

    def test_shape_rules(self):
        self.assertFalse(r.ok_shape("ممم"))
        self.assertFalse(r.ok_shape("ابءت"))
        self.assertTrue(r.ok_shape("ماء"))


if __name__ == "__main__":
    unittest.main()
