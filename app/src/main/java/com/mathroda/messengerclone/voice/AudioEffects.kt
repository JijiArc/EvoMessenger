package com.mathroda.messengerclone.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Voice effects written as plain Kotlin so they run on any Android version and can be tested on
 * a normal JVM. Nothing here touches Android APIs.
 *
 * Pitch changes use resampling followed by WSOLA time-stretching, so the voice gets deeper or
 * higher without getting faster or slower. Every effect takes a strength between
 * [MIN_STRENGTH] and [MAX_STRENGTH]; lower values stay closer to the original voice, which keeps
 * speech understandable.
 *
 * Audio is mono float samples in the range -1..1.
 */
object AudioEffects {

    const val MIN_STRENGTH = 0.2f
    const val MAX_STRENGTH = 1.0f
    const val DEFAULT_STRENGTH = 0.8f

    private const val PEAK_LIMIT = 0.95f

    fun shortsToFloats(src: ShortArray, length: Int = src.size): FloatArray {
        val n = min(length, src.size)
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = src[i] / 32768f
        return out
    }

    fun floatsToShorts(src: FloatArray): ShortArray {
        val out = ShortArray(src.size)
        for (i in src.indices) {
            val v = src[i].coerceIn(-1f, 1f)
            out[i] = (v * 32767f).roundToInt().toShort()
        }
        return out
    }

    /** Returns a processed copy of [input]. The input array is never modified. */
    fun apply(input: FloatArray, sampleRate: Int, effect: VoiceEffect, strength: Float): FloatArray {
        if (input.isEmpty()) return FloatArray(0)
        val s = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)

        val processed: FloatArray = when (effect) {
            VoiceEffect.NORMAL -> return input.copyOf()

            VoiceEffect.MONSTER ->
                pitchShift(input, sampleRate, lerp(1f, 0.70f, s))

            VoiceEffect.GHOST -> {
                val shifted = pitchShift(input, sampleRate, lerp(1f, 0.88f, s))
                tremolo(shifted, sampleRate, rateHz = 5f, depth = 0.30f * s)
                echo(
                    shifted,
                    delaySamples = (sampleRate * 0.17f).toInt(),
                    feedback = 0.45f * s,
                    tailSamples = (sampleRate * 0.9f).toInt()
                )
            }

            VoiceEffect.ROBOT -> {
                val modulated = ringModulate(input, sampleRate, freqHz = 55f, depth = 0.8f * s)
                comb(modulated, delaySamples = max(1, sampleRate / 110), gain = 0.45f * s)
            }

            VoiceEffect.ALIEN -> {
                val shifted = pitchShift(input, sampleRate, lerp(1f, 1.35f, s))
                vibrato(shifted, sampleRate, rateHz = 6f, depthSamples = sampleRate * 0.0015f * s)
            }

            VoiceEffect.DIGITAL_MONSTER -> {
                val shifted = pitchShift(input, sampleRate, lerp(1f, 0.66f, s))
                bitCrush(
                    shifted,
                    hold = lerp(1f, 3f, s).roundToInt().coerceAtLeast(1),
                    bits = lerp(16f, 6f, s).roundToInt().coerceIn(4, 16)
                )
            }
        }
        return limitPeak(processed)
    }

    // ---------------------------------------------------------------- pitch

    /** Changes pitch by [factor] (0.7 = deeper, 1.35 = higher) while keeping the duration. */
    fun pitchShift(x: FloatArray, sampleRate: Int, factor: Float): FloatArray {
        if (x.isEmpty() || abs(factor - 1f) < 0.01f) return x.copyOf()
        val resampled = resample(x, factor.toDouble())
        return wsolaStretch(resampled, x.size, sampleRate)
    }

    private fun resample(x: FloatArray, factor: Double): FloatArray {
        val outLen = max(1, (x.size / factor).toInt())
        val y = FloatArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * factor
            val i0 = pos.toInt()
            val frac = (pos - i0).toFloat()
            val a = if (i0 < x.size) x[i0] else 0f
            val b = if (i0 + 1 < x.size) x[i0 + 1] else a
            y[i] = a + (b - a) * frac
        }
        return y
    }

    /**
     * Waveform-similarity overlap-add. Stretches or squeezes [x] to exactly [targetLen] samples
     * without changing its pitch.
     */
    private fun wsolaStretch(x: FloatArray, targetLen: Int, sampleRate: Int): FloatArray {
        if (targetLen <= 0) return FloatArray(0)

        var frame = (sampleRate * 0.032).toInt()
        frame -= frame % 2
        if (frame < 64) frame = 64

        // Too short to analyse: copy or pad.
        if (x.size < frame * 2) {
            val out = FloatArray(targetLen)
            for (i in out.indices) out[i] = if (i < x.size) x[i] else 0f
            return out
        }

        val hop = frame / 2
        val tolerance = max(8, (sampleRate * 0.008).toInt())
        val ratio = targetLen.toDouble() / x.size
        val analysisHop = hop / ratio

        val padded = FloatArray(x.size + frame * 3 + tolerance * 2)
        System.arraycopy(x, 0, padded, 0, x.size)

        // Periodic Hann window: overlapping at 50% sums to exactly 1.
        val window = FloatArray(frame) { i -> (0.5 - 0.5 * cos(2.0 * PI * i / frame)).toFloat() }

        val out = FloatArray(targetLen + frame * 2)
        val weight = FloatArray(out.size)

        var previous = 0
        var k = 0
        while (k * hop < targetLen) {
            val nominal = (k * analysisHop).toInt()
            val chosen =
                if (k == 0) 0
                else bestOffset(padded, previous + hop, nominal, tolerance, hop, x.size)
            val outStart = k * hop
            for (i in 0 until frame) {
                val w = window[i]
                out[outStart + i] += padded[chosen + i] * w
                weight[outStart + i] += w
            }
            previous = chosen
            k++
        }

        val result = FloatArray(targetLen)
        for (i in 0 until targetLen) {
            val w = weight[i]
            result[i] = if (w > 1e-3f) out[i] / w else out[i]
        }
        return result
    }

    /** Finds the analysis position near [nominal] whose start best matches the signal at [refStart]. */
    private fun bestOffset(
        sig: FloatArray,
        refStart: Int,
        nominal: Int,
        tolerance: Int,
        overlap: Int,
        validLength: Int
    ): Int {
        val lo = max(0, nominal - tolerance)
        val hi = min(validLength - 1, nominal + tolerance)
        if (lo > hi) return hi.coerceAtLeast(0)

        var best = lo
        var bestScore = -Float.MAX_VALUE
        var c = lo
        while (c <= hi) {
            var dot = 0f
            var energy = 0f
            var j = 0
            while (j < overlap) {
                val a = sig[refStart + j]
                val b = sig[c + j]
                dot += a * b
                energy += b * b
                j += 2
            }
            val score = dot / sqrt(energy + 1e-6f)
            if (score > bestScore) {
                bestScore = score
                best = c
            }
            c += 2
        }
        return best
    }

    // -------------------------------------------------------------- effects

    private fun tremolo(y: FloatArray, sampleRate: Int, rateHz: Float, depth: Float) {
        val step = 2.0 * PI * rateHz / sampleRate
        for (i in y.indices) {
            val lfo = 0.5f * (1f + sin(step * i).toFloat())
            y[i] *= 1f - depth * lfo
        }
    }

    /** Recursive echo. The output is longer than the input so the echo tail is not cut off. */
    private fun echo(y: FloatArray, delaySamples: Int, feedback: Float, tailSamples: Int): FloatArray {
        val delay = max(1, delaySamples)
        val out = FloatArray(y.size + max(0, tailSamples))
        for (i in out.indices) {
            val dry = if (i < y.size) y[i] else 0f
            val wet = if (i >= delay) feedback * out[i - delay] else 0f
            out[i] = dry + wet
        }
        return out
    }

    private fun ringModulate(y: FloatArray, sampleRate: Int, freqHz: Float, depth: Float): FloatArray {
        val out = FloatArray(y.size)
        val step = 2.0 * PI * freqHz / sampleRate
        for (i in y.indices) {
            val carrier = sin(step * i).toFloat()
            out[i] = y[i] * (1f - depth + depth * carrier)
        }
        return out
    }

    private fun comb(y: FloatArray, delaySamples: Int, gain: Float): FloatArray {
        val out = FloatArray(y.size)
        for (i in y.indices) {
            val fb = if (i >= delaySamples) gain * out[i - delaySamples] else 0f
            out[i] = y[i] + fb
        }
        return out
    }

    private fun vibrato(y: FloatArray, sampleRate: Int, rateHz: Float, depthSamples: Float): FloatArray {
        val out = FloatArray(y.size)
        val step = 2.0 * PI * rateHz / sampleRate
        for (i in y.indices) {
            val pos = i + depthSamples * sin(step * i).toFloat()
            val i0 = pos.toInt().coerceIn(0, y.size - 1)
            val i1 = min(i0 + 1, y.size - 1)
            val frac = (pos - i0).coerceIn(0f, 1f)
            out[i] = y[i0] + (y[i1] - y[i0]) * frac
        }
        return out
    }

    private fun bitCrush(y: FloatArray, hold: Int, bits: Int): FloatArray {
        val out = FloatArray(y.size)
        val levels = 2.0.pow(bits - 1).toFloat()
        var held = 0f
        for (i in y.indices) {
            if (i % hold == 0) {
                val clamped = y[i].coerceIn(-1f, 1f)
                held = (clamped * levels).roundToInt() / levels
            }
            out[i] = held
        }
        return out
    }

    // -------------------------------------------------------------- helpers

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Scales the signal down if it would clip, and replaces any invalid samples with silence. */
    fun limitPeak(y: FloatArray): FloatArray {
        var peak = 0f
        for (i in y.indices) {
            val v = y[i]
            if (v.isNaN() || v.isInfinite()) {
                y[i] = 0f
            } else {
                peak = max(peak, abs(v))
            }
        }
        if (peak > PEAK_LIMIT) {
            val scale = PEAK_LIMIT / peak
            for (i in y.indices) y[i] *= scale
        }
        return y
    }
}
