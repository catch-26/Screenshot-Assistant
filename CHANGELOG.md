# 20261026 (2026-10-26)

- 🆔 New app ID `de.user_mb.screenshotassistant`, signed with a new key
- 🤖 Target Android 17 (API 37); requires Android 14 or newer
- 🖼️ Save screenshots in the format chosen in settings (was always JPEG); default is PNG
- 📅 Versions are now named by release date

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