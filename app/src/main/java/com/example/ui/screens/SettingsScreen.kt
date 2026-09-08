package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    totalGamesCount: Int,
    currentUser: UserProfile?,
    isFirebaseConfigured: Boolean,
    isCloudSyncing: Boolean,
    lastSyncTimestamp: Long?,
    onOpenAuth: () -> Unit,
    onOpenProfile: () -> Unit,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onSignOut: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: () -> Unit,
    onResetSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Title Header
        item {
            Column {
                Text(
                    text = "Settings & Vault Data",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Cloud sync, account profile, data export, and library management",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Section: Account & Cloud Vault (Firebase)
        item {
            Text(
                text = "ACCOUNT & CLOUD VAULT",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (currentUser != null) {
                // Logged in user card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(CyberPurple.copy(alpha = 0.6f), NeonCyan.copy(alpha = 0.6f))),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, NeonCyan, CircleShape)
                                    .background(DarkBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentUser.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser.photoUrl,
                                        contentDescription = "User Photo",
                                        modifier = Modifier.size(52.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val initial = currentUser.fullName.firstOrNull()?.uppercase() ?: "G"
                                    Text(
                                        text = initial,
                                        color = NeonCyan,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUser.fullName.ifEmpty { "GameVault Gamer" },
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (currentUser.isEmailVerified) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (!currentUser.gamerTag.isNullOrBlank()) {
                                    Text(
                                        text = "@${currentUser.gamerTag}",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = currentUser.email,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = onOpenProfile,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Cloud Sync Status Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurface)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Cloud Firestore Vault",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (lastSyncTimestamp != null && lastSyncTimestamp > 0)
                                                "Last synced: ${dateFormat.format(Date(lastSyncTimestamp))}"
                                            else "Not synced yet during this session",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSyncToCloud,
                                enabled = !isCloudSyncing,
                                modifier = Modifier.weight(1f).height(38.dp).testTag("settings_sync_cloud"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onRestoreFromCloud,
                                enabled = !isCloudSyncing,
                                modifier = Modifier.weight(1f).height(38.dp).testTag("settings_restore_cloud"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan, CyberPurple)))
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onSignOut,
                                modifier = Modifier.height(38.dp).testTag("settings_sign_out"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRose.copy(alpha = 0.15f))
                            ) {
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = AccentRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                // Not signed in card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(CyberPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.5f))),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Firebase Cloud Integration",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Authentication & Cloud Firestore",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = "Sign in to back up your video game collection to Cloud Firestore, maintain your ratings across devices, and keep your vault permanently safe.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseConfigured) AccentEmerald else AccentAmber)
                            )
                            Text(
                                text = if (isFirebaseConfigured) "Firebase Cloud Connected" else "Offline fallback active (Firebase ready)",
                                color = if (isFirebaseConfigured) AccentEmerald else AccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onOpenAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("settings_open_auth_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign In / Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section: Data Backup & Transfer
        item {
            Text(
                text = "BACKUP & TRANSFER",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsActionRow(
                        title = "Export Library as JSON",
                        subtitle = "Full JSON export containing all games, ratings, reviews and stats",
                        icon = Icons.Default.FileDownload,
                        iconTint = NeonCyan,
                        onClick = onExportJson,
                        tag = "setting_export_json"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )

                    SettingsActionRow(
                        title = "Export Library as CSV",
                        subtitle = "Spreadsheet-ready CSV table file formatted for Excel or Google Sheets",
                        icon = Icons.Default.TableChart,
                        iconTint = NeonCyan,
                        onClick = onExportCsv,
                        tag = "setting_export_csv"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )

                    SettingsActionRow(
                        title = "Import Library from JSON",
                        subtitle = "Restore or add games by pasting previously exported JSON data",
                        icon = Icons.Default.FileUpload,
                        iconTint = CyberPurple,
                        onClick = onImportJson,
                        tag = "setting_import_json"
                    )
                }
            }
        }

        // Section: Sample Data & Reset
        item {
            Text(
                text = "LIBRARY MANAGEMENT",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsActionRow(
                        title = "Reset to Sample Games Collection",
                        subtitle = "Replace current library with curated starter collection (Elden Ring, Hades II, etc.)",
                        icon = Icons.Default.Refresh,
                        iconTint = CyberPurple,
                        onClick = onResetSampleData,
                        tag = "setting_reset_sample"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )

                    SettingsActionRow(
                        title = "Clear All Library Games",
                        subtitle = "Permanently remove all $totalGamesCount local games from the device database",
                        icon = Icons.Default.DeleteForever,
                        iconTint = AccentRose,
                        onClick = onClearAllData,
                        tag = "setting_clear_all"
                    )
                }
            }
        }

        // Section: About & Tech Stack
        item {
            Text(
                text = "SYSTEM ARCHITECTURE",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GameVault Mobile Pro",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Version 2.0.0 · Production Ready",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )

                    TechBadgeRow("Backend", "Firebase Authentication + Cloud Firestore")
                    TechBadgeRow("Local DB", "Android Jetpack Room + SQLite (KSP)")
                    TechBadgeRow("Architecture", "MVVM + Kotlin Coroutines & Flow")
                    TechBadgeRow("UI Framework", "Jetpack Compose + Material Design 3")
                    TechBadgeRow("Image Pipeline", "Coil Async Image Loader (Memory & Disk Cache)")
                    TechBadgeRow("Target FPS", "60–120 FPS Optimized Rendering Pipeline")
                }
            }
        }
    }
}

@Composable
private fun TechBadgeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
