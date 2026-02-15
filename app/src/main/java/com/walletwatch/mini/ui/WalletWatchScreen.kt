package com.walletwatch.mini.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletWatchScreen(viewModel: WalletWatchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var addressInput by remember { mutableStateOf("") }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var dropThresholdInput by remember { mutableStateOf(state.alertDropPercentThreshold.toString()) }
    var lowBalanceInput by remember { mutableStateOf(state.alertLowBalanceUsdThreshold.toPlainString()) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scanned ->
            addressInput = scanned.trim()
        }
    }

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
                    text = "Demo mode: add ALCHEMY_API_KEY to fetch live balances.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = { Text("Wallet address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    viewModel.addWallet(addressInput)
                    addressInput = ""
                }) { Text("Add") }

                TextButton(onClick = {
                    val pasted = clipboardManager.getText()?.text.orEmpty()
                    if (pasted.isNotBlank()) addressInput = pasted.trim()
                }) { Text("Paste") }

                TextButton(onClick = {
                    val options = ScanOptions().apply {
                        setPrompt("Scan wallet QR")
                        setBeepEnabled(false)
                        setOrientationLocked(true)
                    }
                    scanLauncher.launch(options)
                }) { Text("Scan QR") }

                TextButton(onClick = { viewModel.refreshAll() }) { Text("Refresh") }
            }

            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded }
            ) {
                OutlinedTextField(
                    value = state.selectedCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    state.availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                viewModel.setCurrency(currency)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            if (state.alerts.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Alerts", fontWeight = FontWeight.SemiBold)
                        state.alerts.forEach { alert ->
                            Text("• ${alert.walletLabel}: ${alert.message}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alert settings", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dropThresholdInput,
                            onValueChange = { dropThresholdInput = it },
                            label = { Text("Drop %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lowBalanceInput,
                            onValueChange = { lowBalanceInput = it },
                            label = { Text("Low bal (USD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(onClick = {
                        viewModel.setAlertSettings(dropThresholdInput.toIntOrNull() ?: 10, lowBalanceInput)
                    }) { Text("Apply alert thresholds") }
                }
            }

            if (state.chainTotals.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Multi-chain total value view", fontWeight = FontWeight.SemiBold)
                        state.chainTotals.forEach { total ->
                            Text("${total.chainName}: ${viewModel.displayValue(total.usdValue)}")
                        }
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.snapshots) { snapshot ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(snapshot.wallet.label, fontWeight = FontWeight.Bold)
                                Row {
                                    TextButton(onClick = { viewModel.toggleFavorite(snapshot.wallet.address) }) {
                                        val isFavorite = state.favoriteAddresses.contains(snapshot.wallet.address)
                                        Text(if (isFavorite) "★ Favorite" else "☆ Favorite")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = { viewModel.removeWallet(snapshot.wallet.address) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                            Text(snapshot.wallet.address, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(snapshot.wallet.address))
                                    }
                                ) { Text("Copy") }
                            }
                            Text(
                                text = "Total: ${viewModel.displayValue(snapshot.totalUsd)}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mini chart: ${viewModel.miniChart(snapshot.wallet.address)}",
                                style = MaterialTheme.typography.bodySmall
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
