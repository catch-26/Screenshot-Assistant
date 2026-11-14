package de.paradox.screenshotassistant.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSessionService

class InteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?) = InteractionSession(this)
}
