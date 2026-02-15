# CLAUDE.md

## Project Overview

Telegrammer is a Telegram-like 1-on-1 encrypted chat app. Full Kotlin stack: Ktor server + KMP shared module + Android Compose UI.

## Build & Run

```bash
# Start MongoDB
cd server && docker compose up -d

# Run server (JDK 17 required)
./gradlew :server:run

# Build Android APK
./gradlew :androidApp:assembleDebug
```

## Key Directories

- `server/src/main/kotlin/com/telegrammer/server/` — Backend (routes, repositories, models, WebSocket)
- `shared/src/commonMain/kotlin/com/telegrammer/shared/` — KMP shared code
  - `api/` — HTTP API clients (AuthApi, ContactApi, ChatApi, KeyApi)
  - `crypto/` — E2E encryption (KeyManager, X3DH, Ratchet, CryptoSession)
  - `ws/` — WebSocket client (ChatSocket, WsMessage types)
  - `repository/` — Data layer (AuthRepository, ChatRepository, ContactRepository)
  - `db/` — SQLDelight DAOs (MessageDb, ConversationDb)
  - `platform/` — expect/actual (SecureStorage, DriverFactory)
- `androidApp/src/main/kotlin/com/telegrammer/android/` — Android UI
  - `navigation/NavGraph.kt` — All navigation routes
  - `AppDependencies.kt` — Manual DI wiring

## Architecture Decisions

- **No DI framework** — Manual constructor injection via `AppDependencies`
- **No use-case classes** — ViewModels call repositories directly
- **No mapper classes** — Data classes use companion `from()` methods
- **Server stores only ciphertext** — E2E encrypted, never sees plaintext
- Dependencies exposed from shared to androidApp via `api()` not `implementation()`

## Network Config

Server IP is hardcoded in two files (update when network changes):
- `shared/.../api/ApiClient.kt` — `apiHost` parameter
- `shared/.../ws/ChatSocket.kt` — `wsHost` parameter

## Common Issues

- **Gradle needs JDK 17**: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- **APK install on phone**: Use `adb install -t` flag for debug builds
- **Cleartext HTTP**: `android:usesCleartextTraffic="true"` is set in AndroidManifest.xml
- **libsodium calls**: Do not use named parameters — the bindings don't support them
- **KMP commonMain**: No `System.currentTimeMillis()` — use `kotlinx.datetime.Clock.System`
