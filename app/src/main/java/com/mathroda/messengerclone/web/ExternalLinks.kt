package com.mathroda.messengerclone.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.mathroda.messengerclone.R

/** Opens links outside the embedded Facebook page. */
object ExternalLinks {

    /**
     * Opens a Chrome Custom Tab (or the default browser if Chrome is not available).
     * A Custom Tab is Chrome itself, so it is also the fallback if Facebook refuses to sign in
     * inside an embedded WebView. It does not share the WebView's login session.
     */
    fun openInCustomTab(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }

    fun view(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_for_link, Toast.LENGTH_LONG).show()
        }
    }
}
