package com.walletwatch.mini.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.walletwatch.mini.data.model.Chain
import com.walletwatch.mini.data.model.ChainBalance
import com.walletwatch.mini.data.model.Wallet
import com.walletwatch.mini.data.model.WalletSnapshot
import com.walletwatch.mini.data.remote.AlchemyApi
import com.walletwatch.mini.data.remote.JsonRpcBalanceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import retrofit2.Retrofit

class WalletRepository(
    private val alchemyApiKey: String,
    private val nativeUsdPriceMap: Map<Chain, BigDecimal> = defaultNativePrices
) {

    suspend fun fetchWalletSnapshot(wallet: Wallet): WalletSnapshot = withContext(Dispatchers.IO) {
        val balances = if (alchemyApiKey.isBlank()) {
            demoBalances(wallet.address)
        } else {
            fetchRealBalances(wallet.address)
        }
        WalletSnapshot(wallet = wallet, chainBalances = balances)
    }

    private suspend fun fetchRealBalances(address: String): List<ChainBalance> = coroutineScope {
        Chain.entries.map { chain ->
            async {
                val api = apiForChain(chain)
                val response = api.getBalance(
                    JsonRpcBalanceRequest(params = listOf(address, "latest"))
                )

                val weiHex = response.result ?: "0x0"
                val nativeBalance = weiHexToNative(weiHex)
                val usdValue = nativeBalance.multiply(nativeUsdPriceMap.getValue(chain))
                ChainBalance(chain = chain, nativeBalance = nativeBalance, valueInUsd = usdValue)
            }
        }.awaitAll()
    }

    private fun demoBalances(address: String): List<ChainBalance> {
        val seed = address.takeLast(6).hashCode().toBigDecimal().abs() + BigDecimal.ONE
        return Chain.entries.mapIndexed { index, chain ->
            val native = seed
                .remainder(BigDecimal(index + 5))
                .divide(BigDecimal("3.5"), 4, RoundingMode.HALF_UP)
            val usd = native.multiply(nativeUsdPriceMap.getValue(chain))
            ChainBalance(chain = chain, nativeBalance = native, valueInUsd = usd)
        }
    }

    private fun apiForChain(chain: Chain): AlchemyApi {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        return Retrofit.Builder()
            .baseUrl(chain.rpcBaseUrl + alchemyApiKey)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(client)
            .build()
            .create(AlchemyApi::class.java)
    }

    private fun weiHexToNative(weiHex: String): BigDecimal {
        val wei = BigInteger(weiHex.removePrefix("0x"), 16)
        return wei.toBigDecimal().divide(WEI_DIVISOR, 8, RoundingMode.HALF_UP)
    }

    companion object {
        private val WEI_DIVISOR = BigDecimal("1000000000000000000")

        private val defaultNativePrices = mapOf(
            Chain.ETHEREUM to BigDecimal("2800"),
            Chain.BSC to BigDecimal("600"),
            Chain.SOLANA to BigDecimal("150"),
            Chain.ARBITRUM to BigDecimal("2800"),
            Chain.OPTIMISM to BigDecimal("2800")
        )
    }
}
