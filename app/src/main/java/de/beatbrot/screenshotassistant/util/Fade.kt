package de.beatbrot.screenshotassistant.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

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
