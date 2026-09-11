package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryRed

/**
 * Reusable, compact, theme-aligned "Ask AI" button component.
 * Provides a clean entry point across Dashboard, Library, Game Details, Franchise, and Search.
 */
@Composable
fun AskAiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ask AI",
    compact: Boolean = false,
    iconOnly: Boolean = false,
    outlined: Boolean = false
) {
    if (iconOnly) {
        IconButton(
            onClick = onClick,
            modifier = modifier.testTag("ask_ai_icon_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = label,
                tint = PrimaryRed,
                modifier = Modifier.size(22.dp)
            )
        }
        return
    }

    val shape = RoundedCornerShape(12.dp)

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .testTag("ask_ai_outlined_button")
                .then(if (compact) Modifier.height(34.dp) else Modifier.height(42.dp)),
            shape = shape,
            border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.6f)),
            contentPadding = if (compact) PaddingValues(horizontal = 10.dp, vertical = 0.dp) else PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryRed
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryRed,
                    modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryRed
                )
            }
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .testTag("ask_ai_tonal_button")
                .then(if (compact) Modifier.height(34.dp) else Modifier.height(42.dp)),
            shape = shape,
            contentPadding = if (compact) PaddingValues(horizontal = 10.dp, vertical = 0.dp) else PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = PrimaryRed.copy(alpha = 0.15f),
                contentColor = PrimaryRed
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryRed,
                    modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryRed
                )
            }
        }
    }
}
