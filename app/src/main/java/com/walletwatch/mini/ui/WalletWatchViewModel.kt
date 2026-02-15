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

data class WalletWatchUiState(
    val wallets: List<Wallet> = emptyList(),
    val snapshots: List<WalletSnapshot> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCurrency: String = "USD",
    val availableCurrencies: List<String> = listOf("USD", "GBP", "EUR"),
    val usdRates: Map<String, Double> = mapOf("USD" to 1.0, "GBP" to 0.79, "EUR" to 0.92),
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
            state.copy(wallets = nextWallets, snapshots = nextSnapshots, error = null)
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snapshots = snapshots,
                        usdRates = rates,
                        availableCurrencies = currencyList
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
