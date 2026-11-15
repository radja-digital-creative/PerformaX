# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PerformaX is a **Kotlin/Jetpack Compose Android app** for employee performance management, backed by Supabase. The repo also contains a standalone **Node.js CLI tool** for Supabase backup/restore operations.

- App version: 3.9.9 | compileSdk 36 | minSdk 29 | targetSdk 35
- Package: `id.biz.yuka.performax`

## Build Commands

```bash
# Build the Android app
./gradlew build                         # Full build (Windows: gradlew.bat build)
./gradlew assembleDebug                 # Debug APK
./gradlew assembleRelease               # Release APK

# Unit tests
./gradlew test                          # App unit tests
./gradlew :lib-performx:test            # Library unit tests
./gradlew :lib-performx:test --tests "id.biz.yuka.libperformx.AuthRepositoryTest"  # Single test class

# Instrumented (on-device) tests
./gradlew connectedAndroidTest

# Supabase CLI tool
cd supabase-performx
npm start           # Interactive backup/restore menu
npm run backup      # Run backup
npm run test-connection  # Test Supabase connection
```

## Module Architecture

The project has two Android modules and one Node.js tool:

### `:lib-performx` — Shared Data Layer (Android Library)
Contains everything not UI-specific:
- **`entity/`** — Kotlinx Serializable data classes mapping to Supabase tables
- **`repository/`** — All data access (Auth, Employee, Attendance, Task, KPI, Leave, Bonus, Deduction, Salary, Schedule, Store, Appeal, Notification)
- **`request/` & `response/`** — DTOs for API calls
- **`SupabaseConfig.kt`** — Singleton Supabase client initialization
- Unit tests live in `lib-performx/src/test/kotlin/`

### `:app` — Android UI Layer
- **`ui/screen/`** — One directory per feature (attendance, bonus, deduction, employee, home, kpi, leave, login, notification, performa, profile, salary, schedule, splash, store, task, violation, features)
- **`ui/navigation/`** — `Screen.kt` (sealed class for routes) + `AppNav.kt` (NavHost)
- **`ui/components/`** — Shared composables (e.g., `LeafletMap.kt`)
- **`ui/theme/`** — Material 3 theme
- **`di/`** — Hilt modules: `AppModule`, `NetworkModule`, `RepositoryModule`
- **`service/`** — `MyFirebaseMessagingService`, `FcmManager`, `ChatEventBus`, `ChatRoomObserver`
- **`data/SessionManager.kt`** — Auth session state

### `supabase-performx/` — Node.js Backup CLI
Separate tool, not part of the Android build. Uses `lib/backup/` and `lib/restore/` modules for schema, data, cron, storage, functions, and secrets backup/restore.

## Key Architectural Patterns

**Repository pattern with `ResultState<T>`**: All data operations in `lib-performx/repository/` return a `ResultState<T>` wrapper. Use the `safeSupabaseCall {}` utility to wrap Supabase API calls uniformly.

**Hilt DI**: The `SupabaseClient` singleton, repositories, and services are provided via Hilt modules in `app/di/`. All screens/ViewModels use `@HiltViewModel` and `@AndroidEntryPoint`.

**Navigation**: Routes are defined as a sealed class in `Screen.kt`. `AppNav.kt` wires the `NavHost`. Navigation follows stack semantics with `popUpTo`/`inclusive` for auth flows.

**Supabase integration**: Uses PostgREST (database), GoTrue (auth), Storage (files), and Realtime (live updates). The client is configured once in `SupabaseConfig.kt` and injected everywhere.

**Face detection**: TensorFlow Lite + ML Kit Face Detection are used for biometric attendance. Related utilities are under `ui/screen/attendance/`.

**Map rendering**: Leaflet maps are rendered inside a WebView via `LeafletMap.kt` — not a native map SDK.

**Push notifications**: Firebase Cloud Messaging via `MyFirebaseMessagingService`. FCM token management is in `FcmManager.kt`. Chat updates use an in-process `ChatEventBus` (EventBus pattern).

## Dependency Versions

All dependency versions are centralized in `gradle/libs.versions.toml`. When adding or updating dependencies, edit the version catalog there rather than hardcoding versions in individual `build.gradle.kts` files.
