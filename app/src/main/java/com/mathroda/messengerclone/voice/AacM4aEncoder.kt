package com.mathroda.messengerclone.voice

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.nio.ByteOrder

/**
 * Encodes 16-bit mono PCM to AAC-LC inside an MP4/M4A container using the phone's own hardware or
 * software codec (MediaCodec + MediaMuxer). No native libraries are needed, so it works on both
 * arm64-v8a and armeabi-v7a.
 */
object AacM4aEncoder {

    private const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC
    private const val TIMEOUT_US = 10_000L
    private const val MAX_WAIT_MS = 60_000L

    @Throws(IOException::class)
    fun encode(pcm: ShortArray, sampleRate: Int, outFile: File, bitRate: Int = 48_000) {
        require(pcm.isNotEmpty()) { "Nothing to encode" }

        val format = MediaFormat.createAudioFormat(MIME, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val c = MediaCodec.createEncoderByType(MIME)
            codec = c
            c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            c.start()

            val m = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = m

            val info = MediaCodec.BufferInfo()
            var trackIndex = -1
            var inputPos = 0
            var inputDone = false
            var outputDone = false
            val deadline = System.currentTimeMillis() + MAX_WAIT_MS

            while (!outputDone) {
                if (System.currentTimeMillis() > deadline) {
                    throw IOException("The audio encoder took too long")
                }

                if (!inputDone) {
                    val inIndex = c.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buf = c.getInputBuffer(inIndex)
                            ?: throw IOException("Encoder gave no input buffer")
                        buf.clear()
                        buf.order(ByteOrder.LITTLE_ENDIAN)
                        val shorts = buf.asShortBuffer()
                        val count = minOf(shorts.remaining(), pcm.size - inputPos)
                        val ptsUs = inputPos * 1_000_000L / sampleRate
                        if (count <= 0) {
                            c.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            shorts.put(pcm, inputPos, count)
                            c.queueInputBuffer(inIndex, 0, count * 2, ptsUs, 0)
                            inputPos += count
                        }
                    }
                }

                val outIndex = c.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw IOException("Encoder format changed twice")
                        trackIndex = m.addTrack(c.outputFormat)
                        m.start()
                        muxerStarted = true
                    }
                    outIndex >= 0 -> {
                        val encoded = c.getOutputBuffer(outIndex)
                            ?: throw IOException("Encoder gave no output buffer")
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            info.size = 0
                        }
                        if (info.size > 0 && muxerStarted) {
                            encoded.position(info.offset)
                            encoded.limit(info.offset + info.size)
                            m.writeSampleData(trackIndex, encoded, info)
                        }
                        c.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    // INFO_TRY_AGAIN_LATER and other codes: loop again.
                }
            }

            if (!muxerStarted) throw IOException("The encoder produced no audio")
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Audio encoding failed: ${e.javaClass.simpleName}", e)
        } finally {
            try { codec?.stop() } catch (e: Exception) { /* already stopped */ }
            try { codec?.release() } catch (e: Exception) { /* ignore */ }
            try { if (muxerStarted) muxer?.stop() } catch (e: Exception) { /* ignore */ }
            try { muxer?.release() } catch (e: Exception) { /* ignore */ }
        }
    }
}
