package de.beatbrot.screenshotassistant.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager

const val LOG_TAG = "ScreenshotAssistant"

val Context.sharedPrefs: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

// Some devices ship without the system folder picker or a given settings screen.
fun <I> ActivityResultLauncher<I>.launchIfAvailable(input: I): Boolean = try {
    launch(input)
    true
} catch (e: ActivityNotFoundException) {
    false
}

fun Context.startFirstAvailable(vararg intents: Intent) {
    for (intent in intents) {
        try {
            startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
        }
    }
}
