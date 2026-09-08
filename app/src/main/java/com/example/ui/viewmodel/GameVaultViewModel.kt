package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavDestination(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "home"),
    GAME_DATABASE("Game Database", "search"),
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
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(application)
    private val authRepository: AuthRepository = AuthRepository(application, firestoreRepository)

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

    private val _currentDestination = MutableStateFlow(NavDestination.AUTH)
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
    private val rawgRepository: RawgRepository = RawgRepository()

    private val _rawgSearchQuery = MutableStateFlow("")
    val rawgSearchQuery: StateFlow<String> = _rawgSearchQuery.asStateFlow()

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
    private var activeSessionUserId: String? = null

    init {
        val db = AppDatabase.getDatabase(application)
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

        // Check auto-login on startup
        viewModelScope.launch {
            authRepository.checkAutoLogin()
            val user = authRepository.currentUser.value
            if (user != null && user.isEmailVerified) {
                _currentDestination.value = NavDestination.DASHBOARD
            } else {
                _currentDestination.value = NavDestination.AUTH
                // Clear any leftover data when unauthenticated
                repository.clearAllGames()
            }
        }

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

        loadPopularRawgGames()
    }

    private suspend fun handleUserSessionChanged(user: UserProfile?) {
        if (user != null && user.isEmailVerified) {
            if (activeSessionUserId != user.uid) {
                Log.i("GameVaultViewModel", "Switching active session to user: ${user.uid}")
                activeSessionUserId = user.uid
                // 1. Clear Room database so no previous user's games linger
                repository.clearAllGames()
                // 2. Clear transient in-memory state
                clearTransientState()
                // 3. Load only this authenticated user's games from Firestore (users/{uid}/games)
                loadUserGamesFromCloud(user.uid)
            }
        } else {
            // User signed out or not verified
            if (activeSessionUserId != null) {
                Log.i("GameVaultViewModel", "User logged out. Clearing local cache.")
                activeSessionUserId = null
                repository.clearAllGames()
                clearTransientState()
            }
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
        val user = currentUser.value
        if (destination != NavDestination.AUTH && (user == null || !user.isEmailVerified)) {
            _currentDestination.value = NavDestination.AUTH
            viewModelScope.launch {
                _snackbarMessage.emit("Please sign in with a verified email to access the app.")
            }
            return
        }
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
                    isFavorite = isFavorite,
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
            activeSessionUserId = null
            // Clear local Room database completely so next user doesn't see previous user's games
            repository.clearAllGames()
            clearTransientState()
            authRepository.signOut()
            _currentDestination.value = NavDestination.AUTH
            _isProfileModalOpen.value = false
            _snackbarMessage.emit("Signed out of GameVault.")
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

    // --- RAWG Search Methods ---

    /**
     * Updates the search query and sends request automatically with debounce.
     */
    fun setRawgSearchQuery(query: String) {
        _rawgSearchQuery.value = query
        rawgSearchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
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

    /**
     * Executes immediate search for the given query.
     */
    fun searchRawg(query: String? = null) {
        val target = (query ?: _rawgSearchQuery.value).trim()
        if (target.isEmpty()) return
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

        val result = repository.searchRawgGames(query)
        _isRawgLoading.value = false
        result.onSuccess { games ->
            _rawgSearchResults.value = games
            _rawgErrorMessage.value = null
        }.onFailure { error ->
            _rawgSearchResults.value = emptyList()
            _rawgErrorMessage.value = error.localizedMessage ?: "Failed to load games from RAWG"
        }
    }

    /**
     * Adds a game returned from RAWG API search into the user's local Room database and Cloud Firestore under users/{uid}/games.
     */
    fun addRawgGameToVault(rawgGame: RawgGameDto, status: GameStatus = GameStatus.BACKLOG, rating: Int = 0) {
        val user = currentUser.value
        val uid = user?.uid ?: ""
        viewModelScope.launch {
            val exists = allGames.value.any { it.title.equals(rawgGame.name, ignoreCase = true) }
            if (exists) {
                _snackbarMessage.emit("\"${rawgGame.name}\" is already in your Vault!")
                return@launch
            }

            val firstPlatform = rawgGame.platforms?.firstOrNull()?.platform?.name ?: "PC"
            val firstGenre = rawgGame.genres?.firstOrNull()?.name ?: "Action"
            val year = rawgGame.released?.take(4)?.toIntOrNull() ?: 2024
            val defaultRating = rawgGame.rating?.let { (it * 2).toInt().coerceIn(1, 10) } ?: 0

            val newGame = Game(
                title = rawgGame.name,
                coverUrl = rawgGame.backgroundImage ?: "",
                platform = firstPlatform,
                genre = firstGenre,
                releaseYear = year,
                status = status,
                playtimeHours = rawgGame.playtime?.toDouble() ?: 0.0,
                rating = if (rating > 0) rating else defaultRating,
                notes = "Added from RAWG Video Games Database",
                userId = uid
            )
            val newId = repository.insertGame(newGame)
            val savedGame = newGame.copy(id = newId)
            if (uid.isNotBlank()) {
                firestoreRepository.saveUserGame(uid, savedGame)
            }
            _snackbarMessage.emit("Added \"${rawgGame.name}\" to your Vault (${status.displayName})!")
        }
    }
}
