# Dependencies

Everything the app uses comes from Google Maven, Maven Central and Stream's artifacts, all declared in `app/build.gradle`.
The voice effects, URL checks and VirusTotal client use **only the Android platform and the Kotlin standard library**, so no extra audio or networking library was added.

## Added for Purple Messenger Lite

- `androidx.security:security-crypto:1.0.0` for encrypted storage of the chat token and the VirusTotal key.

## Platform APIs used (no library)

| Feature | API |
|---|---|
| Recording | `AudioRecord`, `AudioManager` audio focus |
| Voice effects | Plain Kotlin DSP in `voice/AudioEffects.kt` (resampling + WSOLA time-stretch, ring modulation, echo, vibrato, bit crushing) |
| M4A export | `MediaCodec` (AAC-LC) + `MediaMuxer` |
| Preview | `MediaPlayer` |
| Sharing | `FileProvider` (own subclass) + `Intent.ACTION_SEND` |
| Link scanning | `HttpURLConnection` over HTTPS, `org.json` |

## Full `app/build.gradle` dependency list

- `implementation 'androidx.core:core-ktx:1.8.0'`
- `implementation "androidx.compose.ui:ui:$compose_version"`
- `implementation "androidx.compose.material:material:$compose_version"`
- `implementation "androidx.compose.ui:ui-tooling-preview:$compose_version"`
- `implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.5.0'`
- `implementation 'androidx.activity:activity-compose:1.5.0'`
- `implementation "androidx.compose.material:material-icons-extended:$compose_version"`
- `implementation "io.getstream:stream-chat-android-compose:5.6.1"`
- `implementation 'androidx.appcompat:appcompat:1.4.2'`
- `implementation 'com.google.android.material:material:1.6.1'`
- `implementation 'androidx.navigation:navigation-fragment-ktx:2.5.1'`
- `implementation 'androidx.navigation:navigation-ui-ktx:2.5.1'`
- `implementation 'androidx.security:security-crypto:1.0.0'`
- `testImplementation 'junit:junit:4.13.2'`
- `androidTestImplementation 'androidx.test.ext:junit:1.1.3'`
- `androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'`
- `androidTestImplementation "androidx.compose.ui:ui-test-junit4:$compose_version"`
- `debugImplementation "androidx.compose.ui:ui-tooling:$compose_version"`
- `debugImplementation "androidx.compose.ui:ui-test-manifest:$compose_version"`

## Build tooling

- Android Gradle Plugin 7.2.1, Gradle 7.3.3 (from the wrapper), Kotlin 1.6.10, Compose 1.1.1 (unchanged from the original project).
- JDK 11 or newer to run Gradle. Android Studio's bundled JDK works.
