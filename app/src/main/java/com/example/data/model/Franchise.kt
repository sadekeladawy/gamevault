package com.example.data.model

data class Franchise(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val rawgId: Long? = null
)

/**
 * Represents a single game belonging to a franchise/series.
 * Crucially, this can represent both games currently owned in the user's GameVault
 * and games in the global franchise that the user does NOT own yet.
 */
data class SeriesGameItem(
    val id: String = "",
    val title: String,
    val coverUrl: String = "",
    val releaseYear: Int = 0,
    val releaseDate: String? = null,
    val platform: String = "Multi-platform",
    val seriesOrder: Int? = null,
    val isInVault: Boolean = false,
    val vaultGame: Game? = null,
    val vaultStatus: GameStatus? = vaultGame?.status,
    val rawgRating: Double = 0.0,
    val metacritic: Int? = null,
    val rawgId: Long? = null,
    val genre: String = ""
) {
    val status: GameStatus? get() = vaultStatus ?: vaultGame?.status
}

data class FranchiseDetails(
    val franchise: Franchise,
    val games: List<Game> = emptyList(),
    val seriesGames: List<SeriesGameItem> = emptyList(),
    val totalGames: Int = if (seriesGames.isNotEmpty()) seriesGames.size else games.size,
    val totalSeriesGames: Int = totalGames,
    val totalOwnedGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.isInVault } else games.size,
    val completedGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.vaultGame?.status == GameStatus.COMPLETED } else games.count { it.status == GameStatus.COMPLETED },
    val currentlyPlayingGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.vaultGame?.status == GameStatus.CURRENTLY_PLAYING } else games.count { it.status == GameStatus.CURRENTLY_PLAYING },
    val backlogGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.vaultGame?.status == GameStatus.BACKLOG } else games.count { it.status == GameStatus.BACKLOG },
    val wishlistGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.vaultGame?.status == GameStatus.WISHLIST } else games.count { it.status == GameStatus.WISHLIST },
    val abandonedGames: Int = if (seriesGames.isNotEmpty()) seriesGames.count { it.vaultGame?.status == GameStatus.DROPPED } else games.count { it.status == GameStatus.DROPPED },
    val completionPercentage: Int = if (totalSeriesGames > 0) ((completedGames.toDouble() / totalSeriesGames) * 100).toInt() else 0,
    val totalPlaytimeHours: Double = if (seriesGames.isNotEmpty()) seriesGames.sumOf { it.vaultGame?.playtimeHours ?: 0.0 } else games.sumOf { it.playtimeHours },
    val averageRating: Double = if (seriesGames.isNotEmpty()) {
        val ratings = seriesGames.mapNotNull { it.vaultGame?.rating?.takeIf { r -> r > 0 }?.toDouble() }
        if (ratings.isNotEmpty()) ratings.average() else 0.0
    } else {
        games.filter { it.rating > 0 }.map { it.rating.toDouble() }.let { if (it.isNotEmpty()) it.average() else 0.0 }
    },
    val playNextGame: SeriesGameItem? = determinePlayNextSeries(seriesGames, games)
)

private fun determinePlayNextSeries(seriesGames: List<SeriesGameItem>, fallbackGames: List<Game>): SeriesGameItem? {
    if (seriesGames.isNotEmpty()) {
        val sorted = seriesGames.sortedWith(
            compareBy<SeriesGameItem> { it.seriesOrder ?: Int.MAX_VALUE }
                .thenBy { it.releaseYear }
        )
        // 1. First priority: Game currently in progress in vault
        val currentlyPlaying = sorted.firstOrNull { it.vaultGame?.status == GameStatus.CURRENTLY_PLAYING }
        if (currentlyPlaying != null) return currentlyPlaying

        // 2. Next uncompleted owned game in series order (Backlog or Wishlist)
        val nextUncompleted = sorted.firstOrNull {
            it.isInVault && (it.vaultGame?.status == GameStatus.BACKLOG || it.vaultGame?.status == GameStatus.WISHLIST)
        }
        if (nextUncompleted != null) return nextUncompleted

        // 3. Fallback: Any first owned game not completed
        val anyOwnedNotCompleted = sorted.firstOrNull { it.isInVault && it.vaultGame?.status != GameStatus.COMPLETED && it.vaultGame?.status != GameStatus.DROPPED }
        if (anyOwnedNotCompleted != null) return anyOwnedNotCompleted

        return sorted.firstOrNull()
    }

    if (fallbackGames.isEmpty()) return null
    val sorted = fallbackGames.sortedWith(
        compareBy<Game> { it.seriesOrder ?: Int.MAX_VALUE }
            .thenBy { it.releaseYear }
            .thenBy { it.id }
    )
    val candidate = sorted.firstOrNull { it.status == GameStatus.CURRENTLY_PLAYING }
        ?: sorted.firstOrNull { it.status == GameStatus.BACKLOG || it.status == GameStatus.WISHLIST }
        ?: sorted.firstOrNull { it.status != GameStatus.COMPLETED && it.status != GameStatus.DROPPED }
        ?: sorted.firstOrNull()

    return candidate?.let { g ->
        SeriesGameItem(
            id = g.id.toString(),
            title = g.title,
            coverUrl = g.coverUrl,
            releaseYear = g.releaseYear,
            platform = g.platform,
            seriesOrder = g.seriesOrder,
            isInVault = true,
            vaultGame = g,
            genre = g.genre
        )
    }
}
