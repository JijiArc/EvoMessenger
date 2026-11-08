# Purple Messenger Lite

An Android app that shows **Facebook's own Messages website** (`https://www.facebook.com/messages/`) inside a native Jetpack Compose shell, with two built-in extras:

- **Voice changer.** A bar docked under the chat. Pick a voice (Normal, Monster, Ghost, Robot, Alien, Digital Monster), record, preview, then **Attach to chat**: the app offers the clip in Facebook's own attach (paperclip) picker, so it goes out from the real chat box as an M4A audio file. It is **not** a native Messenger voice note. A share button is also there for sending through another app.
- **Link scanner.** Add your own free VirusTotal API key and check a link before you open it. The app never opens the scanned link for you.

This is a fork of [MathRoda's Messenger clone](https://github.com/MathRoda/Messenger-clone) (Apache 2.0). The Stream Chat SDK and every Stream-based screen have been removed; there are no fake or mock conversations.

> **Status: built by CI, not yet tested on a device.** Nothing about Facebook login, conversations, sending or receiving has been tried on a phone. Read [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) and [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md) first.

## Why a website and not an API

Meta offers no supported API for reading or sending in a personal Messenger inbox. The old messenger.com site no longer works for messaging either: Meta shut it down in April 2026 and redirects it to `facebook.com/messages` ([report](https://piunikaweb.com/2026/02/17/meta-messenger-com-shutdown-april-2026/)). The Messenger Platform is for Facebook Pages and Instagram Professional accounts: it uses Page access tokens and the `pages_messaging` permission, needs app review, and only lets a Page reply within a 24-hour window after a person writes to it. Facebook Login alone does not give access to your inbox. So this app does not try to read your messages itself. It displays Facebook's real website and Facebook's page does everything.

## How it works

- **Sign-in** happens on Facebook's own login page inside the WebView, including its security checks and two-factor prompts. The app has no login form, never sees or stores your password, and never reads cookies or tokens. The WebView keeps its own cookies, which is how you stay signed in between launches.
- **Everything Messenger does** (conversations, history, sending, receiving, groups, reactions, read receipts, search, contacts, media, voice messages) is Facebook's page doing it. The app does not inject scripts into the page and has no JavaScript bridge.
- **Staying on Facebook.** The WebView only loads Facebook's own domains over HTTPS. Links to anything else open in a Chrome Custom Tab. Certificate errors stop the load.
- **Two viewing modes** (menu > Full-screen mode / App shell mode):
  - *App shell*: purple top bar and bottom navigation around the page.
  - *Full screen*: only the page; a small handle on the left edge opens the menu.
  Switching modes does not reload the page.
- **Desktop site is the default.** Facebook's mobile site only shows a "get the app" page for messaging, and a first test of this app hit that wall. The app now asks for the desktop site, rewrites links to Facebook's mobile hosts (m., touch., mbasic.) to `www.facebook.com`, and offers a **Switch site** button if Facebook says the page isn't available. The monitor icon in the top bar toggles it. Whether the desktop messaging page then loads and is usable on a phone is **not yet confirmed**.
- **If messaging still won't load**, the menu has: *Page info* (shows which address and mode you are on, with ids hidden, so you can tell me what happened), *Zoom in/out* (the desktop layout is small on a phone), *Open in Chrome tab*, and *Open the official Messenger app* (shown only when it is installed).
- **Chrome Custom Tab fallback.** If Facebook refuses to load or sign in inside the WebView, the error screen and the menu offer "Open in Chrome tab". That is Chrome itself, with its own separate login.
- **Sign out and clear data** removes this app's Facebook cookies and site data.

## Build

You need Android Studio (a version that opens AGP 7.2.1 projects, such as Chipmunk or newer) with the Android SDK 32 platform, or just use the GitHub Actions workflow in this repo, which runs the unit tests and builds the debug APK.

1. Open this folder in Android Studio and let Gradle sync.
2. Run the unit tests: `./gradlew testDebugUnitTest`.
3. Build a debuggable APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the phone

1. Copy `app-debug.apk` to the phone, or connect it with USB debugging on and run `adb install -r app-debug.apk`.
2. If you install by tapping the file, allow "Install unknown apps" for your file manager when asked.

## Using the voice changer

Tap **Voice** in the bottom bar (or the menu). A bar opens under the chat. Choose a voice, tap the round microphone button to record, tap stop, then use the play button to preview. You can switch voice or change the strength slider after recording and the same clip is re-rendered.

To send it from the chat: tap **Attach to chat** (the bar says it is armed), then tap **Facebook's own paperclip / attach button** in the conversation. The app offers your clip in that picker: tap **Attach voice clip**, then press send in the chat. Facebook receives it as an audio-file attachment, not a native voice note. The photo and video buttons do not take audio, so the app leaves those pickers alone. The share icon sends through Messenger's app or the Android share sheet instead. Recording stops if you leave the app or a call comes in; what was recorded so far is kept.

This is separate from Facebook's own voice-message button inside the web page, which uses the microphone through the WebView.

## Using the link scanner

Tap **Link scan** in the bottom bar (or the menu). Paste your VirusTotal API key once ([free key](https://www.virustotal.com/gui/join-us)). Type or paste a link and tap **Scan link**.

## Privacy

- Permissions: Internet, microphone and camera (both asked only when needed: the microphone when you record in the voice changer or when Facebook's page asks to record a voice message, the camera only if the page asks), plus the no-prompt `MODIFY_AUDIO_SETTINGS` that WebView needs for audio capture.
- Facebook's page decides what Facebook collects. This app collects no analytics. It stores only display preferences (view mode, desktop site) and your VirusTotal key, which is kept with `EncryptedSharedPreferences`.
- The WebView accepts cookies, including third-party cookies, because Facebook's site uses them. They stay in the WebView's own store and are never read by the app.
- Audio from the voice changer is processed on the phone; temporary files are removed within about an hour of sharing.
- Scanning a link sends that link to VirusTotal. Do not scan links that contain private information.

## Checks you can run without Android

`tools/jvm-checks/run-checks.sh` compiles and runs the plain-Kotlin checks for the voice effects and the link scanner logic (needs `kotlinc`). The navigation rules in `web/WebConfig.kt` have JVM unit tests in `app/src/test`.

More: [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md), [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md), [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

---

# Original project README

> Historical: this section is the upstream README for the original Stream-based project, kept for attribution. It does not describe this fork.

![](MessengerClone/messenger-clone.jpg)

# **Messenger Clone** 

### Built for Android using [Stream SDK](https://getstream.io/chat/sdk/compose?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_May2022_SlackAndroidClone&utm_term=DevRelOss) & [Jetpack Compose](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=&cad=rja&uact=8&ved=2ahUKEwjspaT-4Zv5AhWziP0HHXYtBIEQFnoECAsQAQ&url=https%3A%2F%2Fdeveloper.android.com%2Fjetpack%2Fcompose&usg=AOvVaw2BpAQ4DMwUiKSOtVRyUczM)

This project is developed by [MathRoda](https://www.linkedin.com/in/mathroda/) for more projects [GitHub](https://github.com/MathRoda/) .

# Showcase Video
watch video here [Messenger Clone Showcase](https://youtu.be/T34epxQhFJc)


# Blog ✍

I wrote a article contains more details about the project , read it here : https://medium.com/@mathroda/messenger-clone-using-jetpack-compose-stream-sdk-2defd636eaf0

## Application Install

***You can Install and test latest Messenger Clone app from below 👇***

[![MessengerClone](https://img.shields.io/badge/MessengerClone✅-APK-red.svg?style=for-the-badge&logo=android)](https://github.com/MathRoda/Messenger-clone/releases/download/1.0.0/MesengerClone.apk)

<a href="https://getstream.io/chat/sdk/compose?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_May2022_SlackAndroidClone&utm_term=DevRelOss">
<img src="https://user-images.githubusercontent.com/24237865/138428440-b92e5fb7-89f8-41aa-96b1-71a5486c5849.png" align="right" width="12%"/>
</a>

## 🛥 Stream Chat SDK
Messenger Android clone was built with __[Stream Chat SDK for Compose](https://getstream.io/chat/sdk/compose?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_May2022_SlackAndroidClone&utm_term=DevRelOss)__ to implement messaging systems.

## About
 
- Clean and Simple UI with Jetpack Compose.
- Best and Clean Code Quality following Modern Android Development
- Stream SDK for messaging systems

## 📸 Screenshots 

|   |   |   |   |
|---|---|---|---|
|![](MessengerClone/switch-account-screen.png) | ![](MessengerClone/main-screen.png) | ![](MessengerClone/profile-screen.png) | ![](MessengerClone/search-screen.png) 
|![](MessengerClone/reacted-screen.png) | ![](MessengerClone/reaction-screen.png) | ![](MessengerClone/sent-message-screen.png) | ![](MessengerClone/message-deleted-screen.png)

## 🛠 Tech Sacks & Open Source Libraries
- 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) based + [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) + [Flow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/) for asynchronous tasks.
- [Compose Chat SDK from Stream](https://getstream.io/chat/sdk/compose?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_May2022_SlackAndroidClone&utm_term=DevRelOss): The Jetpack Compose Chat Messaging SDK is built on a low-level chat client and provides modular, customizable Compose UI components that you can easily drop into your app.
- Jetpack
  - Compose: Android’s modern toolkit for building native UI.
  - Lifecycle: Observe lifecycle changes.
  - ViewModel: UI related data holder and lifecycle aware.
  - Room Persistence: Constructs Database by providing an abstraction layer over SQLite to allow fluent database access.
  - Paging3: Loads and displays pages of data from a larger dataset from local storage or over network.
- [Retrofit2 & OkHttp3](https://github.com/square/retrofit): Construct the REST APIs and paging network data.
- [Timber](https://github.com/JakeWharton/timber): A logger with a small, extensible API which provides utility.


## 🏛️ Package Structure

![](MessengerClone/package.jpg)


## 💯 MAD Score

![](MessengerClone/madscore.png)

## Copyrights & Credits

All copyrights of the resources, logo, branding, content, concepts, and phrases that are used in this open-source project belong to [Meta(Facebbok)](https://facebook.com/).

---

## If you like my projects you can buy me a cup of coffee 💙
  
<a href="https://www.buymeacoffee.com/mathroda"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=VaibhavJaiswal&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff"></a>


 ## Contact
you can connect with me here [LinkedIn](https://www.linkedin.com/in/mathroda/) .

# License
```xml
Copyright 2022 MathRoda, Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
 
