package de.beatbrot.screenshotassistant.util

import android.content.SharedPreferences
import android.graphics.Bitmap

val SharedPreferences.imageQuality: Int
    get() = getInt("image_quality", 100)

val SharedPreferences.imageFormat: Bitmap.CompressFormat
    get() = Bitmap.CompressFormat.valueOf(getString("image_format", null) ?: "PNG")

val SharedPreferences.penColor: Int
    get() = getInt("pen_color", 0xFF000000.toInt())

val SharedPreferences.markerColor: Int
    get() = getInt("marker_color", 0xFFFACC15.toInt())
