package de.beatbrot.screenshotassistant.util

import android.content.Context
import android.net.Uri
import java.io.File

fun Context.assistantScreenshotFile(): File = File(File(cacheDir, "screenshots"), "scr")

fun Context.isAssistantScreenshot(uri: Uri): Boolean {
    val path = uri.path
    return uri.scheme == "file" && path != null && File(path).canonicalFile == assistantScreenshotFile().canonicalFile
}
