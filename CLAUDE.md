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
  - `api/` — HTTP API clients (AuthApi, ContactApi, ChatApi, KeyApi, UserApi)
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

Steps 1-9 complete. Next up: step 10.

### Step 1 — Initialize libsodium on app startup — DONE
`LibsodiumInitializer.initialize()` called in `TelegrammerApp.onCreate()`.

### Step 2 — Generate identity keys on registration — DONE
After OTP verification, `AuthRepository.verifyOtp()` generates identity key pair, signed prekey, and one-time prekeys via `KeyManager`. Also generates on app startup if logged in but no keys exist.

### Step 3 — Upload prekey bundle to server — DONE
Bundle uploaded via `POST /keys/bundle` after key generation. Test user bundle uploaded via pynacl script.

### Step 4 — Wire up the full encrypt/send flow — DONE
`CryptoSession.encrypt()` fetches recipient's bundle, does X3DH, initializes Double Ratchet, encrypts with XChaCha20-Poly1305. Fixed `@Serializable` on `MessageHeader`. Added error handling in `ChatViewModel`.

### Step 5 — Test end-to-end messaging — DONE
Encrypted messages sent successfully from phone to test user. Send-side verified working.

### Step 6 — Conversation list refresh — DONE
Added `syncConversations()` to `ChatRepository` — fetches chats from server, resolves user info via `UserApi`, upserts into local DB. `sendMessage()` now calls `createOrGetConversation()` to ensure the row exists before updating. `ConversationListViewModel` syncs on init.

### Step 7 — Delivery/read receipts UI — DONE
Check marks on messages: single = sent, double = delivered, blue double = read. `localId` field added to WsSendMessage/WsMessageAck to match server acks to local messages (UUID vs MongoDB ObjectId). Blue color `#34B7F1` for read status.

### Step 8 — Real SMS integration (Twilio) — DONE
Added `TwilioSmsGateway` alongside `ConsoleSmsGateway`. Config selects gateway via `app.sms.provider` ("twilio" or "console"). Twilio credentials via env vars: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`. Set `SMS_PROVIDER=twilio` to activate.

### Step 9 — Profile editing UI — DONE
Added `updateProfile()` to `UserApi` calling `PUT /users/me`. New `ProfileEditScreen` with initials avatar, read-only phone number, editable display name, and save button. Person icon in ConversationListScreen top bar navigates to profile edit. Auto-navigates back on save.

### Step 10 — iOS app (NEXT)
Build the iOS UI layer on top of the existing KMP shared module.

### Step 11 — Docker production deployment
Containerize server + MongoDB with proper config, TLS, and environment variables.

## Testing Setup

To test messaging, a second user is needed. Create one via curl:
```bash
# Request OTP
curl -s http://localhost:8080/auth/request-otp -H "Content-Type: application/json" -d '{"phoneNumber": "+14155551234"}'
# Check server console for OTP code, then verify
curl -s http://localhost:8080/auth/verify-otp -H "Content-Type: application/json" -d '{"phoneNumber": "+14155551234", "code": "<OTP>"}'
```
The test user needs a prekey bundle uploaded — use pynacl to generate Ed25519 identity + X25519 prekeys and POST to `/keys/bundle`.

## Common Issues

- **Gradle needs JDK 17**: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
- **APK install on phone**: Use `adb install -t` flag for debug builds
- **Cleartext HTTP**: `android:usesCleartextTraffic="true"` is set in AndroidManifest.xml
- **libsodium calls**: Do not use named parameters — the bindings don't support them
- **KMP commonMain**: No `System.currentTimeMillis()` — use `kotlinx.datetime.Clock.System`
