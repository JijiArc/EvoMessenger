package com.mathroda.messengerclone.scanner

import java.net.IDN
import java.security.MessageDigest

/**
 * Checks and tidies a link before it is sent anywhere. Plain Kotlin with no Android classes.
 *
 * Only http and https links are accepted. A link typed without a scheme ("example.com/page") is
 * treated as https. Addresses that only exist on the user's own network are refused, because a
 * public scanner cannot see them and sending them would only leak internal details.
 */
object UrlValidator {

    sealed class Result {
        data class Valid(val url: String) : Result()
        data class Invalid(val reason: String) : Result()
    }

    private const val MAX_LENGTH = 2048

    private val SCHEME_PREFIX = Regex("^([A-Za-z][A-Za-z0-9+.-]*):(//)?")
    private val PORT_ONLY = Regex("^\\d{1,5}([/?#].*)?$")
    private val HOST_LABEL = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val IPV4 = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")

    fun check(input: String): Result {
        val text = input.trim()
        if (text.isEmpty()) return Result.Invalid("Enter a link to scan.")
        if (text.length > MAX_LENGTH) return Result.Invalid("That link is too long.")
        if (text.any { it.isWhitespace() || it.isISOControl() }) {
            return Result.Invalid("A link cannot contain spaces or hidden characters.")
        }

        // Work out the scheme, adding https:// when the user left it off.
        val prefix = SCHEME_PREFIX.find(text)
        val scheme: String
        val afterScheme: String
        if (prefix == null) {
            scheme = "https"
            afterScheme = text
        } else {
            val found = prefix.groupValues[1].lowercase()
            val hasSlashes = prefix.groupValues[2].isNotEmpty()
            val rest = text.substring(prefix.range.last + 1)
            if (hasSlashes) {
                if (found != "http" && found != "https") {
                    return Result.Invalid("Only http and https links can be scanned.")
                }
                scheme = found
                afterScheme = rest
            } else if (PORT_ONLY.matches(rest)) {
                // Looks like "example.com:8080/path": no scheme was typed.
                scheme = "https"
                afterScheme = text
            } else {
                return Result.Invalid("Only http and https links can be scanned.")
            }
        }

        // Split "userinfo@host:port" from the path, query and fragment.
        val authorityEnd = afterScheme.indexOfFirst { it == '/' || it == '?' || it == '#' }
            .let { if (it == -1) afterScheme.length else it }
        val authority = afterScheme.substring(0, authorityEnd)
        val tail = afterScheme.substring(authorityEnd)
        if (authority.isEmpty()) return Result.Invalid("The link has no website address.")

        val atIndex = authority.lastIndexOf('@')
        val userInfo = if (atIndex >= 0) authority.substring(0, atIndex) else null
        val hostAndPort = if (atIndex >= 0) authority.substring(atIndex + 1) else authority

        if (hostAndPort.startsWith("[")) {
            return Result.Invalid("Addresses written in IPv6 form are not supported.")
        }

        val colon = hostAndPort.lastIndexOf(':')
        val rawHost = if (colon >= 0) hostAndPort.substring(0, colon) else hostAndPort
        val port: Int? = if (colon >= 0) {
            val portText = hostAndPort.substring(colon + 1)
            val parsed = portText.toIntOrNull()
            if (parsed == null || parsed !in 1..65535) {
                return Result.Invalid("The port number in the link is not valid.")
            }
            parsed
        } else null

        if (rawHost.isEmpty()) return Result.Invalid("The link has no website address.")

        val host = try {
            IDN.toASCII(rawHost.trimEnd('.'), IDN.ALLOW_UNASSIGNED).lowercase()
        } catch (e: IllegalArgumentException) {
            return Result.Invalid("The website address in the link is not valid.")
        }
        if (host.isEmpty() || host.length > 253) {
            return Result.Invalid("The website address in the link is not valid.")
        }

        val ipv4 = IPV4.matchEntire(host)
        if (ipv4 != null) {
            val parts = ipv4.groupValues.drop(1).map { it.toInt() }
            if (parts.any { it > 255 }) return Result.Invalid("That IP address is not valid.")
            if (isPrivateIpv4(parts)) {
                return Result.Invalid("Private or local addresses cannot be scanned.")
            }
        } else {
            if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) {
                return Result.Invalid("Private or local addresses cannot be scanned.")
            }
            val labels = host.split('.')
            if (labels.size < 2 || labels.any { !HOST_LABEL.matches(it) }) {
                return Result.Invalid("That does not look like a website address.")
            }
            val tld = labels.last()
            val tldOk = tld.startsWith("xn--") || (tld.length >= 2 && tld.all { it in 'a'..'z' })
            if (!tldOk) return Result.Invalid("That does not look like a website address.")
        }

        val rebuilt = buildString {
            append(scheme).append("://")
            if (userInfo != null) append(userInfo).append('@')
            append(host)
            if (port != null) append(':').append(port)
            append(tail)
        }
        return Result.Valid(rebuilt)
    }

    private fun isPrivateIpv4(p: List<Int>): Boolean =
        p[0] == 0 ||
            p[0] == 10 ||
            p[0] == 127 ||
            (p[0] == 169 && p[1] == 254) ||
            (p[0] == 172 && p[1] in 16..31) ||
            (p[0] == 192 && p[1] == 168) ||
            (p[0] == 100 && p[1] in 64..127)

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
