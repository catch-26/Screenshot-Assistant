package de.beatbrot.screenshotassistant.setup

import android.annotation.SuppressLint
import android.app.voiceinteraction.VoiceInteractionManager
import android.content.Context
import android.content.Intent
import android.os.Build

enum class ScreenContextAccess { GRANTED, REQUESTABLE, UNREQUESTABLE, UNKNOWN }

// API 37 lets the user withhold the screen from the assistant. The API is behind a feature flag, so a device can still lack it.
@SuppressLint("NewApi", "FlaggedApi")
private inline fun <T> Context.withScreenContextApi(beforeApi: T, unavailable: T, block: (VoiceInteractionManager) -> T): T {
    if (Build.VERSION.SDK_INT < 37) return beforeApi
    return try {
        getSystemService(VoiceInteractionManager::class.java)?.let(block) ?: unavailable
    } catch (e: LinkageError) {
        unavailable
    } catch (e: RuntimeException) {
        unavailable
    }
}

@SuppressLint("NewApi", "FlaggedApi")
fun Context.screenContextAccess(): ScreenContextAccess =
    withScreenContextApi(ScreenContextAccess.GRANTED, ScreenContextAccess.UNKNOWN) { manager ->
        when {
            manager.canReadScreenContext() -> ScreenContextAccess.GRANTED
            manager.readScreenContextRequestState == VoiceInteractionManager.READ_SCREEN_CONTEXT_REQUEST_STATE_REQUESTABLE -> ScreenContextAccess.REQUESTABLE
            else -> ScreenContextAccess.UNREQUESTABLE
        }
    }

@SuppressLint("NewApi", "FlaggedApi")
fun Context.screenContextRequestIntent(): Intent? =
    withScreenContextApi(null, null) { it.createRequestReadScreenContextIntent() }
