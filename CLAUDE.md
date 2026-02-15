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

## Implementation Roadmap

Current progress: Steps 1-4 complete, step 5 partially complete.

### Step 1 — Initialize libsodium on app startup (NEXT)
Call `LibsodiumInitializer.initialize()` in `TelegrammerApp.onCreate()` so crypto primitives are available.

### Step 2 — Generate identity keys on registration
After OTP verification, generate identity key pair, signed prekey, and batch of one-time prekeys via `KeyManager`.

### Step 3 — Upload prekey bundle to server
Call `POST /keys/bundle` after key generation so other users can establish E2E sessions.

### Step 4 — Wire up the full encrypt/send flow
`CryptoSession.encrypt()` fetches recipient's bundle, performs X3DH key agreement, initializes Double Ratchet, and encrypts the message. Fix the crash on send.

### Step 5 — Test end-to-end messaging
Send a message between the phone and a test user (simulated via curl/WebSocket on the server side). Verify encrypt/decrypt round-trip works.

### Step 6 — Conversation list refresh
Update conversation list in real-time when messages arrive via WebSocket.

### Step 7 — Delivery/read receipts UI
Show check marks on messages (single = sent, double = delivered, blue = read).

### Step 8 — Real SMS integration
Replace `ConsoleSmsGateway` with a real SMS provider (e.g. Twilio). The `SmsGateway` interface is already in place — just needs a new implementation.

### Step 9 — Profile editing UI
Add screen to edit display name and avatar.

### Step 10 — iOS app
Build the iOS UI layer on top of the existing KMP shared module.

### Step 11 — Docker production deployment
Containerize server + MongoDB with proper config, TLS, and environment variables.

## Common Issues

- **Gradle needs JDK 17**: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- **APK install on phone**: Use `adb install -t` flag for debug builds
- **Cleartext HTTP**: `android:usesCleartextTraffic="true"` is set in AndroidManifest.xml
- **libsodium calls**: Do not use named parameters — the bindings don't support them
- **KMP commonMain**: No `System.currentTimeMillis()` — use `kotlinx.datetime.Clock.System`
