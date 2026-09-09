package com.example.data.remote.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class GameVaultAiRepository {
    companion object {
        private const val TAG = "GameVaultAiRepository"
    }

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

    suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating content via Firebase AI Logic (model: ${AiConfig.MODEL_NAME})...")
            val response = generativeModel.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic error: ${e.message}", e)
            throw e
        }
    }

    fun generateContentStream(prompt: String): Flow<String> = flow {
        Log.d(TAG, "Streaming content via Firebase AI Logic (model: ${AiConfig.MODEL_NAME})...")
        generativeModel.generateContentStream(prompt).collect { chunk ->
            chunk.text?.let { textChunk ->
                emit(textChunk)
            }
        }
    }.flowOn(Dispatchers.IO)
}
