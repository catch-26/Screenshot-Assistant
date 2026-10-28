package de.beatbrot.screenshotassistant.util

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import de.beatbrot.screenshotassistant.sheets.drawsettings.DrawSettingsViewModel

val SharedPreferences.imageQuality: Int
    get() = getInt("image_quality", 100)

val SharedPreferences.imageFormat: Bitmap.CompressFormat
    get() = Bitmap.CompressFormat.valueOf(getString("image_format", null) ?: "PNG")

var SharedPreferences.penColor: Int
    get() = getInt("pen_color", Color.BLACK)
    set(value) = edit().putInt("pen_color", value).apply()

var SharedPreferences.markerColor: Int
    get() = getInt("marker_color", DrawSettingsViewModel.DEFAULT_MARKER_COLOR)
    set(value) = edit().putInt("marker_color", value).apply()
