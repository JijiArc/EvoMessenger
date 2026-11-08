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

        /** mailto:, tel:, market: or fb-messenger: link: hand it to the matching app. */
        data class ViewIntent(val url: String) : Navigation()

        /** Anything else (intent:, fb:, data:, blob:, javascript:...): do nothing. */
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
            // mailto/tel, and the "get Messenger" links Facebook shows: the official Messenger app or the
            // Play Store. These go to the matching app only if the user taps them.
            "mailto", "tel", "market", "fb-messenger" -> Navigation.ViewIntent(url.trim())
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

    private val MOBILE_FACEBOOK_HOSTS = setOf(
        "m.facebook.com", "mobile.facebook.com", "touch.facebook.com", "mbasic.facebook.com"
    )

    /**
     * Facebook's mobile hosts do not offer messaging in a browser. When the desktop site is on, a link to
     * one of them is rewritten to the same address on www.facebook.com. Returns null if no change is needed.
     */
    fun toDesktopHost(url: String): String? {
        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            return null
        }
        if (uri.scheme?.lowercase() != "https") return null
        val host = uri.host?.lowercase() ?: return null
        if (host !in MOBILE_FACEBOOK_HOSTS) return null
        // Leave addresses with user-info or a port alone: they are not ordinary Facebook links.
        if (uri.rawAuthority?.lowercase() != host) return null
        return "https://www.facebook.com" + (uri.rawPath ?: "") +
            (uri.rawQuery?.let { "?$it" } ?: "") + (uri.rawFragment?.let { "#$it" } ?: "")
    }

    /**
     * Whether a file picker that asks for [acceptTypes] can take an audio clip. An empty list means
     * "any file". The photo and video buttons ask for image and video types only, so they do not qualify.
     */
    fun acceptsAudio(acceptTypes: Array<String>?): Boolean {
        val types = acceptTypes.orEmpty()
            .flatMap { it.split(',') }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (types.isEmpty()) return true
        return types.any {
            it == "*/*" || it == "audio/*" || it.startsWith("audio/") ||
                it == ".m4a" || it == ".mp4" || it == ".aac"
        }
    }

    /**
     * Facebook's error pages carry titles like "This content isn't available right now" or "Page not found".
     * Used only to offer help (switch site mode, open in Chrome); it never blocks anything.
     */
    fun looksUnavailable(title: String?): Boolean {
        val t = title?.lowercase()?.trim() ?: return false
        return t.contains("isn't available") || t.contains("isn\u2019t available") ||
            t.contains("is not available") || t.contains("not found") ||
            t.contains("broken") || t.contains("unavailable")
    }

    /**
     * A version of [url] that is safe to show or share when asking for help: no query, no fragment, and
     * no more than the first two path segments (conversation and profile ids are cut off).
     */
    fun safeAddress(url: String?): String {
        if (url.isNullOrBlank()) return "(none)"
        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            return "(unreadable)"
        }
        val scheme = uri.scheme ?: return "(unreadable)"
        val host = uri.host ?: return "$scheme:"
        val segments = (uri.path ?: "").split('/').filter { it.isNotEmpty() }
        val shown = segments.take(2).joinToString("/")
        val more = if (segments.size > 2) "/\u2026" else ""
        return "$scheme://$host" + if (shown.isEmpty()) "" else "/$shown$more"
    }
}
