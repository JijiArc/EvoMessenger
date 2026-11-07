# Known limitations

Read this before building. It lists what is unfinished, unverified or deliberately different from the original brief.

## Not verified on a phone or emulator

- **The APK has not been built.** The workspace this was written in has no Android SDK and no network access, so Gradle could not run. The project is source only. Build it with Android Studio (see README).
- **Android and Stream code has never been compiled.** The pure-Kotlin parts (voice effects, URL checks, verdict wording) were compiled and run on a JVM. A syntax pass over the whole tree found no syntax errors, but type-checking against the Android SDK, Compose and the Stream SDK was not possible. Expect to fix a few compile errors on the first build, most likely around Stream SDK calls (`devToken`, composer parameters).
- **No test has been run on Android 10, a Realme 5, or any device.** See `TEST_REPORT.md`.
- **Effects were never listened to.** They were checked on synthetic tones for pitch, duration, clipping and rhythm. How they sound on a real voice, and how understandable they are, is untested. Use the strength slider if an effect is too strong.

## Differences from the brief

- **targetSdk is 32, not the newest.** Android Gradle Plugin 7.2.1 (kept from the original project) only supports up to API 32. Raising it means upgrading AGP, Gradle, Kotlin, Compose and the Stream SDK together, which was too large a change to make without being able to compile.
- **Java 8 bytecode, not Java 17.** Java 17 source compatibility is not supported by AGP 7.2.1.
- **Voice effects are Kotlin, not native (NDK) code.** The brief preferred native "if practical". Without a compiler for Android, adding C++ would have been guesswork. The Kotlin version is fast enough in the JVM check (about 0.2 s for a 2-minute clip on a server; not measured on a Realme 5). The app ships no native libraries of its own.
- **Pitch-up effects can alias slightly.** Raising pitch resamples without a low-pass filter, so Alien may sound a little harsh on bright voices.
- **Messaging is Stream Chat, not your Facebook inbox.** The app signs in to a Stream project you create. Stream is a hosted service that needs an account, so this does not fit a "no accounts" goal. Replacing it would be a separate project.
- **WebView and Safe Browsing do not apply.** The app has no WebView, so there is no WebView configuration to secure.
- **Push notifications are not implemented.** Messages arrive while the app is open. No background service is used, so Realme battery restrictions have nothing to kill, but you will not be notified of new messages when the app is closed.

## Voice changer

- **It is a file share, not a Messenger voice note.** Messenger has no public API for that. The app opens Messenger's share target if it is installed and accepts audio, otherwise Android's share sheet. You pick the chat and send it there. Some Messenger versions may not accept audio files from a share; the "Share with another app" button covers that.
- **Voice clips are not posted into the in-app Stream conversation.** They go out through sharing only.
- **Maximum length 2 minutes**, mono, 16 kHz (44.1 kHz if the phone refuses 16 kHz), AAC at 48 kbps.
- **Recording stops when the app leaves the screen**, when another app takes audio focus (for example a call), or when the screen turns off. This is deliberate: Android 10 returns silence to background recorders. The part already recorded is kept. No foreground service is used.
- **A clip is held in memory** until it is sent or cancelled. If Android kills the process (low memory), the unsent clip is lost and the app restarts at the start screen.
- **Temporary files** live in the app cache. They are deleted when you cancel, replace a clip or close the sheet without sharing. After sharing, the file stays for up to an hour so the receiving app can read it, and is removed by the next cleanup.

## Link scanner

- **You need your own VirusTotal key.** Free keys allow 4 requests a minute and 500 a day. A scan uses 1 submit plus up to 5 checks, so rapid repeated scans will hit the limit. The app says so and offers "Check again" and a website fallback.
- **"No threats detected" is not "safe".** Brand-new bad links can score zero. The wording says this.
- **Three or more malicious votes count as "Dangerous"**; one or two (or any suspicious votes) count as "Suspicious". This threshold is a judgement call in `ScanVerdict.kt`.
- **The fallback copies the link to the clipboard** and opens VirusTotal's own page. You paste it there.
- **Private and local addresses are refused** (for example 192.168.x.x, localhost), because a public service cannot see them.
- IPv6 addresses typed directly are not supported.

## Interface

- Tap targets: the voice sheet and scanner use at least 48 dp. The channel-list header buttons and composer icons are 40 dp, and the rest of the Stream chat screens keep their original sizes.
- Some decorative items from the original sample may still do nothing.
- The Android package name is still `com.mathroda.messengerclone`.
- Status colours adapt to dark mode, but the original Stream screens were not re-checked for contrast.

## Security notes

- A Stream **API key is not secret** (it identifies your app). The **user token is secret** and is stored encrypted. Development tokens (empty token field) are only safe for testing; for real use generate tokens on a server you control.
- If encrypted storage fails on a phone, secrets are kept in memory for the session and you sign in again next time.
- The original sample's hardcoded demo users and tokens were removed from the source, but they remain in this repository's git history.
