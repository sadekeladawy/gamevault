package com.example.data.model

data class MasterGame(
    val id: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val developer: String = "",
    val publisher: String = "",
    val platform: String = "PC",
    val platforms: List<String> = emptyList(),
    val genre: String = "Action RPG",
    val releaseYear: Int = 2023,
    val globalRating: Double = 9.0, // 0.0 - 10.0
    val description: String = "",
    val isPopular: Boolean = false,
    val source: String = "Firestore Database"
) {
    fun toGame(status: GameStatus = GameStatus.BACKLOG, rating: Int = 0): Game {
        return Game(
            title = title,
            coverUrl = coverUrl,
            platform = platform,
            genre = genre,
            releaseYear = releaseYear,
            status = status,
            rating = rating,
            notes = if (description.isNotBlank()) "$description\n\n[Added from $source]" else "[Added from $source]",
            isFavorite = false
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "coverUrl" to coverUrl,
        "developer" to developer,
        "publisher" to publisher,
        "platform" to platform,
        "platforms" to platforms,
        "genre" to genre,
        "releaseYear" to releaseYear,
        "globalRating" to globalRating,
        "description" to description,
        "isPopular" to isPopular,
        "source" to source,
        "updatedAt" to System.currentTimeMillis()
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): MasterGame {
            val plats = (map["platforms"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val singlePlat = map["platform"] as? String ?: (plats.firstOrNull() ?: "PC")
            return MasterGame(
                id = id.ifBlank { map["id"] as? String ?: "" },
                title = map["title"] as? String ?: "Unknown Title",
                coverUrl = map["coverUrl"] as? String ?: "",
                developer = map["developer"] as? String ?: "",
                publisher = map["publisher"] as? String ?: "",
                platform = singlePlat,
                platforms = if (plats.isNotEmpty()) plats else listOf(singlePlat),
                genre = map["genre"] as? String ?: "Action RPG",
                releaseYear = (map["releaseYear"] as? Number)?.toInt() ?: 2023,
                globalRating = (map["globalRating"] as? Number)?.toDouble() ?: 8.5,
                description = map["description"] as? String ?: "",
                isPopular = map["isPopular"] as? Boolean ?: false,
                source = map["source"] as? String ?: "Firestore Database"
            )
        }
    }
}
