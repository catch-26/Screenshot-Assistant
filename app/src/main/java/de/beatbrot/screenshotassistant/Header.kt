package de.beatbrot.screenshotassistant

import de.beatbrot.screenshotassistant.databinding.ViewHeaderBinding

fun ViewHeaderBinding.showAppVersion() {
    versionText.text = root.context.getString(R.string.version_value, BuildConfig.VERSION_NAME)
}
