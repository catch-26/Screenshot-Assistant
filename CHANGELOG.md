# 20261026 (2026-10-26)

- Application ID is now `de.user_mb.screenshotassistant`. The FileProvider authority is derived from it, so the app installs alongside the original Screenshot Assistant.
- Compile and target SDK 37 (Android 17). Minimum SDK 34 (Android 14).
- Builds with Android Gradle Plugin 9: BuildConfig generation enabled, and the editing-mode `when` in `MainActivity` handles a null mode.
- Version name is the release date (`YYYYMMDD`), version code `YYYYMMDDnn`.

# 2.0.2 (2026-09-15)

- 🔧 Build against current Android SDK (compileSdk/targetSdk 36, AGP 9.4, Gradle 9.6, JDK 17)
- 📦 Replace unmaintained JCenter dependencies: Android Image Cropper → CanHub fork, Spectrum → built-in color picker

# 2.0.1 (2019-09-19)

- 💾 Save editor state accross configuration changes
- 🈂️ Add translation to settings
- 💥 Fix broken About-Activity

# 2.0 (2019-09-11)

### Features

- ✏️ Paint tools
- 🇩🇪 Added german translation
- ⚙️ Added settings for image format and quality
- 🌙 Complete support for dark mode

### BugFixes

- Do not completely reset image when switching on configuration change (WIP)