package de.beatbrot.screenshotassistant

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.beatbrot.screenshotassistant.databinding.ActivityLauncherBinding
import de.beatbrot.screenshotassistant.util.ScreenContextAccess
import de.beatbrot.screenshotassistant.util.screenContextAccess
import de.beatbrot.screenshotassistant.util.screenContextRequestIntent

class LauncherActivity : AppCompatActivity() {
    private lateinit var v: ActivityLauncherBinding
    private val screenContextRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(v.root)

        v.assistantSettingsButton.setOnClickListener {
            openSettings(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        v.appInfoButton.setOnClickListener {
            openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
        v.screenAccessButton.setOnClickListener {
            screenContextRequestIntent()?.let {
                try {
                    screenContextRequest.launch(it)
                } catch (e: ActivityNotFoundException) {
                }
            }
        }
        v.aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isAssistant = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
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
