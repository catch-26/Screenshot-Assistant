package de.beatbrot.screenshotassistant.sheets.drawsettings

import android.app.Application
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import de.beatbrot.screenshotassistant.util.liveDataOf
import de.beatbrot.screenshotassistant.util.markerColor
import de.beatbrot.screenshotassistant.util.penColor
import de.beatbrot.screenshotassistant.util.sharedPrefs

class DrawSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.sharedPrefs

    val editingMode = liveDataOf(DrawMode.PEN)

    val penColor = liveDataOf(prefs.penColor)
    val markerColor = liveDataOf(prefs.markerColor)

    val strokeColor = MediatorLiveData<Int>().apply {
        addSource(penColor) { newValue ->
            if (editingMode.value == DrawMode.PEN) {
                value = newValue
            }
        }
        addSource(markerColor) { newValue ->
            if (editingMode.value == DrawMode.MARKER) {
                value = newValue
            }
        }
        addSource(editingMode) { drawMode ->
            value = when (drawMode) {
                DrawMode.PEN -> penColor.value
                DrawMode.MARKER -> markerColor.value
                else -> value
            }
        }
    }

    val strokeCap = MediatorLiveData<Paint.Cap>().apply {
        addSource(editingMode) { newMode ->
            value = when (newMode) {
                DrawMode.PEN -> Paint.Cap.ROUND
                DrawMode.MARKER -> Paint.Cap.SQUARE
                else -> value
            }
        }
    }

    val strokeWidth = MediatorLiveData<Float>().apply {
        addSource(editingMode) { newMode ->
            value = when (newMode) {
                DrawMode.PEN -> 6F
                DrawMode.MARKER -> 30F
                else -> value
            }
        }
    }

    fun selectColor(color: Int) {
        when (editingMode.value) {
            DrawMode.MARKER -> {
                markerColor.value = color
                prefs.edit().putInt("marker_color", color).apply()
            }
            else -> {
                penColor.value = color
                prefs.edit().putInt("pen_color", color).apply()
            }
        }
    }
}
