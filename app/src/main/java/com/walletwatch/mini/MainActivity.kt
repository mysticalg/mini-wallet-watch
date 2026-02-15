package com.walletwatch.mini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walletwatch.mini.ui.WalletWatchScreen
import com.walletwatch.mini.ui.WalletWatchViewModel
import com.walletwatch.mini.ui.WalletWatchViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: WalletWatchViewModel = viewModel(factory = WalletWatchViewModelFactory())
                    WalletWatchScreen(viewModel = viewModel)
                }
            }
        }
    }
}
