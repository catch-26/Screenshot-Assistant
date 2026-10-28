package de.beatbrot.screenshotassistant.sheets.drawsettings

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.beatbrot.screenshotassistant.R
import java.util.Locale

class ColorPickerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val density = resources.displayMetrics.density
        val size = (44 * density).toInt()
        val margin = (8 * density).toInt()
        val selected = requireArguments().getInt(COLOR)

        val grid = GridLayout(context).apply {
            columnCount = 4
            setPadding(margin * 2, margin * 2, margin * 2, 0)
        }
        resources.getIntArray(R.array.colorPicker).forEach { color ->
            val swatch = FrameLayout(context).apply {
                background = swatch(context, color)
                foreground = ContextCompat.getDrawable(context, R.drawable.ripple_oval)
                contentDescription = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                if (color == selected) {
                    addView(ImageView(context).apply {
                        setImageResource(R.drawable.ic_check_black_24dp)
                        imageTintList = ColorStateList.valueOf(
                            if (ColorUtils.calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE
                        )
                    }, FrameLayout.LayoutParams(size / 2, size / 2, Gravity.CENTER))
                }
                setOnClickListener {
                    parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt(COLOR, color) })
                    dismiss()
                }
            }
            grid.addView(swatch)
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pick_color)
            .setView(grid)
            .create()
    }

    companion object {
        const val RESULT = "color_picker"
        const val COLOR = "color"

        fun newInstance(selected: Int) = ColorPickerDialog().apply {
            arguments = Bundle().apply { putInt(COLOR, selected) }
        }

        fun swatch(context: Context, color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(
                (1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                ContextCompat.getColor(context, R.color.cardStroke)
            )
        }
    }
}
