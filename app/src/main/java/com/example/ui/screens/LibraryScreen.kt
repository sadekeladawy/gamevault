package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.VideogameAssetOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.components.CommonPlatforms
import com.example.ui.components.GameCard
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonCyan
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
    filters: LibraryFilters,
    showStatusFilter: Boolean = true,
    onSearchChange: (String) -> Unit,
    onStatusChange: (GameStatus?) -> Unit,
    onPlatformChange: (String?) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onGameClick: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onAddGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 155.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
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
                            text = title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${games.size} ${if (games.size == 1) "game" else "games"}",
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
                                tint = NeonCyan,
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
                                            color = if (filters.sortOption == option) NeonCyan else TextPrimary,
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
                        focusedBorderColor = CyberPurple,
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

                // Status Filters
                if (showStatusFilter) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = filters.selectedStatus == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { onStatusChange(null) },
                            label = { Text("All Status") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPurple.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyan,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = CyberPurple
                            )
                        )

                        GameStatus.entries.forEach { s ->
                            val isSelected = filters.selectedStatus == s
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStatusChange(if (isSelected) null else s) },
                                label = { Text(s.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = CyberPurple
                                )
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
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = DarkCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAllPlatforms,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = NeonCyan
                        )
                    )

                    CommonPlatforms.take(5).forEach { p ->
                        val isSelected = filters.selectedPlatform == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformChange(if (isSelected) null else p) },
                            label = { Text(p) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkCardBorder,
                                selectedBorderColor = NeonCyan
                            )
                        )
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
                                imageVector = Icons.Outlined.VideogameAssetOff,
                                contentDescription = null,
                                tint = CyberPurple.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Games Found",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (filters.searchQuery.isNotBlank() || filters.selectedPlatform != null || filters.selectedStatus != null) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
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
                GameCard(
                    game = game,
                    onClick = { onGameClick(game) },
                    onToggleFavorite = { onToggleFavorite(game) }
                )
            }
        }
    }
}
