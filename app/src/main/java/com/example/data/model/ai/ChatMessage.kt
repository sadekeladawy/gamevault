package com.example.data.model.ai

import com.example.data.remote.rawg.RawgGameDto
import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedGames: List<RawgGameDto> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val rawgQueryUsed: String? = null
)
