package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameVaultColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1012),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE2E2E8),
    onSecondary = Color(0xFF141416),
    secondaryContainer = Color(0xFF242429),
    onSecondaryContainer = TextPrimary,
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF27272C)
)

@Composable
fun GameVaultTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GameVaultColorScheme,
        typography = Typography,
        content = content
    )
}
