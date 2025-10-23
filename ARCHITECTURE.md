# PayQuick Frontend Architecture (Android)

It’s basically the classic 3‑tier clean setup, nothing fancy.
We’ve got app for UI/state, domain for the pure Kotlin contracts + use cases, and data for the Retrofit/auth/session stuff.
I skipped mappers or over-engineering or overthinking the models and how they are shared across modules,

but you still get the clean separation:
- UI only talks to the interfaces,
- data plugs in the real implementations,
- and domain stays oblivious to Android.

This keeps everything easy to test (fake the repos, unit test the use cases, hit the ViewModels in isolation) while letting each layer evolve without needing complicated refactor and changes over the others.

## Component Overview

- Session handling lives in retrofit interceptors, not inside the screens.
- Presentation/App module (com.payquick.app)
  - Jetpack Compose feature packages (auth, home, transactions, send, receive, session, etc.) wired to the nav host.
  - State management
    - Every screen has its own ViewModel exposing a StateFlow of immutable UI state plus a SharedFlow for one-off events. 
    - ViewModels only talk to domain use cases.
- Domain module (:domain)
  - Plain Kotlin models, repository interfaces, and use cases. 
  - Constructors are @Inject so Hilt drops them where needed.
- Data module (:data) 
  - Retrofit/OkHttp clients, auth plumbing (AuthInterceptor, TokenAuthenticator, PayQuickApi), session helpers (SessionManager), and Hilt bindings.
  - Timber for logging, interceptors for auth, encrypted session storage so state stays reactive and safe.

## Data Flow & Interaction

1. Compose surfaces user intent to ViewModel.
2. ViewModel calls a domain use case on viewModelScope.
3. Use case hits a repository; repository does the Retrofit call (if applicable) and updates session in encrypted storage.
4. Network stack adds auth headers. 401s trigger the refresh flow automatically.
5. Results bubble back as Result objects. ViewModel reduces into new UI state and Compose re-renders.
6. Side effects (navigation, snackbars) are listened to through SharedFlow to the UI.

## Security Considerations

- Token storage - Access/refresh tokens live in SessionManager, backed by EncryptedSharedPreferences (Android Keystore under the hood).
- Transport - Talking to the mock API over http://10.0.2.2:3000/api/.
- Auth flow - Username/password only. MFA is mocked out, no recovery flow, no session expiry UI. Refresh token keeps things alive until logout.
- Input handling - Basic validation on forms.
- Threat surface - No FLAG_SECURE, Play Integrity, or overlay protections yet. 

## Maintainability & Extensibility

- Modularisation - app, domain, data point inward so you can swap implementations and test each layer without hated spaghetti.
- DI - Hilt glues everything: domain use cases drop straight into ViewModels, bindings sit in data/di.
- Testing - ViewModels exercise nicely with fake use cases; domain logic with fake repos; network code rides behind MockWebServer.
- Design system - PayQuickTheme keeps colors/typography consistent across screens.
- Telemetry - Timber centralised in Application; easy to swap in crash/analytics hooks later.

## Key Decisions & Trade-offs

- Compose > XML - Faster iteration, declarative ergonomics. Slight learning curve, worth it.
- MVVM + StateFlow - Gives unidirectional data flow without hauling in a full Redux stack. Requires discipline around mutation, solved with immutable state objects.
- Hilt - Saves you from manual factories. I considered Koin but I think overall Hilt is better for performance and safety.
- Retrofit/OkHttp - Reliable, well-known. I considered ktor but Retrofit/Okhttp is easy for new teammates to recognise and interceptors are a game changer for token refresh mechanism.
- No local cache yet - Everything comes straight from the mock API. Offline/Room would be a future add.
- Feature packaging - Screaming architecture layout makes it obvious where code lives, supports splitting into feature modules later.
- Token refresh via authenticator - Centralised interception keeps auth logic out of every call. Depends on okhttp pipeline, but the wrapper is light and well-contained.
