package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.GameDao
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgApiClient
import com.example.data.remote.rawg.RawgApiService
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.sample.SampleGames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class GameRepository(
    private val gameDao: GameDao,
    private val rawgApiService: RawgApiService = RawgApiClient.apiService,
    private val rawgApiKey: String = RawgApiClient.HARDCODED_RAWG_API_KEY
) {
    companion object {
        private const val TAG = "GameRepository"
    }

    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    suspend fun getGameById(id: Long): Flow<Game?> = gameDao.getGameById(id)

    suspend fun insertGame(game: Game): Long = withContext(Dispatchers.IO) {
        gameDao.insertGame(game)
    }

    suspend fun updateGame(game: Game) = withContext(Dispatchers.IO) {
        gameDao.updateGame(game)
    }

    suspend fun deleteGame(game: Game) = withContext(Dispatchers.IO) {
        gameDao.deleteGame(game)
    }

    suspend fun toggleFavorite(game: Game) = withContext(Dispatchers.IO) {
        gameDao.updateGame(game.copy(isFavorite = !game.isFavorite))
    }

    // --- RAWG Video Games API (Real Network Requests) ---

    /**
     * Searches RAWG Video Games Database via live REST network request.
     */
    suspend fun searchRawgGames(query: String): Result<List<RawgGameDto>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            Log.d(TAG, "Executing RAWG API search for \"$trimmed\"")
            val response = rawgApiService.searchGames(
                apiKey = rawgApiKey,
                search = trimmed,
                pageSize = 25
            )
            val results = response.results ?: emptyList()
            Log.d(TAG, "RAWG API search succeeded with ${results.size} games")
            Result.success(results)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during RAWG search: ${e.message}", e)
            Result.failure(Exception("Network error connecting to RAWG API. Please check your internet connection.", e))
        } catch (e: HttpException) {
            val code = e.code()
            Log.e(TAG, "HTTP $code during RAWG search: ${e.message}", e)
            val msg = when (code) {
                401 -> "RAWG API unauthorized (invalid API key). Please check your key in RawgApiClient."
                429 -> "RAWG API rate limit reached. Please wait a moment and try again."
                else -> "RAWG API error ($code). Please try again shortly."
            }
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Error during RAWG search: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to search RAWG database.", e))
        }
    }

    /**
     * Fetches top games from RAWG API.
     */
    suspend fun fetchPopularRawgGames(pageSize: Int = 20): Result<List<RawgGameDto>> = withContext(Dispatchers.IO) {
        try {
            val response = rawgApiService.getGames(
                apiKey = rawgApiKey,
                pageSize = pageSize,
                ordering = "-rating"
            )
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top RAWG games: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(game: Game, newStatus: GameStatus, completionDate: String? = null) = withContext(Dispatchers.IO) {
        val updated = game.copy(
            status = newStatus,
            completionDate = if (newStatus == GameStatus.COMPLETED) {
                completionDate ?: game.completionDate ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            } else {
                game.completionDate
            }
        )
        gameDao.updateGame(updated)
    }

    /**
     * Deprecated: Automatic sample data seeding is disabled to ensure user data isolation.
     * New users start with an empty profile and empty game library.
     */
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        // Intentionally no-op: do not seed sample games automatically for new users
    }

    suspend fun resetToSampleData(userId: String = "") = withContext(Dispatchers.IO) {
        gameDao.deleteAllGames()
        val taggedGames = SampleGames.initialGames.map { it.copy(id = 0, userId = userId) }
        gameDao.insertGames(taggedGames)
    }

    suspend fun clearAllGames() = withContext(Dispatchers.IO) {
        gameDao.deleteAllGames()
    }

    suspend fun clearGamesForUser(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            gameDao.deleteGamesForUser(userId)
        } else {
            gameDao.deleteAllGames()
        }
    }

    suspend fun exportToJson(games: List<Game>): String = withContext(Dispatchers.Default) {
        val jsonArray = JSONArray()
        for (game in games) {
            val obj = JSONObject().apply {
                put("id", game.id)
                put("title", game.title)
                put("coverUrl", game.coverUrl)
                put("platform", game.platform)
                put("genre", game.genre)
                put("releaseYear", game.releaseYear)
                put("status", game.status.name)
                put("completionDate", game.completionDate ?: "")
                put("playtimeHours", game.playtimeHours)
                put("rating", game.rating)
                put("notes", game.notes)
                put("isFavorite", game.isFavorite)
                put("createdAt", game.createdAt)
            }
            jsonArray.put(obj)
        }
        jsonArray.toString(2)
    }

    suspend fun exportToCsv(games: List<Game>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("Title,Platform,Genre,ReleaseYear,Status,CompletionDate,PlaytimeHours,Rating,Favorite,Notes\n")
        for (g in games) {
            val title = "\"${g.title.replace("\"", "\"\"")}\""
            val platform = "\"${g.platform.replace("\"", "\"\"")}\""
            val genre = "\"${g.genre.replace("\"", "\"\"")}\""
            val notes = "\"${g.notes.replace("\"", "\"\"")}\""
            val compDate = g.completionDate ?: ""
            sb.append("$title,$platform,$genre,${g.releaseYear},${g.status.name},$compDate,${g.playtimeHours},${g.rating},${g.isFavorite},$notes\n")
        }
        sb.toString()
    }

    suspend fun importFromJson(jsonString: String, userId: String = ""): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray(jsonString.trim())
            val importedList = mutableListOf<Game>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val game = Game(
                    id = 0, // Auto-generate new IDs to avoid collision
                    title = obj.optString("title", "Untitled Game"),
                    coverUrl = obj.optString("coverUrl", ""),
                    platform = obj.optString("platform", "PC"),
                    genre = obj.optString("genre", "Action"),
                    releaseYear = obj.optInt("releaseYear", 2024),
                    status = GameStatus.fromString(obj.optString("status", "BACKLOG")),
                    completionDate = obj.optString("completionDate").takeIf { it.isNotBlank() },
                    playtimeHours = obj.optDouble("playtimeHours", 0.0),
                    rating = obj.optInt("rating", 0).coerceIn(0, 10),
                    notes = obj.optString("notes", ""),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    userId = userId
                )
                importedList.add(game)
            }
            if (importedList.isNotEmpty()) {
                gameDao.insertGames(importedList)
            }
            Result.success(importedList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importGames(games: List<Game>): Int = withContext(Dispatchers.IO) {
        if (games.isNotEmpty()) {
            gameDao.insertGames(games)
        }
        games.size
    }
}
