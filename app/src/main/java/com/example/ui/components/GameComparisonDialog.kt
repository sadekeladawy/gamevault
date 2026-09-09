package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.data.remote.rawg.RawgGameDto
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

data class ComparableGame(
    val title: String,
    val coverUrl: String,
    val releaseYear: String,
    val genre: String,
    val platform: String,
    val rawgRating: String,
    val personalRating: String,
    val metacritic: String,
    val playtime: String,
    val developer: String,
    val publisher: String
) {
    companion object {
        fun fromGame(game: Game): ComparableGame = ComparableGame(
            title = game.title,
            coverUrl = game.coverUrl,
            releaseYear = game.releaseYear.toString(),
            genre = game.genre,
            platform = game.platform,
            rawgRating = if (game.rawgRating > 0) "Rating ${String.format(Locale.US, "%.1f", game.rawgRating)}/5.0" else "N/A",
            personalRating = if (game.rating > 0) "${game.rating}/10" else "Unrated",
            metacritic = game.metacriticScore?.toString() ?: "N/A",
            playtime = "${game.playtimeHours.toInt()} hrs",
            developer = game.developer.ifBlank { "N/A" },
            publisher = game.publisher.ifBlank { "N/A" }
        )

        fun fromRawgDto(dto: RawgGameDto): ComparableGame = ComparableGame(
            title = dto.name,
            coverUrl = dto.backgroundImage ?: "",
            releaseYear = dto.released?.take(4) ?: "N/A",
            genre = dto.genres?.firstOrNull()?.name ?: "Action",
            platform = dto.platforms?.firstOrNull()?.platform?.name ?: "PC",
            rawgRating = if ((dto.rating ?: 0.0) > 0) "Rating ${String.format(Locale.US, "%.1f", dto.rating)}/5.0" else "N/A",
            personalRating = "N/A",
            metacritic = dto.metacritic?.toString() ?: "N/A",
            playtime = "${dto.playtime ?: 0} hrs",
            developer = dto.developers?.firstOrNull()?.name ?: "N/A",
            publisher = dto.publishers?.firstOrNull()?.name ?: "N/A"
        )
    }
}

@Composable
fun GameComparisonDialog(
    game1: ComparableGame,
    game2: ComparableGame,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Game Comparison",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Comparative Table
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Game Covers & Titles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ComparisonHeaderCard(game = game1, modifier = Modifier.weight(1f))
                        ComparisonHeaderCard(game = game2, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Comparison Table Rows
                    ComparisonMetricRow(label = "Release Year", val1 = game1.releaseYear, val2 = game2.releaseYear)
                    ComparisonMetricRow(label = "Main Genre", val1 = game1.genre, val2 = game2.genre)
                    ComparisonMetricRow(label = "Primary Platform", val1 = game1.platform, val2 = game2.platform)
                    ComparisonMetricRow(label = "RAWG Score", val1 = game1.rawgRating, val2 = game2.rawgRating, isHighlight1 = true)
                    ComparisonMetricRow(label = "Metacritic", val1 = game1.metacritic, val2 = game2.metacritic)
                    ComparisonMetricRow(label = "My Personal Rating", val1 = game1.personalRating, val2 = game2.personalRating)
                    ComparisonMetricRow(label = "Avg Playtime", val1 = game1.playtime, val2 = game2.playtime)
                    ComparisonMetricRow(label = "Developer", val1 = game1.developer, val2 = game2.developer)
                    ComparisonMetricRow(label = "Publisher", val1 = game1.publisher, val2 = game2.publisher)
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeaderCard(game: ComparableGame, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
            ) {
                if (game.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = game.coverUrl,
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = game.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    label: String,
    val1: String,
    val2: String,
    isHighlight1: Boolean = false
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = PrimaryRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = val1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighlight1) AccentAmber else TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(DarkCardBorder)
                )
                Text(
                    text = val2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
