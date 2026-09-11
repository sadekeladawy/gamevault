package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.ui.components.AskAiButton
import com.example.ui.components.BarChartItem
import com.example.ui.components.CompletionBarChart
import com.example.ui.components.GameCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.StatusPlayingColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavDestination
import com.example.ui.viewmodel.VaultStats
import java.util.Calendar
import java.util.Locale

private val PLATFORM_CHIPS = listOf("PC", "PlayStation 5", "Xbox Series X/S", "Nintendo Switch", "Android")

@Composable
fun DashboardScreen(
    stats: VaultStats,
    popularGames: List<RawgGameDto> = emptyList(),
    personalizedRecommendations: List<RawgGameDto> = emptyList(),
    onNavigate: (NavDestination) -> Unit,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onAddGame: () -> Unit,
    onSearchDatabase: (String) -> Unit = {},
    onSelectRawgGame: (RawgGameDto) -> Unit = {},
    onAddRawgGameToVault: (RawgGameDto, GameStatus) -> Unit = { _, _ -> },
    isGameInVault: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Hero Banner
        item(key = "dashboard_hero", contentType = "hero") {
            DashboardHeroBanner(
                totalGames = stats.totalGames,
                completedThisYear = stats.completedThisYear,
                streakMonths = stats.completionStreakMonths,
                onAddGame = onAddGame
            )
        }

        // Live RAWG Search & Discovery Bar (Self-contained state prevents whole-screen recomposition on input)
        item(key = "dashboard_search", contentType = "search") {
            DashboardSearchCard(
                onSearchDatabase = onSearchDatabase,
                onNavigate = onNavigate
            )
        }

        // Ask AI Copilot Banner
        item(key = "dashboard_ai_copilot", contentType = "ai_copilot") {
            AskAiButton(
                onClick = { onNavigate(NavDestination.AI_CHAT) },
                label = "Ask AI Game Assistant",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Platform Quick Discovery Row
        item(key = "dashboard_platforms", contentType = "platforms") {
            DashboardPlatformChips(onSearchDatabase = onSearchDatabase)
        }

        // Popular RAWG Games Horizontal Carousel
        if (popularGames.isNotEmpty()) {
            item(key = "dashboard_popular_games", contentType = "popular_carousel") {
                DashboardPopularCarousel(
                    popularGames = popularGames,
                    isGameInVault = isGameInVault,
                    onSelectRawgGame = onSelectRawgGame,
                    onAddRawgGameToVault = onAddRawgGameToVault,
                    onNavigate = onNavigate
                )
            }
        }

        // 2x2 Metric Cards Grid
        item(key = "dashboard_metrics", contentType = "metrics") {
            DashboardMetricGrid(
                stats = stats,
                onNavigate = onNavigate
            )
        }

        // Monthly Completions Chart Card
        item(key = "dashboard_chart", contentType = "chart") {
            DashboardChartCard(
                chartItems = chartItems,
                currentYear = currentYear,
                onNavigate = onNavigate
            )
        }

        // Recently Completed Games Horizontal Row
        if (stats.recentlyCompletedGames.isNotEmpty()) {
            item(key = "dashboard_recently_completed", contentType = "recent_carousel") {
                DashboardRecentlyCompletedCarousel(
                    recentGames = stats.recentlyCompletedGames,
                    onGameClick = onGameClick,
                    onToggleFavorite = onToggleFavorite,
                    onNavigate = onNavigate
                )
            }
        }

        // Quick Navigation Links to Statistics & Library
        item(key = "dashboard_quick_vault", contentType = "quick_vault") {
            DashboardQuickVaultManagement(onNavigate = onNavigate)
        }
    }
}

@Composable
private fun DashboardHeroBanner(
    totalGames: Int,
    completedThisYear: Int,
    streakMonths: Int,
    onAddGame: () -> Unit
) {
    val heroGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                PrimaryRed.copy(alpha = 0.22f),
                DarkCard,
                Color(0xFF141416)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_hero_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GameVault Commander",
                            color = PrimaryRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track, Rate & Master Your Collection",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onAddGame,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("dashboard_add_game_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Game",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Game", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero stats pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroPill(
                        label = "Total Vault",
                        value = "$totalGames Games",
                        modifier = Modifier.weight(1f)
                    )
                    HeroPill(
                        label = "Annual Cleared",
                        value = "$completedThisYear Completed",
                        modifier = Modifier.weight(1f)
                    )
                    HeroPill(
                        label = "Clear Streak",
                        value = "$streakMonths Months",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardSearchCard(
    onSearchDatabase: (String) -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    var dbSearchInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_rawg_search_box"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideogameAsset,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Discover & Add Games",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "500,000+ Games",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = dbSearchInput,
                    onValueChange = { dbSearchInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_rawg_input_field"),
                    placeholder = {
                        Text(
                            text = "Search Elden Ring, Witcher, GTA...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryRed
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (dbSearchInput.isNotBlank()) {
                            onSearchDatabase(dbSearchInput.trim())
                        } else {
                            onNavigate(NavDestination.GAME_DATABASE)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("dashboard_rawg_search_btn")
                ) {
                    Text("Search", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DashboardPlatformChips(
    onSearchDatabase: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Explore by Platform:",
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(PLATFORM_CHIPS, key = { it }) { platformName ->
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.clickable {
                        onSearchDatabase(platformName)
                    }
                ) {
                    Text(
                        text = platformName,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardPopularCarousel(
    popularGames: List<RawgGameDto>,
    isGameInVault: (String) -> Boolean,
    onSelectRawgGame: (RawgGameDto) -> Unit,
    onAddRawgGameToVault: (RawgGameDto, GameStatus) -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = PrimaryRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Trending & Popular Games",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Explore All",
                color = PrimaryRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigate(NavDestination.GAME_DATABASE) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(popularGames, key = { it.id }) { rawgGame ->
                DashboardRawgGameCard(
                    rawgGame = rawgGame,
                    inVault = isGameInVault(rawgGame.name),
                    onSelect = { onSelectRawgGame(rawgGame) },
                    onAdd = { onAddRawgGameToVault(rawgGame, GameStatus.BACKLOG) }
                )
            }
        }
    }
}

@Composable
private fun DashboardRawgGameCard(
    rawgGame: RawgGameDto,
    inVault: Boolean,
    onSelect: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val ratingText = remember(rawgGame.rating) {
        val r = rawgGame.rating ?: 0.0
        if (r > 0.0) String.format(Locale.US, "Rating: %.1f/5.0", r) else ""
    }
    val imageRequest = remember(rawgGame.backgroundImage, context) {
        if (!rawgGame.backgroundImage.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(rawgGame.backgroundImage)
                .size(320, 320)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(100)
                .build()
        } else null
    }

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onSelect)
            .testTag("popular_rawg_card_${rawgGame.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(DarkSurface)
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = rawgGame.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideogameAsset,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Clean Rating badge overlay (No decorative star symbols)
                if (ratingText.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = ratingText,
                            color = AccentAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = rawgGame.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = rawgGame.released?.take(4) ?: "RAWG API",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (inVault) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusCompletedColor.copy(alpha = 0.15f))
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusCompletedColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "In Vault",
                            color = StatusCompletedColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
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
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricGrid(
    stats: VaultStats,
    onNavigate: (NavDestination) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Completed",
                value = "${stats.completedCount}",
                subtitle = "${stats.completedThisYear} this year",
                icon = Icons.Default.CheckCircle,
                iconTint = StatusCompletedColor,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.COMPLETED) }
            )
            MetricCard(
                title = "Playing",
                value = "${stats.currentlyPlayingCount}",
                subtitle = "Active adventures",
                icon = Icons.Default.PlayCircle,
                iconTint = StatusPlayingColor,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.PLAYING) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Backlog",
                value = "${stats.backlogCount}",
                subtitle = "Awaiting play",
                icon = Icons.Outlined.MenuBook,
                iconTint = PrimaryRed,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.BACKLOG) }
            )
            MetricCard(
                title = "Favorites",
                value = "${stats.favoritesCount}",
                subtitle = "Top tier titles",
                icon = Icons.Default.Favorite,
                iconTint = AccentAmber,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(NavDestination.FAVORITES) }
            )
        }
    }
}

@Composable
private fun DashboardChartCard(
    chartItems: List<BarChartItem>,
    currentYear: Int,
    onNavigate: (NavDestination) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Yearly Completions",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Games completed year-by-year",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { onNavigate(NavDestination.STATISTICS) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Statistics",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CompletionBarChart(
                title = "Annual Progress",
                subtitle = "Games cleared through $currentYear",
                items = chartItems,
                accentGradient = listOf(PrimaryRed, Color(0xFFFF6B6B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
private fun DashboardRecentlyCompletedCarousel(
    recentGames: List<Game>,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Completed",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "See All",
                color = PrimaryRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigate(NavDestination.COMPLETED) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(recentGames, key = { it.id }) { game ->
                GameCard(
                    game = game,
                    onClick = { onGameClick(game) },
                    onToggleFavorite = { onToggleFavorite(game) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardQuickVaultManagement(
    onNavigate: (NavDestination) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Vault Management",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(NavDestination.LIBRARY) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onNavigate(NavDestination.STATISTICS) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analytics", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_metric_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
