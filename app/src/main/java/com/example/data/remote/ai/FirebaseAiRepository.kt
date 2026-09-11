package com.example.data.remote.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Production-ready AI repository powered by the official Firebase AI Logic Android SDK.
 * Communicates with Google's Gemini models (gemini-2.5-flash) without hardcoded API keys.
 *
 * Supports:
 * - Multi-turn conversational chat sessions
 * - Real-time streaming response chunks via Kotlin Flow
 * - Immediate request cancellation when user navigates away or stops generation
 * - Contextual system instructions
 * - Robust error handling with user-friendly error messages
 */
class FirebaseAiRepository {

    companion object {
        private const val TAG = "FirebaseAiRepository"
    }

    private val generativeModel: GenerativeModel by lazy {
        Log.d(TAG, "Initializing Firebase AI Logic GenerativeModel (model: ${AiConfig.MODEL_NAME})...")
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AiConfig.MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
                maxOutputTokens = 2048
            },
            systemInstruction = content {
                text(GameVaultAiContextBuilder.SYSTEM_INSTRUCTION)
            }
        )
    }

    /**
     * Starts a multi-turn chat session with optional prior conversation history.
     */
    fun startChat(history: List<Content> = emptyList()): Chat {
        Log.d(TAG, "Starting multi-turn chat session with ${history.size} history messages")
        return generativeModel.startChat(history)
    }

    /**
     * Sends a message within an active chat session and awaits the complete response text.
     */
    suspend fun sendMessage(chat: Chat, prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message to Gemini chat session...")
            val response = chat.sendMessage(prompt)
            response.text
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "sendMessage error: ${e.message}", e)
            throw Exception(handleAiError(e), e)
        }
    }

    /**
     * Sends a message within an active chat session and streams back text chunks as they arrive.
     */
    fun sendMessageStream(chat: Chat, prompt: String): Flow<String> = flow {
        Log.d(TAG, "Streaming message to Gemini chat session...")
        chat.sendMessageStream(prompt).collect { chunk ->
            chunk.text?.let { textChunk ->
                if (textChunk.isNotEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        Log.e(TAG, "sendMessageStream error: ${cause.message}", cause)
        throw Exception(handleAiError(cause), cause)
    }.flowOn(Dispatchers.IO)

    /**
     * Single-turn content generation.
     */
    suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating single-turn content...")
            val response = generativeModel.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "generateContent error: ${e.message}", e)
            throw Exception(handleAiError(e), e)
        }
    }

    /**
     * Single-turn content streaming.
     */
    fun generateContentStream(prompt: String): Flow<String> = flow {
        Log.d(TAG, "Streaming single-turn content...")
        generativeModel.generateContentStream(prompt).collect { chunk ->
            chunk.text?.let { textChunk ->
                if (textChunk.isNotEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        Log.e(TAG, "generateContentStream error: ${cause.message}", cause)
        throw Exception(handleAiError(cause), cause)
    }.flowOn(Dispatchers.IO)

    private fun handleAiError(e: Throwable): String {
        return when (e) {
            is PromptBlockedException -> "The prompt was blocked by safety filters. Please rephrase your gaming question."
            is ResponseStoppedException -> "Response generation was stopped by content policies."
            is FirebaseAIException -> "AI service issue: ${e.localizedMessage ?: "Unknown error"}. Please try again shortly."
            is IOException -> "Network connectivity issue while reaching AI Copilot. Please check your internet connection."
            else -> e.localizedMessage ?: "Unexpected error communicating with AI Copilot."
        }
    }
}
