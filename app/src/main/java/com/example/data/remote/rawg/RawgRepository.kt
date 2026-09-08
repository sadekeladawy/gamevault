package com.example.data.remote.rawg

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class RawgRepository(
    private val apiService: RawgApiService = RawgApiClient.apiService,
    val apiKey: String = RawgApiClient.HARDCODED_RAWG_API_KEY
) {
    companion object {
        private const val TAG = "RawgRepository"
    }

    suspend fun searchGames(query: String): Result<List<RawgGameDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            Log.d(TAG, "Searching RAWG for \"$trimmed\" using key: ${apiKey.take(5)}...")
            val response = apiService.searchGames(
                apiKey = apiKey,
                search = trimmed
            )
            val results = response.results ?: emptyList()
            Log.d(TAG, "RAWG search for \"$trimmed\" succeeded with ${results.size} games")
            Result.success(results)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during RAWG search: ${e.message}", e)
            Result.failure(Exception("Network error connecting to RAWG API. Please check your internet connection.", e))
        } catch (e: HttpException) {
            val code = e.code()
            Log.e(TAG, "HTTP error $code during RAWG search: ${e.message}", e)
            val msg = when (code) {
                401 -> "RAWG API unauthorized (invalid API key). Please verify your RAWG key."
                429 -> "RAWG API rate limit reached. Please wait a moment and try again."
                else -> "RAWG server error ($code). Please try again shortly."
            }
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during RAWG search: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to search RAWG database.", e))
        }
    }
}
