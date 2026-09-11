package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.CachePolicy
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.StatusBacklogColor
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.StatusDroppedColor
import com.example.ui.theme.StatusPlayingColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun GameStatusBadge(status: GameStatus, modifier: Modifier = Modifier) {
    val (bg, textCol) = remember(status) {
        when (status) {
            GameStatus.WISHLIST -> Pair(PrimaryRed.copy(alpha = 0.2f), PrimaryRed)
            GameStatus.COMPLETED -> Pair(StatusCompletedColor.copy(alpha = 0.18f), StatusCompletedColor)
            GameStatus.CURRENTLY_PLAYING -> Pair(StatusPlayingColor.copy(alpha = 0.18f), StatusPlayingColor)
            GameStatus.BACKLOG -> Pair(Color(0xFF26262B), TextSecondary)
            GameStatus.DROPPED -> Pair(StatusDroppedColor.copy(alpha = 0.18f), StatusDroppedColor)
        }
    }
    val badgeShape = remember { RoundedCornerShape(8.dp) }
    val borderStroke = remember(textCol) { BorderStroke(1.dp, textCol.copy(alpha = 0.35f)) }

    Surface(
        color = bg,
        shape = badgeShape,
        border = borderStroke,
        modifier = modifier
    ) {
        Text(
            text = status.displayName,
            color = textCol,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = remember { RoundedCornerShape(16.dp) }
    val cardBorder = remember { BorderStroke(1.dp, DarkCardBorder) }
    val placeholderGradient = remember {
        Brush.verticalGradient(listOf(Color(0xFF1E1E22), Color(0xFF121215)))
    }
    val coverShadowGradient = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color(0xDE0B0B0D)))
    }

    val imageRequest = remember(game.coverUrl, context) {
        if (game.coverUrl.isNotBlank()) {
            val processedUrl = if (game.coverUrl.contains("/media/games/")) {
                game.coverUrl.replace("/media/games/", "/media/crop/600/400/games/")
            } else game.coverUrl

            ImageRequest.Builder(context)
                .data(processedUrl)
                .size(360, 480)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(100)
                .build()
        } else null
    }

    val ratingText = remember(game.rating, game.rawgRating) {
        when {
            game.rating > 0 -> "My Rating: ${game.rating}/10"
            game.rawgRating > 0 -> String.format(Locale.US, "Rating: %.1f/5.0", game.rawgRating)
            else -> "Unrated"
        }
    }

    val playtimeText = remember(game.playtimeHours) {
        "${game.playtimeHours.toInt()}h"
    }

    val onCardClick = remember(game.id, onClick) { onClick }
    val onFavClick = remember(game.id, onToggleFavorite) { onToggleFavorite }

    Card(
        modifier = modifier
            .testTag("game_card_${game.id}")
            .clip(cardShape)
            .clickable(onClick = onCardClick)
            .border(cardBorder, cardShape),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = cardShape
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image Container with 3:4 Poster Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(Color(0xFF151924))
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "${game.title} cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder when no URL
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(placeholderGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideogameAsset,
                                contentDescription = null,
                                tint = PrimaryRed.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = game.title.take(12),
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Gradient shadow overlay at bottom of cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(coverShadowGradient)
                )

                // Top row badges: Status badge on left, Favorite heart on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GameStatusBadge(status = game.status)
                        if (game.isArchived) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archived",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "ARCHIVED",
                                        color = AccentAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    val favScale by animateFloatAsState(
                        targetValue = if (game.isFavorite) 1.22f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "fav_scale"
                    )
                    val favTint by animateColorAsState(
                        targetValue = if (game.isFavorite) AccentRose else Color.White.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = 200),
                        label = "fav_tint"
                    )

                    IconButton(
                        onClick = onFavClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("favorite_button_${game.id}")
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = favTint,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer(scaleX = favScale, scaleY = favScale)
                        )
                    }
                }

                // Bottom badge overlay on cover: Platform badge & Year
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = game.platform,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = game.releaseYear.toString(),
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Game info details below cover
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = game.genre,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom metrics: Rating & Playtime
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ratingText,
                        color = if (game.rating > 0 || game.rawgRating > 0) AccentAmber else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Playtime
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = playtimeText,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
