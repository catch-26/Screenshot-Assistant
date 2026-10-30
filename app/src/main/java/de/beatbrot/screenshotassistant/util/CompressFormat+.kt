package de.beatbrot.screenshotassistant.util

import android.graphics.Bitmap
import java.util.*

val Enum<Bitmap.CompressFormat>.mimeType
    get() = "image/${name.lowercase(Locale.ROOT)}"

val Enum<Bitmap.CompressFormat>.fileExtension
    get() = label.lowercase(Locale.ROOT)

val Enum<Bitmap.CompressFormat>.label
    get() = if (this == Bitmap.CompressFormat.JPEG) "JPG" else name
