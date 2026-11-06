package de.beatbrot.screenshotassistant.editor.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class Stroke(val path: Path, val paint: Paint)

/** Strokes in full screenshot coordinates, so the crop can grow again after editing. */
data class DrawStack(val undo: List<Stroke> = emptyList(), val redo: List<Stroke> = emptyList()) {
    fun paintedOnto(screenshot: Bitmap): Bitmap {
        if (undo.isEmpty()) return screenshot
        val result = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        drawStrokes(undo, Canvas(result))
        return result
    }
}

internal fun drawStrokes(strokes: Iterable<Stroke>, canvas: Canvas) {
    for (stroke in strokes) {
        canvas.drawPath(stroke.path, stroke.paint)
    }
}
