package de.beatbrot.screenshotassistant.custom

import android.view.View
import com.canhub.cropper.CropImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

fun containsImage() = object : TypeSafeMatcher<View>(CropImageView::class.java) {
    override fun matchesSafely(item: View?): Boolean {
        val crop = item as CropImageView
        return (crop.getCroppedImage()?.byteCount ?: 0) > 10
    }

    override fun describeTo(description: Description?) {
        description?.appendText("has an image set")
    }
}
