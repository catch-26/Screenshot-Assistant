package de.beatbrot.screenshotassistant.editor.draw

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint

/** Width is in view pixels at the current zoom. */
enum class DrawMode(val defaultColor: Int, val alpha: Int, val width: Float, val cap: Paint.Cap) {
    PEN(Color.BLACK, 255, 6F, Paint.Cap.ROUND),
    MARKER(0xFFFACC15.toInt(), 255 / 3, 45F, Paint.Cap.SQUARE)
}

/** The color is stored without the mode's alpha. */
data class Pen(val mode: DrawMode, val color: Int) {
    companion object {
        val DEFAULT = Pen(DrawMode.PEN, DrawMode.PEN.defaultColor)
    }
}

private val DrawMode.colorKey: String
    get() = when (this) {
        DrawMode.PEN -> "pen_color"
        DrawMode.MARKER -> "marker_color"
    }

fun SharedPreferences.drawColor(mode: DrawMode): Int = getInt(mode.colorKey, mode.defaultColor)

fun SharedPreferences.setDrawColor(mode: DrawMode, color: Int) = edit().putInt(mode.colorKey, color).apply()
