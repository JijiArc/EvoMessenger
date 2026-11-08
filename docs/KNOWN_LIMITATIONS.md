# Known limitations

Read this before building. It lists what is unfinished, unverified or deliberately different from the original brief.

## Nothing about Facebook has been tested

- **No Facebook feature has been run on a phone or emulator.** That includes login, two-factor prompts, seeing existing conversations, opening one, sending, receiving, group chats, images, video, voice messages, reactions, read receipts, search, contacts, session persistence across app restarts, back navigation, and file uploads. The code for hosting the page exists; whether Facebook's site behaves inside this WebView is unknown until someone tries it. See `TEST_REPORT.md`, where each item is NOT RUN.
- **No Android SDK was available where the code was written.** The Android code (WebView host, screens, voice, scanner) can only be compiled by the GitHub Actions workflow or Android Studio. Only the plain-Kotlin parts were run on a JVM.

## What the integration is, and is not

- **It is Facebook's website in a WebView, not a Messenger client.** Meta provides no supported API for personal Messenger inboxes (the Messenger Platform is for Pages and Instagram Professional accounts). The app therefore shows `facebook.com/messages` and everything messaging-related comes from that page. The app cannot list conversations, read message text, send messages, show read receipts or update a badge by itself, and it does not try to.
- **First test result (reported by the project owner, no details given):** the page loaded and the app ran without bugs, but Messenger itself was not available in the WebView, and the full-screen mode showed a broken-link page. The likely cause is that the mobile site cannot message. This build therefore defaults to the desktop site (new settings key, so an old "mobile" choice is ignored), rewrites m./touch./mbasic. links to www.facebook.com, opens Facebook's "get Messenger" links in the matching app, and shows a "Switch site" banner when the page title says content isn't available. **None of that has been tried on a phone yet.** If it still fails, use Menu > Page info and report the address and title it shows.
- **Facebook may block or degrade this.** Facebook can refuse to sign in or show messaging inside an embedded WebView, show a CAPTCHA or checkpoint, or redirect mobile visitors to an "install Messenger" page. The app does not work around any of that: no CAPTCHA bypass, no spoofed identity beyond the user-chosen desktop-site toggle, no script injection. The fallback is a Chrome Custom Tab ("Open in Chrome tab"), which is Chrome with its own login. It cannot share the WebView's session, and the page there is not inside the app.
- **Desktop site is an untested guess.** The desktop-site mode changes the user agent to a desktop one and uses a wide viewport, like Chrome's "Request desktop site". The desktop layout is small on a phone; pinch zoom and the menu's Zoom in/out are available. Whether Facebook serves a usable messaging page in either mode is not verified. messenger.com no longer messages (Meta redirects it to facebook.com/messages), so there is no second web address to fall back to.
- **Unavailable-page detection is a guess.** The banner appears when the page title contains words like "isn't available" or "not found". Facebook's real error titles were not checked, so it may miss some error pages or appear on an odd conversation name. It only offers help and never blocks anything.
- **This may conflict with Meta's terms.** Meta's terms restrict automated access. Showing the real website to a signed-in user, without scraping or automation, is the approach here, but whether Meta accepts embedded-WebView use of facebook.com is Meta's decision, not something this project can guarantee.
- **No notifications.** There is no push, no background service and no unread badge. Messages update live only while the page is open (the WebView is paused when the app is in the background).
- **Not offline.** There is no local cache of conversations and no Room database. Messages exist only in Facebook's page, so nothing is readable without a connection beyond what the WebView cache happens to hold.
- **Downloads are not supported.** Saving a file from the page needs the login cookies, which this app does not read. Tapping a download shows a message; use "Open in Chrome tab" for that. `blob:` links and `data:` links are blocked.
- **Camera and calls are untested.** The page can ask for the microphone (voice messages) and the camera. The app asks Android for the permission and passes only audio or video capture through, and only to Facebook's own domains. Calls and video calls inside the page were not tried. Location sharing is switched off.
- **File uploads use Android's system picker** via the WebView's file chooser. Which file types Facebook accepts is up to Facebook.
- **Full-screen video** is supported through the WebView's custom-view hook but was not tried. System bars are hidden while it plays.
- **Third-party cookies are on**, because Facebook's site may rely on them. They are only available to pages the WebView loads, and the WebView only loads Facebook's domains (outside links go to Chrome).
- **Link allow-list.** The WebView loads `facebook.com`, `messenger.com`, `fb.com`, `fbsbx.com`, `meta.com` and their subdomains over HTTPS. If Facebook's login or security checks need another domain for a main-page navigation, it will open in Chrome instead and login may stall. That is the first thing to check if login misbehaves.
- **Sign out and clear data** removes the WebView's cookies, storage and cache for this app. It does not log other devices out.

## Differences from the brief

- **targetSdk is 32, not the newest.** Android Gradle Plugin 7.2.1 (kept from the original project) only supports up to API 32. Raising it means upgrading AGP, Gradle, Kotlin and Compose together.
- **Java 8 bytecode, not Java 17.** Java 17 source compatibility is not supported by AGP 7.2.1.
- **Voice effects are Kotlin, not native (NDK) code.** The Kotlin version was fast enough in the JVM check; not measured on a Realme 5.
- **Pitch-up effects can alias slightly.** Raising pitch resamples without a low-pass filter, so Alien may sound a little harsh on bright voices.
- **Room, Paging 3, Retrofit, OkHttp and Timber were never in this project**, so nothing was removed or kept; none is needed for a WebView host.

## Voice changer

- **It is a file share, not a Messenger voice note.** Messenger has no public API for that. The app opens Messenger's share target if it is installed and accepts audio, otherwise Android's share sheet. You pick the chat and send it there. Some Messenger versions may not accept audio files from a share; the "Share with another app" button covers that.
- **Voice clips reach the chat through Facebook's own attach picker.** The app cannot type into or click inside Facebook's page (no script injection, by design). "Attach to chat" arms the clip, and when the user taps Facebook's attach button the app offers the clip in that file picker. This only works if Facebook's picker accepts audio files; the photo and video pickers do not, and the app leaves them alone. It was **not tested** that Facebook accepts an M4A, shows it as a playable audio file, or what file-size limit applies. It will never be a native Messenger voice note. Facebook's own voice-message button inside the page is separate.
- **Voice clips can still be shared outside the chat** with the share icon (Messenger app or the Android share sheet).
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

- The voice bar takes about 150 to 200 dp of height at the bottom while it is open, so the chat area is smaller then. Close it with the X.
- The menu handle in full-screen mode is 24 dp wide and sits on the left edge, where it can overlap Facebook's own left-edge controls in desktop mode.
- Tap targets in the app's own controls are at least 48 dp except the full-screen handle's visible pill (the touch area is 24 x 72 dp).
- Dark mode applies to the app's own screens; Facebook's page follows its own settings.
- The Android package name is still `com.mathroda.messengerclone`.
- If the web view's renderer crashes, the error screen's Retry rebuilds the WebView and starts again at the Messages page.

## Security notes

- The app has no JavaScript bridge, no script injection, no cookie or token reading, and no password field. Passwords are only ever typed into Facebook's page.
- File access and content access are off in the WebView, mixed content is blocked, and certificate errors cancel the load.
- Remote WebView debugging (chrome://inspect) is on only in the debuggable build, which is the build this project ships. Anyone with USB debugging access to the phone could inspect the page, including a signed-in session. Do not use the debug APK on a phone you do not control.
- The VirusTotal key is stored encrypted. If encrypted storage fails on a phone, it is kept in memory for the session only.
- The original sample's hardcoded Stream demo users and tokens were removed from the source in an earlier commit, but they remain in this repository's git history.
