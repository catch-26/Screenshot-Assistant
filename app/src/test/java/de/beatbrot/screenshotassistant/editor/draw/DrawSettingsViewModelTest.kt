package de.beatbrot.screenshotassistant.editor.draw

import android.graphics.Color
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DrawSettingsViewModelTest {

    private lateinit var viewModel: DrawSettingsViewModel

    private val pen: Pen
        get() = viewModel.pen.value!!

    @Before
    fun initVM() {
        ArchTaskExecutor.getInstance().setDelegate(TestExecutor())
        viewModel = DrawSettingsViewModel()
    }

    @After
    fun resetExecutor() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun startsWithPenInItsDefaultColor() {
        assertEquals(Pen(DrawMode.PEN, DrawMode.PEN.defaultColor), pen)
    }

    @Test
    fun eachModeKeepsItsOwnColor() {
        viewModel.selectColor(Color.RED)
        viewModel.selectMode(DrawMode.MARKER)
        assertEquals(Pen(DrawMode.MARKER, DrawMode.MARKER.defaultColor), pen)

        viewModel.selectColor(Color.BLUE)
        viewModel.selectMode(DrawMode.PEN)
        assertEquals(Pen(DrawMode.PEN, Color.RED), pen)

        viewModel.selectMode(DrawMode.MARKER)
        assertEquals(Pen(DrawMode.MARKER, Color.BLUE), pen)
    }

    @Test
    fun restoredColorsApplyToTheCurrentMode() {
        viewModel.selectMode(DrawMode.MARKER)
        viewModel.restoreColors { mode -> if (mode == DrawMode.MARKER) Color.GREEN else Color.RED }
        assertEquals(Pen(DrawMode.MARKER, Color.GREEN), pen)

        viewModel.selectMode(DrawMode.PEN)
        assertEquals(Pen(DrawMode.PEN, Color.RED), pen)
    }

    private class TestExecutor : TaskExecutor() {
        override fun executeOnDiskIO(r: Runnable) = r.run()

        override fun isMainThread() = true

        override fun postToMainThread(r: Runnable) = r.run()
    }
}
