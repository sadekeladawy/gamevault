package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditGameDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.GameDetailDialog
import com.example.ui.components.ImportDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameVaultViewModel
import com.example.ui.viewmodel.NavDestination
import kotlinx.coroutines.launch

fun getDestinationIcon(dest: NavDestination): ImageVector {
    return when (dest) {
        NavDestination.DASHBOARD -> Icons.Filled.Home
        NavDestination.LIBRARY -> Icons.Filled.SportsEsports
        NavDestination.COMPLETED -> Icons.Filled.CheckCircle
        NavDestination.PLAYING -> Icons.Filled.PlayCircle
        NavDestination.BACKLOG -> Icons.Filled.MenuBook
        NavDestination.FAVORITES -> Icons.Filled.Favorite
        NavDestination.STATISTICS -> Icons.Filled.BarChart
        NavDestination.SETTINGS -> Icons.Filled.Settings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GameVaultViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val filteredGames by viewModel.filteredGames.collectAsStateWithLifecycle()
    val filters by viewModel.libraryFilters.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val selectedGameForDetails by viewModel.selectedGameForDetails.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val gameToEdit by viewModel.gameToEdit.collectAsStateWithLifecycle()
    val gameToDelete by viewModel.gameToDelete.collectAsStateWithLifecycle()
    val exportModalData by viewModel.exportModalData.collectAsStateWithLifecycle()
    val isImportModalOpen by viewModel.isImportModalOpen.collectAsStateWithLifecycle()

    var showResetSampleConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Listen to snackbar messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Tablet / Desktop layout with persistent Sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar Navigation
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Brand Logo Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(CyberPurple, NeonCyan))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VideogameAsset,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GameVault",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Gaming Library",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Prominent + Add Game Button
                        Button(
                            onClick = { viewModel.openAddGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("sidebar_add_game_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Add Game", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Navigation Items
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NavDestination.entries.forEach { dest ->
                                val isSelected = currentDestination == dest
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            imageVector = getDestinationIcon(dest),
                                            contentDescription = null,
                                            tint = if (isSelected) NeonCyan else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(dest) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = CyberPurple.copy(alpha = 0.22f),
                                        selectedTextColor = TextPrimary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_item_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Main Content
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = DarkBg,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        ScreenRouter(
                            destination = currentDestination,
                            viewModel = viewModel,
                            allGames = allGames,
                            filteredGames = filteredGames,
                            filters = filters,
                            stats = stats,
                            onResetSample = { showResetSampleConfirm = true },
                            onClearAll = { showClearAllConfirm = true }
                        )
                    }
                }
            }
        } else {
            // Mobile Compact layout with Drawer + Bottom Bar + FAB
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = DarkSurface,
                        drawerTonalElevation = 0.dp,
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Brand Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(listOf(CyberPurple, NeonCyan))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VideogameAsset,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "GameVault",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Personal Game Tracker",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Add Game action
                            Button(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.openAddGame()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Game", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Drawer Destinations
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                NavDestination.entries.forEach { dest ->
                                    val isSelected = currentDestination == dest
                                    NavigationDrawerItem(
                                        icon = {
                                            Icon(
                                                imageVector = getDestinationIcon(dest),
                                                contentDescription = null,
                                                tint = if (isSelected) NeonCyan else TextMuted
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = dest.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            viewModel.navigateTo(dest)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = CyberPurple.copy(alpha = 0.22f),
                                            selectedTextColor = TextPrimary,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("drawer_item_${dest.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = currentDestination.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("app_bar_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = TextPrimary
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.openAddGame() },
                                    modifier = Modifier.testTag("app_bar_add_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Game",
                                        tint = NeonCyan
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkBg,
                                titleContentColor = TextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.border(1.dp, DarkCardBorder, RoundedCornerShape(0.dp))
                        ) {
                            val bottomNavItems = listOf(
                                NavDestination.DASHBOARD,
                                NavDestination.LIBRARY,
                                NavDestination.COMPLETED,
                                NavDestination.STATISTICS
                            )

                            bottomNavItems.forEach { dest ->
                                val isSelected = currentDestination == dest
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = getDestinationIcon(dest),
                                            contentDescription = dest.title,
                                            tint = if (isSelected) NeonCyan else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(dest) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = CyberPurple.copy(alpha = 0.25f),
                                        selectedTextColor = TextPrimary,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { viewModel.openAddGame() },
                            containerColor = CyberPurple,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("main_fab_add_game")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Game"
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = DarkBg
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        ScreenRouter(
                            destination = currentDestination,
                            viewModel = viewModel,
                            allGames = allGames,
                            filteredGames = filteredGames,
                            filters = filters,
                            stats = stats,
                            onResetSample = { showResetSampleConfirm = true },
                            onClearAll = { showClearAllConfirm = true }
                        )
                    }
                }
            }
        }

        // Host Dialogs
        selectedGameForDetails?.let { game ->
            GameDetailDialog(
                game = game,
                onDismiss = { viewModel.closeGameDetails() },
                onEdit = { viewModel.openEditGame(game) },
                onDelete = { viewModel.confirmDeleteGame(game) },
                onToggleFavorite = { viewModel.toggleFavorite(game) },
                onStatusChange = { newStatus -> viewModel.updateGameStatus(game, newStatus) }
            )
        }

        if (isAddEditOpen) {
            AddEditGameDialog(
                game = gameToEdit,
                onDismiss = { viewModel.closeAddEdit() },
                onSave = { id, title, coverUrl, platform, genre, relYear, status, compDate, hours, rating, notes, fav ->
                    viewModel.saveGame(id, title, coverUrl, platform, genre, relYear, status, compDate, hours, rating, notes, fav)
                }
            )
        }

        gameToDelete?.let { game ->
            ConfirmationDialog(
                title = "Delete \"${game.title}\"?",
                message = "Are you sure you want to remove this game from your Vault? This action cannot be undone.",
                confirmButtonText = "Delete Game",
                isDestructive = true,
                onConfirm = { viewModel.executeDeleteGame() },
                onDismiss = { viewModel.cancelDeleteGame() }
            )
        }

        if (showResetSampleConfirm) {
            ConfirmationDialog(
                title = "Reset to Sample Games?",
                message = "This will replace your current library with the curated starter game collection.",
                confirmButtonText = "Reset Collection",
                isDestructive = false,
                onConfirm = {
                    showResetSampleConfirm = false
                    viewModel.resetToSampleData()
                },
                onDismiss = { showResetSampleConfirm = false }
            )
        }

        if (showClearAllConfirm) {
            ConfirmationDialog(
                title = "Clear Entire Library?",
                message = "Are you sure you want to delete all games from your vault? All progress and notes will be permanently erased.",
                confirmButtonText = "Clear Everything",
                isDestructive = true,
                onConfirm = {
                    showClearAllConfirm = false
                    viewModel.clearAllGames()
                },
                onDismiss = { showClearAllConfirm = false }
            )
        }

        exportModalData?.let { (type, content) ->
            ExportDialog(
                type = type,
                data = content,
                onDismiss = { viewModel.closeExportModal() }
            )
        }

        if (isImportModalOpen) {
            ImportDialog(
                onDismiss = { viewModel.closeImportModal() },
                onImport = { jsonStr -> viewModel.importLibraryJson(jsonStr) }
            )
        }
    }
}

@Composable
fun ScreenRouter(
    destination: NavDestination,
    viewModel: GameVaultViewModel,
    allGames: List<com.example.data.model.Game>,
    filteredGames: List<com.example.data.model.Game>,
    filters: com.example.ui.viewmodel.LibraryFilters,
    stats: com.example.ui.viewmodel.VaultStats,
    onResetSample: () -> Unit,
    onClearAll: () -> Unit
) {
    when (destination) {
        NavDestination.DASHBOARD -> {
            DashboardScreen(
                stats = stats,
                onNavigate = { viewModel.navigateTo(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.LIBRARY -> {
            LibraryScreen(
                title = "All Games",
                games = filteredGames,
                filters = filters,
                showStatusFilter = true,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = { viewModel.setStatusFilter(it) },
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.COMPLETED -> {
            LibraryScreen(
                title = "Completed Games",
                games = filteredGames,
                filters = filters,
                showStatusFilter = false,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = {},
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.PLAYING -> {
            LibraryScreen(
                title = "Currently Playing",
                games = filteredGames,
                filters = filters,
                showStatusFilter = false,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = {},
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.BACKLOG -> {
            LibraryScreen(
                title = "Game Backlog",
                games = filteredGames,
                filters = filters,
                showStatusFilter = false,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = {},
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.FAVORITES -> {
            LibraryScreen(
                title = "Favorite Games",
                games = filteredGames,
                filters = filters,
                showStatusFilter = true,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = { viewModel.setStatusFilter(it) },
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.STATISTICS -> {
            StatisticsScreen(
                stats = stats,
                onGameClick = { viewModel.openGameDetails(it) }
            )
        }

        NavDestination.SETTINGS -> {
            SettingsScreen(
                totalGamesCount = allGames.size,
                onExportJson = { viewModel.exportLibrary("JSON") },
                onExportCsv = { viewModel.exportLibrary("CSV") },
                onImportJson = { viewModel.openImportModal() },
                onResetSampleData = onResetSample,
                onClearAllData = onClearAll
            )
        }
    }
}
