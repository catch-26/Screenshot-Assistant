package de.beatbrot.screenshotassistant.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.IOException

const val DEFAULT_SAVE_FOLDER = "Pictures/Screenshots"
private const val TAG = "ScreenshotAssistant"
private const val TREE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

sealed interface SaveResult {
    data class Saved(val folder: String) : SaveResult
    data object AlreadySaved : SaveResult
    data object NoAccess : SaveResult
    data class Failed(val error: Exception) : SaveResult
}

// The default folder goes through MediaStore, which needs no permission for the app's own images.
fun Context.hasSaveAccess(): Boolean {
    val folder = sharedPrefs.saveFolder ?: return true
    val granted = contentResolver.persistedUriPermissions.any { it.uri == folder && it.isWritePermission }
    return granted && folderExists(folder)
}

fun Context.setSaveFolder(folder: Uri?) {
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

fun Context.saveFolderLabel(): String {
    val folder = sharedPrefs.saveFolder ?: return DEFAULT_SAVE_FOLDER
    val name = try {
        contentResolver.query(folder.rootDocument, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }
    return name ?: folder.lastPathSegment.orEmpty()
}

fun Context.saveImage(image: Bitmap, format: Bitmap.CompressFormat, quality: Int, fileName: String): SaveResult {
    if (!hasSaveAccess()) {
        return SaveResult.NoAccess
    }
    return try {
        when (val folder = sharedPrefs.saveFolder) {
            null -> saveToMediaStore(image, format, quality, fileName)
            else -> saveToFolder(folder, image, format, quality, fileName)
        }
        SaveResult.Saved(saveFolderLabel())
    } catch (e: Exception) {
        Log.e(TAG, "Could not save $fileName", e)
        SaveResult.Failed(e)
    }
}

private fun Context.saveToMediaStore(image: Bitmap, format: Bitmap.CompressFormat, quality: Int, fileName: String) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, DEFAULT_SAVE_FOLDER)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = contentResolver.insert(collection, values) ?: throw IOException("MediaStore did not create $fileName")
    try {
        writeImage(uri, image, format, quality)
        contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    } catch (e: Exception) {
        contentResolver.delete(uri, null, null)
        throw e
    }
}

private fun Context.saveToFolder(folder: Uri, image: Bitmap, format: Bitmap.CompressFormat, quality: Int, fileName: String) {
    val uri = DocumentsContract.createDocument(contentResolver, folder.rootDocument, format.mimeType, fileName)
        ?: throw IOException("Could not create $fileName in $folder")
    try {
        writeImage(uri, image, format, quality)
    } catch (e: Exception) {
        DocumentsContract.deleteDocument(contentResolver, uri)
        throw e
    }
}

private fun Context.writeImage(uri: Uri, image: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    val stream = contentResolver.openOutputStream(uri) ?: throw IOException("Could not open $uri")
    stream.use {
        if (!image.compress(format, quality, it)) {
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

private val Uri.rootDocument: Uri
    get() = DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))
