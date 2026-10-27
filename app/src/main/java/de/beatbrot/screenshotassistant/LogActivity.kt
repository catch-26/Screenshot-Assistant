package de.beatbrot.screenshotassistant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import de.beatbrot.screenshotassistant.databinding.ActivityLogsBinding
import java.io.IOException
import kotlin.concurrent.thread

class LogActivity : AppCompatActivity() {
    private lateinit var v: ActivityLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(v.root)

        v.refreshButton.setOnClickListener { loadLog() }
        v.shareLogButton.setOnClickListener { shareLog() }
        loadLog()
    }

    private fun loadLog() {
        v.logScroll.visibility = View.INVISIBLE
        v.logProgress.visibility = View.VISIBLE
        thread {
            val log = readLog()
            runOnUiThread {
                v.logText.text = log
                v.logScroll.doOnNextLayout {
                    v.logScroll.scrollTo(0, v.logText.height)
                    v.logProgress.visibility = View.GONE
                    v.logScroll.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun readLog(): String {
        val header = buildString {
            appendLine("type: logcat")
            appendLine("osVersion: ${Build.FINGERPRINT}")
            appendLine("package: $packageName:${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME}), targetSdk ${applicationInfo.targetSdkVersion}")
            appendLine()
        }
        return try {
            val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-b", "main,system,crash,events", "-t", "2000")
                .redirectErrorStream(true)
                .start()
            header + process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            header + getString(R.string.logs_error, e.message)
        }
    }

    private fun shareLog() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.logs_title))
            putExtra(Intent.EXTRA_TEXT, v.logText.text.toString())
        }
        startActivity(Intent.createChooser(send, getString(R.string.logs_share)))
    }
}
