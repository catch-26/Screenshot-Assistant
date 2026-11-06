package de.beatbrot.screenshotassistant.editor.draw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DrawSettingsViewModel : ViewModel() {
    private val colors = DrawMode.entries.associateWith { it.defaultColor }.toMutableMap()

    private val _pen = MutableLiveData(Pen.DEFAULT)
    val pen: LiveData<Pen>
        get() = _pen

    private val mode: DrawMode
        get() = checkNotNull(_pen.value).mode

    fun restoreColors(saved: (DrawMode) -> Int) {
        DrawMode.entries.forEach { colors[it] = saved(it) }
        selectMode(mode)
    }

    fun selectMode(mode: DrawMode) {
        _pen.value = Pen(mode, colors.getValue(mode))
    }

    fun selectColor(color: Int) {
        colors[mode] = color
        _pen.value = Pen(mode, color)
    }
}
