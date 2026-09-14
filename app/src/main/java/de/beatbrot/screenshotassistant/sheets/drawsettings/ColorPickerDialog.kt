package de.beatbrot.screenshotassistant.sheets.drawsettings

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.beatbrot.screenshotassistant.R

class ColorPickerDialog : DialogFragment() {
    var onColorSelected: ((Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val density = resources.displayMetrics.density
        val size = (48 * density).toInt()
        val margin = (12 * density).toInt()

        val grid = GridLayout(requireContext()).apply {
            columnCount = 3
            setPadding(margin, margin, margin, 0)
        }
        resources.getIntArray(R.array.colorPicker).forEach { color ->
            val swatch = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener {
                    onColorSelected?.invoke(color)
                    dismiss()
                }
            }
            grid.addView(swatch)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pick_color)
            .setView(grid)
            .create()
    }
}
