# Telegrammer

A Telegram-like 1-on-1 encrypted chat application built with a full Kotlin stack.

## Architecture

- **Server** — Ktor backend with MongoDB, JWT auth, WebSocket messaging
- **Shared** — Kotlin Multiplatform module (Android + iOS) with API clients, crypto, local DB
- **Android App** — Jetpack Compose UI
- **iOS App** — SwiftUI, powered by the KMP shared framework

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Ktor (Netty), MongoDB, JWT (java-jwt) |
| Shared | KMP, Ktor Client, SQLDelight, libsodium-bindings |
| Android | Jetpack Compose, Material3 |
| iOS | SwiftUI |
| Crypto | Signal Protocol (X3DH + Double Ratchet), XChaCha20-Poly1305 |
| Auth | Phone + OTP, JWT access/refresh tokens |
| Real-time | WebSocket with JSON envelope protocol |
| Infra | Docker Compose, nginx, Let's Encrypt TLS |

## Project Structure

```
telegrammer/
  server/       Ktor backend (~20 source files)
  shared/       KMP shared module (API, crypto, DB, models)
  androidApp/   Android Compose application
  iosApp/       iOS SwiftUI application
```

## Prerequisites

- JDK 17
- Docker & Docker Compose
- Android SDK (API 35)
- Xcode.app (iOS builds only)

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

OTP codes are printed to the server console by default. To use Twilio for real SMS:

```bash
export SMS_PROVIDER=twilio
export TWILIO_ACCOUNT_SID=your_sid
export TWILIO_AUTH_TOKEN=your_token
export TWILIO_FROM_NUMBER=+1234567890
./gradlew :server:run
```

### 3. Build & Run Android App

Open in Android Studio, sync Gradle, run on device/emulator.

Or from CLI:

```bash
./gradlew :androidApp:assembleDebug
adb install -t androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 4. Build & Run iOS App

Requires Xcode.app installed.

```bash
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

### 5. Network Configuration

Set the server host in `local.properties` (Android) or `iosApp/Config.xcconfig` (iOS):

```
# local.properties
SERVER_HOST=192.168.1.x
SERVER_PORT=8080
```

## Production Deployment

Uses `docker-compose.prod.yml` with nginx TLS termination and automatic Let's Encrypt certificate renewal.

```bash
cd server
cp .env.example .env          # fill in MONGO_USER, MONGO_PASSWORD, JWT_SECRET, DOMAIN, etc.
# replace DOMAIN_PLACEHOLDER in nginx/nginx.conf with your actual domain
./init-letsencrypt.sh chat.example.com your@email.com   # first-time cert only
docker compose -f docker-compose.prod.yml up -d
```

Verify:
```bash
curl https://<domain>/health   # 200 OK
curl http://<domain>/health    # 301 → HTTPS
```

The certbot container renews certificates automatically every 12 hours. MongoDB is on an internal Docker network with no exposed ports.

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

All 11 planned steps complete.

- Server: full REST API, WebSocket, MongoDB, JWT auth, OTP flow
- Android: auth flow, conversation list, contacts search, E2E chat, profile editing
- iOS: SwiftUI app mirroring Android, powered by KMP shared module
- E2E encryption: X3DH + Double Ratchet + XChaCha20-Poly1305, server never sees plaintext
- Production: Docker Compose with nginx TLS and auto-renewing Let's Encrypt certs

## Roadmap

| # | Step | Status |
|---|------|--------|
| 1 | Initialize libsodium on app startup | Done |
| 2 | Generate identity keys on registration | Done |
| 3 | Upload prekey bundle to server | Done |
| 4 | Wire up full encrypt/send flow | Done |
| 5 | Test end-to-end encrypted messaging | Done |
| 6 | Conversation list real-time refresh | Done |
| 7 | Delivery/read receipts UI (check marks) | Done |
| 8 | Real SMS integration (Twilio) | Done |
| 9 | Profile editing UI | Done |
| 10 | iOS app | Done |
| 11 | Docker production deployment | Done |
