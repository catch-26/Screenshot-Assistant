package de.beatbrot.screenshotassistant.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import de.beatbrot.screenshotassistant.util.LOG_TAG

private const val CHECK_WINDOW_MS = 30_000L
private const val POLL_MS = 2_000L

// Android may bind the assistant a few seconds after the broadcast, so wait for it before warning.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val deadline = SystemClock.uptimeMillis() + CHECK_WINDOW_MS
        fun check() {
            val state = app.assistantState()
            if (state == AssistantState.OK || SystemClock.uptimeMillis() >= deadline) {
                Log.i(LOG_TAG, "$action: assistant $state (${app.assistantDiagnosis()})")
                if (app.checkAssistantOnBoot) app.checkAssistantAndNotify(state)
                pending.finish()
            } else {
                handler.postDelayed({ check() }, POLL_MS)
            }
        }
        check()
    }
}
