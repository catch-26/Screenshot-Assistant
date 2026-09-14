package de.beatbrot.screenshotassistant.util

import android.graphics.Bitmap
import com.canhub.cropper.CropImageView

fun CropImageView.tryExport(): Bitmap? {
    return try {
        getCroppedImage()
    } catch (exception: Exception) {
        null
    }
}
