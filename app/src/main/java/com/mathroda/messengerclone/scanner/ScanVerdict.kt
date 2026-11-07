package com.mathroda.messengerclone.scanner

/** Vote counts reported by VirusTotal for one link. */
data class ScanStats(
    val malicious: Int,
    val suspicious: Int,
    val harmless: Int,
    val undetected: Int
) {
    /** Number of engines that gave an answer. */
    val total: Int get() = malicious + suspicious + harmless + undetected
}

enum class Verdict { DANGEROUS, SUSPICIOUS, NO_DETECTIONS, UNKNOWN }

/**
 * Turns raw counts into a plain-language answer. "No detections" is deliberately not called
 * "safe": a brand-new bad link can score zero, so the wording always says so.
 */
object ScanVerdicts {

    /** Three or more malicious votes is treated as dangerous; one or two may be a false alarm. */
    const val DANGEROUS_THRESHOLD = 3

    fun from(stats: ScanStats): Verdict = when {
        stats.total == 0 -> Verdict.UNKNOWN
        stats.malicious >= DANGEROUS_THRESHOLD -> Verdict.DANGEROUS
        stats.malicious > 0 || stats.suspicious > 0 -> Verdict.SUSPICIOUS
        else -> Verdict.NO_DETECTIONS
    }

    fun title(verdict: Verdict): String = when (verdict) {
        Verdict.DANGEROUS -> "Dangerous: do not open this link"
        Verdict.SUSPICIOUS -> "Suspicious: be careful"
        Verdict.NO_DETECTIONS -> "No threats detected"
        Verdict.UNKNOWN -> "No result available"
    }

    fun explanation(verdict: Verdict, stats: ScanStats): String = when (verdict) {
        Verdict.DANGEROUS ->
            "${stats.malicious} of ${stats.total} security engines flagged this link as malicious. " +
                "Do not open it or enter any information on it."
        Verdict.SUSPICIOUS ->
            "${stats.malicious} engine(s) flagged it as malicious and ${stats.suspicious} as " +
                "suspicious, out of ${stats.total}. It may be a false alarm, but it is safest not " +
                "to open it unless you trust where it came from."
        Verdict.NO_DETECTIONS ->
            "None of the ${stats.total} security engines flagged this link. That lowers the risk " +
                "but does not guarantee it is safe, especially for a brand-new link."
        Verdict.UNKNOWN ->
            "VirusTotal returned no engine results for this link. Treat it as unchecked, not as safe."
    }
}
