package de.beatbrot.screenshotassistant.editor.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils

/**
 * Shows an image and lets the user draw strokes on it. Strokes are stored in the coordinates
 * of the full screenshot, so they survive layout changes, export at full resolution and keep
 * their place when the shown image is only a cropped part of the screenshot.
 */
class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var pen = Pen.DEFAULT

    val drawStack: DrawStack
        get() = DrawStack(undo.toList(), redo.toList())

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

    private var offsetX = 0F
    private var offsetY = 0F

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

    fun show(screenshot: Bitmap, crop: Rect?, stack: DrawStack) {
        val whole = Rect(0, 0, screenshot.width, screenshot.height)
        val part = Rect(crop ?: whole).takeIf { it.intersect(whole) && !it.isEmpty } ?: whole
        setImageBitmap(Bitmap.createBitmap(screenshot, part.left, part.top, part.width(), part.height()))
        offsetX = part.left.toFloat()
        offsetY = part.top.toFloat()
        undo = ArrayDeque(stack.undo)
        redo = ArrayDeque(stack.redo)
        current = null
        invalidate()
        notifyHistoryChanged()
    }

    fun exportImage(): Bitmap? {
        val image = drawable ?: return null
        val result = Bitmap.createBitmap(image.intrinsicWidth, image.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val bounds = image.copyBounds()
        image.setBounds(0, 0, image.intrinsicWidth, image.intrinsicHeight)
        image.draw(canvas)
        image.bounds = bounds
        canvas.translate(-offsetX, -offsetY)
        drawStrokes(undo, canvas)
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val image = drawable ?: return
        val save = canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.concat(imageMatrix)
        canvas.clipRect(0, 0, image.intrinsicWidth, image.intrinsicHeight)
        canvas.translate(-offsetX, -offsetY)
        drawStrokes(undo, canvas)
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
            strokeCap = pen.mode.cap
            color = ColorUtils.setAlphaComponent(pen.color, pen.mode.alpha)
            strokeWidth = toImage.mapRadius(pen.mode.width)
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
        point[0] += offsetX
        point[1] += offsetY
    }

    private fun notifyHistoryChanged() {
        onHistoryChanged?.invoke(canUndo(), canRedo())
    }

    private companion object {
        const val SMOOTHING = 0.6F
    }
}
