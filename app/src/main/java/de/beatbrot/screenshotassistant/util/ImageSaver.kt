package de.beatbrot.screenshotassistant.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.IOException

const val DEFAULT_SAVE_FOLDER = "Pictures/Screenshots"
private const val TAG = "ScreenshotAssistant"
private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val PRIMARY_VOLUME = "primary"
private const val TREE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

sealed interface SaveResult {
    data class Saved(val folder: String) : SaveResult
    data class AlreadySaved(val folder: String) : SaveResult
    data object StillSaving : SaveResult
    data object NoAccess : SaveResult
    data class Failed(val error: Exception) : SaveResult
}

// The default folder goes through MediaStore, which needs no permission for the app's own images.
fun Context.hasSaveAccess(): Boolean {
    val folder = sharedPrefs.saveFolder ?: return true
    val granted = contentResolver.persistedUriPermissions.any { it.uri == folder && it.isWritePermission }
    return granted && folderExists(folder)
}

// Picking Pictures/Screenshots on internal storage is the default folder, so it is stored as the default.
fun Context.setSaveFolder(picked: Uri?) {
    val folder = picked?.takeUnless { it.isDefaultSaveFolder }
    val old = sharedPrefs.saveFolder
    if (folder != null) {
        contentResolver.takePersistableUriPermission(folder, TREE_FLAGS)
    }
    if (old != null && old != folder) {
        try {
            contentResolver.releasePersistableUriPermission(old, TREE_FLAGS)
        } catch (e: SecurityException) {
        }
    }
    sharedPrefs.saveFolder = folder
}

val Uri.isDefaultSaveFolder: Boolean
    get() = storageLocation?.let { (volume, path) -> volume == PRIMARY_VOLUME && path.equals(DEFAULT_SAVE_FOLDER, ignoreCase = true) } == true

// Folders on device storage show their path from the storage root, like the default Pictures/Screenshots.
fun Context.saveFolderLabel(): String {
    val folder = sharedPrefs.saveFolder ?: return DEFAULT_SAVE_FOLDER
    val (volume, path) = folder.storageLocation ?: return folderName(folder)
    val volumeName = if (volume == PRIMARY_VOLUME && path.isNotEmpty()) null else storageVolumeName(volume)
    return listOfNotNull(volumeName, path.ifEmpty { null }).joinToString("/")
}

private fun Context.folderName(folder: Uri): String {
    val name = try {
        contentResolver.query(folder.rootDocument, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }
    return name ?: folder.lastPathSegment.orEmpty()
}

// JPG and WEBP are always written at maximum quality.
const val IMAGE_QUALITY = 100

fun Context.saveImage(image: Bitmap, format: Bitmap.CompressFormat, fileName: String): SaveResult {
    if (!hasSaveAccess()) {
        return SaveResult.NoAccess
    }
    return try {
        when (val folder = sharedPrefs.saveFolder) {
            null -> saveToMediaStore(image, format, fileName)
            else -> saveToFolder(folder, image, format, fileName)
        }
        SaveResult.Saved(saveFolderLabel())
    } catch (e: Exception) {
        Log.e(TAG, "Could not save $fileName", e)
        SaveResult.Failed(e)
    }
}

private fun Context.saveToMediaStore(image: Bitmap, format: Bitmap.CompressFormat, fileName: String) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, DEFAULT_SAVE_FOLDER)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = contentResolver.insert(collection, values) ?: throw IOException("MediaStore did not create $fileName")
    try {
        writeImage(uri, image, format)
        contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    } catch (e: Exception) {
        contentResolver.delete(uri, null, null)
        throw e
    }
}

private fun Context.saveToFolder(folder: Uri, image: Bitmap, format: Bitmap.CompressFormat, fileName: String) {
    val uri = DocumentsContract.createDocument(contentResolver, folder.rootDocument, format.mimeType, fileName)
        ?: throw IOException("Could not create $fileName in $folder")
    try {
        writeImage(uri, image, format)
    } catch (e: Exception) {
        DocumentsContract.deleteDocument(contentResolver, uri)
        throw e
    }
}

private fun Context.writeImage(uri: Uri, image: Bitmap, format: Bitmap.CompressFormat) {
    val stream = contentResolver.openOutputStream(uri) ?: throw IOException("Could not open $uri")
    stream.use {
        if (!image.compress(format, IMAGE_QUALITY, it)) {
            throw IOException("Could not encode the image as ${format.name}")
        }
    }
}

private fun Context.folderExists(folder: Uri): Boolean = try {
    contentResolver.query(folder.rootDocument, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
        ?.use { it.moveToFirst() } ?: false
} catch (e: Exception) {
    false
}

private fun Context.storageVolumeName(volume: String): String {
    val volumes = getSystemService(StorageManager::class.java).storageVolumes
    val match = volumes.firstOrNull { if (volume == PRIMARY_VOLUME) it.isPrimary else it.uuid.equals(volume, ignoreCase = true) }
    return match?.getDescription(this) ?: volume
}

// Device storage folders have tree IDs like "primary:Pictures/Screenshots" (volume, then path from its root).
private val Uri.storageLocation: Pair<String, String>?
    get() {
        if (authority != EXTERNAL_STORAGE_AUTHORITY) {
            return null
        }
        val parts = DocumentsContract.getTreeDocumentId(this).split(':', limit = 2)
        return if (parts.size == 2) parts[0] to parts[1].trim('/') else null
    }

private val Uri.rootDocument: Uri
    get() = DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))
