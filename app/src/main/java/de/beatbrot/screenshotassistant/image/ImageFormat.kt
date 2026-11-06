package de.beatbrot.screenshotassistant.image

import android.content.SharedPreferences
import android.graphics.Bitmap
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Entry names are stored in the preferences, so renaming one resets that choice.
@Suppress("DEPRECATION")
enum class ImageFormat(val label: String, private val compressFormat: Bitmap.CompressFormat) {
    JPEG("JPG", Bitmap.CompressFormat.JPEG),
    PNG("PNG", Bitmap.CompressFormat.PNG),
    WEBP("WEBP", Bitmap.CompressFormat.WEBP);

    val mimeType: String
        get() = "image/${name.lowercase(Locale.ROOT)}"

    val fileExtension: String
        get() = label.lowercase(Locale.ROOT)

    // JPG and WEBP are always written at maximum quality.
    fun encode(image: Bitmap, stream: OutputStream): Boolean = image.compress(compressFormat, 100, stream)

    fun newFileName(): String =
        "Screenshot-${SimpleDateFormat("yyyy-MM-dd'-'HH-mm-ss", Locale.US).format(Date())}.$fileExtension"
}

private const val FORMAT_KEY = "image_format"

var SharedPreferences.imageFormat: ImageFormat
    get() = ImageFormat.valueOf(getString(FORMAT_KEY, null) ?: ImageFormat.PNG.name)
    set(value) = edit().putString(FORMAT_KEY, value.name).apply()
