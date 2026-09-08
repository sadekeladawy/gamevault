package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavDestination(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "home"),
    LIBRARY("My Games", "sports_esports"),
    COMPLETED("Completed", "check_circle"),
    PLAYING("Currently Playing", "play_circle"),
    BACKLOG("Backlog", "menu_book"),
    FAVORITES("Favorites", "star"),
    STATISTICS("Statistics", "bar_chart"),
    SETTINGS("Settings", "settings")
}

enum class SortOption(val displayName: String) {
    RECENTLY_COMPLETED("Recently Completed"),
    RATING("Highest Rating"),
    PLAYTIME("Most Playtime"),
    ALPHABETICAL("A to Z"),
    RELEASE_YEAR("Release Year")
}

data class LibraryFilters(
    val searchQuery: String = "",
    val selectedPlatform: String? = null,
    val selectedGenre: String? = null,
    val selectedStatus: GameStatus? = null,
    val sortOption: SortOption = SortOption.RECENTLY_COMPLETED
)

data class VaultStats(
    val totalGames: Int = 0,
    val completedCount: Int = 0,
    val completedThisYear: Int = 0,
    val currentlyPlayingCount: Int = 0,
    val backlogCount: Int = 0,
    val droppedCount: Int = 0,
    val favoritesCount: Int = 0,
    val totalPlaytimeHours: Double = 0.0,
    val averageRating: Double = 0.0,
    val completionStreakMonths: Int = 0,
    val yearlyCompletions: Map<Int, Int> = emptyMap(),
    val monthlyCompletions: Map<Int, Int> = emptyMap(), // 1 to 12
    val genreDistribution: List<Pair<String, Int>> = emptyList(),
    val platformDistribution: List<Pair<String, Int>> = emptyList(),
    val highestRatedGames: List<Game> = emptyList(),
    val recentlyCompletedGames: List<Game> = emptyList()
)

class GameVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    private val _currentDestination = MutableStateFlow(NavDestination.DASHBOARD)
    val currentDestination: StateFlow<NavDestination> = _currentDestination.asStateFlow()

    private val _libraryFilters = MutableStateFlow(LibraryFilters())
    val libraryFilters: StateFlow<LibraryFilters> = _libraryFilters.asStateFlow()

    private val _selectedGameForDetails = MutableStateFlow<Game?>(null)
    val selectedGameForDetails: StateFlow<Game?> = _selectedGameForDetails.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _gameToEdit = MutableStateFlow<Game?>(null)
    val gameToEdit: StateFlow<Game?> = _gameToEdit.asStateFlow()

    private val _gameToDelete = MutableStateFlow<Game?>(null)
    val gameToDelete: StateFlow<Game?> = _gameToDelete.asStateFlow()

    private val _exportModalData = MutableStateFlow<Pair<String, String>?>(null) // Pair(type, content)
    val exportModalData: StateFlow<Pair<String, String>?> = _exportModalData.asStateFlow()

    private val _isImportModalOpen = MutableStateFlow(false)
    val isImportModalOpen: StateFlow<Boolean> = _isImportModalOpen.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val allGames: StateFlow<List<Game>>

    val filteredGames: StateFlow<List<Game>>

    val stats: StateFlow<VaultStats>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GameRepository(db.gameDao())

        // Ensure sample data is loaded on first launch
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }

        allGames = repository.allGames.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Filter and sort games reactively
        filteredGames = combine(allGames, _libraryFilters, _currentDestination) { games, filters, destination ->
            var list = games

            // If on a destination-specific view, pre-filter by that status or favorite
            when (destination) {
                NavDestination.COMPLETED -> list = list.filter { it.status == GameStatus.COMPLETED }
                NavDestination.PLAYING -> list = list.filter { it.status == GameStatus.CURRENTLY_PLAYING }
                NavDestination.BACKLOG -> list = list.filter { it.status == GameStatus.BACKLOG }
                NavDestination.FAVORITES -> list = list.filter { it.isFavorite }
                else -> {
                    // In My Games / Library, apply filters.selectedStatus if specified
                    if (filters.selectedStatus != null) {
                        list = list.filter { it.status == filters.selectedStatus }
                    }
                }
            }

            // Search query filter (title, genre, platform, status)
            if (filters.searchQuery.isNotBlank()) {
                val q = filters.searchQuery.trim().lowercase()
                list = list.filter { game ->
                    game.title.lowercase().contains(q) ||
                    game.genre.lowercase().contains(q) ||
                    game.platform.lowercase().contains(q) ||
                    game.status.displayName.lowercase().contains(q) ||
                    game.releaseYear.toString().contains(q)
                }
            }

            // Platform filter
            if (!filters.selectedPlatform.isNullOrBlank() && filters.selectedPlatform != "All") {
                list = list.filter { it.platform.equals(filters.selectedPlatform, ignoreCase = true) }
            }

            // Genre filter
            if (!filters.selectedGenre.isNullOrBlank() && filters.selectedGenre != "All") {
                list = list.filter { it.genre.equals(filters.selectedGenre, ignoreCase = true) }
            }

            // Sort
            when (filters.sortOption) {
                SortOption.RECENTLY_COMPLETED -> {
                    list.sortedWith(
                        compareByDescending<Game> { it.completionDate.orEmpty() }
                            .thenByDescending { it.createdAt }
                    )
                }
                SortOption.RATING -> list.sortedByDescending { it.rating }
                SortOption.PLAYTIME -> list.sortedByDescending { it.playtimeHours }
                SortOption.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_YEAR -> list.sortedByDescending { it.releaseYear }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Calculate statistics reactively
        stats = allGames.combine(_currentDestination) { games, _ ->
            calculateStats(games)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VaultStats()
        )
    }

    private fun calculateStats(games: List<Game>): VaultStats {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val completed = games.filter { it.status == GameStatus.COMPLETED }
        val playing = games.filter { it.status == GameStatus.CURRENTLY_PLAYING }
        val backlog = games.filter { it.status == GameStatus.BACKLOG }
        val dropped = games.filter { it.status == GameStatus.DROPPED }
        val favorites = games.filter { it.isFavorite }

        val totalPlaytime = games.sumOf { it.playtimeHours }
        val ratedGames = games.filter { it.rating > 0 }
        val avgRating = if (ratedGames.isNotEmpty()) {
            ratedGames.map { it.rating }.average()
        } else {
            0.0
        }

        val completedThisYearCount = completed.count {
            it.completionDate?.startsWith(currentYear.toString()) == true
        }

        // Yearly completions
        val yearly = mutableMapOf<Int, Int>()
        // Initialize last 5 years with 0
        for (y in (currentYear - 4)..currentYear) {
            yearly[y] = 0
        }
        for (g in completed) {
            val yearStr = g.completionDate?.take(4)
            val y = yearStr?.toIntOrNull()
            if (y != null) {
                yearly[y] = (yearly[y] ?: 0) + 1
            }
        }

        // Monthly completions for current year
        val monthly = mutableMapOf<Int, Int>()
        for (m in 1..12) {
            monthly[m] = 0
        }
        for (g in completed) {
            val parts = g.completionDate?.split("-")
            if (parts != null && parts.size >= 2 && parts[0] == currentYear.toString()) {
                val m = parts[1].toIntOrNull()
                if (m != null && m in 1..12) {
                    monthly[m] = (monthly[m] ?: 0) + 1
                }
            }
        }

        // Genre distribution
        val genreMap = mutableMapOf<String, Int>()
        for (g in games) {
            val mainGenre = g.genre.split("/").firstOrNull()?.trim() ?: g.genre
            genreMap[mainGenre] = (genreMap[mainGenre] ?: 0) + 1
        }
        val genreDist = genreMap.toList().sortedByDescending { it.second }.take(6)

        // Platform distribution
        val platMap = mutableMapOf<String, Int>()
        for (g in games) {
            platMap[g.platform] = (platMap[g.platform] ?: 0) + 1
        }
        val platDist = platMap.toList().sortedByDescending { it.second }.take(6)

        // Highest rated games (top 5 rated 8+)
        val topRated = ratedGames.sortedWith(
            compareByDescending<Game> { it.rating }
                .thenByDescending { it.playtimeHours }
        ).take(5)

        // Recently completed (top 6)
        val recentCompleted = completed.sortedWith(
            compareByDescending<Game> { it.completionDate.orEmpty() }
                .thenByDescending { it.id }
        ).take(6)

        // Streak: consecutive months with at least 1 completed game (max estimate)
        val streak = if (completed.isNotEmpty()) {
            var s = 0
            val cal = Calendar.getInstance()
            for (i in 0..12) {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val yStr = y.toString()
                val mStr = String.format("%02d", m)
                val hasComp = completed.any { it.completionDate?.startsWith("$yStr-$mStr") == true }
                if (hasComp) {
                    s++
                    cal.add(Calendar.MONTH, -1)
                } else {
                    if (i == 0) {
                        // If none in current month yet, check previous month
                        cal.add(Calendar.MONTH, -1)
                    } else {
                        break
                    }
                }
            }
            s.coerceAtLeast(1)
        } else {
            0
        }

        return VaultStats(
            totalGames = games.size,
            completedCount = completed.size,
            completedThisYear = completedThisYearCount,
            currentlyPlayingCount = playing.size,
            backlogCount = backlog.size,
            droppedCount = dropped.size,
            favoritesCount = favorites.size,
            totalPlaytimeHours = totalPlaytime,
            averageRating = avgRating,
            completionStreakMonths = streak,
            yearlyCompletions = yearly,
            monthlyCompletions = monthly,
            genreDistribution = genreDist,
            platformDistribution = platDist,
            highestRatedGames = topRated,
            recentlyCompletedGames = recentCompleted
        )
    }

    fun navigateTo(destination: NavDestination) {
        _currentDestination.value = destination
    }

    fun setSearchQuery(query: String) {
        _libraryFilters.value = _libraryFilters.value.copy(searchQuery = query)
    }

    fun setPlatformFilter(platform: String?) {
        _libraryFilters.value = _libraryFilters.value.copy(selectedPlatform = platform)
    }

    fun setGenreFilter(genre: String?) {
        _libraryFilters.value = _libraryFilters.value.copy(selectedGenre = genre)
    }

    fun setStatusFilter(status: GameStatus?) {
        _libraryFilters.value = _libraryFilters.value.copy(selectedStatus = status)
    }

    fun setSortOption(sort: SortOption) {
        _libraryFilters.value = _libraryFilters.value.copy(sortOption = sort)
    }

    fun openGameDetails(game: Game) {
        _selectedGameForDetails.value = game
    }

    fun closeGameDetails() {
        _selectedGameForDetails.value = null
    }

    fun openAddGame() {
        _gameToEdit.value = null
        _isAddEditOpen.value = true
    }

    fun openEditGame(game: Game) {
        _gameToEdit.value = game
        _isAddEditOpen.value = true
        _selectedGameForDetails.value = null
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _gameToEdit.value = null
    }

    fun confirmDeleteGame(game: Game) {
        _gameToDelete.value = game
    }

    fun cancelDeleteGame() {
        _gameToDelete.value = null
    }

    fun saveGame(
        id: Long = 0,
        title: String,
        coverUrl: String,
        platform: String,
        genre: String,
        releaseYear: Int,
        status: GameStatus,
        completionDate: String?,
        playtimeHours: Double,
        rating: Int,
        notes: String,
        isFavorite: Boolean
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                val newGame = Game(
                    title = title,
                    coverUrl = coverUrl,
                    platform = platform,
                    genre = genre,
                    releaseYear = releaseYear,
                    status = status,
                    completionDate = if (status == GameStatus.COMPLETED && completionDate.isNullOrBlank()) {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    } else completionDate,
                    playtimeHours = playtimeHours,
                    rating = rating,
                    notes = notes,
                    isFavorite = isFavorite
                )
                repository.insertGame(newGame)
                _snackbarMessage.emit("Added \"$title\" to your Vault")
            } else {
                val updatedGame = Game(
                    id = id,
                    title = title,
                    coverUrl = coverUrl,
                    platform = platform,
                    genre = genre,
                    releaseYear = releaseYear,
                    status = status,
                    completionDate = if (status == GameStatus.COMPLETED && completionDate.isNullOrBlank()) {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    } else completionDate,
                    playtimeHours = playtimeHours,
                    rating = rating,
                    notes = notes,
                    isFavorite = isFavorite
                )
                repository.updateGame(updatedGame)
                _snackbarMessage.emit("Updated \"$title\"")
            }
            closeAddEdit()
        }
    }

    fun executeDeleteGame() {
        val game = _gameToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteGame(game)
            _gameToDelete.value = null
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = null
            }
            _snackbarMessage.emit("Removed \"${game.title}\" from Vault")
        }
    }

    fun toggleFavorite(game: Game) {
        viewModelScope.launch {
            repository.toggleFavorite(game)
            // Update currently opened details if same game
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = game.copy(isFavorite = !game.isFavorite)
            }
            val statusStr = if (!game.isFavorite) "marked as favorite" else "removed from favorites"
            _snackbarMessage.emit("\"${game.title}\" $statusStr")
        }
    }

    fun updateGameStatus(game: Game, newStatus: GameStatus) {
        viewModelScope.launch {
            repository.updateStatus(game, newStatus)
            if (_selectedGameForDetails.value?.id == game.id) {
                val updatedDate = if (newStatus == GameStatus.COMPLETED) {
                    game.completionDate ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                } else game.completionDate
                _selectedGameForDetails.value = game.copy(status = newStatus, completionDate = updatedDate)
            }
            _snackbarMessage.emit("Moved \"${game.title}\" to ${newStatus.displayName}")
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            _snackbarMessage.emit("Reset library to sample game collection")
        }
    }

    fun clearAllGames() {
        viewModelScope.launch {
            repository.clearAllGames()
            _selectedGameForDetails.value = null
            _snackbarMessage.emit("Cleared all games from your Vault")
        }
    }

    fun exportLibrary(type: String) {
        viewModelScope.launch {
            val games = allGames.value
            val content = if (type == "JSON") {
                repository.exportToJson(games)
            } else {
                repository.exportToCsv(games)
            }
            _exportModalData.value = Pair(type, content)
        }
    }

    fun closeExportModal() {
        _exportModalData.value = null
    }

    fun openImportModal() {
        _isImportModalOpen.value = true
    }

    fun closeImportModal() {
        _isImportModalOpen.value = false
    }

    fun importLibraryJson(jsonString: String) {
        viewModelScope.launch {
            val result = repository.importFromJson(jsonString)
            result.onSuccess { count ->
                _isImportModalOpen.value = false
                _snackbarMessage.emit("Successfully imported $count games into your Vault!")
            }.onFailure { err ->
                _snackbarMessage.emit("Import failed: ${err.localizedMessage ?: "Invalid JSON format"}")
            }
        }
    }
}
