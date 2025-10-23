# PayQuick Frontend Architecture (Android)

## Component Overview

- **App module (`com.payquick.app`)** – Jetpack Compose presentation layer organised with a screaming-architecture layout. Feature packages (`auth`, `home`, `transactions`, `send`, `receive`, `session`) expose route-level composables that drive the Material 3 UI and navigation host while keeping session orchestration separate.
- **State management** – Each screen owns a `ViewModel` that emits immutable `UiState` through `StateFlow` and surfaces one-off side effects through `SharedFlow` event channels. Presentation code never touches repositories directly; it depends on domain use cases.
- **Domain module (`:domain`)** – Pure Kotlin module containing canonical models, repository contracts, and application-specific use cases (`ObserveSessionUseCase`, `FetchTransactionsPageUseCase`, `SubmitMockTransferUseCase`, etc.). Constructors are `@Inject`-able to keep DI friction low.
- **Data module (`:data`)** – Android library that fulfils domain contracts using Retrofit/OkHttp, Kotlinx Serialization, and DataStore. It provides networking/auth plumbing (`AuthInterceptor`, `TokenAuthenticator`, `PayQuickApi`) plus persistence helpers (`SessionManager`). Hilt modules live here.
- **Cross-cutting infrastructure** – Logging uses Timber, networking honours bearer-token auth with automatic refresh, and DataStore keeps session state reactive for all layers.

## Data Flow & Interaction

1. User actions in Compose trigger `UiAction`s to the relevant `ViewModel`.
2. `ViewModel` invokes domain use cases on `viewModelScope` with appropriate dispatcher.
3. Use cases call repository APIs; repositories orchestrate network calls (Retrofit) and persist session snapshots via DataStore.
4. Network layer attaches auth headers via `AuthInterceptor`. On 401, it delegates to `SessionManager` which refreshes tokens using stored refresh token and retries transparently.
5. Results propagate back as `Result` objects; `ViewModel` reduces them into new `UiState`, which re-renders the Compose UI.
6. Side effects (navigation, toasts) emitted through `SharedFlow` channels consumed by UI or a `Navigator` helper.

## Security Considerations

- **Token Storage**: Session tokens live in a `Preferences` DataStore via `SessionManager`. They are not encrypted, which is acceptable for the challenge scope but would need hardening before production.
- **Transport**: The app targets the mock API over plain HTTP (`http://10.0.2.2:3000/api/`). TLS, hostname verification, and certificate pinning are intentionally out of scope.
- **Authentication Flow**: Only username/password login is implemented. There is no MFA, account recovery, or session expiry UI; backend refresh tokens keep sessions alive until logout.
- **Input Handling**: Forms perform basic validation (non-blank email/password, positive transfer amounts) but no additional sanitisation or server-side protection is in place.
- **Threat Surface**: The app does not set `FLAG_SECURE`, integrate SafetyNet/Play Integrity, or guard against overlay attacks. Those measures remain future work.

## Maintainability & Extensibility

- **Modularization**: Three-module clean split (`app`, `domain`, `data`) keeps dependencies pointing inward and makes it trivial to unit-test domain logic or swap data implementations.
- **Dependency Injection**: Hilt powers constructor injection throughout – domain use cases drop straight into presentation, while data-level bindings live in `data/di` modules.
- **Testing Strategy**: ViewModels can be exercised with fake use cases, domain logic with fake repositories, and Retrofit endpoints via MockWebServer sitting behind `PayQuickApi`.
- **Design System**: `PayQuickTheme` centralises the expressive Material 3 palette, typography, and rounded shapes so features stay visually consistent.
- **Analytics & Logging**: Timber-based logging remains centralised in the `Application` class; analytics providers can plug in behind interfaces when needed.

## Key Decisions & Trade-offs

- **Jetpack Compose vs XML**: Compose chosen for faster iteration and declarative UI; trade-off is steeper learning curve, but mitigated by Compose stability and tooling.
- **MVVM + StateFlow**: Provides unidirectional data flow without full Redux overhead. Trade-off: requires discipline to avoid mutable state leaks; solved via sealed `UiState` and reducer pattern.
- **Hilt DI**: Simplifies dependency graphs vs manual factories. Cost is build-time overhead, acceptable for improved testability.
- **Retrofit/OkHttp**: Mature ecosystem with interceptors for auth and logging. Alternative Ktor considered but Retrofit better fits Android team skills.
- **No Local Cache**: Data is fetched on demand from the mock API. There is no Room/SQL layer today, so offline support would require future investment.
- **Feature Modularization**: Increases initial setup but supports scaling team and codebase, facilitating dynamic feature delivery.
- **Token Refresh via Authenticator**: Centralized refresh avoids scattering logic; trade-off is reliance on the OkHttp pipeline, mitigated slightly by the lightweight `TokenAuthenticator` implementation.
