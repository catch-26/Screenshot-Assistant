package de.beatbrot.screenshotassistant.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.beatbrot.screenshotassistant.editor.draw.DrawStack
import de.beatbrot.screenshotassistant.image.ImageFormat
import de.beatbrot.screenshotassistant.image.SaveResult
import de.beatbrot.screenshotassistant.image.imageFormat
import de.beatbrot.screenshotassistant.image.saveImage
import de.beatbrot.screenshotassistant.image.setSaveFolder
import de.beatbrot.screenshotassistant.util.sharedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Application
        get() = getApplication()

    private val _saveEvent = MutableLiveData<SaveEvent?>()
    val saveEvent: LiveData<SaveEvent?>
        get() = _saveEvent

    private var lastSaved: SavedImage? = null
    private var lastSavedFolder = ""

    private var saving = false

    // The edited image waiting for the folder picker, so it survives leaving the editor for the picker.
    private var pendingSave: Bitmap? = null

    // Starts from the start-screen defaults; a change in the editor only applies to this screenshot.
    val format = MutableLiveData(context.sharedPrefs.imageFormat)

    val formatDrawerOpen = MutableLiveData(false)

    val editingMode = MutableLiveData(EditingMode.CROP)

    // The full screenshot stays untouched; edits and crop are only combined on Save or Share.
    var screenshot: Bitmap? = null

    var drawStack = DrawStack()

    // Null means the whole screenshot.
    var cropRect: Rect? = null

    val imageFormat: ImageFormat
        get() = checkNotNull(format.value)

    // Pressing Save again only writes a new file if the image or format changed since the last save.
    @MainThread
    fun saveImage(image: Bitmap) {
        if (saving) {
            _saveEvent.value = SaveEvent.StillSaving
            return
        }
        val saved = SavedImage(image, imageFormat)
        val previous = lastSaved
        val previousFolder = lastSavedFolder
        saving = true
        viewModelScope.launch {
            val event = withContext(Dispatchers.IO) {
                if (previous?.matches(saved) == true) {
                    SaveEvent.AlreadySaved(previousFolder)
                } else {
                    SaveEvent.Done(context.saveImage(saved.image, saved.format))
                }
            }
            saving = false
            val result = (event as? SaveEvent.Done)?.result
            if (result is SaveResult.Saved) {
                lastSaved = saved
                lastSavedFolder = result.folder
            }
            _saveEvent.value = if (result == SaveResult.NoAccess) {
                pendingSave = image
                SaveEvent.ChooseFolder
            } else {
                event
            }
        }
    }

    @MainThread
    fun onSaveFolderPicked(folder: Uri?) {
        val image = pendingSave
        pendingSave = null
        if (folder == null || image == null) {
            _saveEvent.value = SaveEvent.Done(SaveResult.NoAccess)
            return
        }
        context.setSaveFolder(folder)
        saveImage(image)
    }

    fun onSaveEventShown() {
        _saveEvent.value = null
    }
}

sealed interface SaveEvent {
    data class Done(val result: SaveResult) : SaveEvent
    data class AlreadySaved(val folder: String) : SaveEvent
    data object StillSaving : SaveEvent
    data object ChooseFolder : SaveEvent
}

private class SavedImage(val image: Bitmap, val format: ImageFormat) {
    @WorkerThread
    fun matches(other: SavedImage) = format == other.format && image.sameAs(other.image)
}
