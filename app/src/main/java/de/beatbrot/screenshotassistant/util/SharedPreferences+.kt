package de.beatbrot.screenshotassistant.util

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import de.beatbrot.screenshotassistant.sheets.drawsettings.DrawSettingsViewModel

var SharedPreferences.imageFormat: Bitmap.CompressFormat
    get() = Bitmap.CompressFormat.valueOf(getString("image_format", null) ?: "PNG")
    set(value) = edit().putString("image_format", value.name).apply()

var SharedPreferences.penColor: Int
    get() = getInt("pen_color", Color.BLACK)
    set(value) = edit().putInt("pen_color", value).apply()

var SharedPreferences.markerColor: Int
    get() = getInt("marker_color", DrawSettingsViewModel.DEFAULT_MARKER_COLOR)
    set(value) = edit().putInt("marker_color", value).apply()

var SharedPreferences.saveFolder: Uri?
    get() = getString("save_folder", null)?.let(Uri::parse)
    set(value) = edit().putString("save_folder", value?.toString()).apply()
