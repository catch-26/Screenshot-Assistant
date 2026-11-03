package de.beatbrot.screenshotassistant.crop

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import com.canhub.cropper.CropImageView
import de.beatbrot.screenshotassistant.util.darkenFilter

/**
 * Draws the image darkened outside the crop window. It sits between the cropper's
 * image and its crop overlay, so the frame and handles stay on top.
 */
@SuppressLint("ViewConstructor")
class CropOutsideFadeView private constructor(private val cropView: CropImageView) : View(cropView.context) {

    private val image: ImageView = cropView.findViewById(com.canhub.cropper.R.id.ImageView_image)

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = darkenFilter(FADE)
    }

    private val window = RectF()
    private val lastMatrix = FloatArray(9)
    private val matrixValues = FloatArray(9)
    private var lastCrop: Rect? = null

    private val redrawWhenMoved = ViewTreeObserver.OnPreDrawListener {
        image.imageMatrix.getValues(matrixValues)
        val crop = cropView.cropRect
        if (!matrixValues.contentEquals(lastMatrix) || crop != lastCrop) {
            matrixValues.copyInto(lastMatrix)
            lastCrop = crop
            invalidate()
        }
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(redrawWhenMoved)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(redrawWhenMoved)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = (image.drawable as? BitmapDrawable)?.bitmap ?: return
        val crop = cropView.cropRect ?: return
        val matrix = Matrix(image.imageMatrix)
        window.set(crop)
        matrix.mapRect(window)

        val save = canvas.save()
        canvas.translate(image.paddingLeft.toFloat(), image.paddingTop.toFloat())
        canvas.clipOutRect(window)
        canvas.concat(matrix)
        canvas.drawBitmap(bitmap, 0F, 0F, paint)
        canvas.restoreToCount(save)
    }

    companion object {
        const val FADE = 0.5F

        fun attachTo(cropView: CropImageView) {
            cropView.addView(
                CropOutsideFadeView(cropView),
                1,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }
    }
}
