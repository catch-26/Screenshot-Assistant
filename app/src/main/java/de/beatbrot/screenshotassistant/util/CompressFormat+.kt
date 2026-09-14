package de.beatbrot.screenshotassistant.util

import android.graphics.Bitmap
import java.util.*

val Enum<Bitmap.CompressFormat>.mimeType
    get() = "image/${name.lowercase(Locale.ROOT)}"

val Enum<Bitmap.CompressFormat>.fileExtension
    get() = name.lowercase(Locale.ROOT)
