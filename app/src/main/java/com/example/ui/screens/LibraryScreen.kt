package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.VideogameAssetOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FranchiseDetails
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.components.CommonPlatforms
import com.example.ui.components.GameCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibraryFilters
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    title: String = "My Game Library",
    games: List<Game>,
    allVaultGames: List<Game> = emptyList(),
    filters: LibraryFilters,
    showStatusFilter: Boolean = true,
    showArchiveToggle: Boolean = true,
    onSearchChange: (String) -> Unit,
    onStatusChange: (GameStatus?) -> Unit,
    onPlatformChange: (String?) -> Unit,
    onFranchiseChange: ((String?) -> Unit)? = null,
    onSortChange: (SortOption) -> Unit,
    onToggleShowArchived: (() -> Unit)? = null,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onAddGame: () -> Unit,
    selectedViewTab: Int = 0,
    onViewTabChange: ((Int) -> Unit)? = null,
    franchisesList: List<FranchiseDetails> = emptyList(),
    onSelectFranchise: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }

    if (selectedViewTab == 1 && onViewTabChange != null) {
        Column(modifier = modifier.fillMaxSize()) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewTabChange(0) }
                    ) {
                        Text(
                            text = "Games (${games.size})",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        color = PrimaryRed,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewTabChange(1) }
                    ) {
                        Text(
                            text = "Franchises (${franchisesList.size})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            FranchiseListScreen(
                franchises = franchisesList,
                onSelectFranchise = { onSelectFranchise?.invoke(it) },
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val totalVaultGames = if (allVaultGames.isNotEmpty()) allVaultGames else games
    val activeGamesCount = totalVaultGames.count { !it.isArchived }
    val archivedGamesCount = totalVaultGames.count { it.isArchived }
    val completedGamesCount = totalVaultGames.count { it.status == GameStatus.COMPLETED && !it.isArchived }
    val completionRatio = if (activeGamesCount > 0) {
        (completedGamesCount.toFloat() / activeGamesCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val completionPercentageInt = (completionRatio * 100).toInt()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Sub-Navigation Tab Bar: Games | Franchises
        if (onViewTabChange != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = PrimaryRed,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onViewTabChange(0) }
                        ) {
                            Text(
                                text = "Games (${games.size})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onViewTabChange(1) }
                        ) {
                            Text(
                                text = "Franchises (${franchisesList.size})",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search and Filters Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Screen Title and Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (filters.showArchived) "Archived Games" else title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (filters.showArchived) {
                                "${games.size} ${if (games.size == 1) "game" else "games"} hidden from active library"
                            } else {
                                "${games.size} ${if (games.size == 1) "game" else "games"}"
                            },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    // Sort Button & Menu
                    Box {
                        OutlinedButton(
                            onClick = { isSortMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard),
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = PrimaryRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = filters.sortOption.displayName,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false },
                            modifier = Modifier
                                .background(DarkCard)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            color = if (filters.sortOption == option) PrimaryRed else TextPrimary,
                                            fontWeight = if (filters.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSortChange(option)
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Summary Card at the top of Library
                if (totalVaultGames.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_progress_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald.copy(alpha = 0.15f))
                                            .border(1.dp, AccentEmerald.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Completion Progress",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$completedGamesCount of $activeGamesCount games completed",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = AccentEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "$completionPercentageInt%",
                                        color = AccentEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { completionRatio },
                                color = AccentEmerald,
                                trackColor = Color(0xFF1E2536),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Active vs Archived View Toggle Chips
                if (showArchiveToggle && onToggleShowArchived != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !filters.showArchived,
                            onClick = { if (filters.showArchived) onToggleShowArchived() },
                            label = { Text("Active Library ($activeGamesCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed.copy(alpha = 0.18f),
                                selectedLabelColor = PrimaryRed,
                                selectedLeadingIconColor = PrimaryRed,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !filters.showArchived,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = PrimaryRed
                            ),
                            modifier = Modifier.testTag("filter_chip_active_games")
                        )

                        FilterChip(
                            selected = filters.showArchived,
                            onClick = { if (!filters.showArchived) onToggleShowArchived() },
                            label = { Text("Archived ($archivedGamesCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentAmber.copy(alpha = 0.2f),
                                selectedLabelColor = AccentAmber,
                                selectedLeadingIconColor = AccentAmber,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filters.showArchived,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = AccentAmber
                            ),
                            modifier = Modifier.testTag("filter_chip_archived_games")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search Bar
                OutlinedTextField(
                    value = filters.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by title, genre, platform, status...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted
                        )
                    },
                    trailingIcon = {
                        if (filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Filter Chips
                if (showStatusFilter) {
                    val statusCounts = remember(totalVaultGames, filters.showArchived) {
                        totalVaultGames.filter { it.isArchived == filters.showArchived }.groupingBy { it.status }.eachCount()
                    }
                    val currentSectionTotal = if (filters.showArchived) archivedGamesCount else activeGamesCount

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = filters.selectedStatus == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { onStatusChange(null) },
                            label = { Text("All ($currentSectionTotal)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed.copy(alpha = 0.18f),
                                selectedLabelColor = PrimaryRed,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = PrimaryRed
                            ),
                            modifier = Modifier.testTag("status_chip_all")
                        )

                        GameStatus.entries.forEach { s ->
                            val isSelected = filters.selectedStatus == s
                            val count = statusCounts[s] ?: 0
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStatusChange(if (isSelected) null else s) },
                                label = { Text("${s.displayName} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryRed.copy(alpha = 0.18f),
                                    selectedLabelColor = PrimaryRed,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = PrimaryRed
                                ),
                                modifier = Modifier.testTag("status_chip_${s.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Platform Filters
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllPlatforms = filters.selectedPlatform == null || filters.selectedPlatform == "All"
                    FilterChip(
                        selected = isAllPlatforms,
                        onClick = { onPlatformChange(null) },
                        label = { Text("All Platforms") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed.copy(alpha = 0.18f),
                            selectedLabelColor = PrimaryRed,
                            containerColor = DarkCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAllPlatforms,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = PrimaryRed
                        )
                    )

                    CommonPlatforms.take(5).forEach { p ->
                        val isSelected = filters.selectedPlatform == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformChange(if (isSelected) null else p) },
                            label = { Text(p) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed.copy(alpha = 0.18f),
                                selectedLabelColor = PrimaryRed,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = PrimaryRed
                            )
                        )
                    }
                }

                // Franchise Filter Chips
                val franchises = remember(totalVaultGames) {
                    totalVaultGames.mapNotNull { it.franchiseName?.takeIf { f -> f.isNotBlank() } }.distinct()
                }
                if (franchises.isNotEmpty() && onFranchiseChange != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FRANCHISE / SERIES",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val isAllFranchises = filters.selectedFranchise == null || filters.selectedFranchise == "All"
                        FilterChip(
                            selected = isAllFranchises,
                            onClick = { onFranchiseChange(null) },
                            label = { Text("All Series (${franchises.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryRed,
                                selectedLabelColor = Color.White,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllFranchises,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = PrimaryRed
                            )
                        )
                        franchises.forEach { f ->
                            val isSelected = filters.selectedFranchise.equals(f, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onFranchiseChange(if (isSelected) null else f) },
                                label = { Text(f, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = PrimaryRed
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Empty State
        if (games.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkCard)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (filters.showArchived) Icons.Default.Archive else Icons.Outlined.VideogameAssetOff,
                                contentDescription = null,
                                tint = if (filters.showArchived) AccentAmber.copy(alpha = 0.8f) else PrimaryRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (filters.showArchived) "No Archived Games" else "No Games Found",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (filters.showArchived) {
                                "You haven't hidden or archived any games yet. Open any game in your library and tap \"Archive Game\" to tuck it away and keep your active library focused."
                            } else if (filters.searchQuery.isNotBlank() || filters.selectedPlatform != null || filters.selectedStatus != null) {
                                "No games match your active filters. Try adjusting search or clearing filters."
                            } else {
                                "Your vault is currently empty. Add your first video game to start tracking!"
                            },
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onAddGame,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Game")
                        }
                    }
                }
            }
        } else {
            // Game Cards
            items(
                items = games,
                key = { it.id },
                contentType = { "game_card" }
            ) { game ->
                val onSelect = remember(game.id, onGameClick) { { onGameClick(game) } }
                val onFav = remember(game.id, onToggleFavorite) { { onToggleFavorite(game) } }
                GameCard(
                    game = game,
                    onClick = onSelect,
                    onToggleFavorite = onFav
                )
            }
        }
    }
}
