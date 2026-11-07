# Test report

**Status: not tested on Android.** No APK exists yet and no device or emulator was available. This report separates what was really run from what still needs doing. Nothing in the second part has been run, and nothing there is marked as passed.

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

## Part 2: Still to run (from the brief, section 10)

Mark each result yourself on the device. Everything is **NOT RUN**.

### Compatibility

| Test | Result |
|---|---|
| Build with `./gradlew assembleDebug` succeeds | NOT RUN |
| APK installs on Android 10 (API 29) | NOT RUN |
| App launches | NOT RUN |
| Runs on arm64-v8a (Realme 5) | NOT RUN |
| Permissions: only Internet and Microphone listed in Settings | NOT RUN |
| Microphone permission requested only when Record is tapped | NOT RUN |
| Screen layout on the Realme 5 display, portrait and rotated | NOT RUN |
| Large font size setting (Settings > Display > Font size) | NOT RUN |

### Messaging

| Test | Result |
|---|---|
| Sign in with your own Stream key, user ID (and token) | NOT RUN |
| Channel list opens, search and profile open, back works | NOT RUN |
| Open a conversation, type and send a text message | NOT RUN |
| Keyboard does not cover the input | NOT RUN |
| Share to Messenger when installed | NOT RUN |
| Share with Messenger not installed (share sheet appears, no crash) | NOT RUN |

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
