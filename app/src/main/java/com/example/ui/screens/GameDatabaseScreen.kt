package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto

/**
 * Game Database Screen powered 100% by the RAWG Video Games Database API.
 * Provides live global discovery, search, and advanced filtering.
 */
@Composable
fun GameDatabaseScreen(
    searchQuery: String,
    searchResults: List<RawgGameDto>,
    isLoading: Boolean,
    errorMessage: String?,
    hasSearched: Boolean,
    filterOptions: RawgFilterOptions = RawgFilterOptions(),
    searchHistory: List<String> = emptyList(),
    onSearchChange: (String) -> Unit,
    onFilterChange: (RawgFilterOptions) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onRemoveSearchQuery: (String) -> Unit = {},
    onRetrySearch: () -> Unit,
    onSelectGame: (RawgGameDto) -> Unit = {},
    onAddGameToVault: (RawgGameDto, GameStatus) -> Unit,
    isGameInVault: (String) -> Boolean,
    getVaultGame: (String) -> Game?,
    modifier: Modifier = Modifier
) {
    RawgSearchScreen(
        searchQuery = searchQuery,
        searchResults = searchResults,
        isLoading = isLoading,
        errorMessage = errorMessage,
        hasSearched = hasSearched,
        filterOptions = filterOptions,
        searchHistory = searchHistory,
        onSearchChange = onSearchChange,
        onFilterChange = onFilterChange,
        onClearSearchHistory = onClearSearchHistory,
        onRemoveSearchQuery = onRemoveSearchQuery,
        onRetrySearch = onRetrySearch,
        onSelectGame = onSelectGame,
        onAddGameToVault = onAddGameToVault,
        isGameInVault = isGameInVault,
        getVaultGame = getVaultGame,
        modifier = modifier.fillMaxSize()
    )
}
