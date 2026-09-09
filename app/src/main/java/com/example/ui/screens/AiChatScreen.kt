package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.data.model.ai.ChatMessage
import com.example.data.model.ai.MessageSender
import com.example.data.remote.rawg.RawgGameDto
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
import com.example.ui.viewmodel.AiChatViewModel
import java.util.Locale

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    userBacklog: List<Game> = emptyList(),
    onSelectRawgGame: (RawgGameDto) -> Unit,
    onAddRawgGameToVault: ((RawgGameDto) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Auto-scroll to the bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .imePadding()
    ) {
        // AI Assistant Header
        AiChatHeader(
            onClearChat = { viewModel.clearChat() }
        )

        // Main Chat Messages Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Quick Suggestion Chips Header on start
                if (messages.size <= 1) {
                    item {
                        SuggestedPromptsSection(
                            onPromptClick = { prompt ->
                                viewModel.sendMessage(prompt, userBacklog)
                            }
                        )
                    }
                }

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        onSelectGame = onSelectRawgGame,
                        onAddRawgGameToVault = onAddRawgGameToVault
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingIndicatorItem()
                    }
                }
            }
        }

        // Error & Retry Banner
        if (errorMessage != null) {
            Surface(
                color = Color(0xFF3B151E),
                contentColor = Color(0xFFFFB4AB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage ?: "Error sending message",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.retryLastMessage(userBacklog) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1227)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
        }

        // Input Field Bar
        ChatInputBar(
            inputText = inputText,
            isThinking = isThinking,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.sendMessage(userBacklog = userBacklog) }
        )
    }
}

@Composable
private fun AiChatHeader(onClearChat: () -> Unit) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CyberPurple, NeonCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "GameVault AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Intelligent Gaming Assistant & Backlog Advisor",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Chat",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SuggestedPromptsSection(onPromptClick: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Asking GameVault AI:",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val prompts = listOf(
                "🎯 What should I play from my backlog tonight?",
                "🎮 Recommend 5 RPG games released after 2020",
                "⚔️ What games are similar to Elden Ring?",
                "🚀 Best Sci-Fi games on PC rated above 4.0",
                "🔍 Tell me everything about Cyberpunk 2077",
                "🆚 Compare Witcher 3 and Skyrim"
            )

            prompts.forEach { prompt ->
                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPromptClick(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onSelectGame: (RawgGameDto) -> Unit,
    onAddRawgGameToVault: ((RawgGameDto) -> Unit)? = null
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = if (isUser) CyberPurple else DarkCard,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = if (isUser) null else BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Recommended Game Cards Row inside AI Message
        if (!isUser && message.recommendedGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Interactive Game Recommendations (${message.recommendedGames.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.padding(start = 40.dp, bottom = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(start = 40.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = message.recommendedGames,
                    key = { it.id }
                ) { game ->
                    AiRecommendedGameCard(
                        game = game,
                        onClick = { onSelectGame(game) },
                        onAdd = if (onAddRawgGameToVault != null) { { onAddRawgGameToVault(game) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun AiRecommendedGameCard(
    game: RawgGameDto,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    val ratingText = if (game.rating != null && game.rating > 0) "Rating ${String.format(Locale.US, "%.1f", game.rating)}/5.0" else "Unrated"
    val releaseYear = game.released?.take(4) ?: ""
    val genresText = game.genres?.firstOrNull()?.name ?: ""
    val platformText = game.platforms?.firstOrNull()?.platform?.name ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                if (!game.backgroundImage.isNullOrBlank()) {
                    AsyncImage(
                        model = game.backgroundImage,
                        contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Cover", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Metacritic score pill ("Metacritic 92")
                if ((game.metacritic ?: 0) > 0) {
                    val mcColor = when {
                        game.metacritic!! >= 75 -> AccentEmerald
                        game.metacritic >= 50 -> AccentAmber
                        else -> Color(0xFFE53935)
                    }
                    Surface(
                        color = mcColor,
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "Metacritic ${game.metacritic}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = game.name,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ratingText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentAmber
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (genresText.isNotBlank()) {
                        Text(
                            text = genresText,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (releaseYear.isNotBlank()) {
                        Text(
                            text = releaseYear,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                if (platformText.isNotBlank() || onAdd != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (platformText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = platformText,
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (onAdd != null) {
                            Surface(
                                color = CyberPurple.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, CyberPurple),
                                modifier = Modifier.clickable { onAdd() }
                            ) {
                                Text(
                                    text = "+ Add",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicatorItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyberPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier
                    .size(18.dp)
                    .scale(scale)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GameVault AI is analyzing RAWG database...",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isThinking: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = { Text("Ask about games, ratings, backlog...", fontSize = 13.sp, color = TextMuted) },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isThinking,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isThinking)
                            Brush.linearGradient(listOf(CyberPurple, NeonCyan))
                        else
                            Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isThinking) Color.White else TextMuted
                )
            }
        }
    }
}
