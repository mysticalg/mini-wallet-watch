package com.walletwatch.mini.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.walletwatch.mini.BuildConfig
import com.walletwatch.mini.data.model.Currency
import com.walletwatch.mini.data.model.Wallet
import com.walletwatch.mini.data.model.WalletSnapshot
import com.walletwatch.mini.data.repository.WalletRepository
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
    val currency: Currency = Currency.USD,
    val error: String? = null,
    val usesDemoData: Boolean = BuildConfig.ALCHEMY_API_KEY.isBlank()
)

class WalletWatchViewModel(
    private val repository: WalletRepository = WalletRepository(BuildConfig.ALCHEMY_API_KEY)
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletWatchUiState())
    val uiState: StateFlow<WalletWatchUiState> = _uiState.asStateFlow()

    init {
        startAutoRefresh()
    }

    fun addWallet(rawAddress: String) {
        val normalized = rawAddress.trim()
        if (normalized.isBlank()) return

        _uiState.update { state ->
            if (state.wallets.any { it.address.equals(normalized, ignoreCase = true) }) {
                state.copy(error = "Wallet already added")
            } else {
                val label = "Wallet ${state.wallets.size + 1}"
                state.copy(wallets = state.wallets + Wallet(label, normalized), error = null)
            }
        }

        refreshBalances()
    }

    fun setCurrency(currency: Currency) {
        _uiState.update { it.copy(currency = currency) }
    }

    fun refreshBalances() {
        val wallets = _uiState.value.wallets
        if (wallets.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                wallets.map { wallet -> repository.fetchWalletSnapshot(wallet) }
            }.onSuccess { snapshots ->
                _uiState.update { it.copy(isLoading = false, snapshots = snapshots) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Failed to fetch balances")
                }
            }
        }
    }

    fun displayValue(usdValue: BigDecimal): String {
        val converted = when (uiState.value.currency) {
            Currency.USD -> usdValue
            Currency.GBP -> usdValue.multiply(USD_TO_GBP)
        }.setScale(2, RoundingMode.HALF_UP)

        return "${uiState.value.currency.symbol}$converted"
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                refreshBalances()
            }
        }
    }

    companion object {
        private const val AUTO_REFRESH_MS = 60_000L
        private val USD_TO_GBP = BigDecimal("0.79")
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
