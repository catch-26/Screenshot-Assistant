package de.beatbrot.screenshotassistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.canhub.cropper.CropImageView
import de.beatbrot.screenshotassistant.editor.EditorActivity
import de.beatbrot.screenshotassistant.editor.draw.DrawingView
import de.beatbrot.screenshotassistant.editor.editorIntent
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReadmeScreenshots {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val suffix = if (InstrumentationRegistry.getArguments().getString("night") == "yes") " - dark" else ""
    private val outDir = File(context.getExternalFilesDir(null), "readme").apply { mkdirs() }

    @Test
    fun capture() {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        settle(3000)
        var behind = instrumentation.uiAutomation.takeScreenshot()
        repeat(20) {
            if (colors(behind) > 40) return@repeat
            SystemClock.sleep(1000)
            behind = instrumentation.uiAutomation.takeScreenshot()
        }
        settle(1000)
        behind = instrumentation.uiAutomation.takeScreenshot()

        ActivityScenario.launch<EditorActivity>(context.editorIntent(behind).setFlags(0)).use { scenario ->
            settle(2500)
            shot("1 - editor")

            scenario.onActivity { activity ->
                val crop = activity.findViewById<CropImageView>(R.id.cropView)
                val whole = crop.wholeImageRect!!
                crop.cropRect = Rect(
                    whole.width() * 4 / 100,
                    whole.height() * 22 / 100,
                    whole.width() * 96 / 100,
                    whole.height() * 52 / 100
                )
            }
            settle(1500)
            shot("2 - crop")

            onView(withId(R.id.drawButton)).perform(click())
            settle(1500)
            onView(withId(R.id.markerButton)).perform(click())
            scenario.onActivity { activity ->
                val view = activity.findViewById<DrawingView>(R.id.drawingView)
                stroke(view, listOf(0.13f to 0.25f, 0.52f to 0.25f))
            }
            onView(withId(R.id.penButton)).perform(click())
            scenario.onActivity { activity ->
                val view = activity.findViewById<DrawingView>(R.id.drawingView)
                val points = (0..48).map { i ->
                    val a = Math.toRadians(i * 7.5 + 200)
                    (0.33f + 0.27f * Math.cos(a).toFloat()) to (0.56f + 0.2f * Math.sin(a).toFloat())
                }
                stroke(view, points)
            }
            settle(1500)
            shot("3 - draw")

            onView(withId(R.id.doneButton)).perform(click())
            settle(1500)
            onView(withId(R.id.shareButton)).perform(click())
            settle(7000)
            shot("4 - share")
            instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            settle(1500)
        }

        ActivityScenario.launch(LauncherActivity::class.java).use {
            settle(2500)
            shot("5 - setup")
        }

        ActivityScenario.launch(AboutActivity::class.java).use {
            settle(2500)
            shot("6 - about")
        }
    }

    private fun colors(bitmap: Bitmap): Int {
        val seen = HashSet<Int>()
        for (x in 0 until bitmap.width step 20) {
            for (y in 0 until bitmap.height step 20) {
                seen += bitmap.getPixel(x, y)
            }
        }
        return seen.size
    }

    private fun stroke(view: DrawingView, points: List<Pair<Float, Float>>) {
        val image = view.drawable ?: return
        val box = RectF(0f, 0f, image.intrinsicWidth.toFloat(), image.intrinsicHeight.toFloat())
        view.imageMatrix.mapRect(box)
        box.offset(view.paddingLeft.toFloat(), view.paddingTop.toFloat())
        val start = SystemClock.uptimeMillis()
        points.forEachIndexed { i, (fx, fy) ->
            val action = when (i) {
                0 -> MotionEvent.ACTION_DOWN
                points.lastIndex -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val event = MotionEvent.obtain(start, start + i * 16L, action, box.left + fx * box.width(), box.top + fy * box.height(), 0)
            view.onTouchEvent(event)
            event.recycle()
        }
    }

    private fun settle(millis: Long) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(millis)
    }

    private fun shot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(outDir, "$name$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
