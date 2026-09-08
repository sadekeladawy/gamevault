package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class GameStatus(val displayName: String) {
    COMPLETED("Completed"),
    CURRENTLY_PLAYING("Playing"),
    BACKLOG("Backlog"),
    DROPPED("Dropped");

    companion object {
        fun fromString(value: String): GameStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: BACKLOG
        }
    }
}

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val coverUrl: String = "",
    val platform: String = "PC",
    val genre: String = "Action RPG",
    val releaseYear: Int = 2023,
    val status: GameStatus = GameStatus.BACKLOG,
    val completionDate: String? = null,
    val playtimeHours: Double = 0.0,
    val rating: Int = 0, // 1 - 10
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromGameStatus(status: GameStatus): String = status.name

    @TypeConverter
    fun toGameStatus(value: String): GameStatus = GameStatus.fromString(value)
}
