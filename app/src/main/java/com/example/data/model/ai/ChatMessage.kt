package com.example.data.model.ai

import com.example.data.model.Game
import com.example.data.remote.rawg.RawgGameDto
import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

sealed class AiQuickAction(val label: String) {
    data class ViewGame(val game: Game) : AiQuickAction("View ${game.title}")
    data class StartPlaying(val game: Game) : AiQuickAction("Start Playing")
    data class ViewFranchise(val franchiseName: String) : AiQuickAction("View $franchiseName Series")
    data class Navigate(val destinationRoute: String, val buttonLabel: String) : AiQuickAction(buttonLabel)
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedGames: List<RawgGameDto> = emptyList(),
    val recommendedVaultGames: List<Game> = emptyList(),
    val recommendedFranchise: String? = null,
    val suggestedQuickActions: List<AiQuickAction> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val rawgQueryUsed: String? = null
)
