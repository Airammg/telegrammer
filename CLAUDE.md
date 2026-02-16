# CLAUDE.md

## Project Overview

Telegrammer is a Telegram-like 1-on-1 encrypted chat app. Ktor server + KMP shared module + Android Compose UI + iOS SwiftUI.

## Principles

### No third-party libraries
Use only platform/framework essentials. Do NOT add libraries for things that can be done with standard APIs or simple custom code. Acceptable dependencies are:
- **Framework:** Ktor (server + client), Jetpack Compose, SwiftUI — these ARE the platform
- **Kotlin ecosystem:** KotlinX Serialization, KotlinX Coroutines, KotlinX DateTime — official Kotlin libraries
- **Database:** MongoDB driver (server), SQLDelight (client) — direct database access, no ORMs on top
- **Crypto:** libsodium bindings — required for E2E encryption primitives (no pure-Kotlin alternative)
- **Logging:** Logback (server only) — SLF4J standard

Do NOT add: DI frameworks (Koin, Hilt, Dagger), HTTP wrappers on top of Ktor, image loading libraries, analytics SDKs, crash reporters, UI component libraries, state management libraries (Redux, MVI), testing frameworks beyond what Kotlin/Swift provide. If a problem can be solved in <50 lines of custom code, write it yourself.

For iOS: zero Swift dependencies. Everything comes through the KMP shared framework. No CocoaPods, no SPM packages.

### No hardcoded config
All environment-specific values (hosts, ports, secrets, feature flags) must come from configuration, never from source code literals.
- **Server:** HOCON `application.conf` with `${?ENV_VAR}` overrides (already done)
- **Android:** `BuildConfig` fields generated from `build.gradle.kts`, reading from `local.properties` or env vars
- **iOS:** A `Config.xcconfig` file (not committed) referenced in the Xcode build, exposed via Info.plist keys
- **Shared KMP module:** Constructor parameters with NO default values for hosts/ports — force callers to supply config explicitly

Current violations to fix:
- `ApiClient.kt` has `apiHost = "192.168.1.129"` as default — remove default, make it a required param
- `ChatSocket.kt` has `wsHost = "192.168.1.129"` as default — remove default, make it a required param
- `iosApp/AppDependencies.swift` has IPs inline — read from Info.plist / xcconfig instead

### Best practices
- **Validate at boundaries only** — trust internal code, validate user input and external API responses
- **No premature abstraction** — three similar lines > one premature helper
- **Config via environment** — secrets and hosts never in source code, use env vars / config files
- **Fail fast in dev, gracefully in prod** — clear error messages, no silent swallowing of important errors
- **Minimal surface area** — each class/function does one thing, public API is as small as possible

## Build & Run

```bash
# Start MongoDB
cd server && docker compose up -d

# Run server (JDK 17 required)
./gradlew :server:run

# Build Android APK (set SERVER_HOST in local.properties or env)
./gradlew :androidApp:assembleDebug

# Build iOS (requires Xcode.app)
cd iosApp && xcodegen generate && open iosApp.xcodeproj
# Or from command line:
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build
```

## Key Directories

- `server/src/main/kotlin/com/telegrammer/server/` — Backend (routes, repositories, models, WebSocket)
- `shared/src/commonMain/kotlin/com/telegrammer/shared/` — KMP shared code
  - `api/` — HTTP API clients (AuthApi, ContactApi, ChatApi, KeyApi, UserApi)
  - `crypto/` — E2E encryption (KeyManager, X3DH, Ratchet, CryptoSession)
  - `ws/` — WebSocket client (ChatSocket, WsMessage types)
  - `repository/` — Data layer (AuthRepository, ChatRepository, ContactRepository)
  - `db/` — SQLDelight DAOs (MessageDb, ConversationDb)
  - `platform/` — expect/actual (SecureStorage, DriverFactory, FlowWrapper)
- `androidApp/src/main/kotlin/com/telegrammer/android/` — Android UI
  - `navigation/NavGraph.kt` — All navigation routes
  - `AppDependencies.kt` — Manual DI wiring
- `iosApp/iosApp/` — iOS SwiftUI UI
  - `Navigation/AppRouter.swift` — NavigationStack routing
  - `AppDependencies.swift` — Manual DI wiring (mirrors Android)
  - `Views/` — Auth, Chat, Contacts, Profile, Components
  - `FlowObserver.swift` — FlowWrapper→@Published bridge
  - `KMPHelpers.swift` — KMP suspend→async/await wrappers

## Architecture Decisions

- **No DI framework** — Manual constructor injection via `AppDependencies`
- **No use-case classes** — ViewModels call repositories directly
- **No mapper classes** — Data classes use companion `from()` methods
- **Server stores only ciphertext** — E2E encrypted, never sees plaintext
- Dependencies exposed from shared to androidApp via `api()` not `implementation()`

## Configuration

### Server (`server/src/main/resources/application.conf`)
HOCON format. Every value has a dev default + env var override:
- `PORT` — server port (default 8080)
- `MONGODB_URI` — MongoDB connection string
- `JWT_SECRET` — JWT signing secret (**must override in production**)
- `SMS_PROVIDER` — "console" or "twilio"
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` — Twilio credentials

### Android (`androidApp/build.gradle.kts` → `BuildConfig`)
Server host/port injected at build time via `BuildConfig` fields. Source: `local.properties` or env vars.

### iOS (`iosApp/Config.xcconfig` → Info.plist)
Server host/port in xcconfig file (git-ignored), read at runtime from `Bundle.main.infoDictionary`.

### Shared module
`ApiClient` and `ChatSocket` take host/port as **required** constructor parameters (no defaults). Each platform's `AppDependencies` supplies the values from its own config mechanism.

## Implementation Roadmap

Steps 1-10 complete. Next up: step 11.

### Step 1 — Initialize libsodium on app startup — DONE
### Step 2 — Generate identity keys on registration — DONE
### Step 3 — Upload prekey bundle to server — DONE
### Step 4 — Wire up the full encrypt/send flow — DONE
### Step 5 — Test end-to-end messaging — DONE
### Step 6 — Conversation list refresh — DONE
### Step 7 — Delivery/read receipts UI — DONE
### Step 8 — Real SMS integration (Twilio) — DONE
### Step 9 — Profile editing UI — DONE
### Step 10 — iOS app — DONE
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

- **Gradle needs JDK 17**: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"`
- **APK install on phone**: Use `adb install -t` flag for debug builds
- **Cleartext HTTP (Android)**: `android:usesCleartextTraffic="true"` is set in AndroidManifest.xml
- **Cleartext HTTP (iOS)**: `NSAllowsArbitraryLoads` is set in Info.plist
- **libsodium calls**: Do not use named parameters — the bindings don't support them
- **KMP commonMain**: No `System.currentTimeMillis()` — use `kotlinx.datetime.Clock.System`
- **iOS build requires Xcode.app**: CLI tools alone won't work — run `xcode-select -s /Applications/Xcode.app`
- **iOS xcodegen**: Install via `brew install xcodegen`, then `cd iosApp && xcodegen generate`
- **KMP enums in Swift**: Exported as Obj-C classes, use `==` comparison not Swift `switch`
