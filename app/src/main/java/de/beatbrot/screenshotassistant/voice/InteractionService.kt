package de.beatbrot.screenshotassistant.voice

import android.service.voice.VoiceInteractionService
import de.beatbrot.screenshotassistant.setup.cancelAssistantNotification
import de.beatbrot.screenshotassistant.setup.markAssistantReady
import de.beatbrot.screenshotassistant.util.logEvent

// Direct boot aware, so Android binds it at system start and does not depend on a second bind after unlock.
class InteractionService : VoiceInteractionService() {
    override fun onCreate() {
        super.onCreate()
        logEvent("InteractionService created")
    }

    override fun onReady() {
        super.onReady()
        logEvent("InteractionService ready")
        markAssistantReady()
        cancelAssistantNotification()
    }

    override fun onShutdown() {
        logEvent("InteractionService shut down")
        super.onShutdown()
    }

    override fun onDestroy() {
        logEvent("InteractionService destroyed")
        super.onDestroy()
    }
}
