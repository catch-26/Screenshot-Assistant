package de.paradox.screenshotassistant.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserManager
import de.paradox.screenshotassistant.util.logEvent

private const val MAX_WAIT_MS = 8_000L
private const val POLL_MS = 1_000L

// A short check after boot or an app update: wait a few seconds for Android to start the assistant, then remind once
// if it did not. The wait stays well inside Android's time limit for broadcasts, so the app is not killed before it
// can post the reminder. If the service starts later after all, it removes the reminder itself.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            app.logEvent("$action: ${app.assistantDiagnosis()}")
            return
        }
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!app.getSystemService(UserManager::class.java).isUserUnlocked) return
        app.logEvent("$action received: ${app.assistantDiagnosis()}")
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val deadline = SystemClock.uptimeMillis() + MAX_WAIT_MS
        fun check() {
            try {
                val state = app.assistantState()
                if (state == AssistantState.NOT_STARTED && SystemClock.uptimeMillis() < deadline) {
                    handler.postDelayed({ check() }, POLL_MS)
                    return
                }
                finish(app, action, state)
            } catch (e: RuntimeException) {
                app.logEvent("$action check failed: $e")
            }
            pending.finish()
        }
        check()
    }

    private fun finish(app: Context, action: String, state: AssistantState) {
        app.logEvent("$action: assistant $state (${app.assistantDiagnosis()})")
        val remind = app.checkAssistantOnBoot
        val notify = remind && (state != AssistantState.NOT_SELECTED || app.wasAssistant)
        if (notify) app.checkAssistantAndNotify(state)
        else if (state != AssistantState.OK) app.logEvent("$action: no reminder (switch on: $remind, was assistant: ${app.wasAssistant})")
        // Remind about a lost assistant selection once; after that the app counts as deselected.
        app.wasAssistant = state != AssistantState.NOT_SELECTED
    }
}
