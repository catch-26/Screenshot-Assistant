package de.beatbrot.screenshotassistant

import android.Manifest
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
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
import de.beatbrot.screenshotassistant.util.isDefaultSaveFolder
import de.beatbrot.screenshotassistant.util.saveFolder
import de.beatbrot.screenshotassistant.util.saveFolderLabel
import de.beatbrot.screenshotassistant.util.setSaveFolder
import de.beatbrot.screenshotassistant.util.screenContextAccess
import de.beatbrot.screenshotassistant.util.screenContextRequestIntent
import de.beatbrot.screenshotassistant.util.imageFormat
import de.beatbrot.screenshotassistant.util.imageQuality
import de.beatbrot.screenshotassistant.util.sharedPrefs

class LauncherActivity : AppCompatActivity() {
    private lateinit var v: ActivityLauncherBinding
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
        initImageSettings()
        v.saveFolderChangeButton.setOnClickListener {
            try {
                saveFolderPicker.launch(sharedPrefs.saveFolder)
            } catch (e: ActivityNotFoundException) {
            }
        }
        v.saveFolderDefaultButton.setOnClickListener {
            setSaveFolder(null)
            updateSaveFolder()
        }
        v.logsButton.setOnClickListener { startActivity(Intent(this, LogActivity::class.java)) }
        v.header.versionText.text = getString(R.string.version_value, BuildConfig.VERSION_NAME)
        v.aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
    }

    @Suppress("DEPRECATION")
    private fun initImageSettings() {
        val prefs = sharedPrefs
        val formats = mapOf(
            R.id.formatPng to Bitmap.CompressFormat.PNG,
            R.id.formatJpeg to Bitmap.CompressFormat.JPEG,
            R.id.formatWebp to Bitmap.CompressFormat.WEBP
        )
        v.formatGroup.check(formats.entries.first { it.value == prefs.imageFormat }.key)
        v.qualitySlider.value = prefs.imageQuality.toFloat()
        updateQuality(prefs.imageFormat, prefs.imageQuality)

        v.formatGroup.addOnButtonCheckedListener { _, id, checked ->
            val format = formats[id]
            if (checked && format != null) {
                prefs.imageFormat = format
                updateQuality(format, prefs.imageQuality)
            }
        }
        v.qualitySlider.addOnChangeListener { _, value, _ ->
            prefs.imageQuality = value.toInt()
            updateQuality(prefs.imageFormat, value.toInt())
        }
    }

    // PNG is lossless, so quality only applies to JPEG and WEBP.
    private fun updateQuality(format: Bitmap.CompressFormat, quality: Int) {
        val lossy = format != Bitmap.CompressFormat.PNG
        v.qualitySlider.isEnabled = lossy
        v.qualityLabel.isEnabled = lossy
        v.qualityLabel.alpha = if (lossy) 1F else 0.5F
        v.qualityLabel.text = getString(R.string.image_quality_value, quality)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateSaveFolder()
    }

    private fun updateSaveFolder() {
        if (sharedPrefs.saveFolder?.isDefaultSaveFolder == true) {
            setSaveFolder(null)
        }
        v.saveFolderText.text = saveFolderLabel()
        v.saveFolderDefaultButton.visibility = if (sharedPrefs.saveFolder != null) View.VISIBLE else View.GONE
    }

    private fun updateStatus() {
        val isAssistant = isDefaultAssistant
        if (isAssistant) cancelAssistantNotification()
        val access = if (isAssistant) screenContextAccess() else ScreenContextAccess.UNKNOWN
        val status = when {
            !isAssistant -> R.string.launcher_status_inactive
            access == ScreenContextAccess.REQUESTABLE -> R.string.launcher_status_no_screen_access
            access == ScreenContextAccess.UNREQUESTABLE -> R.string.launcher_status_screen_access_denied
            else -> null
        }
        v.setupWarning.visibility = if (status != null) View.VISIBLE else View.GONE
        status?.let { v.statusText.setText(it) }
        v.screenAccessButton.visibility = if (access == ScreenContextAccess.REQUESTABLE) View.VISIBLE else View.GONE
        v.assistantSettingsButton.visibility = if (access == ScreenContextAccess.REQUESTABLE) View.GONE else View.VISIBLE
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
