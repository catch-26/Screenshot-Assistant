package de.beatbrot.screenshotassistant.custom

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import com.canhub.cropper.CropImageView
import de.beatbrot.screenshotassistant.editor.draw.DrawingView
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals

fun cropImageByHalf() = object : ViewAction {
    override fun getDescription() = "Selects the top half of the image"

    override fun getConstraints(): Matcher<View> = Matchers.instanceOf(CropImageView::class.java)

    override fun perform(uiController: UiController?, view: View?) {
        val cropView = view as CropImageView
        cropView.cropRect = Rect(cropView.wholeImageRect).apply { bottom /= 2 }
    }
}

fun draw() = object : ViewAction {
    override fun getDescription() = "Draws on the drawing view"

    override fun getConstraints() = Matchers.instanceOf<View>(DrawingView::class.java)

    override fun perform(uiController: UiController?, view: View?) {
        val drawingView = view as DrawingView
        drawingView.onTouchEvent(motionEvent(MotionEvent.ACTION_DOWN, 10, 20))
        drawingView.onTouchEvent(motionEvent(MotionEvent.ACTION_MOVE, 20, 50))
        drawingView.onTouchEvent(motionEvent(MotionEvent.ACTION_UP, 20, 50))
    }
}

fun checkCropIsTopHalf() = ViewAssertion { view, noViewFound ->
    if (noViewFound != null) throw noViewFound
    val cropView = view as CropImageView
    assertEquals(cropView.wholeImageRect.height() / 2, cropView.getCroppedImage()!!.height)
}

private fun motionEvent(action: Int, x: Int, y: Int): MotionEvent {
    return MotionEvent.obtain(1L, 1L, action, x.toFloat(), y.toFloat(), 0)
}
