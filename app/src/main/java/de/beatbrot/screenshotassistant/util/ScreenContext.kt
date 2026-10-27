package de.beatbrot.screenshotassistant.util

import android.annotation.SuppressLint
import android.app.voiceinteraction.VoiceInteractionManager
import android.content.Context
import android.content.Intent
import android.os.Build

enum class ScreenContextAccess { GRANTED, REQUESTABLE, UNREQUESTABLE, UNKNOWN }

@SuppressLint("NewApi", "FlaggedApi")
fun Context.screenContextAccess(): ScreenContextAccess {
    if (Build.VERSION.SDK_INT < 37) return ScreenContextAccess.GRANTED
    return try {
        val manager = getSystemService(VoiceInteractionManager::class.java) ?: return ScreenContextAccess.UNKNOWN
        when {
            manager.canReadScreenContext() -> ScreenContextAccess.GRANTED
            manager.readScreenContextRequestState == VoiceInteractionManager.READ_SCREEN_CONTEXT_REQUEST_STATE_REQUESTABLE -> ScreenContextAccess.REQUESTABLE
            else -> ScreenContextAccess.UNREQUESTABLE
        }
    } catch (e: LinkageError) {
        ScreenContextAccess.UNKNOWN
    } catch (e: RuntimeException) {
        ScreenContextAccess.UNKNOWN
    }
}

@SuppressLint("NewApi", "FlaggedApi")
fun Context.screenContextRequestIntent(): Intent? {
    if (Build.VERSION.SDK_INT < 37) return null
    return try {
        getSystemService(VoiceInteractionManager::class.java)?.createRequestReadScreenContextIntent()
    } catch (e: LinkageError) {
        null
    } catch (e: RuntimeException) {
        null
    }
}
