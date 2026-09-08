package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.components.BarChartItem
import com.example.ui.components.CompletionBarChart
import com.example.ui.components.GameCard
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CyberPurple
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
    onNavigate: (NavDestination) -> Unit,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onAddGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Welcome Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GameVault",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Your personal gaming archive and completion tracker",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onAddGame,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        modifier = Modifier.testTag("dashboard_add_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Game", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2x3 Metric Cards Grid
        item {
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
                        subtitle = "${stats.completedThisYear} this year",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Playing",
                        value = stats.currentlyPlayingCount.toString(),
                        icon = Icons.Filled.PlayCircle,
                        accentColor = StatusPlayingColor,
                        subtitle = "active quests",
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
                        subtitle = "ready to play",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Playtime",
                        value = "${stats.totalPlaytimeHours.toInt()}h",
                        icon = Icons.Outlined.Schedule,
                        accentColor = NeonCyan,
                        subtitle = "hours invested",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Avg Rating",
                        value = if (stats.averageRating > 0) String.format("%.1f", stats.averageRating) else "—",
                        icon = Icons.Filled.Star,
                        accentColor = AccentAmber,
                        subtitle = "out of 10",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Streak",
                        value = "${stats.completionStreakMonths} mo",
                        icon = Icons.Outlined.CalendarToday,
                        accentColor = CyberPurple,
                        subtitle = "consistent gaming",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Yearly Completion Chart Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            val chartItems = stats.yearlyCompletions.entries
                .sortedBy { it.key }
                .map { BarChartItem(label = it.key.toString(), value = it.value) }

            CompletionBarChart(
                title = "Yearly Completion Trends",
                subtitle = "Games finished by year",
                items = chartItems,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // Recently Completed Games
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
                        text = "View All (${stats.completedCount})",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (stats.recentlyCompletedGames.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stats.recentlyCompletedGames, key = { it.id }) { game ->
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
                    items(stats.highestRatedGames, key = { it.id }) { game ->
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
