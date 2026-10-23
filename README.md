# Translate

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-lightgrey.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-10+-3DDC84.svg)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)](https://developer.android.com)

A real-time screen translation assistant for Android. Translate text on any app screen instantly using your device's assitant button — no need to switch apps or copy-paste.

I built this because I wanted the experience of Google Translate but have it available offline. It was also a nice excuse to play around with MLKit and Assistant APIs

## Screenshots

| Before                                                                                                                           | After                                                                                                                           |
|----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| ![](https://raw.githubusercontent.com/SubhrajyotiSen/Translate/b4b4a9ab6f1d1316860fb76f2e6227b42cd15272/screenshots/before1.png) | ![](https://raw.githubusercontent.com/SubhrajyotiSen/Translate/b4b4a9ab6f1d1316860fb76f2e6227b42cd15272/screenshots/after1.png) |
| ![](https://raw.githubusercontent.com/SubhrajyotiSen/Translate/b4b4a9ab6f1d1316860fb76f2e6227b42cd15272/screenshots/before2.png) | ![](https://raw.githubusercontent.com/SubhrajyotiSen/Translate/b4b4a9ab6f1d1316860fb76f2e6227b42cd15272/screenshots/after2.png) |

## Features

- **Instant Screen Translation** — Press the assistant button from any app, and Translate captures the screen, extracts text via OCR, and displays the translation as an overlay.
- **Voice Interaction Service** — Integrates with Android's Voice Interaction framework for seamless system-level activation.
- **On-Device Translation** — Powered by ML Kit, translations run locally on your device for privacy and offline support.
- **Language Detection** — Automatically detects the source language of the text on screen.
- **Language Management** — Download and manage translation language packs directly from the app.
- **Overlay Display** — Translated text is shown in a smooth animated overlay with a gradient background.

## Requirements

- Android 7.0 (API 24) or higher
- Kotlin 2.0+
- Android Studio Ladybug or newer

## Tech Stack

- **Jetpack Compose** — Modern UI toolkit for the app interface
- **ML Kit** — On-device text recognition (OCR), language identification, and translation
- **VoiceInteractionService** — Android framework service for system-level voice input integration
- **Kotlin Coroutines** — Asynchronous operations for model downloads and translation tasks

## How It Works

1. Open any app and press the microphone button (or use your assigned voice assistant shortcut).
2. Translate captures a screenshot of the current screen.
3. ML Kit performs OCR to extract visible text.
4. The source language is automatically detected.
5. The extracted text is translated using the on-device ML Kit translator.
6. The translated text is displayed in a full-screen overlay with animated transitions.

## Setup

### Prerequisites

- JDK 17
- Android SDK 37 (compile SDK)
- Android Studio with Kotlin plugin

## Configuration

### Setting as Default Voice Assistant

After installing, open the Translate app and tap **"Enable as Default Assistant"** to set it as your system voice input service. You may also enable it manually:

1. Go to **Settings** → **Apps** → **Default apps** → **Digital assistant app**
2. Select **Translate**

### Overlay Permission

The app requires the **Display over other apps** permission to show translations. Grant it when prompted, or enable it manually:

1. Go to **Settings** → **Apps** → **Translate** → **Display over other apps**
2. Toggle the permission on

### Managing Languages

Open the Translate app to view, download, or delete language packs. Models are downloaded on-demand using ML Kit's remote model manager.

## Project Structure

```
app/src/main/java/me/subhrajyoti/translate/
├── MainActivity.kt                # Entry point and language management screen
├── ManageLanguagesScreen.kt       # Compose UI for managing language models
├── LanguageModelManager.kt        # ML Kit model download/delete logic
├── TranslationService.kt          # VoiceInteractionService entry point
├── TranslationSession.kt          # Core translation logic (OCR, detection, overlay)
├── TranslationSessionService.kt   # Session management service
└── TranslationRecognitionService.kt # Speech recognition service
```

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Download language models from ML Kit |
| `SYSTEM_ALERT_WINDOW` | Display translation overlay on top of other apps |

## License

This project is not yet licensed. Feel free to use it for personal purposes.
