package de.beatbrot.screenshotassistant.editor

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

// The screenshot behind the editor is faded more than the cropper's cut-away area.
const val BACKGROUND_FADE = 0.75F
const val CROP_OUTSIDE_FADE = 0.5F

/** Reduces saturation and brightness by the same share, 0 = unchanged, 1 = black. */
fun fadeFilter(reduction: Float): ColorMatrixColorFilter {
    val keep = 1F - reduction
    val matrix = ColorMatrix().apply {
        setSaturation(keep)
        postConcat(ColorMatrix().apply { setScale(keep, keep, keep, 1F) })
    }
    return ColorMatrixColorFilter(matrix)
}

/** Reduces brightness only, keeping colors saturated, 0 = unchanged, 1 = black. */
fun darkenFilter(reduction: Float): ColorMatrixColorFilter {
    val keep = 1F - reduction
    val matrix = ColorMatrix().apply { setScale(keep, keep, keep, 1F) }
    return ColorMatrixColorFilter(matrix)
}
