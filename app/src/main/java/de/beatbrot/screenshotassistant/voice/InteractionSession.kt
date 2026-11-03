package de.beatbrot.screenshotassistant.voice

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.widget.Toast
import de.beatbrot.screenshotassistant.MainActivity
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.util.ScreenContextAccess
import de.beatbrot.screenshotassistant.util.assistantScreenshotFile
import de.beatbrot.screenshotassistant.util.screenContextAccess
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ScreenshotAssistant"

class InteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "onShow showFlags=$showFlags disabledShowContext=$disabledShowContext screenContext=${context.screenContextAccess()}")
        if (showFlags and SHOW_WITH_SCREENSHOT == 0) {
            Log.w(TAG, "onShow without SHOW_WITH_SCREENSHOT, closing")
            showNoScreenshotHint()
            hide()
        }
    }

    override fun onHandleAssist(state: AssistState) {
        Log.d(TAG, "onHandleAssist index=${state.index} count=${state.count} data=${state.assistData != null}")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        Log.d(TAG, "onHandleScreenshot screenshot=${screenshot?.let { "${it.width}x${it.height}" }}")
        if (screenshot != null) {
            val startIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("screenshot", getScreenshotUri(screenshot))
            }
            try {
                startAssistantActivity(startIntent)
                Log.d(TAG, "startAssistantActivity done")
            } catch (e: RuntimeException) {
                Log.e(TAG, "startAssistantActivity failed", e)
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
        Log.d(TAG, "onHide")
    }

    private fun getScreenshotUri(bitmap: Bitmap): Uri {
        val scrFile = createScreenshotFile()

        FileOutputStream(scrFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
        }
        return Uri.fromFile(scrFile)
    }

    private fun createScreenshotFile(): File {
        val scrFile = context.assistantScreenshotFile()
        val scrDir = scrFile.parentFile!!

        scrDir.mkdir()
        if (scrFile.exists()) {
            scrFile.delete()
        }
        scrFile.createNewFile()

        return scrFile
    }
}
