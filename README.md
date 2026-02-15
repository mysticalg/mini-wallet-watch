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
