package de.beatbrot.screenshotassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import de.beatbrot.screenshotassistant.databinding.ActivityAboutBinding
import de.beatbrot.screenshotassistant.databinding.ItemLinkBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var v: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(v.root)

        v.header.versionText.text = getString(R.string.version_value, BuildConfig.VERSION_NAME)

        v.projectList.addLink(R.drawable.ic_github_gray_24dp, getString(R.string.about_source), "catch-26/Screenshot-Assistant", "https://github.com/catch-26/Screenshot-Assistant")
        v.projectList.addLink(R.drawable.ic_info_outline_gray_24dp, getString(R.string.about_releases), getString(R.string.about_releases_subtitle), "https://github.com/catch-26/Screenshot-Assistant/releases")
        v.projectList.addLink(R.drawable.ic_grade_unfocused_24dp, getString(R.string.about_original), "beatbrot/ScreenshotAssistant", "https://github.com/beatbrot/ScreenshotAssistant")
        v.projectList.addLink(R.drawable.ic_book_gray_24dp, getString(R.string.license), getString(R.string.apache_2), "https://github.com/catch-26/Screenshot-Assistant/blob/master/LICENSE")

        for (library in libraries) {
            v.libraryList.addLink(R.drawable.ic_book_gray_24dp, library.name, library.version, library.url)
        }
    }

    private fun ViewGroup.addLink(@DrawableRes icon: Int, title: String, subtitle: String, url: String) {
        val item = ItemLinkBinding.inflate(layoutInflater, this, true)
        item.icon.setImageResource(icon)
        item.title.text = title
        item.subtitle.text = subtitle
        item.root.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private class Library(val name: String, val version: String, val url: String)

    private val libraries = listOf(
        Library("Android Image Cropper", "4.7.0", "https://github.com/CanHub/Android-Image-Cropper"),
        Library("AndroidX, Material Components", "Jetpack", "https://developer.android.com/jetpack/androidx"),
    )
}
