package de.beatbrot.screenshotassistant.boot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import de.beatbrot.screenshotassistant.LauncherActivity
import de.beatbrot.screenshotassistant.R
import de.beatbrot.screenshotassistant.util.sharedPrefs

private const val CHANNEL_ID = "assistant_check"
private const val NOTIFICATION_ID = 1
private const val PREF_KEY = "check_assistant_on_boot"

var Context.checkAssistantOnBoot: Boolean
    get() = sharedPrefs.getBoolean(PREF_KEY, false)
    set(value) = sharedPrefs.edit().putBoolean(PREF_KEY, value).apply()

val Context.isDefaultAssistant: Boolean
    get() = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)

fun Context.checkAssistantAndNotify() {
    val manager = getSystemService(NotificationManager::class.java)
    if (isDefaultAssistant) {
        manager.cancel(NOTIFICATION_ID)
        return
    }
    if (!manager.areNotificationsEnabled()) return

    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, getString(R.string.assistant_check_channel), NotificationManager.IMPORTANCE_DEFAULT)
    )
    val settings = PendingIntent.getActivity(
        this, 0,
        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val app = PendingIntent.getActivity(
        this, 1,
        Intent(this, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.assistant_check_title))
        .setContentText(getString(R.string.assistant_check_text))
        .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.assistant_check_text)))
        .setContentIntent(settings)
        .addAction(0, getString(R.string.assistant_check_open_settings), settings)
        .addAction(0, getString(R.string.assistant_check_open_app), app)
        .setAutoCancel(true)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
}

fun Context.cancelAssistantNotification() {
    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
}
