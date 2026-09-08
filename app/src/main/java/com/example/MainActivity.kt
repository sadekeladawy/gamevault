package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.theme.GameVaultTheme
import com.example.ui.viewmodel.GameVaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable high refresh rate display mode (90Hz / 120Hz+) if supported by hardware
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                val display = display
                if (display != null) {
                    val supportedModes = display.supportedModes
                    val maxMode = supportedModes.maxByOrNull { it.refreshRate }
                    if (maxMode != null && maxMode.modeId != 0) {
                        val params = window.attributes
                        params.preferredDisplayModeId = maxMode.modeId
                        window.attributes = params
                    }
                }
            }
        }

        setContent {
            val viewModel: GameVaultViewModel = viewModel()

            GameVaultTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

