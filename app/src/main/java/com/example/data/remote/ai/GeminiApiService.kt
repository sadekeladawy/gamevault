package com.example.data.remote.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    suspend fun generateChat(request: GeminiChatRequest): GeminiChatResponse
    fun streamChat(request: GeminiChatRequest): Flow<String>
}

class GeminiApiServiceImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : GeminiApiService {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Candidate base URLs to reach the secure server backend:
    // 1. Android emulator loopback to host port 3000
    // 2. Cloud Run development URL via Nginx proxy
    // 3. Localhost loopback (Robolectric / local tests)
    private val candidateBaseUrls = listOf(
        "http://10.0.2.2:3000/",
        "https://ais-dev-vjf2ww6vcmn5pct622gdy4-564733420101.europe-west2.run.app/",
        "http://127.0.0.1:3000/"
    )

    @Volatile
    private var resolvedBaseUrl: String? = null

    private fun getBaseUrl(): String {
        return resolvedBaseUrl ?: candidateBaseUrls.first()
    }

    override suspend fun generateChat(request: GeminiChatRequest): GeminiChatResponse = withContext(Dispatchers.IO) {
        val requestJson = gson.toJson(request)
        val body = requestJson.toRequestBody(jsonMediaType)

        val urlsToTry = if (resolvedBaseUrl != null) {
            listOf(resolvedBaseUrl!!) + candidateBaseUrls.filter { it != resolvedBaseUrl }
        } else {
            candidateBaseUrls
        }

        var lastException: Exception? = null

        for (baseUrl in urlsToTry) {
            try {
                val httpRequest = Request.Builder()
                    .url("${baseUrl}api/chat")
                    .post(body)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    resolvedBaseUrl = baseUrl
                    val json = JsonParser.parseString(respBody).asJsonObject
                    val text = json.get("text")?.asString ?: ""
                    val model = json.get("model")?.asString
                    return@withContext GeminiChatResponse(text = text, model = model)
                } else {
                    val errorBody = response.body?.string().orEmpty()
                    Log.w(TAG, "generateChat non-success from $baseUrl: ${response.code} $errorBody")
                    lastException = IOException("Server returned HTTP ${response.code}: $errorBody")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed to $baseUrl: ${e.message}")
                lastException = e
            }
        }

        throw lastException ?: IOException("Failed to connect to GameVault AI server")
    }

    override fun streamChat(request: GeminiChatRequest): Flow<String> = flow {
        val requestJson = gson.toJson(request)
        val body = requestJson.toRequestBody(jsonMediaType)

        val urlsToTry = if (resolvedBaseUrl != null) {
            listOf(resolvedBaseUrl!!) + candidateBaseUrls.filter { it != resolvedBaseUrl }
        } else {
            candidateBaseUrls
        }

        var success = false
        var lastException: Exception? = null

        for (baseUrl in urlsToTry) {
            try {
                val httpRequest = Request.Builder()
                    .url("${baseUrl}api/chat/stream")
                    .post(body)
                    .header("Accept", "text/event-stream")
                    .build()

                val response = client.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    resolvedBaseUrl = baseUrl
                    val source = response.body?.source() ?: throw IOException("Empty response body from stream")

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue

                        if (trimmed.startsWith("data: ")) {
                            val data = trimmed.substring(6).trim()
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunkJson = JsonParser.parseString(data).asJsonObject
                                if (chunkJson.has("error")) {
                                    val err = chunkJson.get("error").asString
                                    throw IOException(err)
                                }
                                val chunkText = chunkJson.get("text")?.asString
                                if (!chunkText.isNullOrEmpty()) {
                                    emit(chunkText)
                                }
                            } catch (parseEx: Exception) {
                                if (parseEx is IOException) throw parseEx
                                // Non-JSON chunk, skip
                            }
                        }
                    }
                    success = true
                    break
                } else {
                    val err = response.body?.string().orEmpty()
                    Log.w(TAG, "Stream returned HTTP ${response.code} from $baseUrl: $err")
                    lastException = IOException("Stream HTTP ${response.code}: $err")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Stream connection failed to $baseUrl: ${e.message}")
                lastException = e
            }
        }

        if (!success) {
            throw lastException ?: IOException("Unable to stream response from GameVault AI server")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "GeminiApiService"
    }
}
