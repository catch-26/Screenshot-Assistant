package de.beatbrot.screenshotassistant

import android.Manifest
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.beatbrot.screenshotassistant.boot.cancelAssistantNotification
import de.beatbrot.screenshotassistant.boot.checkAssistantOnBoot
import de.beatbrot.screenshotassistant.boot.isDefaultAssistant
import de.beatbrot.screenshotassistant.databinding.ActivityLauncherBinding
import de.beatbrot.screenshotassistant.util.ScreenContextAccess
import de.beatbrot.screenshotassistant.util.screenContextAccess
import de.beatbrot.screenshotassistant.util.screenContextRequestIntent

class LauncherActivity : AppCompatActivity() {
    private lateinit var v: ActivityLauncherBinding
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        checkAssistantOnBoot = granted
        v.bootCheckSwitch.isChecked = granted
    }
    private val screenContextRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(v.root)

        v.assistantSettingsButton.setOnClickListener {
            openSettings(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        v.screenAccessButton.setOnClickListener {
            screenContextRequestIntent()?.let {
                try {
                    screenContextRequest.launch(it)
                } catch (e: ActivityNotFoundException) {
                }
            }
        }
        v.bootCheckSwitch.isChecked = checkAssistantOnBoot
        v.bootCheckSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAssistantOnBoot = checked
            }
        }
        v.logsButton.setOnClickListener { startActivity(Intent(this, LogActivity::class.java)) }
        v.header.versionText.text = getString(R.string.version_value, BuildConfig.VERSION_NAME)
        v.aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isAssistant = isDefaultAssistant
        if (isAssistant) cancelAssistantNotification()
        val access = if (isAssistant) screenContextAccess() else ScreenContextAccess.UNKNOWN
        v.statusText.setText(
            when {
                !isAssistant -> R.string.launcher_status_inactive
                access == ScreenContextAccess.GRANTED -> R.string.launcher_status_active
                access == ScreenContextAccess.REQUESTABLE -> R.string.launcher_status_no_screen_access
                access == ScreenContextAccess.UNREQUESTABLE -> R.string.launcher_status_screen_access_denied
                else -> R.string.launcher_status_unknown
            }
        )
        v.screenAccessButton.visibility = if (access == ScreenContextAccess.REQUESTABLE) View.VISIBLE else View.GONE
    }

    private fun openSettings(vararg intents: Intent) {
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
            }
        }
    }
}
