import com.mathroda.messengerclone.scanner.*

var failures = 0
fun valid(input: String, expected: String? = null) {
    val r = UrlValidator.check(input)
    val ok = r is UrlValidator.Result.Valid && (expected == null || r.url == expected)
    println((if (ok) "PASS " else "FAIL ") + "accept  [$input] -> $r")
    if (!ok) failures++
}
fun invalid(input: String) {
    val r = UrlValidator.check(input)
    val ok = r is UrlValidator.Result.Invalid
    println((if (ok) "PASS " else "FAIL ") + "reject  [$input] -> $r")
    if (!ok) failures++
}

fun main() {
    valid("https://example.com", "https://example.com")
    valid("  HTTPS://Example.COM/Path?Q=1#frag  ", "https://example.com/Path?Q=1#frag")
    valid("example.com", "https://example.com")
    valid("example.com/login", "https://example.com/login")
    valid("example.com:8080/x", "https://example.com:8080/x")
    valid("http://sub.domain.co.uk:443/a/b?c=d")
    valid("https://paypal.com@evil-site.xyz/login", "https://paypal.com@evil-site.xyz/login")
    valid("https://8.8.8.8/test")
    valid("https://bücher.de/", "https://xn--bcher-kva.de/")
    valid("https://example.com.", "https://example.com")

    invalid("")
    invalid("   ")
    invalid("not a url")
    invalid("https://")
    invalid("https://localhost/admin")
    invalid("localhost:3000")
    invalid("http://192.168.1.1/")
    invalid("http://10.0.0.5/")
    invalid("http://127.0.0.1:8080/")
    invalid("http://172.20.1.1/")
    invalid("http://999.1.1.1/")
    invalid("javascript:alert(1)")
    invalid("file:///etc/passwd")
    invalid("ftp://example.com/file")
    invalid("data:text/html,<script>x</script>")
    invalid("mailto:a@b.com")
    invalid("https://exa mple.com")
    invalid("https://example")
    invalid("https://-bad.com")
    invalid("https://example.c")
    invalid("https://example.com:99999")
    invalid("https://example.com:abc")
    invalid("https://[::1]/")
    invalid("https://" + "a".repeat(2100) + ".com")
    invalid("https://exa\u0000mple.com")

    // Verdicts
    fun v(m: Int, s: Int, h: Int, u: Int, want: Verdict) {
        val stats = ScanStats(m, s, h, u)
        val got = ScanVerdicts.from(stats)
        val ok = got == want
        println((if (ok) "PASS " else "FAIL ") + "verdict $stats -> $got")
        if (!ok) failures++
        // The wording must never call a no-detection result simply "safe": it has to carry the caveat.
        val text = ScanVerdicts.title(got) + " " + ScanVerdicts.explanation(got, stats)
        val wordingOk = text.isNotBlank() && (got != Verdict.NO_DETECTIONS || text.contains("does not guarantee"))
        if (!wordingOk) { println("FAIL wording for $got: $text"); failures++ }
    }
    v(0, 0, 0, 0, Verdict.UNKNOWN)
    v(0, 0, 70, 20, Verdict.NO_DETECTIONS)
    v(1, 0, 70, 20, Verdict.SUSPICIOUS)
    v(2, 0, 70, 20, Verdict.SUSPICIOUS)
    v(0, 1, 70, 20, Verdict.SUSPICIOUS)
    v(3, 0, 70, 20, Verdict.DANGEROUS)
    v(12, 4, 50, 20, Verdict.DANGEROUS)
    println(ScanVerdicts.explanation(Verdict.NO_DETECTIONS, ScanStats(0,0,70,20)))

    // Hash helper: known SHA-256 of "abc"
    val h = UrlValidator.sha256Hex("abc")
    val okHash = h == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    println((if (okHash) "PASS " else "FAIL ") + "sha256 abc -> $h")
    if (!okHash) failures++

    println(if (failures == 0) "ALL CHECKS PASSED" else "$failures CHECK(S) FAILED")
    if (failures != 0) System.exit(1)
}
