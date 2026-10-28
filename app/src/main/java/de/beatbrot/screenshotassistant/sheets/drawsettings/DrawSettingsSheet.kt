package de.beatbrot.screenshotassistant.sheets.drawsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.databinding.SheetColorsettingsBinding
import de.beatbrot.screenshotassistant.draw.DrawingView
import de.beatbrot.screenshotassistant.util.markerColor
import de.beatbrot.screenshotassistant.util.penColor
import de.beatbrot.screenshotassistant.util.sharedPrefs

class DrawSettingsSheet : Fragment() {
    private lateinit var v: SheetColorsettingsBinding

    var drawingView: DrawingView? = null
        set(value) {
            field = value
            field?.onHistoryChanged = { canUndo, canRedo ->
                if (::v.isInitialized) {
                    v.undoButton.isEnabled = canUndo
                    v.redoButton.isEnabled = canRedo
                }
            }
        }

    var onHideListener: (() -> Unit)? = null

    private val viewModel by viewModels<DrawSettingsViewModel>()

    override fun onCreateView(i: LayoutInflater, root: ViewGroup?, state: Bundle?): View {
        v = SheetColorsettingsBinding.inflate(i, root, false)
        return v.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireContext().sharedPrefs
        viewModel.penColor.value = prefs.penColor
        viewModel.markerColor.value = prefs.markerColor
        initViewModel()
        childFragmentManager.setFragmentResultListener(ColorPickerDialog.RESULT, this) { _, result ->
            val color = result.getInt(ColorPickerDialog.COLOR)
            viewModel.selectColor(color)
            if (viewModel.editingMode.value == DrawMode.MARKER) {
                prefs.markerColor = color
            } else {
                prefs.penColor = color
            }
        }
    }

    private fun initViewModel() {
        viewModel.strokeColor.observe(this, Observer { newValue ->
            if (newValue == null) {
                return@Observer
            }
            v.colorButton.background = ColorPickerDialog.swatch(requireContext(), newValue)
            if (viewModel.editingMode.value == DrawMode.PEN) {
                drawingView?.strokeColor = newValue
            } else {
                drawingView?.strokeColor = ColorUtils.setAlphaComponent(newValue, 255 / 3)
            }
        })

        viewModel.strokeCap.observe(this, Observer { newValue ->
            drawingView?.strokeCap = newValue
        })

        viewModel.strokeWidth.observe(this, Observer { newValue ->
            drawingView?.strokeWidth = newValue
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        v.undoButton.isEnabled = drawingView?.canUndo() ?: false
        v.redoButton.isEnabled = drawingView?.canRedo() ?: false

        v.undoButton.setOnClickListener { drawingView?.undo() }
        v.redoButton.setOnClickListener { drawingView?.redo() }
        v.penSelector.check(
            if (viewModel.editingMode.value == DrawMode.MARKER) R.id.markerButton else R.id.penButton
        )
        v.penSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.editingMode.value = when (checkedId) {
                    R.id.markerButton -> DrawMode.MARKER
                    else -> DrawMode.PEN
                }
            }
        }

        v.doneButton.setOnClickListener { onHideListener?.invoke() }
        v.colorButton.setOnClickListener { showColorPicker() }
    }

    private fun showColorPicker() {
        val selected = viewModel.strokeColor.value ?: return
        ColorPickerDialog.newInstance(selected).show(childFragmentManager, "COLOR_PICKER")
    }
}
