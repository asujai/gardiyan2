claude# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Gardiyan** (Turkish: "Guardian") is an Android app that enforces daily time limits on selected apps. It detects restricted apps in the foreground via an accessibility service, tracks elapsed usage time, and displays a lock overlay once the daily limit is exhausted.

## Build & Test Commands

```powershell
# Build
./gradlew.bat assembleDebug
./gradlew.bat assembleRelease

# Unit tests (JVM, Robolectric)
./gradlew.bat test

# Instrumented tests (device/emulator required)
./gradlew.bat connectedAndroidTest

# Lint
./gradlew.bat lint

# Run a single test class
./gradlew.bat test --tests "com.gardiyan.app.GuardianRepositoryRegressionTest"
```

Dependencies are managed via the version catalog at `gradle/libs.versions.toml`.

## Architecture

Layered MVVM + Repository pattern. All UI screens share a single `GuardianViewModel` instance (created via factory in `MainActivity`).

### Layers

**UI** — Jetpack Compose + Material3. Screens live in `ui/screens/`, shared widgets in `ui/components/`. Navigation is defined in `AppNavGraph` with 7 named routes (constants prefixed `ROUTE_`).

**ViewModel** — `GuardianViewModel.kt` (~718 lines). Central orchestrator: manages restricted app list, permission state, service start/stop, and the 5-second hold gesture that unlocks a blocked app session. Exposes state via `StateFlow`.

**Repository** — `GuardianRepository.kt` (~799 lines). Single source of truth. Contains all Room DB operations, daily reset logic (with a 22-hour lockout to prevent exploit), and mission/level evaluation.

**Services** — two long-running background components:
- `AppBlockAccessibilityService` (AccessibilityService): primary event-driven foreground detection via `TYPE_WINDOW_STATE_CHANGED`; secondary adaptive polling via `UsageStatsManager.queryEvents()`. Deducts elapsed time from the daily limit and signals `BlockOverlayService` when an app should be locked.
- `BlockOverlayService` (ForegroundService): draws a lock overlay using `WindowManager.addView()` with an infinite 10-second countdown loop. Can only be dismissed via the 5-second hold gesture in the main app.

**Workers** — `KeepAliveScheduler` (periodic WorkManager task for service health) and `DailySuccessWorker` (evaluates daily success/failure at end of day).

### Database

Room database (`GuardianDatabase`) at schema version 9 with migration history. Entities:
- `RestrictedAppEntity` — package name, daily limit (seconds), remaining seconds, failure state
- `UserSessionEntity` — user profile, level, badges, streak
- `ActiveUsageSessionEntity` — tracks the current foreground session with entry/exit timestamps
- `StatusLogEntity` — audit log of all enforcement events
- `FriendEntity` — placeholder, currently unused

### Time Manipulation Detection

`GuardianRepository` cross-checks monotonic clock (`SystemClock.elapsedRealtime()`), wall clock (`System.currentTimeMillis()`), and boot time to detect backward system clock changes. This feeds into the daily reset lockout logic.

## Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | App entry point, permission checks, bottom nav |
| `GuardianViewModel.kt` | All business logic exposed to UI |
| `GuardianRepository.kt` | DB access + daily reset + mission control |
| `AppBlockAccessibilityService.kt` | Foreground app detection engine |
| `BlockOverlayService.kt` | Lock overlay rendering |
| `AppNavGraph.kt` | Navigation routes |
| `GuardianDatabase.kt` | Room setup + migration definitions |

## SDK & Language

- minSdk 24 / targetSdk 36
- Kotlin 2.2.10
- Coroutines 1.10.2, StateFlow for reactive state
- Tests: JUnit 4, Robolectric 4.16.1, Roborazzi 1.59.0 (screenshot tests), Espresso

## Localization

The app ships Turkish and English strings. String resources live in `res/values/strings.xml` (default/English) and `res/values-tr/strings.xml` (Turkish). Hardcoded Turkish strings in Compose files are a known tech debt — prefer string resources.
