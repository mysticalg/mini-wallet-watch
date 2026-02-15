package com.walletwatch.mini.data.model

import java.math.BigDecimal

enum class Chain(
    val displayName: String,
    val rpcBaseUrl: String
) {
    ETHEREUM("Ethereum", "https://eth-mainnet.g.alchemy.com/v2/"),
    BSC("BSC", "https://bnb-mainnet.g.alchemy.com/v2/"),
    SOLANA("Solana", "https://solana-mainnet.g.alchemy.com/v2/"),
    ARBITRUM("Arbitrum", "https://arb-mainnet.g.alchemy.com/v2/"),
    OPTIMISM("Optimism", "https://opt-mainnet.g.alchemy.com/v2/")
}

data class Wallet(
    val label: String,
    val address: String
)

enum class Currency(val symbol: String) {
    USD("$"),
    GBP("£")
}

data class ChainBalance(
    val chain: Chain,
    val nativeBalance: BigDecimal,
    val valueInUsd: BigDecimal
)

data class WalletSnapshot(
    val wallet: Wallet,
    val chainBalances: List<ChainBalance>
) {
    val totalUsd: BigDecimal = chainBalances.fold(BigDecimal.ZERO) { acc, item ->
        acc + item.valueInUsd
    }
}
