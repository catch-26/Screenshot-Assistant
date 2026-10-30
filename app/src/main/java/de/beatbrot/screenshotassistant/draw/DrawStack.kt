package de.beatbrot.screenshotassistant.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class Stroke internal constructor(internal val path: Path, internal val paint: Paint)

data class DrawStack(val undo: List<Stroke>, val redo: List<Stroke>) {
    // Returns a copy of the full screenshot with the visible strokes painted on it.
    fun paintedOnto(screenshot: Bitmap): Bitmap {
        if (undo.isEmpty()) return screenshot
        val result = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        for (stroke in undo) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
        return result
    }
}
