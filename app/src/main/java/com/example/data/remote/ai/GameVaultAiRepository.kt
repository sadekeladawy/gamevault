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

class GameVaultAiRepository {
    companion object {
        private const val TAG = "GameVaultAiRepository"
    }

    /**
     * Lazily initialized Gemini GenerativeModel using Firebase AI Logic API.
     * Uses Google's Gemini model specified in [AiConfig.MODEL_NAME] ("gemini-2.5-flash").
     */
    private val generativeModel: GenerativeModel by lazy {
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
     * Initializes a multi-turn chat session with optional prior conversation history.
     * @param history List of [Content] objects representing prior turns in the conversation.
     * @return Active [Chat] session supporting multi-turn conversation.
     */
    fun startChat(history: List<Content> = emptyList()): Chat {
        Log.d(TAG, "Starting multi-turn chat session with ${history.size} history items (model: ${AiConfig.MODEL_NAME})...")
        return generativeModel.startChat(history)
    }

    /**
     * Sends a message within an active multi-turn [Chat] session and receives the response.
     */
    suspend fun sendMessage(chat: Chat, prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message to chat session (model: ${AiConfig.MODEL_NAME})...")
            val response = chat.sendMessage(prompt)
            response.text
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Firebase AI Logic sendMessage error: ${e.message}", e)
            throw Exception(handleAiError(e), e)
        }
    }

    /**
     * Sends a message within an active multi-turn [Chat] session and streams back response text chunks.
     */
    fun sendMessageStream(chat: Chat, prompt: String): Flow<String> = flow {
        Log.d(TAG, "Streaming message to chat session (model: ${AiConfig.MODEL_NAME})...")
        chat.sendMessageStream(prompt).collect { chunk ->
            chunk.text?.let { textChunk ->
                if (textChunk.isNotEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        Log.e(TAG, "Firebase AI Logic sendMessageStream error: ${cause.message}", cause)
        throw Exception(handleAiError(cause), cause)
    }.flowOn(Dispatchers.IO)

    /**
     * Single-turn content generation for a prompt.
     */
    suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating content via Firebase AI Logic (model: ${AiConfig.MODEL_NAME})...")
            val response = generativeModel.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Firebase AI Logic generateContent error: ${e.message}", e)
            throw Exception(handleAiError(e), e)
        }
    }

    /**
     * Single-turn streaming content generation for a prompt.
     */
    fun generateContentStream(prompt: String): Flow<String> = flow {
        Log.d(TAG, "Streaming content via Firebase AI Logic (model: ${AiConfig.MODEL_NAME})...")
        generativeModel.generateContentStream(prompt).collect { chunk ->
            chunk.text?.let { textChunk ->
                if (textChunk.isNotEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        Log.e(TAG, "Firebase AI Logic generateContentStream error: ${cause.message}", cause)
        throw Exception(handleAiError(cause), cause)
    }.flowOn(Dispatchers.IO)

    /**
     * Converts SDK exceptions and errors into user-friendly error messages without exposing sensitive internals or secrets.
     */
    private fun handleAiError(throwable: Throwable): String {
        return when (throwable) {
            is PromptBlockedException -> "The requested query was blocked by safety settings. Please rephrase your gaming question."
            is ResponseStoppedException -> "The AI response was stopped before completion. Please try asking again."
            is FirebaseAIException -> "GameVault AI Service error: ${throwable.message ?: "Unable to process request."}"
            is IOException -> "Network error connecting to GameVault AI. Please check your internet connection."
            else -> throwable.localizedMessage ?: "An unexpected error occurred while communicating with GameVault AI."
        }
    }
}
