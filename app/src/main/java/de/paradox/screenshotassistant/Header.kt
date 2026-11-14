package de.paradox.screenshotassistant

import de.paradox.screenshotassistant.databinding.ViewHeaderBinding

fun ViewHeaderBinding.showAppVersion() {
    versionText.text = root.context.getString(R.string.version_value, BuildConfig.VERSION_NAME)
}
