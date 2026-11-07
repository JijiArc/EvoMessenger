# Test report

**Status: not tested on Android.** No device or emulator was available, and no Facebook feature has been tried. This report separates what was really run from what still needs doing. Nothing in the second part has been run, and nothing there is marked as passed.

## Part 1: What was actually run

These ran on a Linux server (not Android), using the Kotlin 2.0.21 compiler bundled with Gradle. The sources are the same files the app uses; the checks are in `tools/jvm-checks/` and can be re-run with `tools/jvm-checks/run-checks.sh` (needs `kotlinc`).

| Check | Result |
|---|---|
| `AudioEffects.kt`, `VoiceEffect.kt`, `UrlValidator.kt`, `ScanVerdict.kt` compile | Yes, no errors |
| Voice effects, 72 checks at 16 kHz and 44.1 kHz | 72 passed, 0 failed |
| URL validation and verdict wording, 43 checks | 43 passed, 0 failed |
| Syntax pass over every Kotlin file in the project | No syntax errors. Type-checking was not possible without the Android SDK, so over 4,000 "missing library" errors were expected and ignored. |

What the 72 voice-effect checks cover, using a synthetic 180 Hz tone (not a human voice):

- Output is finite and never exceeds 95% of full scale (no clipping), for all six voices.
- Length is kept within 2% of the input; Ghost is longer by exactly the 0.9 s echo tail.
- Monster and Digital Monster pitch land within 7% of the intended value; the pitch shifter used by Alien lands within 5%.
- The loudness rhythm of the input is preserved (correlation of at least 0.7), which is a rough stand-in for "speech stays intelligible".
- A stronger Monster setting is deeper than a weaker one.
- Empty, 100-sample and silent inputs do not crash and silence stays silent; the input array is never modified.
- Time to process a 120-second clip on the server: 71 to 219 ms per effect. **This says nothing about a Realme 5**, which is much slower.

What the 43 scanner checks cover: addresses that must be accepted (with and without `https://`, ports, accented domains), addresses that must be rejected (empty, spaces, `javascript:`, `file:`, `ftp:`, private IPs, `localhost`, bad ports, bad hostnames, over-long, hidden characters), the verdict rules, the "does not guarantee" wording, and a SHA-256 known-answer test.

Not covered by these checks: how any effect sounds, MediaCodec/MediaMuxer, AudioRecord, MediaPlayer, sharing, permissions, VirusTotal network calls, and every screen.

### Run in GitHub Actions (real Android compile)

On 2026-11-07 the `webview-messenger` branch was built by `.github/workflows/build-apk.yml` (JDK 11 for Gradle, Android SDK 32). The third attempt passed: `testDebugUnitTest` (including `WebConfigTest`, which covers the navigation allow-list, look-alike hosts, link-shim unwrapping, blocked schemes and the desktop user agent) and `assembleDebug`.

The first two attempts failed and were fixed. These were real defects in code that had never been compiled before this: an opt-in annotation missing in `VoiceChangerSheet.kt`, and a JVM name clash between the `strength` property and `setStrength()` in `VoiceChangerViewModel.kt` (function renamed to `updateStrength`). A workflow problem (the SDK tools need JDK 17) was fixed first.

This proves the project compiles and the pure-logic unit tests pass. It proves nothing about how any screen looks or behaves, or anything about Facebook.

## Part 2: Still to run (from the brief, section 10)

Mark each result yourself on the device. Everything is **NOT RUN**.

### Compatibility

| Test | Result |
|---|---|
| `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` succeed | PASSED in GitHub Actions (run 42335731855, 2026-11-07): unit tests ran and passed, debug APK built (about 12 MB). Not run locally. |
| APK installs on Android 10 (API 29) | NOT RUN |
| App launches | NOT RUN |
| Runs on arm64-v8a (Realme 5) | NOT RUN |
| Permissions: Internet, Microphone and Camera listed; neither prompts at launch | NOT RUN |
| Microphone permission requested only when Record is tapped | NOT RUN |
| Screen layout on the Realme 5 display, portrait and rotated | NOT RUN |
| Large font size setting (Settings > Display > Font size) | NOT RUN |

### Facebook Messages in the WebView

Every item is **NOT RUN**. Nothing here has been seen working.

| Test | Result |
|---|---|
| App opens and shows Facebook's login page (not a blank or "unsupported browser" page) | NOT RUN |
| Facebook login with email and password on Facebook's own page | NOT RUN |
| Two-factor or checkpoint prompt completes inside the WebView | NOT RUN |
| After login, the real conversation list is visible | NOT RUN |
| Mobile site offers messaging, or desktop-site toggle makes it appear | NOT RUN |
| Open an existing conversation and see its history | NOT RUN |
| Send a text message; it shows in the official Messenger app | NOT RUN |
| Receive a message sent from another account while the app is open | NOT RUN |
| Group conversation opens | NOT RUN |
| Pick and send a photo (file chooser works) | NOT RUN |
| Play a received video, including full screen | NOT RUN |
| Record and send a voice message with Facebook's own button (microphone permission prompt) | NOT RUN |
| Reactions, read receipts, conversation search, contacts | NOT RUN |
| Close and reopen the app: still signed in | NOT RUN |
| Force-stop the app and reopen: still signed in | NOT RUN |
| Back button goes back inside the page, then leaves the app at the start page | NOT RUN |
| Airplane mode: error screen with Retry; Retry works after reconnecting | NOT RUN |
| Rotate the phone: page and open conversation stay | NOT RUN |
| Switch App shell mode and Full-screen mode: page does not reload | NOT RUN |
| Full-screen handle opens the menu | NOT RUN |
| Link in a message opens in a Chrome tab, not in the WebView | NOT RUN |
| "Open in Chrome tab" works and shows Facebook there | NOT RUN |
| "Sign out and clear data" returns to the login page | NOT RUN |
| Share to Messenger from the voice changer, with Messenger installed and not installed | NOT RUN |

### Voice changer

| Test | Result |
|---|---|
| Record with Normal and preview | NOT RUN |
| Each of Monster, Ghost, Robot, Alien, Digital Monster: sounds as intended and speech is understandable | NOT RUN |
| Strength slider: lower value sounds closer to normal | NOT RUN |
| Change voice after recording re-renders the same clip | NOT RUN |
| Cancel while recording | NOT RUN |
| Deny microphone permission: message shown, no crash | NOT RUN |
| Deny with "Don't ask again": "Open app settings" button works | NOT RUN |
| Incoming call or alarm during recording: stops, clip so far kept | NOT RUN |
| Press Home during recording: stops, clip kept, no crash | NOT RUN |
| Rotate during recording: recording continues | NOT RUN |
| Another app using the microphone: clear message | NOT RUN |
| Share the M4A: plays correctly in the receiving app | NOT RUN |
| Temporary files removed (check cache/voice) | NOT RUN |

### Link scanner

| Test | Result |
|---|---|
| Save a real VirusTotal key; stored (not shown again) | NOT RUN |
| Scan a harmless HTTPS URL | NOT RUN |
| Scan a known test URL for a malicious result | NOT RUN |
| Malformed link rejected with a clear message | NOT RUN |
| Airplane mode: clear network message, "Try again" works | NOT RUN |
| Rate limit (scan several times quickly): clear message, "Check again" works | NOT RUN |
| Wrong key: clear message | NOT RUN |
| "Use the website" fallback copies the link and opens VirusTotal | NOT RUN |
| The scanned link is never opened by the app | NOT RUN |
| API key not visible in `adb logcat` | NOT RUN |

### Performance and stability

| Test | Result |
|---|---|
| Cold start time | NOT RUN |
| Memory use during a 2-minute recording and while processing | NOT RUN |
| Processing time for a 2-minute clip on the Realme 5 | NOT RUN |
| Repeated navigation between screens | NOT RUN |
| Background and foreground transitions | NOT RUN |
| Low-memory recovery (Developer options: background process limit) | NOT RUN |

Suggested check for the API key: run `adb logcat | grep -i "x-apikey"` while scanning; nothing should appear.
