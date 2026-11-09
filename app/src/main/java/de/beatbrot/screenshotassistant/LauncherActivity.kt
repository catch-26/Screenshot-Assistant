package de.beatbrot.screenshotassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.beatbrot.screenshotassistant.databinding.ActivityLauncherBinding
import de.beatbrot.screenshotassistant.databinding.ItemFormatButtonBinding
import de.beatbrot.screenshotassistant.image.ImageFormat
import de.beatbrot.screenshotassistant.image.imageFormat
import de.beatbrot.screenshotassistant.image.normalizeSaveFolder
import de.beatbrot.screenshotassistant.image.saveFolder
import de.beatbrot.screenshotassistant.image.saveFolderLabel
import de.beatbrot.screenshotassistant.image.setSaveFolder
import de.beatbrot.screenshotassistant.setup.AssistantState
import de.beatbrot.screenshotassistant.setup.ScreenContextAccess
import de.beatbrot.screenshotassistant.setup.assistantState
import de.beatbrot.screenshotassistant.setup.assistantDiagnosis
import de.beatbrot.screenshotassistant.setup.assistantSettingsIntent
import de.beatbrot.screenshotassistant.setup.cancelAssistantNotification
import de.beatbrot.screenshotassistant.setup.checkAssistantOnBoot
import de.beatbrot.screenshotassistant.setup.restartAssistantService
import de.beatbrot.screenshotassistant.setup.screenContextAccess
import de.beatbrot.screenshotassistant.setup.screenContextRequestIntent
import de.beatbrot.screenshotassistant.util.launchIfAvailable
import de.beatbrot.screenshotassistant.util.logEvent
import de.beatbrot.screenshotassistant.util.sharedPrefs
import de.beatbrot.screenshotassistant.util.startFirstAvailable

class LauncherActivity : AppCompatActivity() {
    private lateinit var v: ActivityLauncherBinding
    private var lastState: AssistantState? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        checkAssistantOnBoot = granted
        v.bootCheckSwitch.isChecked = granted
    }
    private val screenContextRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatus() }
    private val saveFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null) {
            setSaveFolder(folder)
            updateSaveFolder()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(v.root)

        listOf(v.assistantSettingsButton, v.assistantSettingsOutlinedButton).forEach { button ->
            button.setOnClickListener {
                startFirstAvailable(assistantSettingsIntent(), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
        v.introText.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ -> matchCheckToIntro(bottom - top) }
        v.restartAssistantButton.setOnClickListener {
            v.restartAssistantButton.isEnabled = false
            restartAssistantService("button")
            Handler(Looper.getMainLooper()).postDelayed({
                v.restartAssistantButton.isEnabled = true
                updateStatus()
            }, 3_000)
        }
        v.screenAccessButton.setOnClickListener {
            screenContextRequestIntent()?.let { screenContextRequest.launchIfAvailable(it) }
        }
        v.bootCheckSwitch.isChecked = checkAssistantOnBoot
        v.bootCheckSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAssistantOnBoot = checked
            }
        }
        initImageSettings()
        v.saveFolderChangeButton.setOnClickListener { saveFolderPicker.launchIfAvailable(saveFolder) }
        v.saveFolderDefaultButton.setOnClickListener {
            setSaveFolder(null)
            updateSaveFolder()
        }
        v.logsButton.setOnClickListener { startActivity(Intent(this, LogActivity::class.java)) }
        v.header.showAppVersion()
        v.aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
    }

    private fun initImageSettings() {
        val prefs = sharedPrefs
        val buttons = mutableMapOf<Int, ImageFormat>()
        ImageFormat.entries.forEach { format ->
            val button = ItemFormatButtonBinding.inflate(layoutInflater, v.formatGroup, false).root
            button.id = View.generateViewId()
            button.text = format.label
            v.formatGroup.addView(button)
            buttons[button.id] = format
        }
        v.formatGroup.check(buttons.entries.first { it.value == prefs.imageFormat }.key)

        v.formatGroup.addOnButtonCheckedListener { _, id, checked ->
            val format = buttons[id]
            if (checked && format != null) {
                prefs.imageFormat = format
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateSaveFolder()
    }

    private fun updateSaveFolder() {
        normalizeSaveFolder()
        v.saveFolderText.text = saveFolderLabel()
        v.saveFolderDefaultButton.visibility = if (saveFolder != null) View.VISIBLE else View.GONE
    }

    private fun updateStatus() {
        val state = assistantState()
        if (state != lastState) {
            logEvent("App: assistant $state (${assistantDiagnosis()})")
            lastState = state
        }
        val isAssistant = state == AssistantState.OK
        if (isAssistant) cancelAssistantNotification()
        val access = if (isAssistant) screenContextAccess() else ScreenContextAccess.UNKNOWN
        val status = when {
            state == AssistantState.NOT_STARTED -> R.string.launcher_status_not_started
            !isAssistant -> R.string.launcher_status_inactive
            access == ScreenContextAccess.REQUESTABLE -> R.string.launcher_status_no_screen_access
            access == ScreenContextAccess.UNREQUESTABLE -> R.string.launcher_status_screen_access_denied
            else -> null
        }
        v.setupWarning.visibility = if (status != null) View.VISIBLE else View.GONE
        status?.let { v.statusText.setText(it) }
        v.restartAssistantButton.visibility = if (state == AssistantState.NOT_STARTED) View.VISIBLE else View.GONE
        v.screenAccessButton.visibility = if (access == ScreenContextAccess.REQUESTABLE) View.VISIBLE else View.GONE
        v.assistantCheck.visibility = if (isAssistant) View.VISIBLE else View.INVISIBLE
        // Filled buttons mark a step that still needs action; once done, the settings button turns outlined like the others.
        val settingsNeedAction = !isAssistant || access == ScreenContextAccess.UNREQUESTABLE
        v.assistantSettingsButton.visibility = if (settingsNeedAction) View.VISIBLE else View.GONE
        v.assistantSettingsOutlinedButton.visibility = if (settingsNeedAction) View.GONE else View.VISIBLE
    }

    // The check mark is as tall as the setup sentence, which can wrap to more lines on narrow screens.
    private fun matchCheckToIntro(height: Int) {
        val params = v.assistantCheck.layoutParams
        if (height > 0 && params.height != height) {
            params.width = height
            params.height = height
            v.assistantCheck.post { v.assistantCheck.layoutParams = params }
        }
    }
}
