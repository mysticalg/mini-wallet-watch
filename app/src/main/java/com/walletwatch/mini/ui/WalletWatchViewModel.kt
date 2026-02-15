package com.walletwatch.mini.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.walletwatch.mini.BuildConfig
import com.walletwatch.mini.data.model.Wallet
import com.walletwatch.mini.data.model.WalletSnapshot
import com.walletwatch.mini.data.repository.CurrencyRepository
import com.walletwatch.mini.data.repository.WalletRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

data class WalletAlert(
    val walletLabel: String,
    val message: String
)

data class ChainTotal(
    val chainName: String,
    val usdValue: BigDecimal
)

data class WalletWatchUiState(
    val wallets: List<Wallet> = emptyList(),
    val snapshots: List<WalletSnapshot> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCurrency: String = "USD",
    val availableCurrencies: List<String> = listOf("USD", "GBP", "EUR"),
    val usdRates: Map<String, Double> = mapOf("USD" to 1.0, "GBP" to 0.79, "EUR" to 0.92),
    val favoriteAddresses: Set<String> = emptySet(),
    val miniChartSeriesByWallet: Map<String, List<BigDecimal>> = emptyMap(),
    val alerts: List<WalletAlert> = emptyList(),
    val alertDropPercentThreshold: Int = 10,
    val alertLowBalanceUsdThreshold: BigDecimal = BigDecimal("1000"),
    val chainTotals: List<ChainTotal> = emptyList(),
    val error: String? = null,
    val usesDemoData: Boolean = BuildConfig.ALCHEMY_API_KEY.isBlank()
)

class WalletWatchViewModel(
    private val walletRepository: WalletRepository = WalletRepository(BuildConfig.ALCHEMY_API_KEY),
    private val currencyRepository: CurrencyRepository = CurrencyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletWatchUiState())
    val uiState: StateFlow<WalletWatchUiState> = _uiState.asStateFlow()

    init {
        refreshCurrencies()
        startAutoRefresh()
    }

    fun addWallet(rawAddress: String) {
        val normalized = rawAddress.trim()
        if (!isValidAddress(normalized)) {
            _uiState.update { it.copy(error = "Please enter a valid wallet address") }
            return
        }

        _uiState.update { state ->
            if (state.wallets.any { it.address.equals(normalized, ignoreCase = true) }) {
                state.copy(error = "Wallet already added")
            } else {
                val label = "Wallet ${state.wallets.size + 1}"
                state.copy(wallets = state.wallets + Wallet(label, normalized), error = null)
            }
        }

        refreshAll()
    }

    fun removeWallet(address: String) {
        _uiState.update { state ->
            val nextWallets = state.wallets.filterNot { it.address == address }
            val nextSnapshots = state.snapshots.filterNot { it.wallet.address == address }
            state.copy(
                wallets = nextWallets,
                snapshots = nextSnapshots,
                favoriteAddresses = state.favoriteAddresses - address,
                miniChartSeriesByWallet = state.miniChartSeriesByWallet - address,
                error = null
            )
        }
    }

    fun toggleFavorite(address: String) {
        _uiState.update { state ->
            val updated = if (state.favoriteAddresses.contains(address)) {
                state.favoriteAddresses - address
            } else {
                state.favoriteAddresses + address
            }
            state.copy(favoriteAddresses = updated)
        }
    }

    fun setAlertSettings(dropPercent: Int, lowBalanceUsd: String) {
        val parsedLowBalance = lowBalanceUsd.toBigDecimalOrNull()
        if (parsedLowBalance == null || parsedLowBalance < BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Enter a valid low-balance threshold") }
            return
        }

        _uiState.update {
            it.copy(
                alertDropPercentThreshold = dropPercent.coerceIn(1, 99),
                alertLowBalanceUsdThreshold = parsedLowBalance,
                error = null
            )
        }
    }

    fun setCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun refreshAll() {
        val wallets = _uiState.value.wallets
        if (wallets.isEmpty()) {
            refreshCurrencies()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                val balancesDeferred = async { wallets.map { walletRepository.fetchWalletSnapshot(it) } }
                val fxDeferred = async { currencyRepository.getUsdRates() }
                balancesDeferred.await() to fxDeferred.await()
            }.onSuccess { (snapshots, rates) ->
                val currencyList = rates.keys
                    .filter { it.length == 3 }
                    .sorted()
                val currentState = _uiState.value
                val nextSeries = buildNextMiniChartSeries(
                    current = currentState.miniChartSeriesByWallet,
                    snapshots = snapshots
                )
                val nextAlerts = buildAlerts(
                    previousSnapshots = currentState.snapshots,
                    nextSnapshots = snapshots,
                    dropPercentThreshold = currentState.alertDropPercentThreshold,
                    lowBalanceThresholdUsd = currentState.alertLowBalanceUsdThreshold
                )
                val chainTotals = buildChainTotals(snapshots)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snapshots = snapshots.sortedWith(
                            compareByDescending<WalletSnapshot> { snap ->
                                it.favoriteAddresses.contains(snap.wallet.address)
                            }.thenByDescending { snap -> snap.totalUsd }
                        ),
                        usdRates = rates,
                        availableCurrencies = currencyList,
                        miniChartSeriesByWallet = nextSeries,
                        alerts = nextAlerts,
                        chainTotals = chainTotals
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Failed to refresh data")
                }
            }
        }
    }

    fun refreshCurrencies() {
        viewModelScope.launch {
            runCatching { currencyRepository.getUsdRates() }
                .onSuccess { rates ->
                    val currencyList = rates.keys.filter { it.length == 3 }.sorted()
                    _uiState.update {
                        it.copy(
                            usdRates = rates,
                            availableCurrencies = currencyList
                        )
                    }
                }
                .onFailure {
                    // Keep fallback rates silently.
                }
        }
    }

    fun displayValue(usdValue: BigDecimal): String {
        val selected = uiState.value.selectedCurrency
        val rate = uiState.value.usdRates[selected]?.toBigDecimal() ?: BigDecimal.ONE
        val converted = usdValue.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return "$selected $converted"
    }

    fun miniChart(address: String): String {
        val points = _uiState.value.miniChartSeriesByWallet[address].orEmpty()
        if (points.size < 2) return "-"
        val min = points.minOrNull() ?: return "-"
        val max = points.maxOrNull() ?: return "-"
        if (min == max) return "▁".repeat(points.size)
        val ticks = "▁▂▃▄▅▆▇█"
        return points.joinToString(separator = "") { point ->
            val ratio = point.subtract(min)
                .divide(max.subtract(min), 4, RoundingMode.HALF_UP)
                .toDouble()
            val index = (ratio * (ticks.lastIndex)).roundToInt().coerceIn(0, ticks.lastIndex)
            ticks[index].toString()
        }
    }

    private fun buildChainTotals(snapshots: List<WalletSnapshot>): List<ChainTotal> {
        return snapshots
            .flatMap { it.chainBalances }
            .groupBy { it.chain.displayName }
            .map { (chainName, balances) ->
                ChainTotal(
                    chainName = chainName,
                    usdValue = balances.fold(BigDecimal.ZERO) { acc, item -> acc + item.valueInUsd }
                )
            }
            .sortedByDescending { it.usdValue }
    }

    private fun buildNextMiniChartSeries(
        current: Map<String, List<BigDecimal>>,
        snapshots: List<WalletSnapshot>
    ): Map<String, List<BigDecimal>> {
        return snapshots.associate { snapshot ->
            val previous = current[snapshot.wallet.address].orEmpty()
            snapshot.wallet.address to (previous + snapshot.totalUsd).takeLast(12)
        }
    }

    private fun buildAlerts(
        previousSnapshots: List<WalletSnapshot>,
        nextSnapshots: List<WalletSnapshot>,
        dropPercentThreshold: Int,
        lowBalanceThresholdUsd: BigDecimal
    ): List<WalletAlert> {
        if (nextSnapshots.isEmpty()) return emptyList()
        val previousByAddress = previousSnapshots.associateBy { it.wallet.address }

        return nextSnapshots.mapNotNull { next ->
            val previous = previousByAddress[next.wallet.address]
            val lowBalanceAlert = if (next.totalUsd < lowBalanceThresholdUsd) {
                WalletAlert(
                    walletLabel = next.wallet.label,
                    message = "Balance below threshold (${displayValue(lowBalanceThresholdUsd)})."
                )
            } else {
                null
            }

            val dropAlert = if (previous != null && previous.totalUsd > BigDecimal.ZERO) {
                val ratio = previous.totalUsd.subtract(next.totalUsd)
                    .divide(previous.totalUsd, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("100"))
                if (ratio >= BigDecimal(dropPercentThreshold)) {
                    WalletAlert(
                        walletLabel = next.wallet.label,
                        message = "Value dropped ${ratio.setScale(1, RoundingMode.HALF_UP)}% since last refresh."
                    )
                } else {
                    null
                }
            } else {
                null
            }

            listOfNotNull(lowBalanceAlert, dropAlert).firstOrNull()?.let { first ->
                val combined = listOfNotNull(lowBalanceAlert?.message, dropAlert?.message).joinToString(" ")
                first.copy(message = combined)
            }
        }
    }

    private fun isValidAddress(address: String): Boolean {
        if (address.isBlank()) return false
        if (address.startsWith("0x") && address.length >= 10) return true
        return address.length in 32..64
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                refreshAll()
            }
        }
    }

    companion object {
        private const val AUTO_REFRESH_MS = 60_000L
    }
}

class WalletWatchViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletWatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletWatchViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
