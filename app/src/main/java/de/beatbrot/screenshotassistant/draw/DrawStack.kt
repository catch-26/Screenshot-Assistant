package de.beatbrot.screenshotassistant.draw

import android.graphics.Paint
import android.graphics.Path

class Stroke internal constructor(internal val path: Path, internal val paint: Paint)

data class DrawStack(val undo: List<Stroke>, val redo: List<Stroke>)
