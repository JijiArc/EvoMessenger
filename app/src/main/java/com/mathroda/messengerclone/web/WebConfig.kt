package com.mathroda.messengerclone.web

import java.net.URI
import java.net.URLDecoder

/**
 * Where the embedded browser may go. Pure Kotlin (no Android classes) so it can be unit-tested on a JVM.
 *
 * The WebView only stays on Facebook's own sites. Any other web link is handed to a Chrome Custom Tab,
 * so the page that holds the Facebook session never browses the open web.
 */
object WebConfig {

    const val MESSAGES_URL = "https://www.facebook.com/messages/"

    // Facebook's own domains that login, security checks and Messenger use.
    private val ALLOWED_DOMAINS = listOf("facebook.com", "messenger.com", "fb.com", "fbsbx.com", "meta.com")

    /** What to do with a main-frame navigation. */
    sealed class Navigation {
        /** Load it inside the WebView. */
        object Allow : Navigation()

        /** An ordinary web link outside Facebook: open it in a Chrome Custom Tab. */
        data class External(val url: String) : Navigation()

        /** mailto: or tel: link: hand it to the matching app. */
        data class ViewIntent(val url: String) : Navigation()

        /** Anything else (intent:, fb:, market:, data:, blob:, javascript:...): do nothing. */
        object Block : Navigation()
    }

    fun classify(url: String): Navigation {
        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            return Navigation.Block
        }
        val scheme = uri.scheme?.lowercase() ?: return Navigation.Block

        if (url.trim().equals("about:blank", ignoreCase = true)) return Navigation.Allow

        return when (scheme) {
            "https" -> {
                unwrapLinkShim(uri)?.let { return Navigation.External(it) }
                if (isAllowedHost(uri.host)) Navigation.Allow else Navigation.External(url.trim())
            }
            // Cleartext is not allowed in the app. A plain-http link is an outside link.
            "http" -> Navigation.External(url.trim())
            "mailto", "tel" -> Navigation.ViewIntent(url.trim())
            else -> Navigation.Block
        }
    }

    fun isAllowedHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return ALLOWED_DOMAINS.any { h == it || h.endsWith(".$it") }
    }

    /** Facebook wraps outgoing links as https://l.facebook.com/l.php?u=<target>. Returns the target. */
    private fun unwrapLinkShim(uri: URI): String? {
        val host = uri.host?.lowercase() ?: return null
        if (host != "l.facebook.com" && host != "lm.facebook.com") return null
        if (uri.path != "/l.php") return null
        val query = uri.rawQuery ?: return null
        val target = query.split('&')
            .firstOrNull { it.startsWith("u=") }
            ?.substring(2)
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?: return null
        val lower = target.lowercase()
        return if (lower.startsWith("https://") || lower.startsWith("http://")) target else null
    }

    /**
     * Turns the WebView's own mobile user agent into a desktop one, like "Request desktop site" in Chrome:
     * the Android/device part becomes a Linux desktop and the "Mobile" token is dropped.
     */
    fun desktopUserAgent(defaultUserAgent: String): String =
        defaultUserAgent
            .replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)")
            .replace(" Mobile", "")
}
