package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.MasterGame
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.GameRepository
import com.example.data.sample.MasterGameCatalog
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

    // --- Firestore Master Game Database ---
    private val _masterGames = MutableStateFlow<List<MasterGame>>(MasterGameCatalog.defaultCatalog)
    val masterGames: StateFlow<List<MasterGame>> = _masterGames.asStateFlow()

    private val _masterDbSearchQuery = MutableStateFlow("")
    val masterDbSearchQuery: StateFlow<String> = _masterDbSearchQuery.asStateFlow()

    private val _selectedMasterPlatform = MutableStateFlow<String?>(null)
    val selectedMasterPlatform: StateFlow<String?> = _selectedMasterPlatform.asStateFlow()

    private val _selectedMasterGenre = MutableStateFlow<String?>(null)
    val selectedMasterGenre: StateFlow<String?> = _selectedMasterGenre.asStateFlow()

    private val _isMasterDbLoading = MutableStateFlow(false)
    val isMasterDbLoading: StateFlow<Boolean> = _isMasterDbLoading.asStateFlow()

    private val _isMasterDbSyncing = MutableStateFlow(false)
    val isMasterDbSyncing: StateFlow<Boolean> = _isMasterDbSyncing.asStateFlow()

    private val _masterDbStatusMessage = MutableStateFlow<String?>("Connected to Firestore games_database")
    val masterDbStatusMessage: StateFlow<String?> = _masterDbStatusMessage.asStateFlow()

    val filteredMasterGames: StateFlow<List<MasterGame>>

    // --- RAWG Video Games API Integration ---
    private val rawgRepository: RawgRepository = RawgRepository()

    private val _rawgSearchQuery = MutableStateFlow("")
    val rawgSearchQuery: StateFlow<String> = _rawgSearchQuery.asStateFlow()

    private val _rawgSearchResults = MutableStateFlow<List<RawgGameDto>>(emptyList())
    val rawgSearchResults: StateFlow<List<RawgGameDto>> = _rawgSearchResults.asStateFlow()

    private val _isRawgLoading = MutableStateFlow(false)
    val isRawgLoading: StateFlow<Boolean> = _isRawgLoading.asStateFlow()

    private val _rawgErrorMessage = MutableStateFlow<String?>(null)
    val rawgErrorMessage: StateFlow<String?> = _rawgErrorMessage.asStateFlow()

    private val _hasSearchedRawg = MutableStateFlow(false)
    val hasSearchedRawg: StateFlow<Boolean> = _hasSearchedRawg.asStateFlow()

    private var rawgSearchJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GameRepository(db.gameDao())

        // Ensure sample data is loaded on first launch
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            authRepository.checkAutoLogin()
            val user = authRepository.currentUser.value
            if (user != null && user.isEmailVerified) {
                _currentDestination.value = NavDestination.DASHBOARD
            } else {
                _currentDestination.value = NavDestination.AUTH
            }
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

        // Filter master game database
        filteredMasterGames = combine(
            _masterGames,
            _masterDbSearchQuery,
            _selectedMasterPlatform,
            _selectedMasterGenre
        ) { games, query, platform, genre ->
            games.filter { game ->
                val matchesQuery = query.isBlank() ||
                    game.title.contains(query, ignoreCase = true) ||
                    game.genre.contains(query, ignoreCase = true) ||
                    game.developer.contains(query, ignoreCase = true) ||
                    game.publisher.contains(query, ignoreCase = true) ||
                    game.platform.contains(query, ignoreCase = true) ||
                    game.platforms.any { it.contains(query, ignoreCase = true) }
                val matchesPlatform = platform == null || game.platform.equals(platform, ignoreCase = true) || game.platforms.any { it.equals(platform, ignoreCase = true) }
                val matchesGenre = genre == null || game.genre.equals(genre, ignoreCase = true)
                matchesQuery && matchesPlatform && matchesGenre
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MasterGameCatalog.defaultCatalog
        )

        loadMasterGameDatabase()
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

    // --- Master Game Database Actions ---

    fun setMasterSearchQuery(query: String) {
        _masterDbSearchQuery.value = query
    }

    fun setMasterPlatformFilter(platform: String?) {
        _selectedMasterPlatform.value = platform
    }

    fun setMasterGenreFilter(genre: String?) {
        _selectedMasterGenre.value = genre
    }

    fun loadMasterGameDatabase() {
        viewModelScope.launch {
            _isMasterDbLoading.value = true
            val result = firestoreRepository.fetchMasterGameDatabase()
            _isMasterDbLoading.value = false
            result.onSuccess { cloudGames ->
                if (cloudGames.isNotEmpty()) {
                    val existingTitles = cloudGames.map { it.title.lowercase().trim() }.toSet()
                    val combined = cloudGames.toMutableList()
                    MasterGameCatalog.defaultCatalog.forEach { defaultGame ->
                        if (!existingTitles.contains(defaultGame.title.lowercase().trim())) {
                            combined.add(defaultGame)
                        }
                    }
                    _masterGames.value = combined
                    _masterDbStatusMessage.value = "Firestore Database Active (${cloudGames.size} cloud records synced)"
                } else {
                    _masterGames.value = MasterGameCatalog.defaultCatalog
                    _masterDbStatusMessage.value = "Firestore Database Ready (${MasterGameCatalog.defaultCatalog.size} catalog games)"
                }
            }.onFailure { err ->
                _masterGames.value = MasterGameCatalog.defaultCatalog
                _masterDbStatusMessage.value = "Catalog Ready (${MasterGameCatalog.defaultCatalog.size} games, Offline Mode)"
            }
        }
    }

    fun syncMasterCatalogToFirestore() {
        viewModelScope.launch {
            _isMasterDbSyncing.value = true
            val result = firestoreRepository.seedMasterGameDatabase(MasterGameCatalog.defaultCatalog)
            _isMasterDbSyncing.value = false
            result.onSuccess { count ->
                _masterDbStatusMessage.value = "Successfully synced $count games to Firestore games_database"
                _snackbarMessage.emit("Uploaded $count curated games to Firestore collection \"games_database\"!")
                loadMasterGameDatabase()
            }.onFailure { err ->
                _masterDbStatusMessage.value = "Firestore upload note: ${err.localizedMessage}"
                _snackbarMessage.emit("Cloud sync: ${err.localizedMessage}")
            }
        }
    }

    fun addMasterGameToVault(masterGame: MasterGame, status: GameStatus = GameStatus.BACKLOG, rating: Int = 0) {
        viewModelScope.launch {
            val exists = allGames.value.any { it.title.equals(masterGame.title, ignoreCase = true) }
            if (exists) {
                _snackbarMessage.emit("\"${masterGame.title}\" is already in your Vault!")
                return@launch
            }
            val newGame = masterGame.toGame(status = status, rating = rating)
            repository.insertGame(newGame)
            _snackbarMessage.emit("Added \"${masterGame.title}\" to your Vault (${status.displayName})!")
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

        val result = rawgRepository.searchGames(query)
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
     * Adds a game returned from RAWG API search into the user's local Room database and Cloud Firestore.
     */
    fun addRawgGameToVault(rawgGame: RawgGameDto, status: GameStatus = GameStatus.BACKLOG, rating: Int = 0) {
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
                notes = "Added from RAWG Video Games Database"
            )
            val newId = repository.insertGame(newGame)
            _snackbarMessage.emit("Added \"${rawgGame.name}\" to your Vault (${status.displayName})!")

            // Also persist user-specific data under users/{uid}/games if user is authenticated
            val user = currentUser.value
            if (user != null && firestoreRepository.isFirestoreAvailable()) {
                firestoreRepository.saveUserSpecificData(
                    uid = user.uid,
                    subcollection = "games",
                    docId = newId.toString(),
                    data = mapOf(
                        "id" to newId,
                        "title" to newGame.title,
                        "coverUrl" to newGame.coverUrl,
                        "platform" to newGame.platform,
                        "genre" to newGame.genre,
                        "releaseYear" to newGame.releaseYear,
                        "status" to newGame.status.name,
                        "rating" to newGame.rating,
                        "rawgId" to rawgGame.id,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
