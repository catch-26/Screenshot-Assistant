package de.beatbrot.screenshotassistant.voice

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.widget.Toast
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.editor.editorIntent
import de.beatbrot.screenshotassistant.setup.ScreenContextAccess
import de.beatbrot.screenshotassistant.setup.screenContextAccess
import de.beatbrot.screenshotassistant.util.LOG_TAG

class InteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(LOG_TAG, "onShow showFlags=$showFlags disabledShowContext=$disabledShowContext screenContext=${context.screenContextAccess()}")
        if (showFlags and SHOW_WITH_SCREENSHOT == 0) {
            Log.w(LOG_TAG, "onShow without SHOW_WITH_SCREENSHOT, closing")
            showNoScreenshotHint()
            hide()
        }
    }

    override fun onHandleAssist(state: AssistState) {
        Log.d(LOG_TAG, "onHandleAssist index=${state.index} count=${state.count} data=${state.assistData != null}")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        Log.d(LOG_TAG, "onHandleScreenshot screenshot=${screenshot?.let { "${it.width}x${it.height}" }}")
        if (screenshot != null) {
            try {
                startAssistantActivity(context.editorIntent(screenshot))
                Log.d(LOG_TAG, "startAssistantActivity done")
            } catch (e: RuntimeException) {
                Log.e(LOG_TAG, "startAssistantActivity failed", e)
                Toast.makeText(context, R.string.error_no_screenshot, Toast.LENGTH_LONG).show()
            }
        } else {
            showNoScreenshotHint()
        }
        hide()
    }

    private fun showNoScreenshotHint() {
        val hint = if (context.screenContextAccess() == ScreenContextAccess.GRANTED) R.string.enable_screenshot else R.string.no_screen_access
        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
    }

    override fun onHide() {
        super.onHide()
        Log.d(LOG_TAG, "onHide")
    }
}
