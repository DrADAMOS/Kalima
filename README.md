# احزر الكلمة 🏆

لعبة تخمين كلمات عربية للأندرويد — مبنية بلغة **Kotlin** و **Jetpack Compose**، بلا أي اعتماد على Flutter.

---

## المتطلبات

- **Android Studio** (Ladybug أو أحدث)
- **JDK 17**
- Android SDK 35
- جهاز أندرويد (API 24+) أو محاكي

**لا حاجة لـ Flutter SDK إطلاقًا.**

---

## التشغيل

1. افتح **Android Studio**
2. `File → Open` ← اختر مجلد `Kalima`
3. انتظر تحميل Gradle (أول مرة دقيقتان)
4. اضغط ▶️

أو من سطر الأوامر (إن كان Gradle مثبتًا):

```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## هيكل المشروع

```
app/src/main/java/com/adamos/kalima/
├── MainActivity.kt            نقطة الدخول
├── data/
│   ├── Dictionary.kt          قاموس عربي موسّع (3–8 حروف)
│   ├── GameState.kt           منطق المطابقة والوضع الصعب
│   ├── Preferences.kt         تخزين محلي (SharedPreferences)
│   └── Rewards.kt             الشارات والكائن المتطور
└── ui/
    ├── AppRoot.kt             التنقل بين الشاشات
    ├── HomeScreen.kt          الشاشة الرئيسية
    ├── GameScreen.kt          شاشة اللعب
    ├── SettingsScreen.kt      الإعدادات + الرقابة الأبوية
    ├── RewardsScreen.kt       المكافآت والإحصائيات
    ├── LearnScreen.kt         وضع التعلّم
    ├── LeaderboardScreen.kt   السجلات والإحصائيات
    ├── HowToPlayScreen.kt     شرح القواعد
    ├── components/
    │   ├── WordGrid.kt        شبكة الكلمات
    │   └── ArabicKeyboard.kt  لوحة المفاتيح العربية
    └── theme/
        ├── ThemeManager.kt    ثيمات كاملة
        └── Theme.kt           ثيم Compose الأساسي
```

---

## الميزات

| الميزة | التفاصيل |
|---|---|
| **الأطوال** | 3 → 8 حروف |
| **الثيمات** | متعددة (الأصلي، الفضاء، الحيوانات، الحلويات، المحيط، الأبطال) |
| **المستويات** | 🐣 مبتدئ (4–7) · 🐥 متوسط (8–11) · 🐓 متقدم (12+) |
| **الشارات** | 10 شارات منها واحدة سرية |
| **الكائن** | 🥚 → 🐣 → 🐥 → 🐓 → 🦅 |
| **وضع التعلّم** | بطاقات وتحدي إملاء |
| **الرقابة الأبوية** | وضع الطفل · حماية بـ PIN · قاموس آمن |
| **الوضع الصعب** | إلزام الحروف الخضراء والصفراء |

### ميزات خاصة بالعربية

- **تطبيع الألف**: أ، إ، آ ← ا
- **تطبيع التاء المربوطة**: ة ← ه
- **لوحة مفاتيح عربية**: 28 حرفًا + همزة + تاء مربوطة
- **واجهة RTL** كاملة

---

## البناء للإصدار

```bash
./gradlew assembleRelease
```

الملف الناتج: `app/build/outputs/apk/release/app-release.apk`
