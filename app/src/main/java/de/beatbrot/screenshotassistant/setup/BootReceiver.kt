package de.beatbrot.screenshotassistant.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserManager
import de.beatbrot.screenshotassistant.util.logEvent

private const val FIRST_WAIT_MS = 20_000L
private const val AFTER_RESTART_WAIT_MS = 15_000L
private const val POLL_MS = 2_000L

// Android may bind the assistant a few seconds after the broadcast, so wait for it. If it still has not,
// ask Android to bind it again once, and only warn if that did not help either.
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
        var restarted = false
        var deadline = SystemClock.uptimeMillis() + FIRST_WAIT_MS
        fun check() {
            val state = app.assistantState()
            when {
                state == AssistantState.OK || state == AssistantState.NOT_SELECTED -> finish(app, action, state, pending)
                SystemClock.uptimeMillis() < deadline -> handler.postDelayed({ check() }, POLL_MS)
                !restarted -> {
                    restarted = true
                    app.restartAssistantService("$action check")
                    deadline = SystemClock.uptimeMillis() + AFTER_RESTART_WAIT_MS
                    handler.postDelayed({ check() }, POLL_MS)
                }
                else -> finish(app, action, state, pending)
            }
        }
        check()
    }

    private fun finish(app: Context, action: String, state: AssistantState, pending: PendingResult) {
        app.logEvent("$action: assistant $state (${app.assistantDiagnosis()})")
        if (app.checkAssistantOnBoot) app.checkAssistantAndNotify(state)
        pending.finish()
    }
}
