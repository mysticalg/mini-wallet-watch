package com.walletwatch.mini

import com.walletwatch.mini.data.model.Chain
import com.walletwatch.mini.data.model.Wallet
import com.walletwatch.mini.data.repository.WalletRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRepositoryTest {

    @Test
    fun `blank api key returns demo balances for all chains`() = runBlocking {
        val repository = WalletRepository(alchemyApiKey = "")

        val snapshot = repository.fetchWalletSnapshot(
            Wallet(label = "Test", address = "0x1234567890abcdef")
        )

        assertEquals(Chain.entries.size, snapshot.chainBalances.size)
        assertTrue(snapshot.chainBalances.all { it.valueInUsd >= java.math.BigDecimal.ZERO })
    }
}
