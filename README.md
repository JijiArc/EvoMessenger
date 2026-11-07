# Purple Messenger Lite

A lightweight, Messenger Lite-style chat app for Android 10 and newer (target device: Realme 5, arm64), with two built-in extras:

- **Voice changer.** Tap the microphone in a conversation, pick a voice (Normal, Monster, Ghost, Robot, Alien, Digital Monster), record, preview, then send. It is shared as an M4A audio file through Messenger's share target or the Android share sheet. It is **not** a native Messenger voice note.
- **Link scanner.** Tap the shield icon in the chat list header, add your own free VirusTotal API key, and check a link before you open it. The app never opens the scanned link for you.

This is a fork of [MathRoda's Messenger clone](https://github.com/MathRoda/Messenger-clone) (Apache 2.0). The chat itself runs on **Stream Chat**, so this is a separate messaging service and **not your Facebook inbox**.

> **Status: source only, not yet built or tested on a device.** No APK is included, because the environment this was written in had no Android SDK or network. Read [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) and [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md) first. Expect to fix a few compile errors on the first build.

## What changed from the original project

- minSdk 29, app renamed, purple theme, microphone permission, `FileProvider`, `allowBackup` off, HTTPS only.
- Removed the hardcoded Stream demo API key and demo users. You now sign in with **your own** Stream key (see below).
- Chat token and VirusTotal key are stored with `EncryptedSharedPreferences`.
- New code: `voice/` (recording, effects, M4A encoding, sharing, bottom sheet) and `scanner/` (URL checks, VirusTotal client, screen).

## Build

You need Android Studio (a version that opens AGP 7.2.1 projects, such as Chipmunk or newer; newer versions may offer to upgrade the plugin, which you can decline for now) with the Android SDK 32 platform installed.

1. Open this folder in Android Studio and let Gradle sync.
2. Build a debuggable APK: `./gradlew assembleDebug` (on Windows: `gradlew.bat assembleDebug`).
3. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. It is a single universal APK (no splits) and the app ships no native code of its own.

## Install on the phone

1. Copy `app-debug.apk` to the phone, or connect it with USB debugging on and run `adb install -r app-debug.apk`.
2. If you install by tapping the file, allow "Install unknown apps" for your file manager when asked.

## Set up chat (Stream)

1. Create a free app at [getstream.io](https://getstream.io/chat/) and copy its **API key**.
2. For testing, in the Stream dashboard turn on **Disable Auth Checks** for the app. Then you can leave the token field empty and the app uses a development token. For real use, generate user tokens on a server you control and paste one in.
3. Open the app and enter the API key and a user ID (letters, numbers, `@`, `_`, `-`). Do the same on a second phone or account with a different user ID to chat.

## Using the voice changer

Open a conversation and tap the microphone icon. Choose a voice, tap the round microphone button to record, tap stop, then **Play preview**. You can switch voice or change the strength slider after recording and the same clip is re-rendered. Tap **Send via Messenger** (or **Share voice message** if Messenger is not installed), then choose the chat inside Messenger and send.

Recording stops if you leave the app or a call comes in; what was recorded so far is kept.

## Using the link scanner

Tap the shield icon at the top of the chat list. Paste your VirusTotal API key once ([free key](https://www.virustotal.com/gui/join-us)). Type or paste a link and tap **Scan link**.

## Privacy

- The only permissions are Internet and Microphone. The microphone is requested only when you tap record.
- Audio is processed on the phone. Recordings are temporary files in the app cache and are deleted when you cancel or replace them; shared files are removed within about an hour.
- Scanning a link sends that link to VirusTotal (a Google service), which may share submitted links with security vendors. Do not scan links that contain private information.
- Your VirusTotal key is only sent to VirusTotal. Your chat token is only sent to Stream. Both are stored encrypted on the phone and are never logged.
- The app collects no analytics and has no hidden data collection of its own. Messages go through Stream, which has its own privacy policy.

## Checks you can run without Android

`tools/jvm-checks/run-checks.sh` compiles and runs the plain-Kotlin checks for the voice effects and the link scanner logic (needs `kotlinc`).

More: [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md), [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md), [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

---

# Original project README

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
 
