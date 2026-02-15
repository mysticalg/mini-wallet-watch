# Wallet Watch Mini

Wallet Watch Mini is a lightweight Android watch-only portfolio app for quickly tracking wallet balances across major chains.

## Implemented MVP

- ✅ Top 5 chains configured: Ethereum, BSC, Solana, Arbitrum, Optimism
- ✅ Alchemy JSON-RPC integration scaffold (real mode with API key)
- ✅ USD / GBP currency switching
- ✅ Minimal Compose UI
- ✅ Add wallet address
- ✅ Fetch balances
- ✅ Auto-refresh every 60 seconds
- ✅ Lightweight multi-wallet portfolio view
- ✅ No login, no wallet connection, no trading

## Tech stack

- Kotlin + Jetpack Compose
- ViewModel + StateFlow
- Retrofit + Kotlinx Serialization
- Coroutines

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

## Demo mode fallback

If `ALCHEMY_API_KEY` is missing, the app runs in demo mode and generates deterministic sample balances so the UI remains usable out of the box.

## Project structure

- `app/src/main/java/com/walletwatch/mini/MainActivity.kt` – app entry point
- `app/src/main/java/com/walletwatch/mini/ui/` – Compose screen + ViewModel
- `app/src/main/java/com/walletwatch/mini/data/model/` – chain/wallet models
- `app/src/main/java/com/walletwatch/mini/data/remote/` – Alchemy JSON-RPC interface
- `app/src/main/java/com/walletwatch/mini/data/repository/` – balance fetching + demo fallback
