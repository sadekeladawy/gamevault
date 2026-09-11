package com.example.data.remote.ai

import android.util.Log
import com.example.data.model.ai.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * GameVault AI Repository.
 *
 * Implements clean MVVM + Repository architecture:
 * UI -> AiChatViewModel -> GameVaultAiRepository -> GeminiApiService -> Secure Backend Gateway -> Gemini Model.
 *
 * Fully replaces Firebase AI Logic with official Gemini API, keeping the API key secure server-side.
 */
class GameVaultAiRepository(
    private val apiService: GeminiApiService = GeminiApiServiceImpl()
) {
    companion object {
        private const val TAG = "GameVaultAiRepository"
    }

    /**
     * Sends a chat prompt or question to the Gemini API service via the secure backend
     * and streams back the generated response chunks in real-time.
     */
    fun streamChat(
        prompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        focusedContext: String? = null,
        systemInstruction: String = GameVaultAiContextBuilder.SYSTEM_INSTRUCTION,
        model: String = AiConfig.MODEL_NAME
    ): Flow<String> {
        val historyTurns = GameVaultAiContextBuilder.buildChatHistory(conversationHistory)
        val request = GeminiChatRequest(
            prompt = prompt,
            messages = historyTurns,
            systemInstruction = systemInstruction,
            context = focusedContext,
            model = model
        )

        Log.d(TAG, "Streaming chat with model $model (history: ${historyTurns.size} turns)...")
        return apiService.streamChat(request)
            .catch { cause ->
                if (cause is CancellationException) throw cause
                Log.e(TAG, "GeminiApiService stream error: ${cause.message}", cause)
                throw IOException(handleAiError(cause), cause)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Non-streaming single call to the Gemini API service via the secure backend.
     */
    suspend fun generateChat(
        prompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        focusedContext: String? = null,
        systemInstruction: String = GameVaultAiContextBuilder.SYSTEM_INSTRUCTION,
        model: String = AiConfig.MODEL_NAME
    ): String = withContext(Dispatchers.IO) {
        try {
            val historyTurns = GameVaultAiContextBuilder.buildChatHistory(conversationHistory)
            val request = GeminiChatRequest(
                prompt = prompt,
                messages = historyTurns,
                systemInstruction = systemInstruction,
                context = focusedContext,
                model = model
            )

            Log.d(TAG, "Generating chat with model $model...")
            val response = apiService.generateChat(request)
            response.text
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "GeminiApiService generateChat error: ${e.message}", e)
            throw IOException(handleAiError(e), e)
        }
    }

    private fun handleAiError(throwable: Throwable): String {
        return when (throwable) {
            is IOException -> throwable.message?.takeIf { it.isNotBlank() }
                ?: "Network error connecting to GameVault AI. Please check your connection."
            else -> throwable.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "An unexpected error occurred while communicating with GameVault AI."
        }
    }
}
