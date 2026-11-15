package de.paradox.screenshotassistant.setup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.UserManager
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import androidx.core.app.NotificationCompat
import de.paradox.screenshotassistant.R
import de.paradox.screenshotassistant.util.logEvent
import de.paradox.screenshotassistant.util.sharedPrefs
import de.paradox.screenshotassistant.voice.InteractionService

private const val CHANNEL_ID = "assistant_check"
private const val NOTIFICATION_ID = 1
private const val PREF_KEY = "check_assistant_on_boot"
private const val STATE_PREFS = "assistant_state"
private const val READY_BOOT_KEY = "ready_boot_count"
private const val WAS_ASSISTANT_KEY = "was_assistant"

// Device protected, so the boot check can read it before the first unlock. Earlier versions kept it in the normal
// settings; that value is copied over the first time the app reads it while the device is unlocked.
var Context.checkAssistantOnBoot: Boolean
    get() {
        if (!statePrefs.contains(PREF_KEY) && getSystemService(UserManager::class.java).isUserUnlocked) {
            checkAssistantOnBoot = sharedPrefs.getBoolean(PREF_KEY, false)
        }
        return statePrefs.getBoolean(PREF_KEY, false)
    }
    set(value) = statePrefs.edit().putBoolean(PREF_KEY, value).apply()

fun assistantSettingsIntent() = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)

val Context.isDefaultAssistant: Boolean
    get() = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)

enum class AssistantState { OK, NOT_SELECTED, NOT_STARTED }

// Device protected, because Android can bind the assistant before the user unlocks the device.
private val Context.statePrefs: SharedPreferences
    get() = createDeviceProtectedStorageContext().getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)

private val Context.bootCount: Int
    get() = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)

fun Context.markAssistantReady() {
    statePrefs.edit().putInt(READY_BOOT_KEY, bootCount).apply()
}

// Whether the app was the selected assistant the last time it looked. A user who picked another assistant gets no reminder.
var Context.wasAssistant: Boolean
    get() = statePrefs.getBoolean(WAS_ASSISTANT_KEY, false)
    set(value) {
        if (statePrefs.getBoolean(WAS_ASSISTANT_KEY, false) != value) {
            statePrefs.edit().putBoolean(WAS_ASSISTANT_KEY, value).apply()
            logEvent("Remembered as assistant: $value")
        }
    }

private val Context.isActiveInteractor: Boolean
    get() = VoiceInteractionService.isActiveService(this, ComponentName(this, InteractionService::class.java))

private val Context.readySinceBoot: Boolean
    get() = bootCount == -1 || statePrefs.getInt(READY_BOOT_KEY, -2) == bootCount

// After a restart Android can keep the app as the assistant but never start its service; the gesture then does nothing.
fun Context.assistantState(): AssistantState = when {
    !isDefaultAssistant -> AssistantState.NOT_SELECTED
    !isActiveInteractor || !readySinceBoot -> AssistantState.NOT_STARTED
    else -> AssistantState.OK
}

fun Context.assistantDiagnosis(): String =
    "role=$isDefaultAssistant wasAssistant=$wasAssistant active=$isActiveInteractor readySinceBoot=$readySinceBoot boot=$bootCount"

fun Context.checkAssistantAndNotify(state: AssistantState = assistantState()) {
    val manager = getSystemService(NotificationManager::class.java)
    if (state == AssistantState.OK) {
        manager.cancel(NOTIFICATION_ID)
        return
    }
    val notStarted = state == AssistantState.NOT_STARTED
    val title = getString(if (notStarted) R.string.assistant_check_title_not_started else R.string.assistant_check_title)
    val text = getString(if (notStarted) R.string.assistant_check_text_not_started else R.string.assistant_check_text)
    if (!manager.areNotificationsEnabled()) {
        logEvent("Reminder not shown: notifications are off for the app")
        return
    }

    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, getString(R.string.assistant_check_channel), NotificationManager.IMPORTANCE_DEFAULT)
    )
    val settings = PendingIntent.getActivity(
        this, 0,
        assistantSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(settings)
        .setAutoCancel(true)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
    logEvent("Reminder shown: assistant $state")
}

fun Context.cancelAssistantNotification() {
    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
}
