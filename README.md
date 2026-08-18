# LakaBoard v2.0

Sinhala & English Phonetic Keyboard — Android IME (Kotlin/Jetpack Compose) + a
self-contained React/TypeScript **Web Preview Simulator**.

Package: `com.dinushlakmal.lakaboard`

## What's fixed / rebuilt vs. your previous build

- **Live re-render bug**: the old version likely tried to patch the host text
  field character-by-character. This build uses **composing text**
  (`InputConnection.setComposingText` / `finishComposingText`) so the whole
  in-progress Singlish word can be replaced atomically each keystroke — this
  is what makes `k` → `ක`, then `ke` → `කෙ`, then `kE` → `කේ` work without
  leaving stray characters behind.
- **Clean MVVM boundary**: `KeyboardViewModel` never touches `Context` or
  `InputConnection` directly — it exposes callbacks (`onCommitText`,
  `onUpdateComposingWord`, `onFinishComposing`, `onDeleteBackward`) that both
  `LakaInputMethodService` (real keyboard) and `MobileFrameSimulator`
  (in-app test studio) wire up independently. That's what lets you test
  typing behaviour without leaving the app.
- **Shared transliteration rules**: `SinglishTransliterationEngine.kt` and
  `web/src/engine.ts` use the *same* table structure (longest-match
  consonant/vowel parsing + a curated common-word dictionary), so the Web
  Simulator is a faithful preview of what the Android keyboard will do.
- **3-state shift** is a single `ShiftState` enum cycled by one function
  (`cycleShift()`), consumed automatically after one character in
  `SHIFT_ONCE` — no more shift getting "stuck" or double-toggling.

## Project layout

```
LakaBoard/
├── android/                          Android Studio / Google AI Studio project
│   └── app/src/main/
│       ├── java/com/dinushlakmal/lakaboard/
│       │   ├── ime/LakaInputMethodService.kt
│       │   ├── engine/SinglishTransliterationEngine.kt
│       │   ├── engine/WijesekaraEngine.kt
│       │   ├── audio/SoundHapticHelper.kt
│       │   ├── viewmodel/KeyboardViewModel.kt
│       │   ├── viewmodel/KeyboardUiState.kt
│       │   ├── ui/LakaBoardKeyboard.kt
│       │   ├── ui/MobileFrameSimulator.kt
│       │   ├── ui/ThemeCustomizerDialog.kt
│       │   ├── ui/TransliterationGuideDialog.kt
│       │   ├── ui/theme/Theme.kt
│       │   ├── MainActivity.kt
│       │   └── LakaBoardApplication.kt
│       ├── AndroidManifest.xml
│       └── res/{xml/method.xml, values/*.xml, drawable/*, mipmap-anydpi-v26/*}
│
└── web/                               Standalone Vite + React 18 + TS + Tailwind app
    └── src/
        ├── engine.ts                  Singlish + Wijesekara logic (mirrors Kotlin)
        ├── sound.ts                   Web Audio key-click synthesis
        ├── themes.ts                  Theme presets + FlorisBoard JSON import/export
        └── App.tsx                    Full interactive simulator UI
```

## Running the Web Simulator locally

```bash
cd web
npm install
npm run dev
```

## Opening the Android project in Google AI Studio / Android Studio

1. Open the `android/` folder as the project root (it already has
   `settings.gradle.kts`, `build.gradle.kts`, and `gradle.properties`).
2. Let Gradle sync — it targets `compileSdk 35`, `minSdk 26`, Kotlin 1.9.24,
   Compose BOM `2024.06.00`.
3. Run `app` on a device/emulator, then **Settings → System → Languages &
   input → On-screen keyboard → Manage keyboards → enable LakaBoard**, then
   switch to it from any text field (or use the in-app "Switch Keyboard"
   button in `MainActivity`).
4. There is no launcher-icon PNG set (only a lightweight vector adaptive
   icon) — swap in real artwork via **Image Asset Studio** before shipping.

## Known simplifications worth flagging to Google AI Studio

- The Singlish engine is a **rule-based phonetic parser + ~90-word curated
  dictionary**, not a full statistical/ML transliterator — uncommon words
  fall back to the algorithmic render, which is usually right but not
  guaranteed for every irregular word. Expanding `COMMON_WORDS` /
  `CONSONANTS` in `engine.ts` (and mirroring in the Kotlin file) is the
  cheapest way to improve accuracy over time.
- Theme persistence (`DataStore`/`SharedPreferences`) and the SAF file
  pickers for background-image upload / theme JSON import-export are wired
  as callback hooks (`onImportJson`, `onExportJson` in
  `ThemeCustomizerDialog`) but not yet backed by real file I/O in
  `MainActivity` — that's an intentional seam so you (or AI Studio) can
  drop in your preferred storage approach.
- `SoundHapticHelper` synthesizes clicks in-memory (no audio assets to
  ship), which keeps the APK small but means the four profiles are
  approximations of "Mechanical / Bubble / Pop / Typewriter", not sampled
  recordings.
