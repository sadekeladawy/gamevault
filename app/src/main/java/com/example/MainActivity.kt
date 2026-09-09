package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.theme.GameVaultTheme
import com.example.ui.viewmodel.GameVaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // True edge-to-edge: content draws behind transparent system bars and the OS
        // compositor clips the window to each device's actual display shape (including
        // rounded corners), so the app's dark background reaches the real screen edges
        // instead of stopping short and exposing a mismatched rectangle underneath.
        enableEdgeToEdge()

        // Explicitly tell the window not to let the system reserve/inset space for the
        // system bars itself. Our Compose content is responsible for its own WindowInsets
        // handling from here on (see MainScreen's root composable), which is what lets the
        // dark background paint all the way to the device's physical rounded corners while
        // interactive content still avoids sitting under a status bar, nav bar, or cutout.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Prevent the OS from drawing its own translucent contrast scrim behind the
            // system bars, which can otherwise look like a squared-off rectangle sitting
            // on top of our rounded, edge-to-edge dark background.
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

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

