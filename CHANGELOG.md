# 20261027d (2026-10-27)

- New option on the start screen: after a restart or an app update, check whether Screenshot Assistant is still the digital assistant app, and if not, show a notification with a button to the digital assistant settings. Turning it on asks for the notification permission.

# 20261027c (2026-10-27)

- "Show logs" on the start screen opens an in-app log viewer with Refresh and Share. Android has no public link to the system's per-app log screen.
- New About page in the style of the start screen: version, source code, releases, the original project, license and libraries.
- Removed the material-about-library dependency.
- Android Image Cropper stays on 4.7.0, the latest release.

# 20261027b (2026-10-27)

- Launcher icon: the app now opens a start screen that shows whether it is the digital assistant app, with buttons for the digital assistant settings and the app info page (for logs).
- Android 17: the start screen offers "Allow screen access" when Android has not given the assistant access to the screen yet. Without it the system sends no screenshot.
- The screenshot editor is now started with `startAssistantActivity`, before the assistant window closes.
- The assistant session logs its callbacks under the `ScreenshotAssistant` tag.

# 20261027 (2026-10-27)

- Fix the assist gesture showing nothing on Android 17: the assistant now declares that it uses the screenshot (`usesAssistData`, `usesAssistScreenshots`), and closes with a hint if the system sends no screenshot.

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