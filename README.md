# PayQuick Android

This repository contains the Android implementation of the PayQuick money movement challenge. The app is written in Kotlin using Jetpack Compose, Hilt for dependency injection, and Retrofit for networking. The high-level architecture notes live in `ARCHITECTURE.md`.

## Features
- Secure login screen backed by the access/refresh token flow, complete with validation, password visibility toggle, and snackbar feedback.
- Material 3 expressive home screen with balance hero card, quick actions, recent activity, and a path to the full ledger.
- Mock send flow with quick amount chips, recipient suggestions, optimistic success state, and snackbar feedback.
- Receive flow that generates shareable codes/links and surfaces convenient copy/share actions.
- Paginated transaction history with logout built into the top app bar and automatic session refresh provided by the authenticator.
- Reactive session bootstrap, token refresh, and transaction fetch powered by the mock PayQuick API.

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer) with Android SDK 34 installed.
- JDK 17+
- Node.js 22+ for running the mock backend.

### Mock API
1. Open a terminal in `fe_challenge_api`.
2. Install dependencies: `npm install`.
3. Start the server: `npm run dev` (defaults to `http://localhost:3000`).
4. For Android emulators the base URL `http://10.0.2.2:3000/` is already baked into the app. For a physical device, update `BASE_URL` in `data/src/main/java/com/payquick/data/di/NetworkModule.kt` to point at your machine's IP address.

### Android App
1. Open the project root in Android Studio and allow it to sync Gradle. (If you prefer the CLI, run `./gradlew assembleDebug` after the wrapper downloads the Gradle distribution.)
2. Select the `app` run configuration and launch it on an emulator or device.
3. Launch the app and sign in with the mock credentials from the API README (`smith@example.com` / `pass123`). Use the top-right logout action to clear the session at any time.

> **Note:** The wrapper JAR is not committed. Android Studio will regenerate it automatically, or you can run `gradle wrapper` locally if you have Gradle installed.

## Project Structure
```
app/                          // Android app module with Compose UI + navigation host
  src/main/java/com/payquick/
    MainActivity.kt           // Hilt entry Activity
    PayQuickAndroidApplication.kt
    app/
      PayQuickApp.kt          // Sets up NavHost and scaffolding
      auth/                   // Login + MFA flows and state holders
      common/                 // Shared UI components (top bar, loading, feed)
      designsystem/           // PayQuick theme, typography, palette
      home/                   // Home dashboard screen + ViewModel
      navigation/             // Navigation destinations and graph wiring
      receive/                // Mock “request money” flow
      send/                   // Mock “send money” flow + quick amounts
      session/                // Session bootstrap + logout orchestration
      splash/                 // Splash screen gating session availability
      transactions/           // Paginated ledger screen + ViewModel
domain/                       // Pure Kotlin models, repository contracts, use cases
  src/main/kotlin/com/payquick/domain/
    model/                    // Domain data classes
    repository/               // Interfaces consumed by the app layer
    usecase/                  // Business logic entry points
data/                         // Retrofit clients, persistence, and Hilt bindings
  src/main/java/com/payquick/data/
    auth/                     // Auth repository implementation
    di/                       // Hilt modules (network/auth/session bindings)
    network/                  // Retrofit API, interceptors, DTOs
    session/                  // Session persistence via encrypted SharedPreferences
    transactions/             // Transaction repository implementation
fe_challenge_api/             // Node + Express mock backend
ARCHITECTURE.md               // Detailed architecture notes
```

## Verification
Due to the sandboxed environment, Gradle tasks could not be executed here. Please run `./gradlew lintDebug testDebugUnitTest` locally once the wrapper has been generated to verify the build.
