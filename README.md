# Wallet Watch Mini

Wallet Watch Mini is a lightweight Android watch-only portfolio app for quickly tracking wallet balances across major chains, with a UX focused on speed and simplicity.

## Implemented MVP+

- ✅ Top 5 chains configured: Ethereum, BSC, Solana, Arbitrum, Optimism
- ✅ Alchemy JSON-RPC integration scaffold (real mode with API key)
- ✅ Easy wallet input helpers:
  - Paste from clipboard
  - QR code scanner for wallet addresses
  - Copy wallet addresses from saved cards
- ✅ Multi-currency display with live FX rates from free API (`frankfurter.app`)
- ✅ Currency selector from major/global currencies returned by API (USD, GBP, EUR, and more)
- ✅ App unlock with phone authentication (biometric and/or device credentials: fingerprint/face/PIN/pattern)
- ✅ Minimal Compose UI, auto-refresh every 60 seconds
- ✅ No login, no wallet connection, no trading

## Tech stack

- Kotlin + Jetpack Compose
- ViewModel + StateFlow
- Retrofit + Kotlinx Serialization
- Coroutines
- ZXing embedded scanner
- Android Biometric API

## Run locally

1. Ensure Android Studio (or Android SDK/Gradle toolchain) is installed.
2. Add an Alchemy key to `~/.gradle/gradle.properties` or project `gradle.properties`:

```properties
ALCHEMY_API_KEY=your_alchemy_key_here
```

3. Build and test:

```bash
./gradlew test
./gradlew assembleDebug
```

## APIs used

- **Alchemy** for on-chain wallet balances
- **Frankfurter** (`https://api.frankfurter.app`) for free USD→other currency FX rates

## Demo mode fallback

If `ALCHEMY_API_KEY` is missing, the app runs in demo mode and generates deterministic sample balances so the UI remains usable out of the box.

## Project structure

- `app/src/main/java/com/walletwatch/mini/MainActivity.kt` – entry point + unlock flow
- `app/src/main/java/com/walletwatch/mini/ui/` – Compose screen + ViewModel
- `app/src/main/java/com/walletwatch/mini/data/model/` – chain/wallet models
- `app/src/main/java/com/walletwatch/mini/data/remote/` – Alchemy + FX API interfaces
- `app/src/main/java/com/walletwatch/mini/data/repository/` – balance + currency repositories

## GitHub Action: Play Store-ready release bundle

A workflow has been added at `.github/workflows/android-release.yml` to build a **signed** release App Bundle (`.aab`) that you can upload directly to Google Play Console.

### How it works

- Runs manually (`workflow_dispatch`) or automatically for tags like `v1.2.3`.
- Builds `bundleRelease`.
- Uploads `app-release.aab` as a workflow artifact.
- Also uploads ProGuard/R8 mapping file when available.

### Required GitHub repository secrets

- `ANDROID_KEYSTORE_BASE64`: Base64 of your upload keystore file (`.jks`)
- `ANDROID_KEY_ALIAS`: key alias in the keystore
- `ANDROID_KEYSTORE_PASSWORD`: keystore password
- `ANDROID_KEY_PASSWORD`: key password
- `ALCHEMY_API_KEY` (optional but recommended)

### One-time keystore base64 command

```bash
base64 -w 0 your-upload-key.jks
```

Copy that output into the `ANDROID_KEYSTORE_BASE64` secret.

### Download and upload

1. Open **Actions** in GitHub.
2. Run **Build signed Android release bundle** (or push a `v*` tag).
3. Download the `app-release-bundle` artifact.
4. Upload `app-release.aab` to Google Play Console.
