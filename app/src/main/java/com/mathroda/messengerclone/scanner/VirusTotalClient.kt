package com.mathroda.messengerclone.scanner

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

class VirusTotalException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { BAD_KEY, RATE_LIMIT, NETWORK, TIMEOUT, SERVER, REJECTED, UNEXPECTED }
}

/**
 * Minimal VirusTotal v3 client using only the platform HTTP stack.
 *
 * Connections are HTTPS only and certificate errors are never ignored. The API key is sent in a
 * request header and is never written to a log, an error message or a URL.
 */
object VirusTotalClient {

    private const val BASE = "https://www.virustotal.com/api/v3"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    data class Analysis(val completed: Boolean, val stats: ScanStats?, val urlId: String?)

    /** Submits [url] for scanning and returns the analysis id to poll. */
    fun submitUrl(url: String, apiKey: String): String {
        val body = "url=" + URLEncoder.encode(url, "UTF-8")
        val text = request("POST", "/urls", apiKey, body)
        try {
            return JSONObject(text).getJSONObject("data").getString("id")
        } catch (e: JSONException) {
            throw VirusTotalException(
                VirusTotalException.Kind.UNEXPECTED,
                "VirusTotal sent a reply this app does not understand."
            )
        }
    }

    fun fetchAnalysis(analysisId: String, apiKey: String): Analysis {
        // Analysis ids are made of letters, digits, '-' and '_'. Refuse anything else before it
        // goes into a URL path.
        if (!analysisId.matches(Regex("^[A-Za-z0-9_=-]{1,200}$"))) {
            throw VirusTotalException(
                VirusTotalException.Kind.UNEXPECTED,
                "VirusTotal sent a reply this app does not understand."
            )
        }
        val text = request("GET", "/analyses/$analysisId", apiKey, null)
        try {
            val root = JSONObject(text)
            val attributes = root.getJSONObject("data").getJSONObject("attributes")
            val completed = attributes.optString("status") == "completed"
            val statsJson = attributes.optJSONObject("stats")
            val stats = statsJson?.let {
                ScanStats(
                    malicious = it.optInt("malicious", 0),
                    suspicious = it.optInt("suspicious", 0),
                    harmless = it.optInt("harmless", 0),
                    undetected = it.optInt("undetected", 0)
                )
            }
            val urlId = root.optJSONObject("meta")
                ?.optJSONObject("url_info")
                ?.optString("id")
                ?.takeIf { it.matches(Regex("^[A-Fa-f0-9]{64}$")) }
            return Analysis(completed, stats, urlId)
        } catch (e: JSONException) {
            throw VirusTotalException(
                VirusTotalException.Kind.UNEXPECTED,
                "VirusTotal sent a reply this app does not understand."
            )
        }
    }

    /** Public report page for a link. [urlId] is the SHA-256 identifier VirusTotal uses for URLs. */
    fun reportUrl(urlId: String): String = "https://www.virustotal.com/gui/url/$urlId"

    const val WEBSITE_FALLBACK = "https://www.virustotal.com/gui/home/url"
    const val API_KEY_PAGE = "https://www.virustotal.com/gui/join-us"

    private fun request(method: String, path: String, apiKey: String, body: String?): String {
        val connection = try {
            URL(BASE + path).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw VirusTotalException(VirusTotalException.Kind.NETWORK, "Could not start the connection.")
        }

        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("x-apikey", apiKey)
            connection.setRequestProperty("accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("content-type", "application/x-www-form-urlencoded")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            if (code in 200..299) {
                return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            throw mapHttpError(code)
        } catch (e: VirusTotalException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw VirusTotalException(
                VirusTotalException.Kind.TIMEOUT,
                "VirusTotal took too long to answer."
            )
        } catch (e: UnknownHostException) {
            throw VirusTotalException(
                VirusTotalException.Kind.NETWORK,
                "Could not reach VirusTotal. Check your internet connection."
            )
        } catch (e: IOException) {
            // Includes TLS failures. They are reported, never bypassed.
            throw VirusTotalException(
                VirusTotalException.Kind.NETWORK,
                "The secure connection to VirusTotal failed. Check your internet connection."
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun mapHttpError(code: Int): VirusTotalException = when {
        code == 401 -> VirusTotalException(
            VirusTotalException.Kind.BAD_KEY,
            "VirusTotal did not accept the API key."
        )
        code == 403 -> VirusTotalException(
            VirusTotalException.Kind.BAD_KEY,
            "VirusTotal refused the request. The key may be missing permission or over its quota."
        )
        code == 429 -> VirusTotalException(
            VirusTotalException.Kind.RATE_LIMIT,
            "VirusTotal's request limit was reached."
        )
        code in 500..599 -> VirusTotalException(
            VirusTotalException.Kind.SERVER,
            "VirusTotal is having problems right now."
        )
        else -> VirusTotalException(
            VirusTotalException.Kind.REJECTED,
            "VirusTotal could not process this link (error $code)."
        )
    }
}
