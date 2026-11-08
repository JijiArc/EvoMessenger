package com.mathroda.messengerclone.web

import android.content.Context

enum class ViewMode {
    /** App bars around the embedded Facebook page. */
    SHELL,

    /** The Facebook page fills the screen; the menu is behind a small edge handle. */
    FULL_SCREEN
}

/** Display preferences only. Nothing here is secret, and no Facebook data is stored in it. */
class WebPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var viewMode: ViewMode
        get() = try {
            ViewMode.valueOf(prefs.getString(KEY_MODE, null) ?: "")
        } catch (e: IllegalArgumentException) {
            ViewMode.SHELL
        }
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    /**
     * Desktop site is the default: Facebook's mobile site does not offer messaging in a browser, only a
     * "get the app" page. The key changed from "desktop_site" so a "mobile" choice saved by an earlier
     * build does not keep the app on the page that cannot message.
     */
    var desktopSite: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP, true)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP, value).apply()

    private companion object {
        const val FILE = "web_prefs"
        const val KEY_MODE = "view_mode"
        const val KEY_DESKTOP = "desktop_site_v2"
    }
}
