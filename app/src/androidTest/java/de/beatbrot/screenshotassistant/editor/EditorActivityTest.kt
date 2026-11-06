package de.beatbrot.screenshotassistant.editor

import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.custom.checkCropIsTopHalf
import de.beatbrot.screenshotassistant.custom.containsImage
import de.beatbrot.screenshotassistant.custom.cropImageByHalf
import de.beatbrot.screenshotassistant.custom.draw
import de.beatbrot.screenshotassistant.editor.draw.DrawingView
import de.beatbrot.screenshotassistant.image.ImageFormat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorActivityTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val activityRule = IntentsTestRule(EditorActivity::class.java, false, false)

    @Before
    fun launchWithAssistantScreenshot() {
        val screenshot = testContext.assets.open("scr.png").use { BitmapFactory.decodeStream(it) }
        activityRule.launchActivity(context.editorIntent(screenshot).setFlags(0))
    }

    @Test
    fun showsTheScreenshot() {
        onView(withId(R.id.cropView))
            .check(matches(containsImage()))
    }

    @Test
    fun cropsTheImage() {
        onView(withId(R.id.cropView))
            .perform(cropImageByHalf())
            .check(checkCropIsTopHalf())
    }

    @Test
    fun drawButtonOpensTheDrawingView() {
        onView(withId(R.id.drawButton))
            .perform(click())

        onView(withId(R.id.drawingView))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun formatDrawerChangesFormat() {
        onView(withId(R.id.formatChip))
            .perform(click())

        onView(allOf(withText(ImageFormat.JPEG.label), isDescendantOfA(withId(R.id.formatDrawer))))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(withId(R.id.formatChip))
            .check(matches(withText(ImageFormat.JPEG.label)))
        onView(withId(R.id.formatDrawer))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun cropSurvivesRecreation() {
        onView(withId(R.id.cropView))
            .perform(cropImageByHalf())

        enableNightMode()

        onView(withId(R.id.cropView))
            .check(checkCropIsTopHalf())
    }

    @Test
    fun strokesSurviveRecreationAndCanBeUndone() {
        onView(withId(R.id.drawButton))
            .perform(click())
        onView(withId(R.id.drawingView))
            .perform(draw())

        enableNightMode()

        val painter: DrawingView = activityRule.activity.findViewById(R.id.drawingView)
        assertTrue(painter.canUndo())
        assertFalse(painter.canRedo())

        onView(withId(R.id.undoButton))
            .perform(click())

        assertFalse(painter.canUndo())
        assertTrue(painter.canRedo())
    }

    private fun enableNightMode() {
        activityRule.runOnUiThread {
            activityRule.activity.delegate.apply {
                localNightMode = MODE_NIGHT_YES
                applyDayNight()
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
