package com.mathroda.messengerclone.web

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets the surrounding screen act on the embedded page (reload, back, sign out) without
 * owning the WebView itself. The controller never reads cookies, tokens or page content.
 */
class WebController {

    var webView: WebView? = null
    var defaultUserAgent: String = ""

    private var appliedDesktop: Boolean? = null

    /** Bumped to throw away the WebView and build a new one (for example after the renderer crashed). */
    var generation by mutableStateOf(0)
        private set

    fun recreate() {
        generation += 1
    }

    fun configureForMode(view: WebView, desktop: Boolean) {
        val settings = view.settings
        settings.userAgentString = if (desktop) WebConfig.desktopUserAgent(defaultUserAgent) else defaultUserAgent
        settings.loadWithOverviewMode = desktop
        appliedDesktop = desktop
    }

    /** Switches between the mobile and desktop site and reloads the page. No-op if already in that mode. */
    fun setDesktopMode(desktop: Boolean) {
        val view = webView ?: return
        if (appliedDesktop == desktop) return
        configureForMode(view, desktop)
        reload()
    }

    fun currentUrl(): String = webView?.url?.takeIf { it.startsWith("https://") } ?: WebConfig.MESSAGES_URL

    fun reload() {
        val view = webView ?: return
        val url = view.url
        if (url.isNullOrBlank() || url == "about:blank") view.loadUrl(WebConfig.MESSAGES_URL) else view.reload()
    }

    fun loadHome() {
        webView?.loadUrl(WebConfig.MESSAGES_URL)
    }

    fun goBack(): Boolean {
        val view = webView ?: return false
        if (!view.canGoBack()) return false
        view.goBack()
        return true
    }

    /** User-requested sign out: removes this app's Facebook cookies and site data, then reloads. */
    fun clearSession() {
        val view = webView
        val cookies = CookieManager.getInstance()
        cookies.removeAllCookies {
            cookies.flush()
            view?.post { view.loadUrl(WebConfig.MESSAGES_URL) }
        }
        WebStorage.getInstance().deleteAllData()
        view?.clearCache(true)
        view?.clearHistory()
        view?.clearFormData()
    }
}
