package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto

/**
 * Game Database Screen powered 100% by the RAWG Video Games Database API.
 * Replaces the legacy Firestore games catalog with live global discovery.
 */
@Composable
fun GameDatabaseScreen(
    searchQuery: String,
    searchResults: List<RawgGameDto>,
    isLoading: Boolean,
    errorMessage: String?,
    hasSearched: Boolean,
    onSearchChange: (String) -> Unit,
    onRetrySearch: () -> Unit,
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
        onSearchChange = onSearchChange,
        onRetrySearch = onRetrySearch,
        onAddGameToVault = onAddGameToVault,
        isGameInVault = isGameInVault,
        getVaultGame = getVaultGame,
        modifier = modifier.fillMaxSize()
    )
}
