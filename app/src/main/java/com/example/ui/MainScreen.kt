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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.components.AddEditGameDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.GameComparisonDialog
import com.example.ui.components.GameDetailDialog
import com.example.ui.components.ImportDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GameDatabaseScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
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
        NavDestination.GAME_DATABASE -> Icons.Outlined.Search
        NavDestination.AI_CHAT -> Icons.Filled.AutoAwesome
        NavDestination.LIBRARY -> Icons.Filled.SportsEsports
        NavDestination.COMPLETED -> Icons.Filled.CheckCircle
        NavDestination.PLAYING -> Icons.Filled.PlayCircle
        NavDestination.BACKLOG -> Icons.Filled.MenuBook
        NavDestination.FAVORITES -> Icons.Filled.Favorite
        NavDestination.STATISTICS -> Icons.Filled.BarChart
        NavDestination.SETTINGS -> Icons.Filled.Settings
        NavDestination.AUTH -> Icons.Filled.AccountCircle
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
    val selectedRawgGameForDetails by viewModel.selectedRawgGameForDetails.collectAsStateWithLifecycle()
    val comparisonPair by viewModel.comparisonPair.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val gameToEdit by viewModel.gameToEdit.collectAsStateWithLifecycle()
    val gameToDelete by viewModel.gameToDelete.collectAsStateWithLifecycle()
    val exportModalData by viewModel.exportModalData.collectAsStateWithLifecycle()
    val isImportModalOpen by viewModel.isImportModalOpen.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val unverifiedEmail by viewModel.unverifiedEmail.collectAsStateWithLifecycle()
    val isResendingEmail by viewModel.isResendingEmail.collectAsStateWithLifecycle()
    val isAuthModalOpen by viewModel.isAuthModalOpen.collectAsStateWithLifecycle()
    val authModalInitialTab by viewModel.authModalInitialTab.collectAsStateWithLifecycle()
    val isProfileModalOpen by viewModel.isProfileModalOpen.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lastCloudSyncTimestamp by viewModel.lastCloudSyncTimestamp.collectAsStateWithLifecycle()
    val activeGamingSession by viewModel.activeGamingSession.collectAsStateWithLifecycle()

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

    // Root composable surface. Deliberately carries NO WindowInsets padding of its own: this
    // Box paints the dark background across the entire window, including underneath the
    // transparent status/navigation bars and out into the device's physical rounded display
    // corners (which the OS compositor clips the window to). Interactive content further down
    // the tree (the Sidebar and both Scaffolds below) explicitly consumes WindowInsets.safeDrawing
    // so it stays clear of system bars, cutouts, and rounded corners without the background
    // itself ever stopping short of the true screen edge.
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
                            // The Surface behind this Column already extends full-height to the
                            // physical screen edge; the Column itself insets its content (logo,
                            // buttons, nav items) away from the status bar / rounded top corner
                            // and the nav bar / rounded bottom corner using real device insets —
                            // no hardcoded corner radius or bar height anywhere.
                            .windowInsetsPadding(WindowInsets.safeDrawing)
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // User Profile or Sign-In in Sidebar
                        if (currentUser != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.navigateTo(NavDestination.AUTH) }
                                    .testTag("sidebar_user_profile"),
                                color = DarkCard
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .border(
                                                1.5.dp,
                                                if (currentUser?.isEmailVerified == true) NeonCyan else AccentAmber,
                                                CircleShape
                                            )
                                            .background(DarkBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentUser?.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentUser?.photoUrl,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.size(34.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = currentUser?.fullName?.firstOrNull()?.uppercase() ?: "G",
                                                color = NeonCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentUser?.fullName.orEmpty().ifEmpty { "Gamer" },
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (currentUser?.isEmailVerified == true) "Cloud Synced" else "Email unverified",
                                            color = if (currentUser?.isEmailVerified == true) AccentEmerald else AccentAmber,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(NavDestination.AUTH) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("sidebar_sign_in_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(CyberPurple, NeonCyan))
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In / Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Main Content
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = DarkBg,
                    // Explicit WindowInsets handling: content is padded away from the status
                    // bar, navigation bar, and display cutouts/rounded corners using the
                    // device's actual reported safe-drawing insets, never a fixed value.
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        ScreenRouter(
                            destination = currentDestination,
                            viewModel = viewModel,
                            currentUser = currentUser,
                            authState = authState,
                            authModalInitialTab = authModalInitialTab,
                            unverifiedEmail = unverifiedEmail,
                            isResendingEmail = isResendingEmail,
                            isCloudSyncing = isCloudSyncing,
                            lastCloudSyncTimestamp = lastCloudSyncTimestamp,
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Drawer User Profile or Sign-In
                            if (currentUser != null) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            viewModel.navigateTo(NavDestination.AUTH)
                                        }
                                        .testTag("drawer_user_profile"),
                                    color = DarkCard
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.5.dp,
                                                    if (currentUser?.isEmailVerified == true) NeonCyan else AccentAmber,
                                                    CircleShape
                                                )
                                                .background(DarkBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!currentUser?.photoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = currentUser?.photoUrl,
                                                    contentDescription = "Avatar",
                                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = currentUser?.fullName?.firstOrNull()?.uppercase() ?: "G",
                                                    color = NeonCyan,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentUser?.fullName.orEmpty().ifEmpty { "Gamer" },
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (currentUser?.isEmailVerified == true) "Cloud Synced" else "Email unverified",
                                                color = if (currentUser?.isEmailVerified == true) AccentEmerald else AccentAmber,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        viewModel.navigateTo(NavDestination.AUTH)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("drawer_sign_in_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(listOf(CyberPurple, NeonCyan))
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign In / Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    // Explicit WindowInsets handling for the mobile compact layout: the
                    // TopAppBar/NavigationBar below consume the status/navigation bar insets
                    // themselves, and the content area is padded using the device's actual
                    // safe-drawing insets so nothing sits under a system bar or a physically
                    // rounded corner — computed live per device, never hardcoded.
                    contentWindowInsets = WindowInsets.safeDrawing,
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
                                if (currentUser != null) {
                                    IconButton(
                                        onClick = { viewModel.navigateTo(NavDestination.AUTH) },
                                        modifier = Modifier.testTag("app_bar_profile_button")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.5.dp,
                                                    if (currentUser?.isEmailVerified == true) NeonCyan else AccentAmber,
                                                    CircleShape
                                                )
                                                .background(DarkSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!currentUser?.photoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = currentUser?.photoUrl,
                                                    contentDescription = "Avatar",
                                                    modifier = Modifier.size(32.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = currentUser?.fullName?.firstOrNull()?.uppercase() ?: "G",
                                                    color = NeonCyan,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.navigateTo(NavDestination.AUTH) },
                                        modifier = Modifier.testTag("app_bar_sign_in_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sign In", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkBg,
                                titleContentColor = TextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = DarkBg,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = DarkCardBorder.copy(alpha = 0.5f),
                                    thickness = 0.5.dp
                                )
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets.navigationBars,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val bottomNavItems = listOf(
                                        NavDestination.DASHBOARD,
                                        NavDestination.GAME_DATABASE,
                                        NavDestination.AI_CHAT,
                                        NavDestination.LIBRARY,
                                        NavDestination.AUTH
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
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) NeonCyan else TextMuted
                                                )
                                            },
                                            selected = isSelected,
                                            onClick = { viewModel.navigateTo(dest) },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = CyberPurple.copy(alpha = 0.2f),
                                                selectedIconColor = NeonCyan,
                                                unselectedIconColor = TextMuted,
                                                selectedTextColor = NeonCyan,
                                                unselectedTextColor = TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentDestination != NavDestination.AI_CHAT) {
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
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = DarkBg
                ) { paddingValues ->
                    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        // Email verification notice banner if logged in and unverified
                        if (currentUser != null && !currentUser!!.isEmailVerified) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentAmber.copy(alpha = 0.15f))
                                    .clickable { viewModel.sendEmailVerification() }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Email unverified. Tap to send verification.",
                                        color = AccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "Send Link",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ScreenRouter(
                                destination = currentDestination,
                                viewModel = viewModel,
                                currentUser = currentUser,
                                authState = authState,
                                authModalInitialTab = authModalInitialTab,
                                unverifiedEmail = unverifiedEmail,
                                isResendingEmail = isResendingEmail,
                                isCloudSyncing = isCloudSyncing,
                                lastCloudSyncTimestamp = lastCloudSyncTimestamp,
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
        }

        // Host Dialogs
        selectedGameForDetails?.let { game ->
            GameDetailDialog(
                game = game,
                activeSession = activeGamingSession,
                onStartSession = { viewModel.startGamingSession(it) },
                onStopSession = { viewModel.stopGamingSession() },
                onDismiss = { viewModel.closeGameDetails() },
                onEdit = { viewModel.openEditGame(game) },
                onDelete = { viewModel.confirmDeleteGame(game) },
                onToggleFavorite = { viewModel.toggleFavorite(game) },
                onArchiveToggle = { viewModel.toggleArchiveGame(it) },
                onStatusChange = { newStatus -> viewModel.updateGameStatus(game, newStatus) }
            )
        }

        selectedRawgGameForDetails?.let { rawgDto ->
            GameDetailDialog(
                rawgGameDto = rawgDto,
                onDismiss = { viewModel.closeRawgGameDetails() },
                onAddToVault = { dto, status -> viewModel.addRawgGameToVault(dto, status) },
                onSelectSimilarGame = { viewModel.openRawgGameDetails(it) },
                isInVault = viewModel.isGameInVault(rawgDto.name),
                vaultGameStatus = viewModel.getVaultGame(rawgDto.name)?.status
            )
        }

        comparisonPair?.let { (g1, g2) ->
            GameComparisonDialog(
                game1 = g1,
                game2 = g2,
                onDismiss = { viewModel.closeComparison() }
            )
        }

        if (isAddEditOpen) {
            AddEditGameDialog(
                game = gameToEdit,
                onDismiss = { viewModel.closeAddEdit() },
                onSearchRawg = { query -> viewModel.searchRawgAutocomplete(query) },
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

        if (isAuthModalOpen) {
            AuthDialog(
                authState = authState,
                isFirebaseConfigured = viewModel.isFirebaseConfigured,
                initialTab = authModalInitialTab,
                unverifiedEmail = unverifiedEmail,
                isResendingEmail = isResendingEmail,
                onDismiss = { viewModel.closeAuthModal() },
                onSignIn = { email, pass, rememberMe -> viewModel.signIn(email, pass, rememberMe) },
                onSignUp = { name, email, pass, confirm, tag, rememberMe ->
                    viewModel.signUp(name, email, pass, confirm, tag, rememberMe)
                },
                onForgotPassword = { email -> viewModel.sendPasswordReset(email) },
                onResendVerification = { email, pass -> viewModel.resendVerificationEmail(email, pass) }
            )
        }

        if (isProfileModalOpen && currentUser != null) {
            UserProfileDialog(
                user = currentUser!!,
                isCloudSyncing = isCloudSyncing,
                lastSyncTimestamp = lastCloudSyncTimestamp,
                totalLocalGames = allGames.size,
                onDismiss = { viewModel.closeProfileModal() },
                onSendVerificationEmail = { viewModel.sendEmailVerification() },
                onRefreshVerification = { viewModel.refreshUserVerification() },
                onUpdateProfile = { name, tag, photoUrl -> viewModel.updateUserProfile(name, tag, photoUrl) },
                onSyncToCloud = { viewModel.syncLibraryToCloud() },
                onRestoreFromCloud = { viewModel.restoreLibraryFromCloud() },
                onSignOut = { viewModel.signOut() }
            )
        }
    }
}

@Composable
fun ScreenRouter(
    destination: NavDestination,
    viewModel: GameVaultViewModel,
    currentUser: UserProfile?,
    authState: com.example.data.repository.AuthState,
    authModalInitialTab: com.example.ui.components.AuthTab,
    unverifiedEmail: String? = null,
    isResendingEmail: Boolean = false,
    isCloudSyncing: Boolean,
    lastCloudSyncTimestamp: Long?,
    allGames: List<com.example.data.model.Game>,
    filteredGames: List<com.example.data.model.Game>,
    filters: com.example.ui.viewmodel.LibraryFilters,
    stats: com.example.ui.viewmodel.VaultStats,
    onResetSample: () -> Unit,
    onClearAll: () -> Unit
) {
    when (destination) {
        NavDestination.DASHBOARD -> {
            val popularGames by viewModel.popularRawgGames.collectAsStateWithLifecycle()
            val personalizedRecs by viewModel.personalizedRecommendations.collectAsStateWithLifecycle()
            DashboardScreen(
                stats = stats,
                popularGames = popularGames,
                personalizedRecommendations = personalizedRecs,
                onNavigate = { viewModel.navigateTo(it) },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() },
                onSearchDatabase = { query ->
                    viewModel.setRawgSearchQuery(query)
                    viewModel.navigateTo(NavDestination.GAME_DATABASE)
                },
                onSelectRawgGame = { viewModel.openRawgGameDetails(it) },
                onAddRawgGameToVault = { game, status ->
                    viewModel.addRawgGameToVault(game, status)
                },
                isGameInVault = { viewModel.isGameInVault(it) }
            )
        }

        NavDestination.GAME_DATABASE -> {
            val rawgSearchQuery by viewModel.rawgSearchQuery.collectAsStateWithLifecycle()
            val rawgSearchResults by viewModel.rawgSearchResults.collectAsStateWithLifecycle()
            val isRawgLoading by viewModel.isRawgLoading.collectAsStateWithLifecycle()
            val rawgErrorMessage by viewModel.rawgErrorMessage.collectAsStateWithLifecycle()
            val hasSearchedRawg by viewModel.hasSearchedRawg.collectAsStateWithLifecycle()
            val rawgFilterOptions by viewModel.rawgFilterOptions.collectAsStateWithLifecycle()
            val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

            GameDatabaseScreen(
                searchQuery = rawgSearchQuery,
                searchResults = rawgSearchResults,
                isLoading = isRawgLoading,
                errorMessage = rawgErrorMessage,
                hasSearched = hasSearchedRawg,
                filterOptions = rawgFilterOptions,
                searchHistory = searchHistory,
                onSearchChange = { query ->
                    viewModel.setRawgSearchQuery(query)
                    if (query.isNotBlank()) viewModel.addSearchHistoryQuery(query)
                },
                onFilterChange = { viewModel.setRawgFilterOptions(it) },
                onClearSearchHistory = { viewModel.clearSearchHistory() },
                onRemoveSearchQuery = { viewModel.removeSearchHistoryQuery(it) },
                onRetrySearch = { viewModel.retryRawgSearch() },
                onSelectGame = { viewModel.openRawgGameDetails(it) },
                onAddGameToVault = { game, status -> viewModel.addRawgGameToVault(game, status) },
                isGameInVault = { viewModel.isGameInVault(it) },
                getVaultGame = { viewModel.getVaultGame(it) }
            )
        }

        NavDestination.AI_CHAT -> {
            AiChatScreen(
                viewModel = viewModel.aiChatViewModel,
                userBacklog = allGames,
                onSelectRawgGame = { viewModel.openRawgGameDetails(it) },
                onAddRawgGameToVault = { viewModel.addRawgGameToVault(it) }
            )
        }

        NavDestination.LIBRARY -> {
            LibraryScreen(
                title = "All Games",
                games = filteredGames,
                allVaultGames = allGames,
                filters = filters,
                showStatusFilter = true,
                showArchiveToggle = true,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onStatusChange = { viewModel.setStatusFilter(it) },
                onPlatformChange = { viewModel.setPlatformFilter(it) },
                onSortChange = { viewModel.setSortOption(it) },
                onToggleShowArchived = { viewModel.toggleShowArchived() },
                onGameClick = { viewModel.openGameDetails(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddGame = { viewModel.openAddGame() }
            )
        }

        NavDestination.COMPLETED -> {
            LibraryScreen(
                title = "Completed Games",
                games = filteredGames,
                allVaultGames = allGames,
                filters = filters,
                showStatusFilter = false,
                showArchiveToggle = false,
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
                allVaultGames = allGames,
                filters = filters,
                showStatusFilter = false,
                showArchiveToggle = false,
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
                allVaultGames = allGames,
                filters = filters,
                showStatusFilter = false,
                showArchiveToggle = false,
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
                allVaultGames = allGames,
                filters = filters,
                showStatusFilter = true,
                showArchiveToggle = false,
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
                currentUser = currentUser,
                isFirebaseConfigured = viewModel.isFirebaseConfigured,
                isCloudSyncing = isCloudSyncing,
                lastSyncTimestamp = lastCloudSyncTimestamp,
                onOpenAuth = { viewModel.navigateTo(NavDestination.AUTH) },
                onOpenProfile = { viewModel.navigateTo(NavDestination.AUTH) },
                onSyncToCloud = { viewModel.syncLibraryToCloud() },
                onRestoreFromCloud = { viewModel.restoreLibraryFromCloud() },
                onSignOut = { viewModel.signOut() },
                onExportJson = { viewModel.exportLibrary("JSON") },
                onExportCsv = { viewModel.exportLibrary("CSV") },
                onImportJson = { viewModel.openImportModal() },
                onResetSampleData = onResetSample,
                onClearAllData = onClearAll
            )
        }

        NavDestination.AUTH -> {
            AuthScreen(
                currentUser = currentUser,
                authState = authState,
                isFirebaseConfigured = viewModel.isFirebaseConfigured,
                isCloudSyncing = isCloudSyncing,
                lastSyncTimestamp = lastCloudSyncTimestamp,
                totalLocalGames = allGames.size,
                initialTab = authModalInitialTab,
                unverifiedEmail = unverifiedEmail,
                isResendingEmail = isResendingEmail,
                onSignIn = { email, pass, rem -> viewModel.signIn(email, pass, rem) },
                onSignUp = { name, email, pass, conf, tag, rem ->
                    viewModel.signUp(name, email, pass, conf, tag, rem)
                },
                onForgotPassword = { email -> viewModel.sendPasswordReset(email) },
                onSendVerificationEmail = { viewModel.sendEmailVerification() },
                onResendVerificationEmail = { email, pass -> viewModel.resendVerificationEmail(email, pass) },
                onRefreshVerification = { viewModel.refreshUserVerification() },
                onUpdateProfile = { name, tag, photoUrl ->
                    viewModel.updateUserProfile(name, tag, photoUrl)
                },
                onSyncToCloud = { viewModel.syncLibraryToCloud() },
                onRestoreFromCloud = { viewModel.restoreLibraryFromCloud() },
                onSignOut = { viewModel.signOut() },
                onNavigateToLibrary = { viewModel.navigateTo(NavDestination.LIBRARY) }
            )
        }
    }
}
