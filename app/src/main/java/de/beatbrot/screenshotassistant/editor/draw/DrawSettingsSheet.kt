package de.beatbrot.screenshotassistant.editor.draw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.databinding.SheetColorsettingsBinding
import de.beatbrot.screenshotassistant.util.sharedPrefs

class DrawSettingsSheet : Fragment() {
    private lateinit var v: SheetColorsettingsBinding

    var drawingView: DrawingView? = null

    var onDone: (() -> Unit)? = null

    private val viewModel by viewModels<DrawSettingsViewModel>()

    private val modeButtons = mapOf(DrawMode.PEN to R.id.penButton, DrawMode.MARKER to R.id.markerButton)

    override fun onCreateView(i: LayoutInflater, root: ViewGroup?, state: Bundle?): View {
        v = SheetColorsettingsBinding.inflate(i, root, false)
        return v.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireContext().sharedPrefs
        viewModel.restoreColors { prefs.drawColor(it) }
        viewModel.pen.observe(this) { pen ->
            v.colorButton.background = ColorPickerDialog.swatch(requireContext(), pen.color)
            drawingView?.pen = pen
        }
        childFragmentManager.setFragmentResultListener(ColorPickerDialog.RESULT, this) { _, result ->
            val color = result.getInt(ColorPickerDialog.COLOR)
            viewModel.selectColor(color)
            viewModel.pen.value?.let { prefs.setDrawColor(it.mode, color) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawingView?.onHistoryChanged = { canUndo, canRedo ->
            v.undoButton.isEnabled = canUndo
            v.redoButton.isEnabled = canRedo
        }

        v.undoButton.setOnClickListener { drawingView?.undo() }
        v.redoButton.setOnClickListener { drawingView?.redo() }
        viewModel.pen.value?.let { v.penSelector.check(modeButtons.getValue(it.mode)) }
        v.penSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            val mode = modeButtons.entries.firstOrNull { it.value == checkedId }?.key
            if (isChecked && mode != null) {
                viewModel.selectMode(mode)
            }
        }

        v.doneButton.setOnClickListener { onDone?.invoke() }
        v.colorButton.setOnClickListener { showColorPicker() }
    }

    private fun showColorPicker() {
        val selected = viewModel.pen.value?.color ?: return
        ColorPickerDialog.newInstance(selected).show(childFragmentManager, "COLOR_PICKER")
    }
}
