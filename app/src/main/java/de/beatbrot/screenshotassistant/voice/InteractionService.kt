package de.beatbrot.screenshotassistant.voice

import android.service.voice.VoiceInteractionService
import android.util.Log
import de.beatbrot.screenshotassistant.setup.cancelAssistantNotification
import de.beatbrot.screenshotassistant.setup.markAssistantReady
import de.beatbrot.screenshotassistant.util.LOG_TAG

// Direct boot aware, so Android binds it at system start and does not depend on a second bind after unlock.
class InteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i(LOG_TAG, "InteractionService ready")
        markAssistantReady()
        cancelAssistantNotification()
    }

    override fun onShutdown() {
        Log.i(LOG_TAG, "InteractionService shut down")
        super.onShutdown()
    }
}
