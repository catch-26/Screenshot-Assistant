package de.beatbrot.screenshotassistant.editor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

private const val EXTRA_SCREENSHOT = "screenshot"

private val Context.handoffFile: File
    get() = File(File(cacheDir, "screenshots"), "scr")

// The assistant hands its screenshot to the editor through one cache file, so a new gesture replaces the last one.
@Suppress("DEPRECATION")
fun Context.editorIntent(screenshot: Bitmap): Intent {
    val file = handoffFile
    file.parentFile?.mkdir()
    file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.WEBP, 100, it) }
    return Intent(this, EditorActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .putExtra(EXTRA_SCREENSHOT, Uri.fromFile(file))
}

// The editor activity is not exported, but the edit alias shares it, so only the handoff file itself is accepted.
fun Intent.assistantScreenshot(context: Context): Uri? {
    val uri = getParcelableExtra(EXTRA_SCREENSHOT, Uri::class.java) ?: return null
    val path = uri.path ?: return null
    return uri.takeIf { it.scheme == "file" && File(path).canonicalFile == context.handoffFile.canonicalFile }
}

fun Context.deleteAssistantScreenshot() {
    handoffFile.delete()
}
