package com.example.data.model

data class Franchise(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val rawgId: Long? = null
)

data class FranchiseDetails(
    val franchise: Franchise,
    val games: List<Game>,
    val totalGames: Int = games.size,
    val completedGames: Int = games.count { it.status == GameStatus.COMPLETED },
    val currentlyPlayingGames: Int = games.count { it.status == GameStatus.CURRENTLY_PLAYING },
    val backlogGames: Int = games.count { it.status == GameStatus.BACKLOG },
    val wishlistGames: Int = games.count { it.status == GameStatus.WISHLIST },
    val abandonedGames: Int = games.count { it.status == GameStatus.DROPPED },
    val completionPercentage: Int = if (games.isNotEmpty()) ((completedGames.toDouble() / games.size) * 100).toInt() else 0,
    val totalPlaytimeHours: Double = games.sumOf { it.playtimeHours },
    val averageRating: Double = games.filter { it.rating > 0 }.map { it.rating.toDouble() }.let { if (it.isNotEmpty()) it.average() else 0.0 },
    val playNextGame: Game? = determinePlayNext(games)
)

private fun determinePlayNext(games: List<Game>): Game? {
    if (games.isEmpty()) return null

    // Sort games by seriesOrder if available, otherwise by releaseYear, then id
    val sorted = games.sortedWith(
        compareBy<Game> { it.seriesOrder ?: Int.MAX_VALUE }
            .thenBy { it.releaseYear }
            .thenBy { it.id }
    )

    // 1. First priority: Game currently in progress
    val currentlyPlaying = sorted.firstOrNull { it.status == GameStatus.CURRENTLY_PLAYING }
    if (currentlyPlaying != null) return currentlyPlaying

    // 2. Second priority: Next uncompleted game in series order (Backlog or Wishlist)
    val nextUncompleted = sorted.firstOrNull { it.status == GameStatus.BACKLOG || it.status == GameStatus.WISHLIST }
    if (nextUncompleted != null) return nextUncompleted

    // 3. Fallback: Any non-completed/non-dropped game
    return sorted.firstOrNull { it.status != GameStatus.COMPLETED && it.status != GameStatus.DROPPED }
}
