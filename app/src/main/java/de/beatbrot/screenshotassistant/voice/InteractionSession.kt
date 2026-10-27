package de.beatbrot.screenshotassistant.voice

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.widget.Toast
import de.beatbrot.screenshotassistant.MainActivity
import de.beatbrot.screenshotassistant.R
import java.io.File
import java.io.FileOutputStream

class InteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (showFlags and SHOW_WITH_SCREENSHOT == 0) {
            Toast.makeText(context, R.string.enable_screenshot, Toast.LENGTH_SHORT).show()
            hide()
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        if (screenshot != null) {
            val startIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("screenshot", getScreenshotUri(screenshot))
            }
            hide()
            context.startActivity(startIntent)
        } else {
            Toast.makeText(context, R.string.enable_screenshot, Toast.LENGTH_SHORT).show()
            hide()
        }
    }

    private fun getScreenshotUri(bitmap: Bitmap): Uri {
        val scrFile = createScreenshotFile()

        FileOutputStream(scrFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
        }
        return Uri.fromFile(scrFile)
    }

    private fun createScreenshotFile(): File {
        val scrDir = File(context.cacheDir, "screenshots")
        val scrFile = File(scrDir, "scr")

        scrDir.mkdir()
        if (scrFile.exists()) {
            scrFile.delete()
        }
        scrFile.createNewFile()

        return scrFile
    }
}
