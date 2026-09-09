package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.remote.rawg.RawgMovieDto
import com.example.data.remote.rawg.RawgRepository
import com.example.data.remote.rawg.RawgScreenshotDto
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailDialog(
    game: Game? = null,
    rawgGameDto: RawgGameDto? = null,
    rawgRepository: RawgRepository = remember { RawgRepository() },
    activeSession: Pair<Game, Long>? = null,
    onStartSession: ((Game) -> Unit)? = null,
    onStopSession: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onArchiveToggle: ((Game) -> Unit)? = null,
    onStatusChange: ((GameStatus) -> Unit)? = null,
    onAddToVault: ((RawgGameDto, GameStatus) -> Unit)? = null,
    onSelectSimilarGame: ((RawgGameDto) -> Unit)? = null,
    isInVault: Boolean = false,
    vaultGameStatus: GameStatus? = null
) {
    val context = LocalContext.current
    var fullRawgDetails by remember { mutableStateOf<RawgGameDto?>(rawgGameDto) }
    var screenshots by remember { mutableStateOf<List<RawgScreenshotDto>>(rawgGameDto?.shortScreenshots ?: emptyList()) }
    var trailers by remember { mutableStateOf<List<RawgMovieDto>>(emptyList()) }
    var activeTrailerForPlayback by remember { mutableStateOf<RawgMovieDto?>(null) }
    var similarGames by remember { mutableStateOf<List<RawgGameDto>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var zoomedScreenshotUrl by remember { mutableStateOf<String?>(null) }

    // Fetch full RAWG details, screenshots & trailers if RAWG ID is provided
    LaunchedEffect(game, rawgGameDto) {
        val rawgId = rawgGameDto?.id?.toString()
        if (rawgId != null && rawgId != "0") {
            isLoadingDetails = true
            val detailResult = rawgRepository.getGameDetails(rawgId)
            detailResult.onSuccess { details ->
                fullRawgDetails = details
            }
            val screenshotResult = rawgRepository.getGameScreenshots(rawgId)
            screenshotResult.onSuccess { list ->
                if (list.isNotEmpty()) screenshots = list
            }
            val trailerResult = rawgRepository.getGameTrailers(rawgId)
            trailerResult.onSuccess { list ->
                trailers = list.filter { !it.getVideoUrl().isNullOrBlank() }
            }
            isLoadingDetails = false
        } else if (game != null) {
            isLoadingDetails = true
            val searchResult = rawgRepository.searchGames(game.title)
            searchResult.onSuccess { searchList ->
                val match = searchList.firstOrNull { it.name.equals(game.title, ignoreCase = true) } ?: searchList.firstOrNull()
                if (match != null && match.id != 0L) {
                    val matchedId = match.id.toString()
                    val detailResult = rawgRepository.getGameDetails(matchedId)
                    detailResult.onSuccess { fullRawgDetails = it }
                    val screenshotResult = rawgRepository.getGameScreenshots(matchedId)
                    screenshotResult.onSuccess { if (it.isNotEmpty()) screenshots = it }
                    val trailerResult = rawgRepository.getGameTrailers(matchedId)
                    trailerResult.onSuccess { list ->
                        trailers = list.filter { !it.getVideoUrl().isNullOrBlank() }
                    }
                }
            }
            isLoadingDetails = false
        }
    }

    // Effective metadata resolution
    val title = game?.title ?: fullRawgDetails?.name ?: rawgGameDto?.name ?: "Unknown Game"
    val coverUrl = game?.coverUrl?.ifBlank { null }
        ?: fullRawgDetails?.backgroundImage
        ?: rawgGameDto?.backgroundImage
        ?: ""
    val releaseDate = fullRawgDetails?.released ?: rawgGameDto?.released ?: (game?.releaseYear?.toString() ?: "N/A")
    val rawgRatingValue = fullRawgDetails?.rating ?: rawgGameDto?.rating ?: game?.rawgRating ?: 0.0
    val personalRating = game?.rating ?: 0
    val metacriticScore = fullRawgDetails?.metacritic ?: rawgGameDto?.metacritic ?: game?.metacriticScore
    val esrbRating = fullRawgDetails?.esrbRating?.name ?: rawgGameDto?.esrbRating?.name
    val playtime = fullRawgDetails?.playtime ?: rawgGameDto?.playtime ?: (game?.playtimeHours?.toInt() ?: 0)
    val description = fullRawgDetails?.descriptionRaw?.takeIf { it.isNotBlank() }
        ?: fullRawgDetails?.description?.takeIf { it.isNotBlank() }
        ?: game?.notes?.takeIf { it.isNotBlank() }
        ?: "No description available for this game."
    val website = fullRawgDetails?.website ?: rawgGameDto?.website

    val genres = fullRawgDetails?.genres?.mapNotNull { it.name }
        ?: rawgGameDto?.genres?.mapNotNull { it.name }
        ?: listOfNotNull(game?.genre)

    val platforms = fullRawgDetails?.platforms?.mapNotNull { it.platform?.name }
        ?: rawgGameDto?.platforms?.mapNotNull { it.platform?.name }
        ?: listOfNotNull(game?.platform)

    val developers = fullRawgDetails?.developers?.mapNotNull { it.name }?.joinToString(", ") ?: game?.developer.orEmpty()
    val publishers = fullRawgDetails?.publishers?.mapNotNull { it.name }?.joinToString(", ") ?: game?.publisher.orEmpty()

    // Fetch similar games based on primary genre
    LaunchedEffect(genres) {
        val firstGenre = genres.firstOrNull()
        if (!firstGenre.isNullOrBlank()) {
            val res = rawgRepository.searchGamesWithFilters(genres = firstGenre.lowercase(), pageSize = 6)
            res.onSuccess { list ->
                similarGames = list.filter { !it.name.equals(title, ignoreCase = true) }.take(5)
            }
        }
    }

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
                    .verticalScroll(rememberScrollState())
            ) {
                // Large Cover Art Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF131722))
                ) {
                    if (coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "$title cover art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1E2638), Color(0xFF0F121A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideogameAsset,
                                contentDescription = null,
                                tint = CyberPurple.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    // Rich gradient bottom fade
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        DarkBg.copy(alpha = 0.95f),
                                        DarkBg
                                    )
                                )
                            )
                    )

                    // Close button top-left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(14.dp)
                            .align(Alignment.TopStart)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    // Favorite button top-right (if in Vault)
                    if (game != null && onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .padding(14.dp)
                                .align(Alignment.TopEnd)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (game.isFavorite) AccentRose else Color.White
                            )
                        }
                    }

                    // Title & Badges at bottom of header
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (game != null) {
                                GameStatusBadge(status = game.status)
                                if (game.isArchived) {
                                    Surface(
                                        color = AccentAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = AccentAmber,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Archived",
                                                color = AccentAmber,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else if (isInVault) {
                                GameStatusBadge(status = vaultGameStatus ?: GameStatus.BACKLOG)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = platforms.firstOrNull() ?: "PC",
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(text = " • ", color = TextMuted, fontSize = 13.sp)
                            Text(
                                text = genres.firstOrNull() ?: "Action",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(text = " • ", color = TextMuted, fontSize = 13.sp)
                            Text(
                                text = releaseDate.take(4),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Body content
                Column(modifier = Modifier.padding(18.dp)) {

                    if (game?.isArchived == true) {
                        Surface(
                            color = AccentAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Archived / Hidden Game",
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Hidden from your active library view to keep focus on current projects. You can unarchive it anytime.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isLoadingDetails) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading full RAWG details...", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    // Quick Status Switcher Chips (for Vault Game)
                    if (game != null && onStatusChange != null) {
                        Text(
                            text = "COMPLETION STATUS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameStatus.entries.forEach { status ->
                                val isSelected = game.status == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onStatusChange(status) },
                                    label = { Text(status.displayName) },
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

                        Spacer(modifier = Modifier.height(20.dp))
                    } else if (!isInVault && onAddToVault != null && (rawgGameDto != null || fullRawgDetails != null)) {
                        // "Add to Vault" Banner Button
                        Button(
                            onClick = {
                                val targetDto = fullRawgDetails ?: rawgGameDto!!
                                onAddToVault(targetDto, GameStatus.BACKLOG)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Game to My Vault", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Stats Grid Cards (Ratings, Playtime, Metacritic)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // RAWG Rating Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RAWG RATING",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (rawgRatingValue > 0) String.format(Locale.US, "%.1f / 5.0", rawgRatingValue) else "Unrated",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Personal Rating Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "MY RATING",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (personalRating > 0) "$personalRating / 10" else "Unrated",
                                    color = AccentAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Metacritic Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "METACRITIC",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (metacriticScore != null) "$metacriticScore" else "N/A",
                                    color = when {
                                        metacriticScore == null -> TextMuted
                                        metacriticScore >= 75 -> AccentEmerald
                                        metacriticScore >= 50 -> AccentAmber
                                        else -> Color(0xFFE53935)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playtime & Gaming Session Timer Card (for Vault Game)
                    if (game != null) {
                        val isCurrentGameInSession = activeSession?.first?.id == game.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isCurrentGameInSession) NeonCyan else DarkCardBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PLAYTIME & SESSION TRACKER",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", game.playtimeHours)} Hours Logged",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isCurrentGameInSession) {
                                    Button(
                                        onClick = { onStopSession?.invoke() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Stop & Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onStartSession?.invoke(game) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start Playing", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Developers & Publishers info
                    if (!developers.isNullOrBlank() || !publishers.isNullOrBlank() || !esrbRating.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (!developers.isNullOrBlank()) {
                                    Row {
                                        Text("Developer: ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(developers, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                                if (!publishers.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Text("Publisher: ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(publishers, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                                if (!esrbRating.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Text("ESRB Rating: ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(esrbRating, color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Expandable Game Overview / Description
                    Text(
                        text = "GAME OVERVIEW",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                .padding(14.dp)
                        ) {
                            val isLongDescription = description.length > 180
                            Text(
                                text = description,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                maxLines = if (!isDescriptionExpanded && isLongDescription) 4 else Int.MAX_VALUE,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isLongDescription) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isDescriptionExpanded) "Show less" else "Read more",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Official Trailer Section (only shown if trailers with valid video URLs exist)
                    val validTrailers = trailers.filter { !it.getVideoUrl().isNullOrBlank() }
                    if (validTrailers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "OFFICIAL TRAILER",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 12.dp)
                        ) {
                            items(validTrailers, key = { it.id ?: it.name.hashCode() }) { trailer ->
                                val previewImage = trailer.preview?.takeIf { it.isNotBlank() } ?: coverUrl
                                val trailerTitleText = trailer.name?.takeIf { it.isNotBlank() } ?: "Official Trailer"

                                Card(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                        .clickable { activeTrailerForPlayback = trailer },
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Trailer Thumbnail Preview Image
                                        if (previewImage.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(previewImage)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = trailerTitleText,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Dark Gradient Overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Black.copy(alpha = 0.2f),
                                                            Color.Black.copy(alpha = 0.7f)
                                                        )
                                                    )
                                                )
                                        )

                                        // Center Play Button Icon with Neon Accent
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(CyberPurple.copy(alpha = 0.85f))
                                                .border(1.5.dp, NeonCyan, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play trailer",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        // Trailer Title
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = trailerTitleText,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Screenshots Gallery Carousel
                    if (screenshots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "SCREENSHOTS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 12.dp)
                        ) {
                            items(screenshots, key = { it.id ?: it.image.hashCode() }) { shot ->
                                val shotUrl = shot.image ?: return@items
                                Box(
                                    modifier = Modifier
                                        .size(width = 200.dp, height = 120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurface)
                                        .clickable { zoomedScreenshotUrl = shotUrl }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(shotUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Game screenshot preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Similar Games Carousel ("You Might Also Like")
                    if (similarGames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "YOU MIGHT ALSO LIKE",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 12.dp)
                        ) {
                            items(similarGames, key = { it.id }) { sim ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                                    border = BorderStroke(1.dp, DarkCardBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { onSelectSimilarGame?.invoke(sim) }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .background(DarkSurface)
                                        ) {
                                            if (!sim.backgroundImage.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = sim.backgroundImage,
                                                    contentDescription = sim.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = sim.name,
                                                maxLines = 1,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = sim.released?.take(4) ?: "",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Official Website Link
                    if (!website.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle intent error
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visit Official Game Website", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action buttons for Vault Game (Archive, Edit, Delete)
                    if (game != null) {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (onArchiveToggle != null) {
                            OutlinedButton(
                                onClick = { onArchiveToggle(game) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_archive_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (game.isArchived) AccentEmerald.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.4f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (game.isArchived) AccentEmerald.copy(alpha = 0.12f) else DarkCard
                                )
                            ) {
                                Icon(
                                    imageVector = if (game.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = if (game.isArchived) AccentEmerald else NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (game.isArchived) "Unarchive Game (Restore to Active Library)" else "Archive Game (Hide from Active Library)",
                                    color = if (game.isArchived) AccentEmerald else NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (onEdit != null && onDelete != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_delete_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = AccentRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Delete", color = AccentRose)
                                }

                                FilledTonalButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_edit_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Edit Game", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Trailer Video Player Dialog
    activeTrailerForPlayback?.let { trailer ->
        val videoUrl = trailer.getVideoUrl()
        if (!videoUrl.isNullOrBlank()) {
            TrailerPlayerDialog(
                trailerTitle = trailer.name ?: title,
                videoUrl = videoUrl,
                onDismiss = { activeTrailerForPlayback = null }
            )
        }
    }

    // Fullscreen Screenshot Zoom Dialog
    zoomedScreenshotUrl?.let { imgUrl ->
        Dialog(
            onDismissRequest = { zoomedScreenshotUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { zoomedScreenshotUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imgUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Zoomed screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                )

                IconButton(
                    onClick = { zoomedScreenshotUrl = null },
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close zoom", tint = Color.White)
                }
            }
        }
    }
}
