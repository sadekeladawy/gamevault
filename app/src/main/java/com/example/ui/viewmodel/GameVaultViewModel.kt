package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SearchHistoryManager
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.GameRepository
import com.example.data.sample.SampleGames
import com.example.data.remote.rawg.RawgApiClient
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.remote.rawg.RawgRepository
import com.example.ui.components.AuthTab
import com.example.ui.components.ComparableGame
import com.example.ui.screens.RawgFilterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class NavDestination(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "home"),
    GAME_DATABASE("Search", "search"),
    AI_CHAT("AI Assistant", "auto_awesome"),
    LIBRARY("My Games", "sports_esports"),
    COMPLETED("Completed", "check_circle"),
    PLAYING("Currently Playing", "play_circle"),
    BACKLOG("Backlog", "menu_book"),
    FAVORITES("Favorites", "star"),
    STATISTICS("Statistics", "bar_chart"),
    SETTINGS("Settings", "settings"),
    AUTH("Account", "account_circle")
}

enum class SortOption(val displayName: String) {
    DATE_ADDED("Date Added"),
    ALPHABETICAL("Name (A to Z)"),
    RELEASE_YEAR("Release Year"),
    RECENTLY_COMPLETED("Recently Completed"),
    RATING("Highest Rating"),
    PLAYTIME("Most Playtime")
}

data class LibraryFilters(
    val searchQuery: String = "",
    val selectedPlatform: String? = null,
    val selectedGenre: String? = null,
    val selectedStatus: GameStatus? = null,
    val sortOption: SortOption = SortOption.RECENTLY_COMPLETED,
    val showArchived: Boolean = false
)

data class VaultStats(
    val totalGames: Int = 0,
    val completedCount: Int = 0,
    val completedThisYear: Int = 0,
    val currentlyPlayingCount: Int = 0,
    val backlogCount: Int = 0,
    val wishlistCount: Int = 0,
    val droppedCount: Int = 0,
    val favoritesCount: Int = 0,
    val archivedCount: Int = 0,
    val totalPlaytimeHours: Double = 0.0,
    val averageRating: Double = 0.0,
    val averagePersonalRating: Double = 0.0,
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
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(application)
    private val authRepository: AuthRepository = AuthRepository(application, firestoreRepository)
    private val searchHistoryManager: SearchHistoryManager = SearchHistoryManager(application)

    val searchHistory: StateFlow<List<String>> = searchHistoryManager.history

    private val _personalizedRecommendations = MutableStateFlow<List<RawgGameDto>>(emptyList())
    val personalizedRecommendations: StateFlow<List<RawgGameDto>> = _personalizedRecommendations.asStateFlow()

    private val _comparisonPair = MutableStateFlow<Pair<ComparableGame, ComparableGame>?>(null)
    val comparisonPair: StateFlow<Pair<ComparableGame, ComparableGame>?> = _comparisonPair.asStateFlow()

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
    val authState: StateFlow<AuthState> = authRepository.authState
    val isFirebaseConfigured: Boolean get() = authRepository.isFirebaseConfigured()
    val unverifiedEmail: StateFlow<String?> = authRepository.unverifiedEmail

    private val _isResendingEmail = MutableStateFlow(false)
    val isResendingEmail: StateFlow<Boolean> = _isResendingEmail.asStateFlow()

    private val _isAuthModalOpen = MutableStateFlow(false)
    val isAuthModalOpen: StateFlow<Boolean> = _isAuthModalOpen.asStateFlow()

    private val _authModalInitialTab = MutableStateFlow(AuthTab.SIGN_IN)
    val authModalInitialTab: StateFlow<AuthTab> = _authModalInitialTab.asStateFlow()

    private val _isProfileModalOpen = MutableStateFlow(false)
    val isProfileModalOpen: StateFlow<Boolean> = _isProfileModalOpen.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastCloudSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastCloudSyncTimestamp: StateFlow<Long?> = _lastCloudSyncTimestamp.asStateFlow()

    // Guest Mode is the default: the app opens straight into the Dashboard, signed in or not.
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

    // --- RAWG Video Games API Integration ---
    private var rawgRepository: RawgRepository = RawgRepository()

    private val _activeGamingSession = MutableStateFlow<Pair<Game, Long>?>(null)
    val activeGamingSession: StateFlow<Pair<Game, Long>?> = _activeGamingSession.asStateFlow()

    val aiChatViewModel: AiChatViewModel = AiChatViewModel()

    private val _rawgSearchQuery = MutableStateFlow("")
    val rawgSearchQuery: StateFlow<String> = _rawgSearchQuery.asStateFlow()

    private val _rawgFilterOptions = MutableStateFlow(RawgFilterOptions())
    val rawgFilterOptions: StateFlow<RawgFilterOptions> = _rawgFilterOptions.asStateFlow()

    private val _selectedRawgGameForDetails = MutableStateFlow<RawgGameDto?>(null)
    val selectedRawgGameForDetails: StateFlow<RawgGameDto?> = _selectedRawgGameForDetails.asStateFlow()

    private val _rawgSearchResults = MutableStateFlow<List<RawgGameDto>>(emptyList())
    val rawgSearchResults: StateFlow<List<RawgGameDto>> = _rawgSearchResults.asStateFlow()

    private val _popularRawgGames = MutableStateFlow<List<RawgGameDto>>(emptyList())
    val popularRawgGames: StateFlow<List<RawgGameDto>> = _popularRawgGames.asStateFlow()

    private val _isPopularRawgLoading = MutableStateFlow(false)
    val isPopularRawgLoading: StateFlow<Boolean> = _isPopularRawgLoading.asStateFlow()

    private val _isRawgLoading = MutableStateFlow(false)
    val isRawgLoading: StateFlow<Boolean> = _isRawgLoading.asStateFlow()

    private val _rawgErrorMessage = MutableStateFlow<String?>(null)
    val rawgErrorMessage: StateFlow<String?> = _rawgErrorMessage.asStateFlow()

    private val _hasSearchedRawg = MutableStateFlow(false)
    val hasSearchedRawg: StateFlow<Boolean> = _hasSearchedRawg.asStateFlow()

    private var rawgSearchJob: Job? = null

    // Identity key used to scope the local Room cache. Guests (signed-out / unverified users)
    // share the empty-string key so their locally-added games persist across app restarts,
    // exactly like a real account's games would, without ever touching Firestore.
    private var activeSessionKey: String = GUEST_SESSION_KEY
    private var isSessionInitialized: Boolean = false

    companion object {
        private const val GUEST_SESSION_KEY = ""
    }

    init {
        val db = AppDatabase.getDatabase(application)
        rawgRepository = RawgRepository(rawgCacheDao = db.rawgCacheDao())
        repository = GameRepository(db.gameDao())

        // Setup allGames state stream from Room
        allGames = repository.allGames.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Observe current user changes for complete data isolation between accounts
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                handleUserSessionChanged(user)
            }
        }

        // Check auto-login on startup. Authentication is optional: the app already opens on
        // the Dashboard (Guest Mode by default — see _currentDestination above) regardless of
        // whether this resolves to a signed-in user, and we never force-navigate away from
        // wherever the person may already be tapping. handleUserSessionChanged() (below) takes
        // care of loading the right data set: cloud games for a verified user, or the Guest's
        // own persisted local library.
        viewModelScope.launch {
            authRepository.checkAutoLogin()
        }

        // Filter and sort games reactively
        filteredGames = combine(allGames, _libraryFilters, _currentDestination) { games, filters, destination ->
            var list = games

            // Archive/Hide filter: keep library focused on current projects by hiding archived games by default
            list = if (filters.showArchived) {
                list.filter { it.isArchived }
            } else {
                list.filter { !it.isArchived }
            }

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
                SortOption.DATE_ADDED -> list.sortedByDescending { it.createdAt }
                SortOption.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_YEAR -> list.sortedByDescending { it.releaseYear }
                SortOption.RECENTLY_COMPLETED -> {
                    list.sortedWith(
                        compareByDescending<Game> { it.completionDate.orEmpty() }
                            .thenByDescending { it.createdAt }
                    )
                }
                SortOption.RATING -> list.sortedByDescending { it.rating }
                SortOption.PLAYTIME -> list.sortedByDescending { it.playtimeHours }
            }
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Calculate statistics reactively on background thread without recomputing on tab switch
        stats = allGames.map { games ->
            calculateStats(games)
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VaultStats()
        )

        loadPopularRawgGames()
    }

    /**
     * Keeps the local Room cache isolated per identity: Guest Mode (no verified user) has its
     * own local-only library, and every authenticated account has its own Firestore-backed
     * library under users/{uid}. Whenever the active identity changes we clear the local cache
     * and reload the correct data set so no previous user's (or guest's) games can leak into
     * the new session.
     *
     * On cold start we do NOT clear anything if the resolved identity is Guest, so a Guest's
     * own locally-added games survive an app restart instead of being wiped every launch.
     */
    private suspend fun handleUserSessionChanged(user: UserProfile?) {
        val newKey = if (user != null && user.isEmailVerified) user.uid else GUEST_SESSION_KEY

        if (!isSessionInitialized) {
            isSessionInitialized = true
            activeSessionKey = newKey
            if (newKey != GUEST_SESSION_KEY) {
                // App launched already signed in (auto-login): start from a clean slate and
                // load this user's cloud games so no stray local data is visible.
                Log.i("GameVaultViewModel", "Starting session for authenticated user: $newKey")
                repository.clearAllGames()
                clearTransientState()
                loadUserGamesFromCloud(newKey)
            }
            // else: cold start in Guest Mode — leave the Guest's existing local library as-is.
            return
        }

        if (activeSessionKey != newKey) {
            Log.i("GameVaultViewModel", "Switching active session: '$activeSessionKey' -> '$newKey'")
            activeSessionKey = newKey
            // Clear local Room cache so the previous identity's games never bleed into the new one.
            repository.clearAllGames()
            clearTransientState()
            if (newKey != GUEST_SESSION_KEY) {
                loadUserGamesFromCloud(newKey)
            }
            // else: switched back to Guest Mode — starts with an empty local library, per design.
        }
    }

    private suspend fun loadUserGamesFromCloud(uid: String) {
        _isCloudSyncing.value = true
        val result = firestoreRepository.fetchGamesFromCloud(uid)
        _isCloudSyncing.value = false
        result.onSuccess { cloudGames ->
            if (cloudGames.isNotEmpty()) {
                repository.importGames(cloudGames)
                Log.i("GameVaultViewModel", "Loaded ${cloudGames.size} games from cloud for user $uid")
            } else {
                Log.i("GameVaultViewModel", "Empty profile for user $uid: 0 games loaded")
            }
        }.onFailure { err ->
            Log.w("GameVaultViewModel", "Could not fetch games from cloud: ${err.message}")
        }
    }

    private fun clearTransientState() {
        _selectedGameForDetails.value = null
        _gameToEdit.value = null
        _gameToDelete.value = null
        _libraryFilters.value = LibraryFilters()
        _rawgSearchResults.value = emptyList()
        _rawgSearchQuery.value = ""
        _hasSearchedRawg.value = false
        _lastCloudSyncTimestamp.value = null
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
            archivedCount = games.count { it.isArchived },
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

    /**
     * Navigation is always allowed, signed in or not: Dashboard, Search, game discovery,
     * Settings, and the rest of the app are open to Guests. Individual actions that genuinely
     * require an account (Cloud Sync, Restore from Cloud, etc.) are gated at the point of use
     * — see syncLibraryToCloud() / restoreLibraryFromCloud() — where a Guest is offered a
     * friendly Sign In / Create Account prompt instead of being blocked from the whole app.
     */
    fun navigateTo(destination: NavDestination) {
        _currentDestination.value = destination
    }

    fun startGamingSession(game: Game) {
        _activeGamingSession.value = Pair(game, System.currentTimeMillis())
        viewModelScope.launch {
            _snackbarMessage.emit("Started gaming session for \"${game.title}\"")
        }
    }

    fun stopGamingSession() {
        val session = _activeGamingSession.value ?: return
        val game = session.first
        val startTime = session.second
        val elapsedMs = System.currentTimeMillis() - startTime
        val addedHours = (elapsedMs / (1000.0 * 3600.0)).coerceAtLeast(0.01)
        val roundedAdded = (Math.round(addedHours * 100.0) / 100.0)
        val newPlaytime = (Math.round((game.playtimeHours + roundedAdded) * 100.0) / 100.0)

        val updatedGame = game.copy(
            playtimeHours = newPlaytime,
            status = if (game.status == GameStatus.BACKLOG || game.status == GameStatus.WISHLIST) GameStatus.CURRENTLY_PLAYING else game.status
        )

        val user = currentUser.value
        viewModelScope.launch {
            repository.updateGame(updatedGame)
            if (user != null) {
                firestoreRepository.saveUserGame(user.uid, updatedGame)
            }
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updatedGame
            }
            _activeGamingSession.value = null
            _snackbarMessage.emit("Logged session: +${String.format(Locale.US, "%.2f", roundedAdded)} hours to \"${game.title}\"")
        }
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

    fun setShowArchived(show: Boolean) {
        _libraryFilters.value = _libraryFilters.value.copy(showArchived = show)
    }

    fun toggleShowArchived() {
        _libraryFilters.value = _libraryFilters.value.copy(showArchived = !_libraryFilters.value.showArchived)
    }

    fun toggleArchiveGame(game: Game) {
        val newArchivedState = !game.isArchived
        val updated = game.copy(isArchived = newArchivedState)
        val user = currentUser.value
        viewModelScope.launch {
            repository.setArchived(game, newArchivedState)
            if (user != null) {
                firestoreRepository.saveUserGame(user.uid, updated)
            }
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updated
            }
            val message = if (newArchivedState) {
                "Archived \"${game.title}\" (hidden from active library)"
            } else {
                "Restored \"${game.title}\" to active library"
            }
            _snackbarMessage.emit(message)
        }
    }

    fun archiveGame(game: Game) {
        if (!game.isArchived) {
            toggleArchiveGame(game)
        }
    }

    fun unarchiveGame(game: Game) {
        if (game.isArchived) {
            toggleArchiveGame(game)
        }
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
        val user = currentUser.value
        val uid = user?.uid ?: ""
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
                    isFavorite = isFavorite,
                    userId = uid
                )
                val newId = repository.insertGame(newGame)
                val savedGame = newGame.copy(id = newId)
                if (uid.isNotBlank()) {
                    firestoreRepository.saveUserGame(uid, savedGame)
                }
                _snackbarMessage.emit("Added \"$title\" to your Vault")
            } else {
                val existingGame = allGames.value.find { it.id == id }
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
                    rawgRating = existingGame?.rawgRating ?: 0.0,
                    metacriticScore = existingGame?.metacriticScore,
                    developer = existingGame?.developer.orEmpty(),
                    publisher = existingGame?.publisher.orEmpty(),
                    notes = notes,
                    isFavorite = isFavorite,
                    isArchived = existingGame?.isArchived ?: false,
                    createdAt = existingGame?.createdAt ?: System.currentTimeMillis(),
                    userId = uid
                )
                repository.updateGame(updatedGame)
                if (uid.isNotBlank()) {
                    firestoreRepository.saveUserGame(uid, updatedGame)
                }
                _snackbarMessage.emit("Updated \"$title\"")
            }
            closeAddEdit()
        }
    }

    fun executeDeleteGame() {
        val game = _gameToDelete.value ?: return
        val user = currentUser.value
        viewModelScope.launch {
            repository.deleteGame(game)
            if (user != null) {
                firestoreRepository.deleteUserGame(user.uid, game.id)
            }
            _gameToDelete.value = null
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = null
            }
            _snackbarMessage.emit("Removed \"${game.title}\" from Vault")
        }
    }

    fun toggleFavorite(game: Game) {
        val updated = game.copy(isFavorite = !game.isFavorite)
        val user = currentUser.value
        viewModelScope.launch {
            repository.updateGame(updated)
            if (user != null) {
                firestoreRepository.saveUserGame(user.uid, updated)
            }
            // Update currently opened details if same game
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updated
            }
            val statusStr = if (updated.isFavorite) "marked as favorite" else "removed from favorites"
            _snackbarMessage.emit("\"${game.title}\" $statusStr")
        }
    }

    fun updateGameStatus(game: Game, newStatus: GameStatus) {
        val updatedDate = if (newStatus == GameStatus.COMPLETED) {
            game.completionDate ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        } else game.completionDate
        val updated = game.copy(status = newStatus, completionDate = updatedDate)
        val user = currentUser.value
        viewModelScope.launch {
            repository.updateGame(updated)
            if (user != null) {
                firestoreRepository.saveUserGame(user.uid, updated)
            }
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updated
            }
            _snackbarMessage.emit("Moved \"${game.title}\" to ${newStatus.displayName}")
        }
    }

    fun updateGameNotes(game: Game, notes: String) {
        val updated = game.copy(notes = notes, updatedAt = System.currentTimeMillis())
        val user = currentUser.value
        viewModelScope.launch {
            repository.updateGame(updated)
            if (user != null) {
                firestoreRepository.saveUserGame(user.uid, updated)
            }
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updated
            }
            _snackbarMessage.emit("Private notes updated for \"${game.title}\"")
        }
    }

    fun resetToSampleData() {
        val user = currentUser.value
        val uid = user?.uid ?: ""
        viewModelScope.launch {
            repository.resetToSampleData(uid)
            if (user != null) {
                val taggedGames = SampleGames.initialGames.map { it.copy(id = 0, userId = uid) }
                firestoreRepository.syncGamesToCloud(user.uid, taggedGames)
            }
            _snackbarMessage.emit("Reset library to sample game collection")
        }
    }

    fun clearAllGames() {
        val user = currentUser.value
        viewModelScope.launch {
            repository.clearAllGames()
            if (user != null) {
                firestoreRepository.clearAllUserGamesInCloud(user.uid)
            }
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
        val user = currentUser.value
        val uid = user?.uid ?: ""
        viewModelScope.launch {
            val result = repository.importFromJson(jsonString, uid)
            result.onSuccess { count ->
                if (user != null) {
                    firestoreRepository.syncGamesToCloud(user.uid, allGames.value)
                }
                _isImportModalOpen.value = false
                _snackbarMessage.emit("Successfully imported $count games into your Vault!")
            }.onFailure { err ->
                _snackbarMessage.emit("Import failed: ${err.localizedMessage ?: "Invalid JSON format"}")
            }
        }
    }

    // --- Firebase Auth & Firestore Sync ---

    fun openAuthModal(initialTab: AuthTab = AuthTab.SIGN_IN) {
        _authModalInitialTab.value = initialTab
        _isAuthModalOpen.value = true
    }

    fun closeAuthModal() {
        _isAuthModalOpen.value = false
    }

    fun openProfileModal() {
        _isProfileModalOpen.value = true
    }

    fun closeProfileModal() {
        _isProfileModalOpen.value = false
    }

    fun signIn(email: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass, rememberMe)
            result.onSuccess { user ->
                _isAuthModalOpen.value = false
                _currentDestination.value = NavDestination.DASHBOARD
                _snackbarMessage.emit("Welcome back, ${user.fullName}!")
            }.onFailure { err ->
                if (err is com.example.data.repository.EmailNotVerifiedException) {
                    _currentDestination.value = NavDestination.AUTH
                    _snackbarMessage.emit("Email verification required. Check your inbox or click Resend.")
                }
            }
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        pass: String,
        confirmPass: String,
        gamerTag: String?,
        rememberMe: Boolean
    ) {
        viewModelScope.launch {
            val result = authRepository.signUp(fullName, email, pass, confirmPass, gamerTag, rememberMe)
            result.onSuccess {
                _isAuthModalOpen.value = false
                _currentDestination.value = NavDestination.AUTH
                _snackbarMessage.emit("Account created! Verification email sent to $email. Please verify your email before logging in.")
            }
        }
    }

    fun resendVerificationEmail(email: String? = null, pass: String? = null) {
        viewModelScope.launch {
            _isResendingEmail.value = true
            val result = authRepository.resendVerificationEmail(email, pass)
            _isResendingEmail.value = false
            result.onSuccess {
                _snackbarMessage.emit("Verification email sent! Please check your inbox.")
            }.onFailure { err ->
                _snackbarMessage.emit(err.localizedMessage ?: "Failed to resend verification email.")
            }
        }
    }

    fun clearAuthErrors() {
        authRepository.clearAuthState()
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            result.onSuccess {
                _snackbarMessage.emit("Password reset email sent! Check your inbox.")
            }.onFailure { err ->
                _snackbarMessage.emit(err.localizedMessage ?: "Failed to send reset email.")
            }
        }
    }

    fun sendEmailVerification() {
        resendVerificationEmail()
    }

    fun refreshUserVerification() {
        viewModelScope.launch {
            val result = authRepository.reloadUserVerification()
            result.onSuccess { user ->
                if (user?.isEmailVerified == true) {
                    _currentDestination.value = NavDestination.DASHBOARD
                    _snackbarMessage.emit("Email verified successfully! Welcome to GameVault.")
                } else {
                    _snackbarMessage.emit("Email is not verified yet. Please check your inbox.")
                }
            }
        }
    }

    fun updateUserProfile(fullName: String, gamerTag: String?, photoUrl: String?) {
        viewModelScope.launch {
            val result = authRepository.updateProfile(fullName, gamerTag, photoUrl)
            result.onSuccess {
                _snackbarMessage.emit("Profile updated successfully!")
            }.onFailure { err ->
                _snackbarMessage.emit("Update failed: ${err.localizedMessage}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Clear local Room database completely so no trace of this account's games remains,
            // then return the user to a fresh, empty Guest Mode session instead of a Sign In wall.
            activeSessionKey = GUEST_SESSION_KEY
            repository.clearAllGames()
            clearTransientState()
            authRepository.signOut()
            _currentDestination.value = NavDestination.DASHBOARD
            _isProfileModalOpen.value = false
            _snackbarMessage.emit("Signed out. You're browsing as a Guest.")
        }
    }

    fun syncLibraryToCloud() {
        val user = currentUser.value
        if (user == null) {
            openAuthModal()
            return
        }

        viewModelScope.launch {
            _isCloudSyncing.value = true
            val currentGames = allGames.value
            val result = firestoreRepository.syncGamesToCloud(user.uid, currentGames)
            _isCloudSyncing.value = false
            result.onSuccess { count ->
                val now = System.currentTimeMillis()
                _lastCloudSyncTimestamp.value = now
                _snackbarMessage.emit("Successfully synced $count games to Cloud Firestore!")
            }.onFailure { err ->
                _snackbarMessage.emit("Cloud sync failed: ${err.localizedMessage}")
            }
        }
    }

    fun restoreLibraryFromCloud() {
        val user = currentUser.value
        if (user == null) {
            openAuthModal()
            return
        }

        viewModelScope.launch {
            _isCloudSyncing.value = true
            val result = firestoreRepository.fetchGamesFromCloud(user.uid)
            _isCloudSyncing.value = false
            result.onSuccess { cloudGames ->
                // Clear Room cache first, then restore this user's cloud games
                repository.clearAllGames()
                if (cloudGames.isEmpty()) {
                    _snackbarMessage.emit("No games found in your Cloud Vault.")
                } else {
                    val count = repository.importGames(cloudGames)
                    _snackbarMessage.emit("Restored $count games from Cloud Firestore into your vault!")
                }
            }.onFailure { err ->
                _snackbarMessage.emit("Failed to restore from cloud: ${err.localizedMessage}")
            }
        }
    }

    // --- RAWG Video Games Discovery & Autocomplete ---

    fun loadPopularRawgGames() {
        viewModelScope.launch {
            _isPopularRawgLoading.value = true
            val result = repository.getTopRawgGames(pageSize = 12)
            _isPopularRawgLoading.value = false
            result.onSuccess { games ->
                _popularRawgGames.value = games
            }.onFailure {
                _popularRawgGames.value = emptyList()
            }
        }
    }

    /**
     * Performs a fast live search for autocomplete in Add / Edit dialog.
     */
    suspend fun searchRawgAutocomplete(query: String): List<RawgGameDto> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        return try {
            val result = repository.searchRawgGames(trimmed, pageSize = 8)
            result.getOrDefault(emptyList())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isGameInVault(title: String): Boolean {
        return allGames.value.any { it.title.equals(title, ignoreCase = true) }
    }

    fun getVaultGame(title: String): Game? {
        return allGames.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
    }

    fun openRawgGameDetails(rawgGame: RawgGameDto) {
        _selectedRawgGameForDetails.value = rawgGame
    }

    fun closeRawgGameDetails() {
        _selectedRawgGameForDetails.value = null
    }

    fun openComparison(game1: ComparableGame, game2: ComparableGame) {
        _comparisonPair.value = Pair(game1, game2)
    }

    fun closeComparison() {
        _comparisonPair.value = null
    }

    fun addSearchHistoryQuery(query: String) {
        searchHistoryManager.addSearchQuery(query)
    }

    fun removeSearchHistoryQuery(query: String) {
        searchHistoryManager.removeSearchQuery(query)
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearSearchHistory()
    }

    fun addRawgGameToVault(rawgGame: RawgGameDto, initialStatus: GameStatus = GameStatus.BACKLOG) {
        val user = currentUser.value
        val uid = user?.uid ?: ""

        // Check if game is already in Vault to prevent duplicates
        val existingGame = allGames.value.firstOrNull { it.title.equals(rawgGame.name, ignoreCase = true) }
        if (existingGame != null) {
            viewModelScope.launch {
                closeRawgGameDetails()
                closeGameDetails()
                _libraryFilters.value = _libraryFilters.value.copy(searchQuery = existingGame.title)
                _currentDestination.value = NavDestination.LIBRARY
                _snackbarMessage.emit("\"${rawgGame.name}\" is already in your Vault!")
            }
            return
        }

        viewModelScope.launch {
            val newGame = Game(
                title = rawgGame.name,
                coverUrl = rawgGame.backgroundImage ?: "",
                platform = rawgGame.platforms?.firstOrNull()?.platform?.name ?: "PC",
                genre = rawgGame.genres?.firstOrNull()?.name ?: "Action",
                releaseYear = rawgGame.released?.take(4)?.toIntOrNull() ?: 2024,
                status = initialStatus,
                playtimeHours = (rawgGame.playtime ?: 0).toDouble(),
                rating = 0, // Personal rating unrated by default
                rawgRating = rawgGame.rating ?: 0.0,
                metacriticScore = rawgGame.metacritic,
                developer = rawgGame.developers?.firstOrNull()?.name ?: "",
                publisher = rawgGame.publishers?.firstOrNull()?.name ?: "",
                notes = rawgGame.descriptionRaw ?: rawgGame.description ?: "Added from RAWG / GameVault AI",
                userId = uid
            )

            val insertedId = repository.insertGame(newGame)

            if (uid.isNotBlank()) {
                val gameToSync = newGame.copy(id = insertedId, userId = uid)
                firestoreRepository.saveUserGame(uid, gameToSync)
            }

            closeRawgGameDetails()
            closeGameDetails()
            _libraryFilters.value = LibraryFilters() // Reset filters so newly added game is visible at top
            _currentDestination.value = NavDestination.LIBRARY
            _snackbarMessage.emit("Added \"${rawgGame.name}\" to your Vault!")
        }
    }

    // --- RAWG Search Methods ---

    fun setRawgFilterOptions(options: RawgFilterOptions) {
        _rawgFilterOptions.value = options
        rawgSearchJob?.cancel()
        rawgSearchJob = viewModelScope.launch {
            delay(300)
            executeRawgSearch(_rawgSearchQuery.value)
        }
    }

    /**
     * Updates the search query and sends request automatically with debounce.
     */
    fun setRawgSearchQuery(query: String) {
        _rawgSearchQuery.value = query
        rawgSearchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty() && !hasActiveFilters(_rawgFilterOptions.value)) {
            _rawgSearchResults.value = emptyList()
            _rawgErrorMessage.value = null
            _isRawgLoading.value = false
            _hasSearchedRawg.value = false
            return
        }

        // Send request automatically after 450ms debounce
        rawgSearchJob = viewModelScope.launch {
            delay(450)
            executeRawgSearch(trimmed)
        }
    }

    private fun hasActiveFilters(options: RawgFilterOptions): Boolean {
        return options.selectedGenre != null || options.selectedPlatform != null ||
                options.selectedYearRange != null || options.minMetacritic != null ||
                options.ordering != "-rating"
    }

    /**
     * Executes immediate search for the given query.
     */
    fun searchRawg(query: String? = null) {
        val target = (query ?: _rawgSearchQuery.value).trim()
        rawgSearchJob?.cancel()
        viewModelScope.launch {
            executeRawgSearch(target)
        }
    }

    fun retryRawgSearch() {
        searchRawg(_rawgSearchQuery.value)
    }

    private suspend fun executeRawgSearch(query: String) {
        _isRawgLoading.value = true
        _rawgErrorMessage.value = null
        _hasSearchedRawg.value = true

        val filters = _rawgFilterOptions.value
        val result = repository.searchRawgGamesWithFilters(
            query = query,
            genres = filters.selectedGenre,
            platforms = filters.selectedPlatform,
            dates = filters.selectedYearRange,
            metacritic = filters.minMetacritic?.let { "$it,100" },
            ordering = filters.ordering
        )
        _isRawgLoading.value = false
        result.onSuccess { games ->
            _rawgSearchResults.value = games
            _rawgErrorMessage.value = null
        }.onFailure { err ->
            _rawgSearchResults.value = emptyList()
            _rawgErrorMessage.value = err.localizedMessage ?: "Failed to query RAWG API."
        }
    }
}
