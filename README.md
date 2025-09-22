# Policy Switcher

Single-screen Android application concept for managing Keenetic router policies. The project uses Jetpack Compose Material 3 and follows the UX specification provided in the task prompt.

## Features

- Secure credential panel with inline validation, connection testing, and last successful sync indicator.
- Responsive policy grid and focus mode with client tables supporting drag-and-drop style gestures.
- Optimistic updates for applying and removing policies, per-client context menus, and registration workflow.
- Assistant command interpreter with ready-made intents and customizable command sheet with JSON import/export.
- Pull-to-refresh, search, empty states, toast + banner feedback, and bottom quick actions for mass operations.

## Project structure

- `app/src/main/java/com/example/policyswitcher/model` – Data models and UI state.
- `app/src/main/java/com/example/policyswitcher/data` – Fake repository and credential storage abstraction.
- `app/src/main/java/com/example/policyswitcher/assistant` – Voice/assistant command parsing and execution helpers.
- `app/src/main/java/com/example/policyswitcher/ui` – ViewModel and Compose UI implementation.
- `app/src/main/java/com/example/policyswitcher/ui/theme` – Compose Material theme definitions.
- `app/src/test/java` – JVM unit tests (assistant command handling).

## Building and testing

The repository ships with a lightweight `gradlew` shim that delegates to the local Gradle installation. Typical commands:

```bash
./gradlew test
./gradlew assembleDebug
```

The project targets Android API 26+ and uses Kotlin 1.9.24 with Jetpack Compose BOM 2024.09.02.
