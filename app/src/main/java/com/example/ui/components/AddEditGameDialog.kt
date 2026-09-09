package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.PrimaryRedMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val CommonPlatforms = listOf("PC", "PlayStation 5", "Xbox Series X", "Nintendo Switch", "Steam Deck", "PlayStation 4", "Retro")
val CommonGenres = listOf("Action RPG", "Open World", "Action Adventure", "Metroidvania", "Roguelike", "CRPG", "JRPG", "Shooter", "Survival Horror", "Indie", "Strategy")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditGameDialog(
    game: Game? = null,
    onDismiss: () -> Unit,
    onSearchRawg: suspend (String) -> List<RawgGameDto> = { emptyList() },
    onSave: (
        id: Long,
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
    ) -> Unit
) {
    val isEditing = game != null

    var title by remember { mutableStateOf(game?.title ?: "") }
    var coverUrl by remember { mutableStateOf(game?.coverUrl ?: "") }
    var platform by remember { mutableStateOf(game?.platform ?: "PC") }
    var genre by remember { mutableStateOf(game?.genre ?: "Action RPG") }
    var releaseYearStr by remember { mutableStateOf(game?.releaseYear?.toString() ?: Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var status by remember { mutableStateOf(game?.status ?: GameStatus.BACKLOG) }
    var completionDate by remember {
        mutableStateOf(
            game?.completionDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }
    var playtimeHours by remember { mutableDoubleStateOf(game?.playtimeHours ?: 0.0) }
    var rating by remember { mutableIntStateOf(game?.rating ?: 0) }
    var notes by remember { mutableStateOf(game?.notes ?: "") }
    var isFavorite by remember { mutableStateOf(game?.isFavorite ?: false) }

    // Live autocomplete search states
    var suggestions by remember { mutableStateOf<List<RawgGameDto>>(emptyList()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }
    var showSuggestionsDropdown by remember { mutableStateOf(false) }
    var isSelectedFromRawg by remember { mutableStateOf(isEditing && game?.coverUrl?.isNotBlank() == true) }
    var isCustomGameMode by remember { mutableStateOf(isEditing && game?.coverUrl.isNullOrBlank()) }
    var lastSelectedTitle by remember { mutableStateOf(game?.title ?: "") }
    var titleError by remember { mutableStateOf(false) }

    // Live search debounced at 450ms (400-500ms)
    LaunchedEffect(title) {
        if (isEditing) return@LaunchedEffect
        if (isSelectedFromRawg && title.equals(lastSelectedTitle, ignoreCase = true)) {
            return@LaunchedEffect
        }
        val query = title.trim()
        if (query.length >= 2 && !isCustomGameMode) {
            delay(450)
            isSearchingSuggestions = true
            showSuggestionsDropdown = true
            val results = onSearchRawg(query)
            suggestions = results
            isSearchingSuggestions = false
        } else {
            suggestions = emptyList()
            isSearchingSuggestions = false
            showSuggestionsDropdown = false
        }
    }

    fun selectSuggestion(item: RawgGameDto) {
        title = item.name
        lastSelectedTitle = item.name
        coverUrl = item.backgroundImage ?: ""
        val year = item.released?.take(4)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        releaseYearStr = year.toString()

        val genreList = item.genres?.mapNotNull { it.name } ?: emptyList()
        genre = if (genreList.isNotEmpty()) genreList.joinToString(", ") else "Action"

        val platformList = item.platforms?.mapNotNull { it.platform?.name } ?: emptyList()
        platform = if (platformList.isNotEmpty()) platformList.first() else "PC"

        val calculatedRating = item.rating?.let { ((it * 2.0).roundToInt()).coerceIn(1, 10) } ?: 0
        rating = calculatedRating

        if (item.playtime != null && item.playtime > 0) {
            playtimeHours = item.playtime.toDouble()
        }

        isSelectedFromRawg = true
        isCustomGameMode = false
        showSuggestionsDropdown = false
        suggestions = emptyList()
        titleError = false
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryRed,
        unfocusedBorderColor = DarkCardBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = PrimaryRed,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Game" else "Add to Vault",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCustomGameMode) "Custom Game Entry" else "Search RAWG API database",
                            color = if (isCustomGameMode) PrimaryRed else TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("add_edit_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Game Title Field (Autocomplete live search)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GAME TITLE *",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isEditing && !isCustomGameMode && isSelectedFromRawg) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentEmerald.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "RAWG Verified",
                                        color = AccentEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                            if (isSelectedFromRawg && !it.equals(lastSelectedTitle, ignoreCase = true)) {
                                isSelectedFromRawg = false
                            }
                        },
                        placeholder = {
                            Text(
                                if (isCustomGameMode) "Enter game title..." else "Type 2+ letters to search RAWG...",
                                color = TextMuted
                            )
                        },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Title is required", color = AccentRose) }
                        } else null,
                        trailingIcon = {
                            if (isSearchingSuggestions) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PrimaryRed,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_game_title")
                    )

                    // Autocomplete Suggestions Dropdown directly below Title field
                    if (!isEditing && showSuggestionsDropdown && !isCustomGameMode) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isSearchingSuggestions) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = PrimaryRed,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Searching RAWG live database...",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else if (suggestions.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RAWG SUGGESTIONS (TAP TO AUTOFILL)",
                                            color = PrimaryRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${suggestions.size} found",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    suggestions.take(5).forEachIndexed { index, gameSuggestion ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = DarkCardBorder.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectSuggestion(gameSuggestion) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("rawg_suggestion_${gameSuggestion.id}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Cover image
                                            if (!gameSuggestion.backgroundImage.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = gameSuggestion.backgroundImage,
                                                    contentDescription = gameSuggestion.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DarkSurface)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DarkSurface),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SportsEsports,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = gameSuggestion.name,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val year = gameSuggestion.released?.take(4) ?: "TBA"
                                                    Text(
                                                        text = year,
                                                        color = TextMuted,
                                                        fontSize = 11.sp
                                                    )
                                                    val ratingScore = gameSuggestion.rating ?: 0.0
                                                    if (ratingScore > 0.0) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Star,
                                                                contentDescription = null,
                                                                tint = AccentAmber,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = String.format(Locale.US, "%.1f", ratingScore),
                                                                color = AccentAmber,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (title.trim().length >= 2 && !isSelectedFromRawg) {
                                    // No matching game found on RAWG -> allow manual custom game
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "No matching games found on RAWG API.",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TextButton(
                                            onClick = {
                                                isCustomGameMode = true
                                                showSuggestionsDropdown = false
                                                isSelectedFromRawg = false
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Create as custom game instead",
                                                color = PrimaryRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cover Image Preview Card (Manual input hidden as required)
                    if (coverUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, PrimaryRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, DarkBg.copy(alpha = 0.85f))
                                            )
                                        )
                                    )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryRed.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RAWG Verified Cover & Metadata",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Auto-populated fields (Release Year, Platform, Genre)
                    if (isSelectedFromRawg && !isCustomGameMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkCard,
                            border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AUTO-POPULATED FROM RAWG API",
                                            color = AccentEmerald,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextButton(
                                        onClick = { isCustomGameMode = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Customize", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("YEAR", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(releaseYearStr, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("PLATFORM", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(platform, color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("GENRE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(genre, color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Custom Game Mode: manual selection of platform, genre, and year
                        if (isCustomGameMode && !isEditing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MANUAL ENTRY MODE",
                                    color = PrimaryRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        isCustomGameMode = false
                                        showSuggestionsDropdown = true
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Switch to RAWG search", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Platform Selection Chips
                        Text(
                            text = "PLATFORM",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CommonPlatforms.forEach { p ->
                                val selected = platform == p
                                FilterChip(
                                    selected = selected,
                                    onClick = { platform = p },
                                    label = { Text(p) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryRed.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryRed,
                                        containerColor = DarkCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = DarkCardBorder,
                                        selectedBorderColor = PrimaryRed
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Genre Selection Chips
                        Text(
                            text = "GENRE",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CommonGenres.forEach { g ->
                                val selected = genre == g
                                FilterChip(
                                    selected = selected,
                                    onClick = { genre = g },
                                    label = { Text(g) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryRed.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryRed,
                                        containerColor = DarkCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = DarkCardBorder,
                                        selectedBorderColor = PrimaryRed
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Release Year Field
                        Text(
                            text = "RELEASE YEAR",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = releaseYearStr,
                            onValueChange = { releaseYearStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_release_year")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Completion Status
                    Text(
                        text = "COMPLETION STATUS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GameStatus.entries.forEach { s ->
                            val selected = status == s
                            FilterChip(
                                selected = selected,
                                onClick = { status = s },
                                label = { Text(s.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryRed.copy(alpha = 0.25f),
                                    selectedLabelColor = PrimaryRed,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = DarkCardBorder,
                                    selectedBorderColor = PrimaryRed
                                )
                            )
                        }
                    }

                    // Completion Date (if Completed)
                    if (status == GameStatus.COMPLETED) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "COMPLETION DATE (YYYY-MM-DD)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = completionDate,
                            onValueChange = { completionDate = it },
                            placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_completion_date")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playtime Hours
                    Text(
                        text = "PLAYTIME (HOURS)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = if (playtimeHours == 0.0) "" else playtimeHours.toString(),
                        onValueChange = {
                            playtimeHours = it.toDoubleOrNull() ?: 0.0
                        },
                        placeholder = { Text("0", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_playtime")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rating Slider (1 to 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RATING (1 - 10)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (rating > 0) "$rating / 10" else "Unrated",
                            color = if (rating > 0) AccentAmber else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.roundToInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentAmber,
                            activeTrackColor = AccentAmber,
                            inactiveTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rating_slider")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Favorite Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) AccentRose else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Favorite Game",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Highlight in your Vault",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isFavorite,
                            onCheckedChange = { isFavorite = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentRose,
                                checkedTrackColor = AccentRose.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardBorder
                            ),
                            modifier = Modifier.testTag("input_favorite_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personal Notes
                    Text(
                        text = "NOTES & THOUGHTS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Write your review, memorable moments, build info...", color = TextMuted) },
                        minLines = 3,
                        maxLines = 6,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notes")
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Bottom Action Buttons (Cancel, Add to Vault / Save Changes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_edit_cancel_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val relYear = releaseYearStr.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                            onSave(
                                game?.id ?: 0L,
                                title.trim(),
                                coverUrl.trim(),
                                platform,
                                genre,
                                relYear,
                                status,
                                if (status == GameStatus.COMPLETED) completionDate else null,
                                playtimeHours,
                                rating,
                                notes.trim(),
                                isFavorite
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_edit_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add to Vault",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
