package de.beatbrot.screenshotassistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import de.beatbrot.screenshotassistant.crop.CropOutsideFadeView
import de.beatbrot.screenshotassistant.databinding.ActivityMainBinding
import de.beatbrot.screenshotassistant.sheets.drawsettings.DrawSettingsSheet
import de.beatbrot.screenshotassistant.util.OpenAnimationListener
import de.beatbrot.screenshotassistant.util.SaveResult
import de.beatbrot.screenshotassistant.util.fadeFilter
import de.beatbrot.screenshotassistant.util.hasSaveAccess
import de.beatbrot.screenshotassistant.util.saveFolder
import de.beatbrot.screenshotassistant.util.setSaveFolder
import de.beatbrot.screenshotassistant.util.sharedPrefs
import de.beatbrot.screenshotassistant.util.tryExport

class MainActivity : AppCompatActivity() {
    private lateinit var v: ActivityMainBinding
    private val drawSettingsSheet = DrawSettingsSheet()
    private val viewModel by viewModels<ScreenshotActivityViewModel>()
    private val saveFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        val image = viewModel.pendingSave
        viewModel.pendingSave = null
        if (folder == null || image == null) {
            showToast(getString(R.string.not_saved_no_access))
        } else {
            setSaveFolder(folder)
            saveImage(image)
        }
    }
    private val closeFormatDrawerOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.formatDrawerOpen.value = false
        }
    }
    private lateinit var formatRows: Map<CheckedTextView, Bitmap.CompressFormat>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        initViewModel()

        val imgPath = when (intent.action) {
            Intent.ACTION_EDIT -> intent.data
            else -> intent.getParcelableExtra("screenshot", Uri::class.java)
        }
        if (imgPath != null && loadInitialImage(imgPath)) {
            viewModel.screenshot?.let { showBackground(it) }
        } else {
            Toast.makeText(baseContext, R.string.error_no_screenshot, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // The editor closes once it leaves the screen, except for the folder picker and a screen turned off.
    override fun onStop() {
        super.onStop()
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (viewModel.pendingSave == null && screenOn && !isChangingConfigurations) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cropRect.value = v.cropView.cropRect
        viewModel.drawStack.value = v.drawingView.drawStack
    }

    private fun initUI() {
        v = ActivityMainBinding.inflate(layoutInflater)
        setContentView(v.root)
        supportFragmentManager.beginTransaction()
            .replace(v.bottomSheet.id, drawSettingsSheet)
            .commitNow()

        drawSettingsSheet.onHideListener = { viewModel.editingMode.value = EditingMode.CROP }
        drawSettingsSheet.drawingView = v.drawingView

        CropOutsideFadeView.attachTo(v.cropView)

        v.cropView.setOnSetImageUriCompleteListener { view, _, _ ->
            view.cropRect = Rect(view.wholeImageRect)
        }

        v.drawButton.setOnClickListener { viewModel.editingMode.value = EditingMode.PAINT }

        v.shareButton.setOnClickListener {
            viewModel.formatDrawerOpen.value = false
            currentImage()?.let { viewModel.shareImage(it) }
        }

        v.saveButton.setOnClickListener {
            viewModel.formatDrawerOpen.value = false
            saveCurrentImage()
        }

        initFormatDrawer()
    }

    private fun initFormatDrawer() {
        formatRows = mapOf(
            v.formatRowPng to Bitmap.CompressFormat.PNG,
            v.formatRowJpeg to Bitmap.CompressFormat.JPEG,
            v.formatRowWebp to Bitmap.CompressFormat.WEBP
        )

        v.formatChip.setOnClickListener {
            viewModel.formatDrawerOpen.value = viewModel.formatDrawerOpen.value != true
        }

        formatRows.forEach { (row, format) ->
            row.setOnClickListener { viewModel.format.value = format }
        }

        // Taps on the screenshot only close the drawer; the buttons below it stay usable.
        v.formatDrawerScrim.setOnClickListener { viewModel.formatDrawerOpen.value = false }

        onBackPressedDispatcher.addCallback(this, closeFormatDrawerOnBack)
    }

    private fun initViewModel() {
        viewModel.currentBitmap.observe(this, Observer { newBitmap ->
            when (viewModel.editingMode.value) {
                EditingMode.CROP -> {
                    v.cropView.findViewById<View>(com.canhub.cropper.R.id.ImageView_image).clearAnimation()
                    v.cropView.setImageBitmap(newBitmap)
                    v.cropView.cropRect = Rect(v.cropView.wholeImageRect)
                    v.cropView.post { v.cropView.requestLayout() }
                    animateImageView(v.cropView)
                }
                EditingMode.PAINT -> {
                    v.drawingView.setImageBitmap(newBitmap)
                    animateImageView(v.drawingView)
                }
                null -> Unit
            }
        })

        viewModel.drawStack.observe(this, Observer { stack ->
            if (stack != null) {
                v.drawingView.drawStack = stack
            }
        })

        viewModel.cropRect.observe(this, Observer { rect ->
            v.cropView.cropRect = rect ?: Rect(v.cropView.wholeImageRect)
        })

        viewModel.shareIntent.observe(this, Observer { intent ->
            startActivity(Intent.createChooser(intent, baseContext.getString(R.string.share_image)))
        })

        viewModel.saveResult.observe(this, Observer { result ->
            val message = when (result) {
                is SaveResult.Saved -> getString(R.string.saved_to, result.format.name, result.folder)
                is SaveResult.AlreadySaved -> getString(R.string.already_saved, result.format.name)
                SaveResult.StillSaving -> getString(R.string.still_saving)
                SaveResult.NoAccess -> getString(R.string.not_saved_no_access)
                is SaveResult.Failed -> getString(R.string.not_saved, result.error.localizedMessage ?: result.error.javaClass.simpleName)
                null -> return@Observer
            }
            showToast(message)
            viewModel.onSaveResultShown()
        })

        viewModel.format.observe(this, Observer { format ->
            v.formatChip.text = format.name
            v.formatChip.contentDescription = getString(R.string.image_format_current, format.name)
            formatRows.forEach { (row, rowFormat) -> row.isChecked = rowFormat == format }
        })

        viewModel.formatDrawerOpen.observe(this, Observer { open ->
            if (open == (v.formatDrawer.visibility == View.VISIBLE)) {
                return@Observer
            }
            TransitionManager.beginDelayedTransition(v.root, AutoTransition().setDuration(FORMAT_DRAWER_DURATION))
            v.formatDrawer.visibility = if (open) View.VISIBLE else View.GONE
            v.formatDrawerScrim.visibility = v.formatDrawer.visibility
            v.formatChip.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, if (open) R.drawable.ic_expand_less_18dp else R.drawable.ic_expand_more_18dp, 0
            )
            closeFormatDrawerOnBack.isEnabled = open
        })

        viewModel.editingMode.observe(this, Observer { newState ->
            when (newState) {
                EditingMode.CROP -> {
                    hideBottomSheet()
                    showActionButtons(true)
                    v.drawingView.visibility = View.GONE
                    v.cropView.visibility = View.VISIBLE
                    if (v.drawingView.drawable != null) {
                        viewModel.currentBitmap.value = v.drawingView.exportImage()
                    }
                }
                EditingMode.PAINT -> {
                    v.cropView.visibility = View.GONE
                    v.drawingView.visibility = View.VISIBLE
                    v.cropView.tryExport()?.let {
                        viewModel.currentBitmap.value = it
                    }
                    viewModel.cropRect.value = null
                    viewModel.formatDrawerOpen.value = false
                    showActionButtons(false)
                    showBottomSheet()
                }
                else -> throw UnsupportedOperationException()
            }
        })
    }

    private fun currentImage(): Bitmap? = when (viewModel.editingMode.value) {
        EditingMode.PAINT -> v.drawingView.takeIf { it.drawable != null }?.exportImage()
        else -> v.cropView.getCroppedImage()
    }

    // A chosen folder can lose its access grant at any time, so it is checked before every save.
    private fun saveCurrentImage() {
        val image = currentImage()
        if (image == null) {
            showToast(getString(R.string.not_saved_no_image))
            return
        }
        saveImage(image)
    }

    private fun saveImage(image: Bitmap) {
        if (hasSaveAccess()) {
            viewModel.saveImage(image)
            return
        }
        showToast(getString(R.string.choose_save_folder_again))
        viewModel.pendingSave = image
        try {
            saveFolderPicker.launch(sharedPrefs.saveFolder)
        } catch (e: ActivityNotFoundException) {
            viewModel.pendingSave = null
            showToast(getString(R.string.not_saved_no_access))
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

    // Shows the screenshot behind the editor, faded more than the cropper's cut-away area.
    private fun showBackground(screenshot: Bitmap) {
        v.root.background = BitmapDrawable(resources, screenshot).apply {
            colorFilter = fadeFilter(BACKGROUND_FADE)
        }
    }

    private fun animateImageView(imageView: View) {
        val startValue = 1.3F
        imageView.apply {
            scaleX = startValue
            scaleY = startValue
            alpha = 0.5F

            visibility = View.VISIBLE

            animate()
                .scaleX(1F)
                .scaleY(1F)
                .alpha(1F)
                .setInterpolator(OvershootInterpolator())
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
        val anim = AnimationUtils.loadAnimation(baseContext, animRes)
        anim.setAnimationListener(object : OpenAnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                onDone()
            }
        })
        view.startAnimation(anim)
    }

    private fun loadInitialImage(uri: Uri): Boolean {
        if (viewModel.currentBitmap.value != null) {
            return true
        }
        val bitmap = try {
            decodeImage(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Could not load image $uri", e)
            return false
        }
        viewModel.screenshot = bitmap
        viewModel.currentBitmap.value = bitmap
        return true
    }

    // Images from other apps can be camera photos, so large ones are scaled down to keep the editor responsive.
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
        const val TAG = "ScreenshotAssistant"
        const val BACKGROUND_FADE = 0.75F
        const val MAX_IMAGE_SIDE = 4096
        const val FORMAT_DRAWER_DURATION = 200L
    }
}
