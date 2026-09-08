package com.example.ui.screens

import java.util.Locale
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.ui.components.BarChartItem
import com.example.ui.components.CompletionBarChart
import com.example.ui.components.GameCard
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusBacklogColor
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.StatusPlayingColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavDestination
import com.example.ui.viewmodel.VaultStats

@Composable
fun DashboardScreen(
    stats: VaultStats,
    popularGames: List<RawgGameDto> = emptyList(),
    onNavigate: (NavDestination) -> Unit,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onAddGame: () -> Unit,
    onSearchDatabase: (String) -> Unit = {},
    onAddRawgGameToVault: (RawgGameDto, GameStatus) -> Unit = { _, _ -> },
    isGameInVault: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var dbSearchInput by remember { mutableStateOf("") }
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    val chartItems = remember(stats.yearlyCompletions, currentYear) {
        (currentYear - 4..currentYear).map { yr ->
            BarChartItem(
                label = yr.toString().takeLast(2),
                value = stats.yearlyCompletions[yr] ?: 0
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Welcome Hero Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "GameVault",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your personal gaming archive and completion tracker",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // --- SEARCH HERO CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .testTag("dashboard_database_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Clean and simple title without decorative shapes or badges
                    Text(
                        text = "Search",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    // Modern Material 3 Search Field with rounded corners (primary focus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dbSearchInput,
                            onValueChange = { dbSearchInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("dashboard_db_search_input"),
                            placeholder = {
                                Text(
                                    text = "Search games...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = CyberPurple,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            )
                        )

                        // Search Button - slightly larger with better-rounded corners
                        Button(
                            onClick = {
                                onSearchDatabase(dbSearchInput)
                                onNavigate(NavDestination.GAME_DATABASE)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("dashboard_search_button")
                        ) {
                            Text(
                                text = "Search",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Quick Chips & Explore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Elden Ring", "Cyberpunk", "RPG").forEach { chip ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier.clickable {
                                        onSearchDatabase(chip)
                                        onNavigate(NavDestination.GAME_DATABASE)
                                    }
                                ) {
                                    Text(
                                        text = chip,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = { onNavigate(NavDestination.GAME_DATABASE) },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Explore",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- DISCOVER ON RAWG CAROUSEL ---
        if (popularGames.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discover on RAWG",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    TextButton(onClick = { onNavigate(NavDestination.GAME_DATABASE) }) {
                        Text(
                            text = "See all",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = popularGames.take(8),
                        key = { it.id },
                        contentType = { "discover_game" }
                    ) { rawgGame ->
                        val inVault = isGameInVault(rawgGame.name)

                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
                                .clickable {
                                    onSearchDatabase(rawgGame.name)
                                    onNavigate(NavDestination.GAME_DATABASE)
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(DarkSurface)
                                ) {
                                    if (!rawgGame.backgroundImage.isNullOrBlank()) {
                                        AsyncImage(
                                            model = rawgGame.backgroundImage,
                                            contentDescription = rawgGame.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.VideogameAsset,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .align(Alignment.Center)
                                        )
                                    }

                                    // Rating Tag
                                    val ratingScore = rawgGame.rating ?: 0.0
                                    if (ratingScore > 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DarkBg.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = AccentAmber,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", ratingScore),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = rawgGame.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val firstGenre = rawgGame.genres?.firstOrNull()?.name ?: "Action"
                                    Text(
                                        text = firstGenre,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (inVault) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AccentEmerald.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AccentEmerald,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "In Vault",
                                                    color = AccentEmerald,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onAddRawgGameToVault(rawgGame, GameStatus.BACKLOG) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2x3 Metric Cards Grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Completed",
                        value = stats.completedCount.toString(),
                        icon = Icons.Filled.CheckCircle,
                        accentColor = StatusCompletedColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Playing",
                        value = stats.currentlyPlayingCount.toString(),
                        icon = Icons.Filled.PlayCircle,
                        accentColor = StatusPlayingColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Backlog",
                        value = stats.backlogCount.toString(),
                        icon = Icons.Outlined.MenuBook,
                        accentColor = StatusBacklogColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Favorites",
                        value = stats.favoritesCount.toString(),
                        icon = Icons.Filled.Star,
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Playtime",
                        value = "${String.format(java.util.Locale.US, "%.1f", stats.totalPlaytimeHours)}h",
                        icon = Icons.Outlined.Schedule,
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Done in $currentYear",
                        value = stats.completedThisYear.toString(),
                        icon = Icons.Outlined.CalendarToday,
                        accentColor = CyberPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Completion Activity Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yearly Completions",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate(NavDestination.STATISTICS) }) {
                        Text(
                            text = "View Analytics",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                CompletionBarChart(
                    title = "Completions by Year",
                    subtitle = "Games beaten over the last 5 years",
                    items = chartItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        // Recently Completed Section
        if (stats.recentlyCompletedGames.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Completed",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate(NavDestination.COMPLETED) }) {
                        Text(
                            text = "See All (${stats.completedCount})",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = stats.recentlyCompletedGames,
                        key = { it.id },
                        contentType = { "game_card" }
                    ) { game ->
                        GameCard(
                            game = game,
                            onClick = { onGameClick(game) },
                            onToggleFavorite = { onToggleFavorite(game) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "No games completed yet. Track your first finished game!",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Top Rated Hall of Fame
        if (stats.highestRatedGames.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Rated Games",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { onNavigate(NavDestination.LIBRARY) }) {
                        Text(
                            text = "Browse Library",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = stats.highestRatedGames,
                        key = { it.id },
                        contentType = { "game_card" }
                    ) { game ->
                        GameCard(
                            game = game,
                            onClick = { onGameClick(game) },
                            onToggleFavorite = { onToggleFavorite(game) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }
    }
}
