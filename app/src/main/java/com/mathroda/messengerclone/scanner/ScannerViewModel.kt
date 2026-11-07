package com.mathroda.messengerclone.scanner

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.mathroda.messengerclone.data.SecurePrefs

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val step: String) : ScanState()
    data class Done(
        val url: String,
        val stats: ScanStats,
        val verdict: Verdict,
        val reportUrl: String
    ) : ScanState()
    data class Failed(
        val message: String,
        val canResume: Boolean
    ) : ScanState()
}

/**
 * Runs scans off the main thread and keeps the screen state across rotation.
 *
 * The VirusTotal key is stored with EncryptedSharedPreferences. If encrypted storage is not
 * available on a phone, the key is kept in memory for this session only.
 */
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    var input by mutableStateOf("")
        private set
    var inputError by mutableStateOf<String?>(null)
        private set
    var state by mutableStateOf<ScanState>(ScanState.Idle)
        private set
    /** null until the stored key has been looked up. */
    var hasKey by mutableStateOf<Boolean?>(null)
        private set
    var keyMessage by mutableStateOf<String?>(null)
        private set

    private val appContext = application.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs: SharedPreferences? by lazy { SecurePrefs.open(appContext, PREFS_NAME) }
    @Volatile private var memoryKey: String? = null

    private var scanThread: Thread? = null
    @Volatile private var scanToken = 0
    private var pendingAnalysisId: String? = null
    private var pendingUrl: String? = null

    init {
        Thread({
            val stored = readKey()
            mainHandler.post { hasKey = stored != null }
        }, "scanner-key-load").start()
    }

    fun onInputChange(value: String) {
        input = value
        inputError = null
    }

    // --------------------------------------------------------------- key

    fun saveKey(raw: String) {
        val key = raw.trim()
        if (!KEY_REGEX.matches(key)) {
            keyMessage = "That does not look like a VirusTotal API key. It is 64 letters and numbers."
            return
        }
        keyMessage = null
        Thread({
            memoryKey = key
            prefs?.edit()?.putString(KEY_NAME, key)?.apply()
            mainHandler.post { hasKey = true }
        }, "scanner-key-save").start()
    }

    fun removeKey() {
        cancel()
        memoryKey = null
        keyMessage = null
        hasKey = false
        Thread({ prefs?.edit()?.remove(KEY_NAME)?.apply() }, "scanner-key-remove").start()
    }

    private fun readKey(): String? = prefs?.getString(KEY_NAME, null) ?: memoryKey

    // -------------------------------------------------------------- scan

    fun scan() {
        if (state is ScanState.Scanning) return

        val checked = UrlValidator.check(input)
        if (checked is UrlValidator.Result.Invalid) {
            inputError = checked.reason
            return
        }
        if (hasKey != true) {
            inputError = "Add your VirusTotal API key above first."
            return
        }
        val url = (checked as UrlValidator.Result.Valid).url
        pendingAnalysisId = null
        pendingUrl = url
        start(url, existingAnalysisId = null)
    }

    /** Continues waiting for a scan that was already submitted. */
    fun resume() {
        val url = pendingUrl
        val id = pendingAnalysisId
        if (state is ScanState.Scanning || url == null || id == null) return
        start(url, existingAnalysisId = id)
    }

    fun cancel() {
        scanToken++
        scanThread?.interrupt()
        scanThread = null
        if (state is ScanState.Scanning) state = ScanState.Idle
    }

    private fun start(url: String, existingAnalysisId: String?) {
        val token = ++scanToken
        state = ScanState.Scanning("Preparing the scan…")
        val thread = Thread({ runScan(token, url, existingAnalysisId) }, "link-scan")
        scanThread = thread
        thread.start()
    }

    private fun runScan(token: Int, url: String, existingAnalysisId: String?) {
        val key = readKey()
        if (key == null) {
            post(token) { state = ScanState.Failed("The API key is missing. Add it above and try again.", false) }
            return
        }

        try {
            val analysisId = existingAnalysisId ?: run {
                post(token) { state = ScanState.Scanning("Sending the link to VirusTotal…") }
                VirusTotalClient.submitUrl(url, key)
            }
            post(token) { pendingAnalysisId = analysisId }

            for (attempt in 0 until MAX_POLLS) {
                post(token) {
                    state = ScanState.Scanning("Waiting for the results (${attempt + 1} of $MAX_POLLS)…")
                }
                if (!sleepUnlessCancelled(token, if (attempt == 0) FIRST_WAIT_MS else POLL_WAIT_MS)) return

                val analysis = VirusTotalClient.fetchAnalysis(analysisId, key)
                val stats = analysis.stats
                if (analysis.completed && stats != null) {
                    val verdict = ScanVerdicts.from(stats)
                    val reportId = analysis.urlId ?: UrlValidator.sha256Hex(url)
                    post(token) {
                        pendingAnalysisId = null
                        state = ScanState.Done(url, stats, verdict, VirusTotalClient.reportUrl(reportId))
                    }
                    return
                }
            }
            post(token) {
                state = ScanState.Failed(
                    "VirusTotal is still analysing this link. Wait a minute, then tap Check again.",
                    canResume = true
                )
            }
        } catch (e: VirusTotalException) {
            post(token) { state = failureFor(e) }
        } catch (e: InterruptedException) {
            // Cancelled by the user.
        } catch (e: Exception) {
            post(token) {
                state = ScanState.Failed("The scan could not be completed.", canResume = pendingAnalysisId != null)
            }
        }
    }

    private fun failureFor(e: VirusTotalException): ScanState.Failed {
        val canResume = pendingAnalysisId != null
        return when (e.kind) {
            VirusTotalException.Kind.BAD_KEY ->
                ScanState.Failed("${e.message} Check the key you saved, or remove it and add it again.", false)
            VirusTotalException.Kind.RATE_LIMIT ->
                ScanState.Failed(
                    "VirusTotal's limit was reached. Free keys allow 4 requests a minute and 500 a day. " +
                        "Wait a minute and try again.",
                    canResume
                )
            else -> ScanState.Failed(e.message ?: "The scan could not be completed.", canResume)
        }
    }

    /** Runs [action] on the main thread unless this scan was cancelled or replaced. */
    private fun post(token: Int, action: () -> Unit) {
        mainHandler.post { if (token == scanToken) action() }
    }

    private fun sleepUnlessCancelled(token: Int, millis: Long): Boolean {
        var waited = 0L
        while (waited < millis) {
            if (token != scanToken) return false
            Thread.sleep(250)
            waited += 250
        }
        return token == scanToken
    }

    override fun onCleared() {
        scanToken++
        scanThread?.interrupt()
        super.onCleared()
    }

    private companion object {
        const val PREFS_NAME = "pml_scanner_secure"
        const val KEY_NAME = "vt_api_key"
        val KEY_REGEX = Regex("^[A-Fa-f0-9]{64}$")

        const val MAX_POLLS = 5
        const val FIRST_WAIT_MS = 6_000L
        const val POLL_WAIT_MS = 12_000L
    }
}
