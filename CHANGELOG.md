# v26.10.40 (2026-10-31)

- Editor: the crop stays adjustable after drawing. The crop window can grow back to the full screenshot, and strokes keep their undo and redo history.
- Editor: after drawing, the crop view zooms in on the crop again.
- Editor: it now behaves like a normal app in the app switcher, stays open while sharing so Save still works afterwards, and a new assistant gesture always starts with the new screenshot.
- Editor: accepting a drawing and opening the app settle back gently instead of bouncing.
- Editor: picking a format closes the format drawer, and formats are listed alphabetically (JPG, PNG, WEBP).
- Crop view: the area outside the crop window is darkened without losing its colors.
- Edit: the editor is only offered for image formats it can open (PNG, JPEG, WebP, GIF, BMP, HEIF, AVIF), and images too large for memory show a message instead of crashing.
- Security: the editor only accepts the image it was meant to open, and the assistant screenshot is deleted from the app cache once it has been read.

# v26.10.23 (2026-10-30)

- Editor: JPEG is now called JPG, and JPG files are saved with the .jpg extension.
- Editor: the Save toast only says where the image was saved.
- Editor: every Save press shows a toast, and Save and Share work while the format drawer is open.
- Editor: the image quality slider is gone. JPG and WEBP are always saved at maximum quality.
- Start screen: one setup sentence replaces the intro, a check shows once the app is the default assistant, and setup buttons are filled only while a step is still needed.
- The German translation is removed. The app is in English only.

# v26.10.15 (2026-10-29)

- Start screen: the top card now says in one sentence what the app is. Setup hints appear only while something is missing, and the digital assistant settings button sits inside that hint as "Open".
- Editor: the format drawer floats above the screenshot instead of shrinking it, stays open while trying PNG, JPEG and WEBP, and is less transparent.
- Save folder: the start screen shows the full path of the save folder, and edits are kept while picking a folder.
- Removed an unused permission that androidx.core added to the app.
- Signed with a new key.

# v26.10.12 (2026-10-29)

- New application ID de.paradox.screenshotassistant. Android installs it as a new app next to the old one.
- Editor: new Save button. It saves the cropped and drawn image to Pictures/Screenshots, or to a folder picked under "Save folder" on the start screen.
- Editor: a format pill next to Save switches between PNG, JPEG and WEBP, with a quality slider, for the current screenshot.
- Screenshot Assistant can be picked for "Edit" and "Edit with" on images from the share sheet, gallery and file apps.
- Image format and quality moved from the editor's three-dot menu to a card on the start screen. The menu is gone.
- New drawing tool built into the app: smoother strokes, full-resolution export, a 12-color palette, and pen and highlighter colors are remembered. Pen and highlighter are wider.
- Editor buttons restyled. They hide while drawing, so Done is no longer covered.
- Share while drawing shares the drawing, and the share sheet shows a preview.
- Crop view: the area outside the crop window and the screen behind the editor are faded instead of darkened. After drawing, the image fits the crop view again instead of sitting in the top-left corner.
- The launcher icon is labeled "Screenshot" so the name is no longer cut off.
- Log screen keeps its margin on all sides.
- Removed the ImagePainter and ToggleButtonLayout libraries.

# v26.10.5 (2026-10-27)

- Start screen, About and log screens: removed the duplicate title bar. The digital assistant settings button now sits above a shorter restart-reminder switch, App info is gone and About is centered. The log appears in one go, already scrolled to the end.
- Versions are now named vYY.MM.N, where N counts the releases within the month from 0. The version code follows the same pattern as YYMMNN (261005).
- Bump material from 1.12.0 to 1.14.0.
- Bump fragment-ktx from 1.9.0 to 1.9.1.
- Build with Android Gradle Plugin 9.4.1 and Gradle 9.8.2.

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