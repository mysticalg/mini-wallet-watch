package com.walletwatch.mini.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletwatch.mini.data.model.Currency

@Composable
fun WalletWatchScreen(viewModel: WalletWatchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addressInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wallet Watch Mini") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.usesDemoData) {
                Text(
                    text = "Demo mode: add ALCHEMY_API_KEY in local.properties to fetch live balances.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = { Text("Wallet address") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.addWallet(addressInput)
                    addressInput = ""
                }) {
                    Text("Add wallet")
                }
                Button(onClick = { viewModel.refreshBalances() }) {
                    Text("Refresh")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Currency.entries.forEach { currency ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.currency == currency,
                            onClick = { viewModel.setCurrency(currency) }
                        )
                        Text(currency.name)
                    }
                }
            }

            state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.snapshots) { snapshot ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(snapshot.wallet.label, fontWeight = FontWeight.Bold)
                            Text(snapshot.wallet.address, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Total: ${viewModel.displayValue(snapshot.totalUsd)}",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            snapshot.chainBalances.forEach { chainBalance ->
                                Text(
                                    text = "${chainBalance.chain.displayName}: ${chainBalance.nativeBalance} ≈ ${viewModel.displayValue(chainBalance.valueInUsd)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
