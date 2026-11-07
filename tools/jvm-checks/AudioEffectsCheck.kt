import com.mathroda.messengerclone.voice.AudioEffects
import com.mathroda.messengerclone.voice.VoiceEffect
import kotlin.math.*

fun tone(sr: Int, seconds: Float, f0: Float): FloatArray {
    val n = (sr * seconds).toInt()
    val x = FloatArray(n)
    for (i in 0 until n) {
        val t = i.toDouble() / sr
        var v = 0.0
        for (h in 1..6) v += sin(2 * PI * f0 * h * t) / h
        val env = 0.6 + 0.4 * sin(2 * PI * 3 * t)
        x[i] = (0.4 * v * env).toFloat()
    }
    return x
}

fun estimatePitch(x: FloatArray, sr: Int, from: Int, len: Int): Double {
    val minLag = sr / 500
    val maxLag = sr / 60
    val scores = DoubleArray(maxLag + 1)
    var best = -1e30
    for (lag in minLag..maxLag) {
        var s = 0.0
        for (i in from until from + len) s += x[i].toDouble() * x[i + lag]
        scores[lag] = s
        if (s > best) best = s
    }
    for (lag in minLag..maxLag) if (scores[lag] > 0.9 * best) return sr.toDouble() / lag
    return 0.0
}

fun envelope(x: FloatArray, sr: Int, n: Int): DoubleArray {
    val w = sr / 50
    val cnt = n / w
    return DoubleArray(cnt) { k ->
        var s = 0.0
        for (i in k * w until (k + 1) * w) s += x[i].toDouble() * x[i]
        sqrt(s / w)
    }
}

fun pearson(a: DoubleArray, b: DoubleArray): Double {
    val n = min(a.size, b.size)
    val ma = a.take(n).average(); val mb = b.take(n).average()
    var sab = 0.0; var saa = 0.0; var sbb = 0.0
    for (i in 0 until n) { sab += (a[i]-ma)*(b[i]-mb); saa += (a[i]-ma)*(a[i]-ma); sbb += (b[i]-mb)*(b[i]-mb) }
    return sab / sqrt(saa * sbb + 1e-12)
}

var failures = 0
fun check(name: String, ok: Boolean, detail: String) {
    println((if (ok) "PASS " else "FAIL ") + name + " — " + detail)
    if (!ok) failures++
}

fun main() {
    for (sr in intArrayOf(16000, 44100)) {
        println("=== sample rate $sr ===")
        val f0 = 180f
        val x = tone(sr, 3f, f0)
        val before = x.copyOf()
        val inPitch = estimatePitch(x, sr, sr, sr / 2)
        println("input pitch estimate: %.1f Hz".format(inPitch))

        for (effect in VoiceEffect.values()) {
            val y = AudioEffects.apply(x, sr, effect, AudioEffects.DEFAULT_STRENGTH)
            val finite = y.all { it.isFinite() }
            val peak = y.maxOf { abs(it) }
            check("${effect.name} finite/peak", finite && peak <= 0.951f, "peak=%.3f len=%d".format(peak, y.size))
            if (effect == VoiceEffect.GHOST) {
                val expected = x.size + (sr * 0.9f).toInt()
                check("GHOST length has echo tail", y.size == expected, "got ${y.size} expected $expected")
            } else {
                val diff = abs(y.size - x.size).toDouble() / x.size
                check("${effect.name} keeps duration", diff < 0.02, "diff=%.4f".format(diff))
            }
            if (effect != VoiceEffect.GHOST) {
                val c = pearson(envelope(x, sr, x.size), envelope(y, sr, min(x.size, y.size)))
                check("${effect.name} keeps speech rhythm", c > 0.7, "envelope correlation=%.2f".format(c))
            }
            val expectedFactor = when (effect) {
                VoiceEffect.MONSTER -> 1f + (0.70f - 1f) * AudioEffects.DEFAULT_STRENGTH
                VoiceEffect.ALIEN -> 1f + (1.35f - 1f) * AudioEffects.DEFAULT_STRENGTH
                VoiceEffect.DIGITAL_MONSTER -> 1f + (0.66f - 1f) * AudioEffects.DEFAULT_STRENGTH
                VoiceEffect.NORMAL -> 1f
                else -> 0f
            }
            if (expectedFactor > 0f && effect != VoiceEffect.ALIEN) {
                val p = estimatePitch(y, sr, sr, sr / 2)
                val want = inPitch * expectedFactor
                check("${effect.name} pitch", abs(p - want) / want < 0.07, "got %.1f Hz want about %.1f Hz".format(p, want))
            }
        }
        check("input not modified", x.contentEquals(before), "apply() works on a copy")

        // Alien has vibrato, so measure its pitch with the warble switched off by checking the shifter directly.
        val shifted = AudioEffects.pitchShift(x, sr, 1.28f)
        val p = estimatePitch(shifted, sr, sr, sr / 2)
        check("pitchShift up", abs(p - inPitch * 1.28) / (inPitch * 1.28) < 0.05, "got %.1f Hz want about %.1f Hz".format(p, inPitch * 1.28))

        // Strength should move the result: stronger monster is deeper than weaker monster.
        val weak = estimatePitch(AudioEffects.apply(x, sr, VoiceEffect.MONSTER, 0.3f), sr, sr, sr / 2)
        val strong = estimatePitch(AudioEffects.apply(x, sr, VoiceEffect.MONSTER, 1.0f), sr, sr, sr / 2)
        check("strength slider changes depth", strong < weak, "weak=%.1f Hz strong=%.1f Hz".format(weak, strong))

        // Edge cases
        check("empty input", AudioEffects.apply(FloatArray(0), sr, VoiceEffect.MONSTER, 0.8f).isEmpty(), "ok")
        for (effect in VoiceEffect.values()) {
            val tiny = AudioEffects.apply(FloatArray(100) { 0.1f }, sr, effect, 1f)
            check("${effect.name} tiny clip", tiny.all { it.isFinite() }, "len=${tiny.size}")
            val silent = AudioEffects.apply(FloatArray(sr), sr, effect, 1f)
            check("${effect.name} silence stays silent", silent.all { abs(it) < 1e-6f }, "len=${silent.size}")
        }
    }

    // Worst-case clip length: 120 seconds
    val sr = 16000
    val long = tone(sr, 120f, 150f)
    for (effect in listOf(VoiceEffect.MONSTER, VoiceEffect.ALIEN, VoiceEffect.DIGITAL_MONSTER, VoiceEffect.GHOST, VoiceEffect.ROBOT)) {
        val t0 = System.nanoTime()
        val y = AudioEffects.apply(long, sr, effect, 1f)
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("120 s clip, ${effect.name}: $ms ms on this server (output ${y.size} samples)")
    }

    println(if (failures == 0) "ALL CHECKS PASSED" else "$failures CHECK(S) FAILED")
    if (failures != 0) System.exit(1)
}
