package com.example.data.repository

import android.content.Context
import com.example.data.local.GameDao
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.sample.SampleGames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(private val gameDao: GameDao) {

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

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val count = gameDao.getGamesCount()
        if (count == 0) {
            gameDao.insertGames(SampleGames.initialGames)
        }
    }

    suspend fun resetToSampleData() = withContext(Dispatchers.IO) {
        gameDao.deleteAllGames()
        gameDao.insertGames(SampleGames.initialGames)
    }

    suspend fun clearAllGames() = withContext(Dispatchers.IO) {
        gameDao.deleteAllGames()
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

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
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
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
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
}
