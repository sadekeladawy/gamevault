package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.StatusPlayingColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

private val POPULAR_RAWG_SEARCHES = listOf(
    "Elden Ring",
    "Witcher 3",
    "Cyberpunk 2077",
    "God of War",
    "Grand Theft Auto V",
    "Zelda",
    "Hades",
    "Red Dead Redemption 2"
)

data class RawgFilterOptions(
    val selectedGenre: String? = null,
    val selectedPlatform: String? = null,
    val selectedYearRange: String? = null, // e.g. "2020-01-01,2026-12-31"
    val minMetacritic: Int? = null,
    val ordering: String = "-rating"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RawgSearchScreen(
    searchQuery: String,
    searchResults: List<RawgGameDto>,
    isLoading: Boolean,
    errorMessage: String?,
    hasSearched: Boolean,
    filterOptions: RawgFilterOptions,
    searchHistory: List<String> = emptyList(),
    onSearchChange: (String) -> Unit,
    onFilterChange: (RawgFilterOptions) -> Unit,
    onClearSearchHistory: () -> Unit = {},
    onRemoveSearchQuery: (String) -> Unit = {},
    onRetrySearch: () -> Unit,
    onSelectGame: (RawgGameDto) -> Unit,
    onAddGameToVault: (RawgGameDto, GameStatus) -> Unit,
    isGameInVault: (String) -> Boolean,
    getVaultGame: (String) -> Game?,
    modifier: Modifier = Modifier
) {
    var quickAddGame by remember { mutableStateOf<RawgGameDto?>(null) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("rawg_search_screen")
    ) {
        // --- Search Input Box + Filters Toggle Bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { onSearchChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rawg_search_input_field"),
                    placeholder = {
                        Text(
                            text = "Search games or use filters...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeonCyan
                        )
                    },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle Filter Panel Button
                Surface(
                    color = if (isFilterPanelExpanded || hasActiveFilters(filterOptions)) CyberPurple else DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isFilterPanelExpanded || hasActiveFilters(filterOptions)) NeonCyan else DarkCardBorder
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = if (isFilterPanelExpanded || hasActiveFilters(filterOptions)) Color.White else TextSecondary
                        )
                    }
                }
            }

            // Expandable Filter Options Panel
            AnimatedVisibility(
                visible = isFilterPanelExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "Advanced RAWG Filters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Genre Selector
                    Text("Genre:", fontSize = 11.sp, color = TextMuted)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        val genres = listOf("All", "Action", "RPG", "Shooter", "Adventure", "Strategy", "Indie", "Horror", "Racing", "Sports", "Puzzle")
                        items(genres) { genre ->
                            val isSelected = (genre == "All" && filterOptions.selectedGenre == null) ||
                                    filterOptions.selectedGenre.equals(genre, ignoreCase = true) ||
                                    (genre == "RPG" && filterOptions.selectedGenre == "5") ||
                                    (genre == "Action" && filterOptions.selectedGenre == "4") ||
                                    (genre == "Shooter" && filterOptions.selectedGenre == "2")
                            FilterChipItem(
                                label = genre,
                                isSelected = isSelected,
                                onClick = {
                                    val newGenre = when (genre) {
                                        "All" -> null
                                        "RPG" -> "5"
                                        "Action" -> "4"
                                        "Shooter" -> "2"
                                        "Adventure" -> "3"
                                        "Strategy" -> "10"
                                        "Indie" -> "51"
                                        else -> genre.lowercase()
                                    }
                                    onFilterChange(filterOptions.copy(selectedGenre = newGenre))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Platform Selector
                    Text("Platform:", fontSize = 11.sp, color = TextMuted)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        val platforms = listOf("All", "PC", "PlayStation", "Xbox", "Switch", "Android")
                        items(platforms) { platform ->
                            val isSelected = (platform == "All" && filterOptions.selectedPlatform == null) ||
                                    (platform == "PC" && filterOptions.selectedPlatform == "4") ||
                                    (platform == "PlayStation" && filterOptions.selectedPlatform == "187,18") ||
                                    (platform == "Xbox" && filterOptions.selectedPlatform == "186,1") ||
                                    (platform == "Switch" && filterOptions.selectedPlatform == "7")
                            FilterChipItem(
                                label = platform,
                                isSelected = isSelected,
                                onClick = {
                                    val newPlatform = when (platform) {
                                        "All" -> null
                                        "PC" -> "4"
                                        "PlayStation" -> "187,18"
                                        "Xbox" -> "186,1"
                                        "Switch" -> "7"
                                        "Android" -> "21"
                                        else -> null
                                    }
                                    onFilterChange(filterOptions.copy(selectedPlatform = newPlatform))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Release Year Range Selector
                    Text("Release Era:", fontSize = 11.sp, color = TextMuted)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        val eras = listOf("All Time", "2020+", "2015 - 2025", "2023", "2024", "Retro (<2010)")
                        items(eras) { era ->
                            val isSelected = when (era) {
                                "All Time" -> filterOptions.selectedYearRange == null
                                "2020+" -> filterOptions.selectedYearRange == "2020-01-01,2026-12-31"
                                "2015 - 2025" -> filterOptions.selectedYearRange == "2015-01-01,2025-12-31"
                                "2023" -> filterOptions.selectedYearRange == "2023-01-01,2023-12-31"
                                "2024" -> filterOptions.selectedYearRange == "2024-01-01,2024-12-31"
                                "Retro (<2010)" -> filterOptions.selectedYearRange == "1980-01-01,2009-12-31"
                                else -> false
                            }
                            FilterChipItem(
                                label = era,
                                isSelected = isSelected,
                                onClick = {
                                    val newRange = when (era) {
                                        "All Time" -> null
                                        "2020+" -> "2020-01-01,2026-12-31"
                                        "2015 - 2025" -> "2015-01-01,2025-12-31"
                                        "2023" -> "2023-01-01,2023-12-31"
                                        "2024" -> "2024-01-01,2024-12-31"
                                        "Retro (<2010)" -> "1980-01-01,2009-12-31"
                                        else -> null
                                    }
                                    onFilterChange(filterOptions.copy(selectedYearRange = newRange))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Filters Button
                        if (hasActiveFilters(filterOptions)) {
                            OutlinedButton(
                                onClick = { onFilterChange(RawgFilterOptions()) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Filters", fontSize = 11.sp, color = TextMuted)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Sorting Dropdown Trigger
                        Box {
                            OutlinedButton(
                                onClick = { isSortMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = getOrderingLabel(filterOptions.ordering),
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = isSortMenuOpen,
                                onDismissRequest = { isSortMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Highest Rating") },
                                    onClick = {
                                        onFilterChange(filterOptions.copy(ordering = "-rating"))
                                        isSortMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Most Popular") },
                                    onClick = {
                                        onFilterChange(filterOptions.copy(ordering = "-added"))
                                        isSortMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Release Date (Newest)") },
                                    onClick = {
                                        onFilterChange(filterOptions.copy(ordering = "-released"))
                                        isSortMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Metacritic Score") },
                                    onClick = {
                                        onFilterChange(filterOptions.copy(ordering = "-metacritic"))
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recent Search History Chips
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recent Searches:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Clear History",
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { onClearSearchHistory() }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(searchHistory) { historyItem ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSearchChange(historyItem) },
                            color = DarkCard,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = historyItem,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onRemoveSearchQuery(historyItem) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick suggestion chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Suggestions:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(POPULAR_RAWG_SEARCHES) { suggestion ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSearchChange(suggestion) },
                            color = if (searchQuery.equals(suggestion, ignoreCase = true)) CyberPurple.copy(alpha = 0.4f) else DarkCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (searchQuery.equals(suggestion, ignoreCase = true)) NeonCyan else DarkCardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = if (searchQuery.equals(suggestion, ignoreCase = true)) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Main Content (LazyColumn with Results / Loader / Error / Empty) ---
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // 1. Loading Indicator
                isLoading && searchResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching RAWG Video Games Database...",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Querying live REST endpoint api.rawg.io/api/games",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 2. Network Error State
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to Search RAWG",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onRetrySearch,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Search", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. No Games Found Friendly State
                hasSearched && searchResults.isEmpty() && !isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Games Found",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No games matched your query and active filters. Try adjusting your search criteria.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }

                // 4. Initial Prompt (Before searching)
                !hasSearched && searchResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(CyberPurple.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.2f)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideogameAsset,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "RAWG Game Discovery System",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Search over 500,000+ video games or filter by Genre, Platform, Release Era, and Rating.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }

                // 5. Results List (LazyColumn with game cards)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Found ${searchResults.size} games",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        items(
                            items = searchResults,
                            key = { it.id },
                            contentType = { "rawg_game_card" }
                        ) { gameDto ->
                            val onSelect = remember(gameDto.id, onSelectGame) { { onSelectGame(gameDto) } }
                            val onAdd = remember(gameDto.id) { { quickAddGame = gameDto } }
                            RawgGameCardItem(
                                game = gameDto,
                                isInVault = isGameInVault(gameDto.name),
                                vaultGame = getVaultGame(gameDto.name),
                                onCardClick = onSelect,
                                onAddToVault = onAdd
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Add Status Selection Dropdown / Sheet
    quickAddGame?.let { gameDto ->
        RawgQuickAddDialog(
            game = gameDto,
            onDismiss = { quickAddGame = null },
            onConfirmAdd = { status ->
                onAddGameToVault(gameDto, status)
                quickAddGame = null
            }
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) CyberPurple else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NeonCyan else DarkCardBorder
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun hasActiveFilters(options: RawgFilterOptions): Boolean {
    return options.selectedGenre != null || options.selectedPlatform != null ||
            options.selectedYearRange != null || options.minMetacritic != null ||
            options.ordering != "-rating"
}

private fun getOrderingLabel(ordering: String): String {
    return when (ordering) {
        "-rating" -> "Highest Rating"
        "-added" -> "Most Popular"
        "-released" -> "Newest First"
        "-metacritic" -> "Metacritic Score"
        else -> "Sorted"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RawgGameCardItem(
    game: RawgGameDto,
    isInVault: Boolean,
    vaultGame: Game?,
    onCardClick: () -> Unit = {},
    onAddToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = remember { RoundedCornerShape(14.dp) }
    val cardBorder = remember { BorderStroke(1.dp, DarkCardBorder) }

    val imageRequest = remember(game.backgroundImage, context) {
        if (!game.backgroundImage.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(game.backgroundImage)
                .crossfade(150)
                .build()
        } else null
    }

    val ratingText = remember(game.rating) {
        val ratingValue = game.rating ?: 0.0
        if (ratingValue > 0.0) String.format(Locale.US, "Rating %.1f/5.0", ratingValue) else "Rating N/A"
    }

    val onClick = remember(game.id, onCardClick) { onCardClick }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Cover Image loaded via Coil
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "${game.name} cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideogameAsset,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No Image",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info: Title, Release Date, Rating, Platforms, Add Button
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Release Date: ",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = game.released?.ifBlank { "TBA" } ?: "TBA",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ratingValue = game.rating ?: 0.0
                    Text(
                        text = ratingText,
                        color = if (ratingValue > 0.0) AccentAmber else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if ((game.ratingsCount ?: 0) > 0) {
                        Text(
                            text = " (${game.ratingsCount})",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (game.metacritic != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        game.metacritic >= 75 -> AccentEmerald.copy(alpha = 0.2f)
                                        game.metacritic >= 50 -> AccentAmber.copy(alpha = 0.2f)
                                        else -> Color(0xFFE53935).copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Metacritic ${game.metacritic}",
                                color = when {
                                    game.metacritic >= 75 -> AccentEmerald
                                    game.metacritic >= 50 -> AccentAmber
                                    else -> Color(0xFFE53935)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!game.genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        game.genres.take(4).forEach { genreDto ->
                            val genreName = genreDto.name ?: return@forEach
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = genreName,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                val platformNames = game.platforms?.mapNotNull { it.platform?.name }
                if (!platformNames.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        platformNames.take(4).forEach { platformName ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberPurple.copy(alpha = 0.18f))
                                    .border(0.5.dp, CyberPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = platformName,
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isInVault) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (vaultGame?.status) {
                                    GameStatus.COMPLETED -> StatusCompletedColor.copy(alpha = 0.15f)
                                    GameStatus.CURRENTLY_PLAYING -> StatusPlayingColor.copy(alpha = 0.15f)
                                    else -> NeonCyan.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = when (vaultGame?.status) {
                                GameStatus.COMPLETED -> StatusCompletedColor
                                GameStatus.CURRENTLY_PLAYING -> StatusPlayingColor
                                else -> NeonCyan
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In Vault: ${vaultGame?.status?.displayName ?: "Tracked"}",
                            color = when (vaultGame?.status) {
                                GameStatus.COMPLETED -> StatusCompletedColor
                                GameStatus.CURRENTLY_PLAYING -> StatusPlayingColor
                                else -> NeonCyan
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAddToVault,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Vault",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add to Vault",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RawgQuickAddDialog(
    game: RawgGameDto,
    onDismiss: () -> Unit,
    onConfirmAdd: (GameStatus) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(GameStatus.BACKLOG) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add to GameVault",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.name,
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Initial Status:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                GameStatus.entries.forEach { status ->
                    val isSelected = selectedStatus == status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberPurple.copy(alpha = 0.25f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.displayName,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(contentColor = TextMuted)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmAdd(selectedStatus) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                    ) {
                        Text("Add Game", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
