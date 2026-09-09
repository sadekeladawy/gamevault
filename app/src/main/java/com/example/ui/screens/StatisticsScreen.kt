package com.example.ui.screens

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.ui.components.BarChartItem
import com.example.ui.components.CompletionBarChart
import com.example.ui.components.StatCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VaultStats
import java.util.Calendar

val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
fun StatisticsScreen(
    stats: VaultStats,
    onGameClick: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Title Header
        item {
            Column {
                Text(
                    text = "Vault Analytics",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Detailed statistics, completion trends, and play habits",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Top Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Completed",
                        value = stats.completedCount.toString(),
                        icon = Icons.Filled.CheckCircle,
                        accentColor = StatusCompletedColor,
                        subtitle = "${stats.completedThisYear} in $currentYear",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Hours Played",
                        value = "${stats.totalPlaytimeHours.toInt()}h",
                        icon = Icons.Outlined.Schedule,
                        accentColor = NeonCyan,
                        subtitle = "across all games",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Personal Rating",
                        value = if (stats.averagePersonalRating > 0) String.format("%.1f / 10", stats.averagePersonalRating) else "—",
                        icon = Icons.Filled.BarChart,
                        accentColor = AccentAmber,
                        subtitle = "my score average",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Streak",
                        value = "${stats.completionStreakMonths} Months",
                        icon = Icons.Outlined.CalendarToday,
                        accentColor = CyberPurple,
                        subtitle = "consecutive active",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly Completions Chart for Current Year
        item {
            val monthlyItems = (1..12).map { month ->
                BarChartItem(
                    label = MonthNames[month - 1],
                    value = stats.monthlyCompletions[month] ?: 0
                )
            }

            CompletionBarChart(
                title = "Monthly Completions ($currentYear)",
                subtitle = "Games finished by month this year",
                items = monthlyItems,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Yearly Completions Chart
        item {
            val yearlyItems = stats.yearlyCompletions.entries
                .sortedBy { it.key }
                .map { BarChartItem(label = it.key.toString(), value = it.value) }

            CompletionBarChart(
                title = "Yearly Completions",
                subtitle = "Historical game completions by year",
                items = yearlyItems,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Most Played Genres Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Most Played Genres",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Library genre distribution",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalGames = stats.totalGames.coerceAtLeast(1)
                    if (stats.genreDistribution.isEmpty()) {
                        Text("No genres recorded yet", color = TextMuted, fontSize = 13.sp)
                    } else {
                        stats.genreDistribution.forEachIndexed { idx, (genre, count) ->
                            val progress = count.toFloat() / totalGames
                            val color = when (idx % 3) {
                                0 -> CyberPurple
                                1 -> NeonCyan
                                else -> AccentAmber
                            }

                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = genre,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$count games (${(progress * 100).toInt()}%)",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = Color.White.copy(alpha = 0.06f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Most Played Platforms Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Most Played Platforms",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hardware breakdown of your vault",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalGames = stats.totalGames.coerceAtLeast(1)
                    if (stats.platformDistribution.isEmpty()) {
                        Text("No platforms recorded yet", color = TextMuted, fontSize = 13.sp)
                    } else {
                        stats.platformDistribution.forEachIndexed { idx, (platform, count) ->
                            val progress = count.toFloat() / totalGames
                            val color = when (idx % 3) {
                                0 -> NeonCyan
                                1 -> CyberPurple
                                else -> StatusCompletedColor
                            }

                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = platform,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$count games (${(progress * 100).toInt()}%)",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = Color.White.copy(alpha = 0.06f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Highest Rated Games Leaderboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Highest Rated Hall of Fame",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your top-rated masterpieces",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (stats.highestRatedGames.isEmpty()) {
                        Text("No games rated yet", color = TextMuted, fontSize = 13.sp)
                    } else {
                        stats.highestRatedGames.forEachIndexed { index, game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onGameClick(game) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rank badge
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index == 0) AccentAmber.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.06f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = if (index == 0) AccentAmber else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = game.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${game.platform} • ${game.playtimeHours.toInt()}h",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "My Rating: ${game.rating}/10",
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
