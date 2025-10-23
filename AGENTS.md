# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Jetpack Compose UI, navigation shell, and Hilt entry points (`MainActivity`, feature packages like `home/`, `send/`, `session/`).
- `domain/`: Pure Kotlin models, repository interfaces, and use cases consumed by other modules.
- `data/`: Retrofit clients, DataStore session handling, and Hilt modules wiring the API to domain contracts.
- `fe_challenge_api/`: Node/Express mock backend powering access tokens and transaction data. See its README for mock credentials.
- Additional references live in `ARCHITECTURE.md` and top-level Gradle scripts.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build the Android app (CLI alternative to Android Studio's Run).
- `./gradlew lintDebug`: run static analysis before submitting changes.
- `./gradlew testDebugUnitTest`: execute JVM unit tests; add new tests here when touching business logic.
- `./gradlew connectedDebugAndroidTest`: launches instrumentation/Compose UI tests on a running emulator.
- `npm install && npm run dev` (from `fe_challenge_api/`): start the mock server on `http://localhost:3000` (`10.0.2.2` for emulators).

## Coding Style & Naming Conventions
- Kotlin + Compose with 4-space indentation and trailing commas for multiline params when possible.
- Prefer `PascalCase` for Composables (`SendScreen`), `CamelCase` for functions/variables, and `ScreamingSnakeCase` for constants.
- Organize feature code by package (`auth/`, `transactions/`) and keep ViewModels in the same package as their screen.
- Use Hilt injection for dependencies; avoid manual singleton wiring.
- Keep string literals in `res/values/strings.xml` when surfaced to users.

## Testing Guidelines
- Project uses JUnit4, coroutines test utilities, and Compose UI test APIs; mirror production packages under `src/test` or `src/androidTest`.
- Name tests after the scenario under test (`SessionViewModelTest`); prefer AAA structure and `runTest` for suspend code.
- Add regression tests for flows, repository implementations, and navigation state before merging significant logic changes.
- When touching the mock API, provide matching contract tests in TypeScript or update fixtures accordingly.

## Commit & Pull Request Guidelines
- Follow the Conventional Commit style already in history (`feat: ...`, `fix: ...`). Scope optional but encouraged (`feat(app-home): ...`).
- Squash small WIP commits locally; ensure messages describe user-facing impact.
- PRs should include: summary of changes, screenshots/GIFs for UI updates, affected tickets, and a checklist confirming lint/tests.
- Highlight API or contract changes and update `ARCHITECTURE.md` or README when architecture shifts.
