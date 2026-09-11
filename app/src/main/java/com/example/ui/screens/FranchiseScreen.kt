package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FranchiseDetails
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.SeriesGameItem
import com.example.ui.components.AskAiButton
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

enum class FranchiseSortOrder(val label: String) {
    SERIES_ORDER("Series Order"),
    RELEASE_DATE("Release Date"),
    TITLE("Title"),
    USER_STATUS("Vault Status")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FranchiseScreen(
    details: FranchiseDetails,
    onBack: () -> Unit,
    onGameClick: (Game) -> Unit,
    onStartSession: (Game) -> Unit,
    onSeriesGameClick: ((SeriesGameItem) -> Unit)? = null,
    onAddSeriesGameToVault: ((SeriesGameItem) -> Unit)? = null,
    onAskAi: ((String, List<SeriesGameItem>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(FranchiseSortOrder.SERIES_ORDER) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    // Normalize items: if details.seriesGames is populated, use it; otherwise map user games
    val allSeriesItems: List<SeriesGameItem> = remember(details.seriesGames, details.games) {
        if (details.seriesGames.isNotEmpty()) {
            details.seriesGames
        } else {
            details.games.map { g ->
                SeriesGameItem(
                    title = g.title,
                    releaseYear = g.releaseYear,
                    seriesOrder = g.seriesOrder,
                    coverUrl = g.coverUrl,
                    rawgId = null,
                    isInVault = true,
                    vaultStatus = g.status,
                    vaultGame = g
                )
            }
        }
    }

    val sortedSeriesGames = remember(allSeriesItems, sortOrder) {
        when (sortOrder) {
            FranchiseSortOrder.SERIES_ORDER -> allSeriesItems.sortedWith(
                compareBy<SeriesGameItem> { it.seriesOrder ?: Int.MAX_VALUE }
                    .thenBy { it.releaseYear ?: 9999 }
                    .thenBy { it.title }
            )
            FranchiseSortOrder.RELEASE_DATE -> allSeriesItems.sortedBy { it.releaseYear ?: 9999 }
            FranchiseSortOrder.TITLE -> allSeriesItems.sortedBy { it.title }
            FranchiseSortOrder.USER_STATUS -> allSeriesItems.sortedWith(
                compareBy<SeriesGameItem> { !it.isInVault }
                    .thenBy { it.vaultStatus?.ordinal ?: 99 }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = details.franchise.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Complete Franchise & Series Overview",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (onAskAi != null) {
                        AskAiButton(
                            onClick = { onAskAi(details.franchise.name, sortedSeriesGames) },
                            iconOnly = true
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Banner Image
            if (details.franchise.imageUrl.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCard)
                    ) {
                        AsyncImage(
                            model = details.franchise.imageUrl,
                            contentDescription = details.franchise.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )
                        Text(
                            text = details.franchise.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }

            // Ask AI Quick Action Banner for Franchise
            if (onAskAi != null) {
                item {
                    AskAiButton(
                        onClick = { onAskAi(details.franchise.name, sortedSeriesGames) },
                        label = "Ask AI Copilot about ${details.franchise.name} series",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Franchise Progress Visualizer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Franchise Vault Progress",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${details.completedGames} / ${details.totalGames} in Vault Completed",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PrimaryRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (details.completionPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = PrimaryRed,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${details.completionPercentage}% Complete • ${details.games.size} of series in your Vault",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            // Useful Franchise Stats Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total Playtime",
                            value = "${String.format(Locale.US, "%.1f", details.totalPlaytimeHours)} hrs",
                            icon = Icons.Default.PlayArrow,
                            accentColor = PrimaryRed,
                            subtitle = "Across all games",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Average Rating",
                            value = if (details.averageRating > 0) "${String.format(Locale.US, "%.1f", details.averageRating)} / 10" else "N/A",
                            icon = Icons.Default.Star,
                            accentColor = AccentAmber,
                            subtitle = "Personal rating",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Backlog / Wishlist",
                            value = "${details.backlogGames + details.wishlistGames}",
                            icon = Icons.Default.VideogameAsset,
                            accentColor = AccentEmerald,
                            subtitle = "Ready to play",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Currently Playing",
                            value = "${details.currentlyPlayingGames}",
                            icon = Icons.Default.CheckCircle,
                            accentColor = PrimaryRed,
                            subtitle = "In progress",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // "PLAY NEXT" Recommendation Card
            details.playNextGame?.let { nextGame ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = PrimaryRed.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PLAY NEXT IN SERIES",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryRed,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (nextGame.isInVault && nextGame.vaultGame != null) {
                                            onGameClick(nextGame.vaultGame)
                                        } else {
                                            onSeriesGameClick?.invoke(nextGame)
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (nextGame.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = nextGame.coverUrl,
                                        contentDescription = nextGame.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp, 72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Surface(
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(54.dp, 72.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideogameAsset,
                                            contentDescription = null,
                                            modifier = Modifier.padding(12.dp),
                                            tint = TextMuted
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextGame.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${nextGame.platform} • ${nextGame.status?.displayName ?: "In Franchise"}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted
                                        )
                                    )
                                }

                                if (nextGame.isInVault && nextGame.vaultGame != null) {
                                    Button(
                                        onClick = { onStartSession(nextGame.vaultGame) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryRed
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Playing",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Play", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onAddSeriesGameToVault?.invoke(nextGame) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryRed
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add to Vault",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Series Header & Sorting Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Complete Series (${sortedSeriesGames.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Shows all games in franchise, even if not yet in vault",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { sortDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "Sort: ${sortOrder.label}",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }

                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false }
                        ) {
                            FranchiseSortOrder.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sortOrder = option
                                        sortDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Series Games List (Complete franchise: in-vault + not-in-vault)
            items(sortedSeriesGames, key = { it.title }) { item ->
                SeriesGameItemCard(
                    item = item,
                    onClick = {
                        if (item.isInVault && item.vaultGame != null) {
                            onGameClick(item.vaultGame)
                        } else {
                            onSeriesGameClick?.invoke(item)
                        }
                    },
                    onAddClick = {
                        onAddSeriesGameToVault?.invoke(item)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SeriesGameItemCard(
    item: SeriesGameItem,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Series Order Badge or Index
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.vaultStatus == GameStatus.COMPLETED)
                            AccentEmerald.copy(alpha = 0.2f)
                        else if (item.isInVault)
                            PrimaryRed.copy(alpha = 0.2f)
                        else
                            Color.White.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.vaultStatus == GameStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = AccentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (item.seriesOrder != null && item.seriesOrder > 0) "#${item.seriesOrder}" else "-",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.isInVault) PrimaryRed else TextMuted
                        )
                    )
                }
            }

            // Cover Image
            if (!item.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp, 64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp, 64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = TextMuted
                    )
                }
            }

            // Game Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (item.releaseYear != null) "Released ${item.releaseYear}" else "Release date unknown",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isInVault && item.vaultStatus != null) {
                        Surface(
                            color = when (item.vaultStatus) {
                                GameStatus.COMPLETED -> AccentEmerald.copy(alpha = 0.2f)
                                GameStatus.CURRENTLY_PLAYING -> PrimaryRed.copy(alpha = 0.2f)
                                GameStatus.BACKLOG -> AccentAmber.copy(alpha = 0.2f)
                                else -> Color.White.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Vault: ${item.vaultStatus.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.vaultStatus) {
                                        GameStatus.COMPLETED -> AccentEmerald
                                        GameStatus.CURRENTLY_PLAYING -> PrimaryRed
                                        GameStatus.BACKLOG -> AccentAmber
                                        else -> TextSecondary
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Not in Vault",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.vaultGame?.rating != null && item.vaultGame.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${item.vaultGame.rating}/10",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            )
                        }
                    }
                }
            }

            // If not in vault, provide a direct "Add" button right here
            if (!item.isInVault) {
                Surface(
                    color = PrimaryRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onAddClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Vault",
                            tint = PrimaryRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryRed
                        )
                    }
                }
            }
        }
    }
}
