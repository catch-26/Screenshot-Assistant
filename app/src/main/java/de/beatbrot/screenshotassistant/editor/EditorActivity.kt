package de.beatbrot.screenshotassistant.editor

import android.animation.TimeInterpolator
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AnimRes
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.databinding.ActivityEditorBinding
import de.beatbrot.screenshotassistant.databinding.ItemFormatRowBinding
import de.beatbrot.screenshotassistant.editor.crop.CropOutsideFadeView
import de.beatbrot.screenshotassistant.editor.crop.stopAutoZoom
import de.beatbrot.screenshotassistant.editor.draw.DrawSettingsSheet
import de.beatbrot.screenshotassistant.image.ImageFormat
import de.beatbrot.screenshotassistant.image.SaveResult
import de.beatbrot.screenshotassistant.image.saveFolder
import de.beatbrot.screenshotassistant.image.shareIntent
import de.beatbrot.screenshotassistant.util.LOG_TAG
import de.beatbrot.screenshotassistant.util.launchIfAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorActivity : AppCompatActivity() {
    private lateinit var v: ActivityEditorBinding
    private val viewModel by viewModels<EditorViewModel>()
    private val saveFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        viewModel.onSaveFolderPicked(folder)
    }
    private val closeFormatDrawerOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.formatDrawerOpen.value = false
        }
    }
    private val formatRows = mutableMapOf<ImageFormat, CheckedTextView>()
    private var shownMode: EditingMode? = null

    // The exported edit alias shares this activity; only the assistant's own screenshot gets the faded background.
    private val fromAssistant: Boolean
        get() = intent.action != Intent.ACTION_EDIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        initViewModel()

        val imgPath = initialImageUri()
        if (imgPath == null) {
            closeWithoutScreenshot()
            return
        }
        lifecycleScope.launch {
            val screenshot = loadInitialImage(imgPath)
            if (screenshot == null) {
                closeWithoutScreenshot()
                return@launch
            }
            if (fromAssistant) {
                showBackground(screenshot)
            }
            observeEditingMode(screenshot)
        }
    }

    private fun closeWithoutScreenshot() {
        Toast.makeText(this, R.string.error_no_screenshot, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun initialImageUri(): Uri? = if (fromAssistant) {
        intent.assistantScreenshot(this)
    } else {
        intent.data?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
    }

    override fun onDestroy() {
        super.onDestroy()
        keepEdits()
    }

    private fun initUI() {
        v = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(v.root)
        val restoredSheet = supportFragmentManager.findFragmentById(v.bottomSheet.id) as? DrawSettingsSheet
        val drawSettingsSheet = restoredSheet ?: DrawSettingsSheet()
        drawSettingsSheet.onDone = { viewModel.editingMode.value = EditingMode.CROP }
        drawSettingsSheet.drawingView = v.drawingView
        if (restoredSheet == null) {
            supportFragmentManager.beginTransaction().add(v.bottomSheet.id, drawSettingsSheet).commitNow()
        }

        CropOutsideFadeView.attachTo(v.cropView)

        v.drawButton.setOnClickListener { viewModel.editingMode.value = EditingMode.PAINT }

        v.shareButton.setOnClickListener {
            viewModel.formatDrawerOpen.value = false
            currentImage()?.let { image ->
                startActivity(Intent.createChooser(shareIntent(image, viewModel.imageFormat), getString(R.string.share_image)))
            }
        }

        v.saveButton.setOnClickListener {
            viewModel.formatDrawerOpen.value = false
            saveCurrentImage()
        }

        initFormatDrawer()
    }

    private fun initFormatDrawer() {
        ImageFormat.entries.forEach { format ->
            val row = ItemFormatRowBinding.inflate(layoutInflater, v.formatDrawer, true).root
            row.text = format.label
            row.setOnClickListener {
                viewModel.format.value = format
                viewModel.formatDrawerOpen.value = false
            }
            formatRows[format] = row
        }

        v.formatChip.setOnClickListener {
            viewModel.formatDrawerOpen.value = viewModel.formatDrawerOpen.value != true
        }

        // Taps on the screenshot only close the drawer; the buttons below it stay usable.
        v.formatDrawerScrim.setOnClickListener { viewModel.formatDrawerOpen.value = false }

        onBackPressedDispatcher.addCallback(this, closeFormatDrawerOnBack)
    }

    private fun initViewModel() {
        viewModel.saveEvent.observe(this) { event ->
            if (event == null) return@observe
            viewModel.onSaveEventShown()
            when (event) {
                is SaveEvent.Done -> showToast(saveMessage(event.result))
                is SaveEvent.AlreadySaved -> showToast(getString(R.string.already_saved, event.folder))
                SaveEvent.StillSaving -> showToast(getString(R.string.still_saving))
                SaveEvent.ChooseFolder -> chooseSaveFolder()
            }
        }

        viewModel.format.observe(this) { format ->
            v.formatChip.text = format.label
            v.formatChip.contentDescription = getString(R.string.image_format_current, format.label)
            formatRows.forEach { (rowFormat, row) -> row.isChecked = rowFormat == format }
        }

        viewModel.formatDrawerOpen.observe(this) { open ->
            if (open == (v.formatDrawer.visibility == View.VISIBLE)) {
                return@observe
            }
            TransitionManager.beginDelayedTransition(v.root, AutoTransition().setDuration(FORMAT_DRAWER_DURATION))
            v.formatDrawer.visibility = if (open) View.VISIBLE else View.GONE
            v.formatDrawerScrim.visibility = v.formatDrawer.visibility
            v.formatChip.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, if (open) R.drawable.ic_expand_less_18dp else R.drawable.ic_expand_more_18dp, 0
            )
            closeFormatDrawerOnBack.isEnabled = open
        }
    }

    private fun observeEditingMode(screenshot: Bitmap) {
        viewModel.editingMode.observe(this) { newState ->
            keepEdits()
            shownMode = newState
            when (newState) {
                EditingMode.CROP -> {
                    hideBottomSheet()
                    showActionButtons(true)
                    v.drawingView.setImageDrawable(null)
                    v.drawingView.visibility = View.GONE
                    showCropper(screenshot)
                }
                EditingMode.PAINT -> {
                    v.cropView.visibility = View.GONE
                    showPainter(screenshot)
                    viewModel.formatDrawerOpen.value = false
                    showActionButtons(false)
                    showBottomSheet()
                }
            }
        }
    }

    private fun keepEdits() {
        when (shownMode) {
            EditingMode.PAINT -> viewModel.drawStack = v.drawingView.drawStack
            EditingMode.CROP -> v.cropView.cropRect?.let { viewModel.cropRect = it }
            null -> Unit
        }
    }

    // The cropper shows the full screenshot with the edits painted on, so the crop can grow back out.
    // A crop from before the edit is set again once laid out, because only then does the cropper zoom in on it.
    private fun showCropper(screenshot: Bitmap) {
        v.cropView.stopAutoZoom()
        v.cropView.setImageBitmap(viewModel.drawStack.paintedOnto(screenshot))
        val crop = viewModel.cropRect
        v.cropView.cropRect = crop ?: Rect(v.cropView.wholeImageRect)
        v.cropView.post {
            v.cropView.requestLayout()
            if (crop != null) {
                v.cropView.post { v.cropView.cropRect = crop }
            }
        }
        animateImageView(v.cropView, CROPPER_START_SCALE, DecelerateInterpolator())
    }

    private fun showPainter(screenshot: Bitmap) {
        v.drawingView.show(screenshot, viewModel.cropRect, viewModel.drawStack)
        animateImageView(v.drawingView, PAINTER_START_SCALE, OvershootInterpolator())
    }

    @MainThread
    private fun currentImage(): Bitmap? = when (viewModel.editingMode.value) {
        EditingMode.PAINT -> v.drawingView.exportImage()
        else -> v.cropView.getCroppedImage()
    }

    @MainThread
    private fun saveCurrentImage() {
        val image = currentImage()
        if (image == null) {
            showToast(getString(R.string.not_saved_no_image))
            return
        }
        viewModel.saveImage(image)
    }

    private fun saveMessage(result: SaveResult): String = when (result) {
        is SaveResult.Saved -> getString(R.string.saved_to, result.folder)
        SaveResult.NoAccess -> getString(R.string.not_saved_no_access)
        is SaveResult.Failed -> getString(R.string.not_saved, result.error.localizedMessage ?: result.error.javaClass.simpleName)
    }

    private fun chooseSaveFolder() {
        showToast(getString(R.string.choose_save_folder_again))
        if (!saveFolderPicker.launchIfAvailable(saveFolder)) {
            viewModel.onSaveFolderPicked(null)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showActionButtons(visible: Boolean) {
        listOf(v.saveButton, v.formatChip, v.shareButton, v.drawButton).forEach { button ->
            button.animate().cancel()
            if (visible) {
                button.visibility = View.VISIBLE
                button.animate().alpha(1F).start()
            } else {
                button.animate().alpha(0F).withEndAction { button.visibility = View.INVISIBLE }.start()
            }
        }
    }

    private fun showBackground(screenshot: Bitmap) {
        v.root.background = BitmapDrawable(resources, screenshot).apply {
            colorFilter = fadeFilter(BACKGROUND_FADE)
        }
    }

    /** Reduces saturation and brightness by the same share, 0 = unchanged, 1 = black. */
    private fun fadeFilter(reduction: Float): ColorMatrixColorFilter {
        val keep = 1F - reduction
        val matrix = ColorMatrix().apply {
            setSaturation(keep)
            postConcat(ColorMatrix().apply { setScale(keep, keep, keep, 1F) })
        }
        return ColorMatrixColorFilter(matrix)
    }

    private fun animateImageView(imageView: View, startScale: Float, interpolator: TimeInterpolator) {
        imageView.apply {
            scaleX = startScale
            scaleY = startScale
            alpha = 0.5F

            visibility = View.VISIBLE

            animate()
                .scaleX(1F)
                .scaleY(1F)
                .alpha(1F)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private fun showBottomSheet() {
        if (v.bottomSheet.visibility == View.VISIBLE) {
            return
        }

        runAnimation(v.bottomSheet, R.anim.slide_up) {
            v.bottomSheet.visibility = View.VISIBLE
        }
    }

    private fun hideBottomSheet() {
        if (v.bottomSheet.visibility != View.VISIBLE) {
            return
        }

        runAnimation(v.bottomSheet, R.anim.slide_down) {
            v.bottomSheet.visibility = View.GONE
        }
    }

    private inline fun runAnimation(
        view: View,
        @AnimRes animRes: Int,
        crossinline onDone: () -> Unit
    ) {
        val anim = AnimationUtils.loadAnimation(this, animRes)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                onDone()
            }
        })
        view.startAnimation(anim)
    }

    private suspend fun loadInitialImage(uri: Uri): Bitmap? {
        viewModel.screenshot?.let { return it }
        val bitmap = try {
            withContext(Dispatchers.IO) { decodeImage(uri) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not load image", e)
            return null
        } catch (e: OutOfMemoryError) {
            Log.e(LOG_TAG, "Image too large to load", e)
            return null
        }
        if (fromAssistant) {
            deleteAssistantScreenshot()
        }
        viewModel.screenshot = bitmap
        return bitmap
    }

    // Images from other apps can be camera photos, so large ones are scaled down to keep the editor responsive.
    @WorkerThread
    private fun decodeImage(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longSide = maxOf(info.size.width, info.size.height)
            if (longSide > MAX_IMAGE_SIDE) {
                val scale = MAX_IMAGE_SIDE.toFloat() / longSide
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private companion object {
        // More than the cropper's cut-away area (CropOutsideFadeView.FADE), so the editor stands out from the screen behind it.
        const val BACKGROUND_FADE = 0.75F
        const val MAX_IMAGE_SIDE = 4096
        const val FORMAT_DRAWER_DURATION = 200L
        const val CROPPER_START_SCALE = 0.95F
        const val PAINTER_START_SCALE = 1.3F
    }
}
