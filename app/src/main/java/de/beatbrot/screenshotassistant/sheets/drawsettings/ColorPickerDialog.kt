package de.beatbrot.screenshotassistant.sheets.drawsettings

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.MaterialColors
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.databinding.DialogColorPickerBinding
import java.util.Locale

class ColorPickerDialog : DialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.ThemeOverlay_App_ColorPicker)
        val context = dialog.context
        val density = resources.displayMetrics.density
        val size = (44 * density).toInt()
        val margin = (6 * density).toInt()
        val selected = requireArguments().getInt(COLOR)
        val v = DialogColorPickerBinding.inflate(dialog.layoutInflater)

        resources.getIntArray(R.array.colorPicker).forEach { color ->
            val swatch = FrameLayout(context).apply {
                background = if (color == selected) selectedSwatch(context, color) else swatch(context, color)
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
                    }, FrameLayout.LayoutParams(size * 5 / 11, size * 5 / 11, Gravity.CENTER))
                }
                setOnClickListener {
                    parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putInt(COLOR, color) })
                    dismiss()
                }
            }
            v.grid.addView(swatch)
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(v.root)
        return dialog
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

        private fun selectedSwatch(context: Context, color: Int): Drawable {
            val density = context.resources.displayMetrics.density
            val ring = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(
                    (2 * density).toInt(),
                    MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
                )
            }
            return LayerDrawable(arrayOf(ring, InsetDrawable(swatch(context, color), (5 * density).toInt())))
        }
    }
}
