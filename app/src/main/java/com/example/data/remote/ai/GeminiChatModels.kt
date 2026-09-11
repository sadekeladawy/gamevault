package com.example.data.remote.ai

data class ChatTurnDto(
    val role: String,
    val text: String
)

data class GeminiChatRequest(
    val prompt: String,
    val messages: List<ChatTurnDto> = emptyList(),
    val systemInstruction: String? = null,
    val context: String? = null,
    val model: String = "gemini-2.5-flash",
    val temperature: Float = 0.7f
)

data class GeminiChatResponse(
    val text: String,
    val model: String? = null,
    val error: String? = null
)
