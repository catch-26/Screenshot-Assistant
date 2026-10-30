package de.beatbrot.screenshotassistant.crop

import android.util.Log
import com.canhub.cropper.CropImageView
import kotlin.math.min

private const val TAG = "CropZoom"

/**
 * Zooms the cropper so the crop window fills [fill] of the view. The cropper's own auto-zoom only
 * reacts to crop windows below 50 percent or above 65 percent of the view, so with it off this
 * sets the zoom it uses itself. The frame keeps its size because the image is zoomed, not the view.
 * Does nothing if the cropper library no longer has the members used here.
 */
fun CropImageView.zoomToCropWindow(fill: Float) {
    val window = cropWindowRect ?: return
    if (window.isEmpty || width == 0 || height == 0) {
        return
    }
    try {
        val zoomField = CropImageView::class.java.getDeclaredField("mZoom").apply { isAccessible = true }
        val applyImageMatrix = CropImageView::class.java.getDeclaredMethod(
            "applyImageMatrix",
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val current = zoomField.getFloat(this)
        val fitted = min(width / (window.width() / current), height / (window.height() / current))
        zoomField.setFloat(this, (fitted * fill).coerceIn(1F, maxZoom.toFloat()))
        applyImageMatrix.invoke(this, width.toFloat(), height.toFloat(), true, false)
    } catch (e: ReflectiveOperationException) {
        Log.e(TAG, "Could not zoom to the crop window", e)
    }
}
