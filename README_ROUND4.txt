Kalima — contrast test fix (Round 4)

Replace only:
app/src/main/java/com/adamos/kalima/ui/theme/ThemeManager.kt

This raises the accentText generator's target contrast from 4.5:1 to 5.0:1 against white, giving enough margin for tinted theme backgrounds. It addresses the reported ANIMALS failure (4.3274:1). This is a targeted source patch; run the full test suite afterward because further assertions may reveal additional issues.

No commit/push performed.