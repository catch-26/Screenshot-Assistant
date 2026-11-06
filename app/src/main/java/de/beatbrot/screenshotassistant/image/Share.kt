package de.beatbrot.screenshotassistant.image

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import de.beatbrot.screenshotassistant.BuildConfig
import java.io.File

// Must match the files-path in res/xml/fileprovider.xml and the provider authority in the manifest.
private const val SHARE_DIR = "screenshots"
private val SHARE_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"

// Only the latest shared image is kept, since the receiving app reads it right away.
fun Context.shareIntent(image: Bitmap, format: ImageFormat): Intent {
    val dir = File(filesDir, SHARE_DIR)
    dir.mkdir()
    dir.listFiles()?.forEach { it.deleteRecursively() }
    val file = File(dir, format.newFileName())
    file.outputStream().use { format.encode(image, it) }
    val uri = FileProvider.getUriForFile(this, SHARE_AUTHORITY, file)
    return Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
