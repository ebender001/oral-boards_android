# Oral Boards AI (Android)

Android client for Oral Boards AI. This repo contains only the Android app; the iOS app and website live in their own repositories.

> Android port of the iOS app, backed by the same Back4App Cloud Code. No analytics SDKs are included.

## Tech stack

- Kotlin
- Jetpack Compose with Material 3
- Gradle (Kotlin DSL) with a version catalog (`gradle/libs.versions.toml`)
- Min SDK 26, target/compile SDK 37
- Application ID: `dev.benderapps.oralboardsai`
- OkHttp for Parse Cloud Code (REST) and the TTS service, Google Play Billing for subscriptions, `SpeechRecognizer` for dictation

## Getting started

1. Install the latest stable [Android Studio](https://developer.android.com/studio).
2. Clone the repo and open it in Android Studio:
   ```
   git clone https://github.com/ebender001/oral-boards_android.git
   ```
3. Copy the keys from `local.properties.template` into `local.properties` (git-ignored): `PARSE_APP_ID`, `PARSE_CLIENT_KEY`, `PARSE_SERVER_URL`.
4. Let Gradle sync, then run the `app` configuration on an emulator or device.

Command line:

```
./gradlew assembleDebug      # build a debug APK
./gradlew test               # unit tests
./gradlew connectedAndroidTest  # instrumented tests (needs a device/emulator)
```

`local.properties` (SDK path) is machine-specific and not tracked; Android Studio creates it for you.

## Project layout

```
app/src/main/java/dev/benderapps/oralboardsai/
  MainActivity.kt      # navigation
  data/                # Parse REST client, services, models, local progress store
  billing/             # Play Billing + free-case paywall gate
  speech/              # examiner audio playback, dictation
  ui/                  # view model, screens, components, theme
```
