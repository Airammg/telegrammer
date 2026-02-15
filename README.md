# Telegrammer

A Telegram-like 1-on-1 encrypted chat application built with a full Kotlin stack.

## Architecture

- **Server** — Ktor backend with MongoDB, JWT auth, WebSocket messaging
- **Shared** — Kotlin Multiplatform module (Android + iOS) with API clients, crypto, local DB
- **Android App** — Jetpack Compose UI

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Ktor (Netty), MongoDB, JWT (java-jwt) |
| Shared | KMP, Ktor Client, SQLDelight, libsodium-bindings |
| Android | Jetpack Compose, Material3 |
| Crypto | Signal Protocol (X3DH + Double Ratchet), XChaCha20-Poly1305 |
| Auth | Phone + OTP, JWT access/refresh tokens |
| Real-time | WebSocket with JSON envelope protocol |

## Project Structure

```
telegrammer/
  server/       Ktor backend (~20 source files)
  shared/       KMP shared module (API, crypto, DB, models)
  androidApp/   Android Compose application
```

## Prerequisites

- JDK 17
- Docker & Docker Compose (for MongoDB)
- Android SDK (API 35)

## Running

### 1. Start MongoDB

```bash
cd server
docker compose up -d
```

### 2. Start the Server

```bash
./gradlew :server:run
```

Server runs at `http://localhost:8080`. Health check: `GET /health`.

OTP codes are printed to the server console (dev mode — no real SMS).

### 3. Build & Run Android App

Open in Android Studio, sync Gradle, run on device/emulator.

Or from CLI:

```bash
./gradlew :androidApp:assembleDebug
adb install -t androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 4. Network Configuration

The app connects to the server via local network IP. Update the IP in:

- `shared/.../api/ApiClient.kt` — `apiHost`
- `shared/.../ws/ChatSocket.kt` — `wsHost`

## API Overview

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/auth/request-otp` | No | Send OTP to phone |
| POST | `/auth/verify-otp` | No | Verify OTP, get JWT |
| POST | `/auth/refresh` | No | Refresh access token |
| GET | `/users/me` | Yes | Get own profile |
| PUT | `/users/me` | Yes | Update profile |
| POST | `/contacts/resolve` | Yes | Find users by phone |
| POST | `/chats` | Yes | Create/get chat |
| GET | `/chats/{id}/messages` | Yes | Message history |
| POST | `/keys/bundle` | Yes | Upload prekey bundle |
| GET | `/keys/bundle/{userId}` | Yes | Fetch prekey bundle |

## WebSocket Protocol

Connect: `ws://host:8080/ws?token=<JWT>`

Messages use `{ "type": "...", "payload": {...} }` envelope format.

| Type | Direction | Purpose |
|------|-----------|---------|
| `message.send` | C->S | Send encrypted message |
| `message.new` | S->C | Deliver message |
| `message.ack` | S->C | Server confirms storage |
| `message.delivered` | C->S / S->C | Delivery receipt |
| `receipt.read` | C->S / S->C | Read receipt |
| `typing` | C->S / S->C | Typing indicator |
| `presence` | S->C | Online/offline |

## E2E Encryption

- **X3DH** key agreement for session establishment
- **Double Ratchet** for forward-secret message encryption
- **XChaCha20-Poly1305** for symmetric encryption
- Server is a dumb relay — never sees plaintext

## Current Status

### Working
- Server: full REST API, WebSocket, MongoDB, JWT auth, OTP flow
- Android: auth flow (phone + OTP), conversation list, contacts search, chat UI
- WebSocket: real-time connection, reconnection

### In Progress
- E2E encryption: crypto primitives implemented, needs key upload/exchange integration
- Delivery/read receipts UI
- Conversation list updates from WebSocket events

### Not Started
- iOS app
- Docker production deployment
- Profile editing UI
- Media messages
