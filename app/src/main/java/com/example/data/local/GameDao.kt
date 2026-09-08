package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGamesForUser(userId: String): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    fun getGameById(id: Long): Flow<Game?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<Game>)

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("DELETE FROM games WHERE userId = :userId")
    suspend fun deleteGamesForUser(userId: String)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGamesCount(): Int

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId")
    suspend fun getGamesCountForUser(userId: String): Int
}
