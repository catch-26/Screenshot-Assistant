package de.beatbrot.screenshotassistant.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val EVENT_FILE = "events.log"
private const val MAX_LINES = 300

// Android drops older lines from its log buffer within minutes, so assistant events are also kept in a small file.
// Device protected, so it also works before the first unlock after a restart.
private val Context.eventFile: File
    get() = File(createDeviceProtectedStorageContext().filesDir, EVENT_FILE)

private val eventLock = Any()

fun Context.logEvent(message: String) {
    Log.i(LOG_TAG, message)
    val line = "${SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} $message"
    synchronized(eventLock) {
        val file = eventFile
        val lines = (if (file.exists()) file.readLines() else emptyList()) + line
        file.writeText(lines.takeLast(MAX_LINES).joinToString("\n", postfix = "\n"))
    }
}

fun Context.readEvents(): String = synchronized(eventLock) {
    eventFile.takeIf { it.exists() }?.readText().orEmpty()
}
