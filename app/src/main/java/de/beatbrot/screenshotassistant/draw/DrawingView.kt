package de.beatbrot.screenshotassistant.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView

/**
 * Shows an image and lets the user draw strokes on it. Strokes are stored in image
 * coordinates, so they survive layout changes and export at full resolution.
 */
class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    @ColorInt
    var strokeColor: Int = Color.BLACK

    /** Width in view pixels at the current zoom. */
    var strokeWidth: Float = 6F

    var strokeCap: Paint.Cap = Paint.Cap.ROUND

    var drawStack: DrawStack
        get() = DrawStack(undo.toList(), redo.toList())
        set(value) {
            undo = ArrayDeque(value.undo)
            redo = ArrayDeque(value.redo)
            current = null
            invalidate()
            notifyHistoryChanged()
        }

    var onHistoryChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null
        set(value) {
            field = value
            notifyHistoryChanged()
        }

    private var undo = ArrayDeque<Stroke>()
    private var redo = ArrayDeque<Stroke>()

    private var current: Stroke? = null
    private var moved = false
    private var lastX = 0F
    private var lastY = 0F

    private val toImage = Matrix()
    private val point = FloatArray(2)

    init {
        scaleType = ScaleType.FIT_CENTER
    }

    fun canUndo() = undo.isNotEmpty()

    fun canRedo() = redo.isNotEmpty()

    fun undo() {
        if (!canUndo()) return
        redo.addFirst(undo.removeLast())
        invalidate()
        notifyHistoryChanged()
    }

    fun redo() {
        if (!canRedo()) return
        undo.addLast(redo.removeFirst())
        invalidate()
        notifyHistoryChanged()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        undo.clear()
        redo.clear()
        current = null
        invalidate()
        notifyHistoryChanged()
    }

    fun exportImage(): Bitmap {
        val image = drawable
        val result = Bitmap.createBitmap(image.intrinsicWidth, image.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val bounds = image.copyBounds()
        image.setBounds(0, 0, image.intrinsicWidth, image.intrinsicHeight)
        image.draw(canvas)
        image.bounds = bounds
        drawStrokes(canvas)
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val image = drawable ?: return
        val save = canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.concat(imageMatrix)
        canvas.clipRect(0, 0, image.intrinsicWidth, image.intrinsicHeight)
        drawStrokes(canvas)
        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startStroke(event.x, event.y)
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    continueStroke(event.getHistoricalX(i), event.getHistoricalY(i))
                }
                continueStroke(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endStroke(event.x, event.y)
                performClick()
            }
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun startStroke(x: Float, y: Float) {
        imageMatrix.invert(toImage)
        mapToImage(x, y)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = this@DrawingView.strokeCap
            color = strokeColor
            strokeWidth = toImage.mapRadius(this@DrawingView.strokeWidth)
        }
        val stroke = Stroke(Path().apply { moveTo(point[0], point[1]) }, paint)
        lastX = point[0]
        lastY = point[1]
        moved = false
        current = stroke
        undo.addLast(stroke)
        redo.clear()
        notifyHistoryChanged()
    }

    // Eases each point towards the finger and joins midpoints with quadratic curves,
    // which removes the jitter of raw touch samples without visible lag.
    private fun continueStroke(x: Float, y: Float) {
        val path = current?.path ?: return
        mapToImage(x, y)
        val smoothX = lastX + (point[0] - lastX) * SMOOTHING
        val smoothY = lastY + (point[1] - lastY) * SMOOTHING
        path.quadTo(lastX, lastY, (lastX + smoothX) / 2, (lastY + smoothY) / 2)
        lastX = smoothX
        lastY = smoothY
        moved = true
    }

    private fun endStroke(x: Float, y: Float) {
        val path = current?.path ?: return
        mapToImage(x, y)
        if (moved) {
            path.quadTo(lastX, lastY, point[0], point[1])
        } else {
            path.lineTo(point[0], point[1])
        }
        current = null
    }

    private fun mapToImage(x: Float, y: Float) {
        point[0] = x - paddingLeft
        point[1] = y - paddingTop
        toImage.mapPoints(point)
    }

    private fun drawStrokes(canvas: Canvas) {
        for (stroke in undo) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
    }

    private fun notifyHistoryChanged() {
        onHistoryChanged?.invoke(canUndo(), canRedo())
    }

    private companion object {
        const val SMOOTHING = 0.6F
    }
}
