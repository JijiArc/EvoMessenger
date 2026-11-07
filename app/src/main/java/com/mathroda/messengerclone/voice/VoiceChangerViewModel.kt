package com.mathroda.messengerclone.voice

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

/**
 * Owns the whole voice-message flow: record, apply an effect, preview, hand over for sharing.
 *
 * It lives in a ViewModel so a screen rotation does not interrupt recording. All observable state
 * is changed on the main thread only; the capture loop and the audio rendering run on their own
 * threads and report back through a main-thread Handler.
 *
 * Android 10 does not allow apps in the background to record, so recording is stopped (and the
 * clip so far is kept) whenever the app leaves the foreground or another app takes audio focus,
 * for example during a phone call.
 */
class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    enum class Phase { IDLE, RECORDING, PROCESSING, READY, ERROR }

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var effect by mutableStateOf(VoiceEffect.NORMAL)
        private set
    var strength by mutableStateOf(AudioEffects.DEFAULT_STRENGTH)
        private set
    var seconds by mutableStateOf(0)
        private set
    var level by mutableStateOf(0f)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    /** True when the microphone permission was refused with "don't ask again". */
    var needsSettings by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var output by mutableStateOf<File?>(null)
        private set

    private val appContext: Context = application.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** One recording. Each has its own buffer, so a cancelled capture can never write into a new one. */
    private class Capture(val rate: Int) {
        @Volatile var data = ShortArray(rate * 10)
        @Volatile var length = 0
        @Volatile var stopRequested = false
        @Volatile var discarded = false
    }

    private var capture: Capture? = null
    private var interrupted = false
    private var recorder: AudioRecord? = null
    private var focusRequest: AudioFocusRequest? = null
    private var player: MediaPlayer? = null
    @Volatile private var renderToken = 0

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            if (phase == Phase.RECORDING) {
                interrupted = true
                stopRecording()
            }
        }
    }

    init {
        worker.execute { VoiceFiles.cleanupOld(appContext) }
    }

    // ------------------------------------------------------------ choices

    fun selectEffect(newEffect: VoiceEffect) {
        if (phase == Phase.RECORDING || phase == Phase.PROCESSING) return
        effect = newEffect
        if (phase == Phase.READY) render()
    }

    fun setStrength(value: Float) {
        strength = value.coerceIn(AudioEffects.MIN_STRENGTH, AudioEffects.MAX_STRENGTH)
    }

    /** Called when the slider is released, so the clip is re-rendered once rather than continuously. */
    fun strengthChangeFinished() {
        if (phase == Phase.READY && effect != VoiceEffect.NORMAL) render()
    }

    // ---------------------------------------------------------- recording

    /** The caller must already hold the RECORD_AUDIO permission. */
    fun startRecording() {
        if (phase == Phase.RECORDING || phase == Phase.PROCESSING || recorder != null) return

        stopPlayback()
        discardOutput()
        message = null
        needsSettings = false
        seconds = 0
        level = 0f
        interrupted = false

        val rate = pickSampleRate()
        if (rate == null) {
            fail("This phone could not open the microphone.")
            return
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = max(minBuffer * 2, rate / 5 * 2)

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
        } catch (e: Exception) {
            fail("This phone could not open the microphone.")
            return
        }

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            fail("The microphone is not available. Check the microphone permission and that no call is active.")
            return
        }

        requestAudioFocus()

        try {
            rec.startRecording()
        } catch (e: IllegalStateException) {
            rec.release()
            abandonAudioFocus()
            fail("The microphone is being used by another app.")
            return
        }
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            rec.release()
            abandonAudioFocus()
            fail("The microphone is being used by another app or a call.")
            return
        }

        val cap = Capture(rate)
        capture = cap
        recorder = rec
        phase = Phase.RECORDING

        Thread({ captureLoop(rec, cap) }, "voice-capture").start()
    }

    private fun pickSampleRate(): Int? {
        for (candidate in intArrayOf(16_000, 44_100)) {
            val size = AudioRecord.getMinBufferSize(
                candidate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (size > 0) return candidate
        }
        return null
    }

    private fun captureLoop(rec: AudioRecord, cap: Capture) {
        val rate = cap.rate
        val chunk = ShortArray(rate / 20) // 50 ms per read
        val maxSamples = rate * MAX_SECONDS
        var errorText: String? = null

        try {
            while (!cap.stopRequested && cap.length < maxSamples) {
                val n = rec.read(chunk, 0, chunk.size)
                if (n > 0) {
                    val need = cap.length + n
                    if (need > cap.data.size) {
                        cap.data = cap.data.copyOf(max(need, cap.data.size * 2))
                    }
                    System.arraycopy(chunk, 0, cap.data, cap.length, n)
                    cap.length = need

                    var peak = 0
                    for (i in 0 until n) peak = max(peak, abs(chunk[i].toInt()))
                    val newLevel = peak / 32768f
                    val newSeconds = need / rate
                    mainHandler.post {
                        if (cap === capture && !cap.discarded) {
                            level = newLevel
                            if (newSeconds != seconds) seconds = newSeconds
                        }
                    }
                } else if (n < 0) {
                    errorText = "The microphone stopped responding."
                    break
                }
            }
        } catch (e: Exception) {
            errorText = "Recording failed."
        } finally {
            try { rec.stop() } catch (e: IllegalStateException) { /* not started */ }
            rec.release()
        }

        val hitLimit = cap.length >= maxSamples
        mainHandler.post { onCaptureFinished(cap, errorText, hitLimit) }
    }

    /** Asks the capture loop to finish. The clip recorded so far is kept and processed. */
    fun stopRecording() {
        if (phase != Phase.RECORDING) return
        capture?.stopRequested = true
    }

    /** Throws away the current recording without producing a clip. */
    fun cancelRecording() {
        if (phase != Phase.RECORDING) return
        capture?.let {
            it.discarded = true
            it.stopRequested = true
        }
        capture = null
        recorder = null
        abandonAudioFocus()
        phase = Phase.IDLE
        seconds = 0
        level = 0f
    }

    private fun onCaptureFinished(cap: Capture, errorText: String?, hitLimit: Boolean) {
        if (cap !== capture || cap.discarded) return

        recorder = null
        abandonAudioFocus()
        level = 0f

        val length = cap.length
        val rate = cap.rate
        val data = cap.data

        if (length < rate * MIN_SECONDS) {
            fail(errorText ?: "That was too short. Hold on a little longer.")
            return
        }

        var peak = 0
        for (i in 0 until length) peak = max(peak, abs(data[i].toInt()))
        if (peak < SILENCE_PEAK) {
            fail(
                "No sound was picked up. Make sure no other app or call is using the " +
                    "microphone, then try again."
            )
            return
        }

        message = when {
            interrupted ->
                "Recording was interrupted (another app or a call used the audio), so the clip was kept up to that point."
            hitLimit -> "You reached the 2-minute limit."
            errorText != null -> "$errorText The clip was kept up to that point."
            else -> null
        }
        seconds = length / rate
        render()
    }

    /** Called when the app goes to the background; Android 10 would silence the microphone anyway. */
    fun onAppBackgrounded() {
        if (phase == Phase.RECORDING) {
            interrupted = true
            stopRecording()
        }
        stopPlayback()
    }

    fun onPermissionDenied(permanent: Boolean) {
        phase = Phase.IDLE
        needsSettings = permanent
        message = if (permanent) {
            "Microphone access is turned off for this app. Turn it on in the app settings to record."
        } else {
            "Microphone permission is needed to record a voice message."
        }
    }

    fun showMessage(text: String) {
        message = text
    }

    fun dismissError() {
        if (phase == Phase.ERROR) phase = Phase.IDLE
        message = null
        needsSettings = false
    }

    // ---------------------------------------------------------- rendering

    private fun render() {
        val cap = capture ?: return
        val token = ++renderToken
        val data = cap.data
        val length = cap.length
        val rate = cap.rate
        val chosen = effect
        val chosenStrength = strength

        stopPlayback()
        phase = Phase.PROCESSING

        worker.execute {
            if (token != renderToken) return@execute // a newer render was requested

            var file: File? = null
            try {
                val samples = AudioEffects.shortsToFloats(data, length)
                val processed = AudioEffects.apply(samples, rate, chosen, chosenStrength)
                val shorts = AudioEffects.floatsToShorts(processed)

                val target = VoiceFiles.newOutputFile(appContext)
                file = target
                AacM4aEncoder.encode(shorts, rate, target)

                mainHandler.post {
                    if (token == renderToken) {
                        discardOutput()
                        output = target
                        phase = Phase.READY
                    } else {
                        VoiceFiles.delete(target)
                    }
                }
            } catch (e: Throwable) { // includes OutOfMemoryError on very low-memory phones
                VoiceFiles.delete(file)
                mainHandler.post {
                    if (token == renderToken) {
                        fail("Could not prepare the audio. Try a shorter clip or close other apps.")
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------- playback

    fun togglePlayback() {
        if (isPlaying) {
            stopPlayback()
            return
        }
        val file = output ?: return
        stopPlayback()

        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                it.start()
                isPlaying = true
            }
            mp.setOnCompletionListener { stopPlayback() }
            mp.setOnErrorListener { _, _, _ ->
                stopPlayback()
                message = "The preview could not be played."
                true
            }
            player = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            try { mp.release() } catch (ignored: Exception) { }
            player = null
            message = "The preview could not be played."
        }
    }

    private fun stopPlayback() {
        player?.let {
            try { it.stop() } catch (e: IllegalStateException) { /* not prepared yet */ }
            try { it.release() } catch (e: Exception) { /* ignore */ }
        }
        player = null
        isPlaying = false
    }

    // ------------------------------------------------------------ cleanup

    private fun discardOutput() {
        VoiceFiles.delete(output)
        output = null
    }

    /** Closes the flow and deletes the temporary clip. */
    fun resetAll() {
        resetState(deleteOutput = true)
    }

    /** Closes the flow after a share. The clip stays on disk briefly so the receiving app can read it. */
    fun finishAfterShare() {
        resetState(deleteOutput = false)
    }

    private fun resetState(deleteOutput: Boolean) {
        capture?.let {
            it.discarded = true
            it.stopRequested = true
        }
        capture = null
        recorder = null
        renderToken++
        abandonAudioFocus()
        stopPlayback()
        if (deleteOutput) discardOutput() else output = null
        phase = Phase.IDLE
        seconds = 0
        level = 0f
        message = null
        needsSettings = false
        interrupted = false
    }

    private fun fail(text: String) {
        recorder = null
        capture?.stopRequested = true
        abandonAudioFocus()
        phase = Phase.ERROR
        message = text
        level = 0f
    }

    // -------------------------------------------------------- audio focus

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener, mainHandler)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    override fun onCleared() {
        capture?.let {
            it.discarded = true
            it.stopRequested = true
        }
        renderToken++
        stopPlayback()
        abandonAudioFocus()
        worker.shutdownNow()
        super.onCleared()
    }

    private companion object {
        const val MAX_SECONDS = 120
        const val MIN_SECONDS = 0.4
        const val SILENCE_PEAK = 60
    }
}
