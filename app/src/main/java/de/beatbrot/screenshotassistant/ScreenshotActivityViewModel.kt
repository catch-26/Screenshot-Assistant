package de.beatbrot.screenshotassistant

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.beatbrot.screenshotassistant.draw.DrawStack
import de.beatbrot.screenshotassistant.util.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val AUTHORITY_NAME = "${BuildConfig.APPLICATION_ID}.fileprovider"
const val MIME_TYPE = "image/png"

class ScreenshotActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _shareIntent: MutableLiveData<Intent> = MutableLiveData()
    val shareIntent: LiveData<Intent>
        get() = _shareIntent

    private val _saveResult: MutableLiveData<SaveResult?> = MutableLiveData()
    val saveResult: LiveData<SaveResult?>
        get() = _saveResult

    private var lastSaved: SavedImage? = null

    private var saving = false

    private val context: Context
        get() = getApplication<Application>().baseContext

    // Starts from the start-screen defaults; a change in the editor only applies to this screenshot.
    val format = liveDataOf(context.sharedPrefs.imageFormat)

    val quality = liveDataOf(context.sharedPrefs.imageQuality)

    val formatDrawerOpen = liveDataOf(false)

    private val imageFormat: Bitmap.CompressFormat
        get() = format.value ?: context.sharedPrefs.imageFormat

    private val imageQuality: Int
        get() = quality.value ?: context.sharedPrefs.imageQuality

    val editingMode = liveDataOf(EditingMode.CROP)

    val currentBitmap = MutableLiveData<Bitmap>()

    var screenshot: Bitmap? = null

    val drawStack = MutableLiveData<DrawStack>()

    val cropRect = MutableLiveData<Rect?>()

    // The edited image waiting for the folder picker, so it survives leaving the editor for the picker.
    var pendingSave: Bitmap? = null

    fun shareImage(croppedImage: Bitmap) {
        val croppedUri = getScreenshotUri(croppedImage)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = imageFormat.mimeType
            putExtra(Intent.EXTRA_STREAM, croppedUri)
            clipData = ClipData.newRawUri(null, croppedUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        _shareIntent.postValue(intent)
    }

    // Pressing Save again only writes a new file if the image, format or quality changed since the last save.
    fun saveImage(image: Bitmap) {
        if (saving) {
            return
        }
        val saved = SavedImage(image, imageFormat, imageQuality)
        if (lastSaved?.matches(saved) == true) {
            _saveResult.value = SaveResult.AlreadySaved
            return
        }
        saving = true
        viewModelScope.launch {
            val fileName = "Screenshot-${currentDateString()}.${saved.format.fileExtension}"
            val result = withContext(Dispatchers.IO) {
                context.saveImage(saved.image, saved.format, saved.quality, fileName)
            }
            if (result is SaveResult.Saved) {
                lastSaved = saved
            }
            saving = false
            _saveResult.value = result
        }
    }

    fun onSaveResultShown() {
        _saveResult.value = null
    }

    private fun getScreenshotUri(croppedImage: Bitmap): Uri {
        val scrFile = createScreenshotFile()

        FileOutputStream(scrFile).use { stream ->
            croppedImage.compress(imageFormat, imageQuality, stream)
        }

        return FileProvider.getUriForFile(context, AUTHORITY_NAME, scrFile)
    }

    private fun createScreenshotFile(): File {
        val scrDir = File(context.filesDir, "screenshots")
        val scrFile = File(
            scrDir,
            "Screenshot-${currentDateString()}.${imageFormat.fileExtension}"
        )

        scrDir.mkdir()
        scrDir.deleteContents()
        scrFile.createNewFile()

        return scrFile
    }

    private fun currentDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'-'HH-mm-ss", Locale.US)
        return format.format(Date())
    }

    private fun File.deleteContents() {
        listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
    }
}

private class SavedImage(val image: Bitmap, val format: Bitmap.CompressFormat, val quality: Int) {
    fun matches(other: SavedImage) = format == other.format && quality == other.quality && image.sameAs(other.image)
}
