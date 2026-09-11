package com.example.data.remote.ai

import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.ai.ChatMessage
import com.example.data.model.ai.MessageSender
import com.example.data.remote.rawg.RawgGameDto
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import java.util.Locale

object GameVaultAiContextBuilder {

    const val SYSTEM_INSTRUCTION = """
You are GameVault AI Copilot, an expert, personalized gaming companion inside the GameVault app.
Your mission is to help the player decide what to play next, manage their gaming backlog, analyze their gaming habits, and guide them through game franchises/series.

CRITICAL COPILOT RULES:
1. When the user asks for recommendations from their GameVault, backlog, or franchises, USE ONLY the games provided in [USER'S GAMEVAULT STATE]. Do not hallucinate games they do not own.
2. Respect playtime, series order, and game status when making gaming decisions.
3. Be concise, direct, helpful, and gamer-friendly.
4. STRICT FORMATTING:
   - NEVER use star emojis (⭐, ★, ☆, etc.).
   - Format ratings as "Rating: X.X/5.0" or "My Rating: X/10".
   - Format Metacritic as "Metacritic: XX".
   - Use bullet points and bold titles.
"""

    fun buildUserBacklogContext(userGames: List<Game>): String {
        if (userGames.isEmpty()) return "\n\n[USER'S GAMEVAULT STATE]: Vault is currently empty.\n"

        val sb = StringBuilder()
        sb.append("\n\n[USER'S GAMEVAULT STATE]:\n")
        sb.append("Total Games in Vault: ${userGames.size}\n")
        val completedCount = userGames.count { it.status == GameStatus.COMPLETED }
        val playingCount = userGames.count { it.status == GameStatus.CURRENTLY_PLAYING }
        val backlogCount = userGames.count { it.status == GameStatus.BACKLOG }
        val wishlistCount = userGames.count { it.status == GameStatus.WISHLIST }
        val totalPlaytime = userGames.sumOf { it.playtimeHours }

        sb.append("Stats Summary: Completed: $completedCount | Playing: $playingCount | Backlog: $backlogCount | Wishlist: $wishlistCount | Total Playtime: ${String.format(Locale.US, "%.1f", totalPlaytime)} hrs\n\n")

        // Currently Playing Games
        val currentlyPlaying = userGames.filter { it.status == GameStatus.CURRENTLY_PLAYING }
        if (currentlyPlaying.isNotEmpty()) {
            sb.append("--- CURRENTLY PLAYING ---\n")
            currentlyPlaying.forEach { g ->
                val fStr = if (!g.franchiseName.isNullOrBlank()) " [Franchise: ${g.franchiseName} #${g.seriesOrder ?: ""}]" else ""
                sb.append("• ${g.title} (${g.platform}, ${g.genre}) - Playtime: ${g.playtimeHours}h$fStr\n")
            }
            sb.append("\n")
        }

        // Franchises Summary
        val franchises = userGames.mapNotNull { it.franchiseName?.takeIf { f -> f.isNotBlank() } }.distinct()
        if (franchises.isNotEmpty()) {
            sb.append("--- FRANCHISES / SERIES IN VAULT ---\n")
            franchises.forEach { fName ->
                val fGames = userGames.filter { it.franchiseName.equals(fName, ignoreCase = true) }
                val fCompleted = fGames.count { it.status == GameStatus.COMPLETED }
                sb.append("• $fName: $fCompleted / ${fGames.size} completed\n")
                fGames.sortedBy { it.seriesOrder ?: Int.MAX_VALUE }.forEach { g ->
                    sb.append("  - [Order #${g.seriesOrder ?: "?"}] ${g.title} (${g.status.displayName})\n")
                }
            }
            sb.append("\n")
        }

        // All Library Games List
        sb.append("--- FULL GAMEVAULT LIST ---\n")
        userGames.take(30).forEach { g ->
            val ratingStr = if (g.rating > 0) " | My Rating: ${g.rating}/10" else ""
            val fStr = if (!g.franchiseName.isNullOrBlank()) " | Franchise: ${g.franchiseName}" else ""
            sb.append("• ${g.title} | Platform: ${g.platform} | Genre: ${g.genre} | Status: ${g.status.displayName} | Playtime: ${g.playtimeHours}h$ratingStr$fStr\n")
        }

        sb.append("[END USER GAMEVAULT STATE]\n")
        return sb.toString()
    }

    fun buildRawgMetadataContext(rawgGames: List<RawgGameDto>): String {
        if (rawgGames.isEmpty()) return ""
        val gamesInfo = rawgGames.joinToString("\n---\n") { g ->
            val genres = g.genres?.joinToString(", ") { it.name.orEmpty() } ?: "N/A"
            val platforms = g.platforms?.joinToString(", ") { it.platform?.name.orEmpty() } ?: "N/A"
            val devs = g.developers?.joinToString(", ") { it.name.orEmpty() } ?: ""
            "Title: ${g.name}\n" +
            "Released: ${g.released ?: "Unknown"}\n" +
            "Rating: Rating ${String.format(Locale.US, "%.1f", g.rating ?: 0.0)}/5.0 (${g.ratingsCount ?: 0} votes)\n" +
            "Metacritic: ${g.metacritic ?: "N/A"}\n" +
            "Genres: $genres\n" +
            "Platforms: $platforms\n" +
            (if (devs.isNotBlank()) "Developers: $devs\n" else "") +
            "Playtime: ${g.playtime ?: 0} hours"
        }
        return "\n\n[RAWG DATABASE METADATA RETRIEVED FOR USER QUERY]:\n$gamesInfo\n[END RAWG METADATA]\n"
    }

    /**
     * Builds focused context when the user taps "Ask AI" from a game details screen.
     */
    fun buildGameFocusedContext(game: Game): String {
        val fStr = if (!game.franchiseName.isNullOrBlank()) " | Franchise: ${game.franchiseName} (Order #${game.seriesOrder ?: "?"})" else ""
        val ratingStr = if (game.rating > 0) " | User's Rating: ${game.rating}/10" else " | User's Rating: Not rated yet"
        val favStr = if (game.isFavorite) " | Marked as Favorite: Yes" else ""
        val notesStr = if (game.notes.isNotBlank()) "\nUser's Personal Notes: \"${game.notes.replace(Regex("\\[RAWG_ID:\\d+\\]"), "").trim()}\"" else ""

        return """
[ACTIVE SCREEN CONTEXT: USER IS CURRENTLY VIEWING THIS GAME]
Game Title: ${game.title}
Platform: ${game.platform}
Genre: ${game.genre}
Release Year: ${game.releaseYear}
GameVault Status: ${game.status.displayName}
Playtime Logged: ${game.playtimeHours} hours$ratingStr$favStr$fStr$notesStr
The user is asking questions specifically regarding "${game.title}". Keep answers directly relevant to this game, comparing with similar games if requested.
[END ACTIVE SCREEN CONTEXT]
""".trimIndent()
    }

    /**
     * Builds focused context when the user taps "Ask AI" from a RAWG search result or discovery card.
     */
    fun buildRawgGameFocusedContext(dto: RawgGameDto): String {
        val genres = dto.genres?.joinToString(", ") { it.name.orEmpty() } ?: "N/A"
        val platforms = dto.platforms?.joinToString(", ") { it.platform?.name.orEmpty() } ?: "N/A"
        val devs = dto.developers?.joinToString(", ") { it.name.orEmpty() } ?: ""
        val devLine = if (devs.isNotBlank()) "\nDeveloper: $devs" else ""

        return """
[ACTIVE SCREEN CONTEXT: USER IS CURRENTLY VIEWING THIS GAME (FROM DATABASE)]
Game Title: ${dto.name}
Release Date: ${dto.released ?: "Unknown"}
Community Rating: ${String.format(Locale.US, "%.1f", dto.rating ?: 0.0)}/5.0 (${dto.ratingsCount ?: 0} ratings)
Metacritic Score: ${dto.metacritic ?: "N/A"}
Genres: $genres
Platforms: $platforms$devLine
The user is considering this game and wants your copilot guidance on whether it's worth playing, how long it takes, and how it compares to other games.
[END ACTIVE SCREEN CONTEXT]
""".trimIndent()
    }

    /**
     * Builds focused context when the user taps "Ask AI" from a franchise/series screen.
     */
    fun buildFranchiseFocusedContext(franchiseName: String, seriesGames: List<com.example.data.model.SeriesGameItem>): String {
        val sb = StringBuilder()
        sb.append("[ACTIVE SCREEN CONTEXT: USER IS CURRENTLY VIEWING FRANCHISE: $franchiseName]\n")
        sb.append("Total Games in Series: ${seriesGames.size}\n")
        val ownedCount = seriesGames.count { it.isInVault }
        val completedCount = seriesGames.count { it.vaultGame?.status == GameStatus.COMPLETED }
        sb.append("User Vault Progress: $ownedCount / ${seriesGames.size} owned, $completedCount completed\n\n")
        sb.append("Full Series Chronology / Play Order:\n")
        seriesGames.forEach { g ->
            val orderStr = if (g.seriesOrder != null && g.seriesOrder > 0) "#${g.seriesOrder}" else "-"
            val vaultStatusStr = if (g.isInVault && g.vaultGame != null) " [IN VAULT: ${g.vaultGame.status.displayName}]" else " [NOT IN VAULT]"
            sb.append("• $orderStr ${g.title} (${g.releaseYear}, ${g.platform})$vaultStatusStr\n")
        }
        sb.append("\nThe user wants advice about this franchise—play order, which games to skip/prioritize, lore, or vault completion. Keep your advice focused on the $franchiseName franchise.\n")
        sb.append("[END ACTIVE SCREEN CONTEXT]\n")
        return sb.toString()
    }

    /**
     * Converts a list of [ChatMessage] into Firebase AI Logic [Content] objects for multi-turn chat sessions.
     */
    fun buildChatHistory(messages: List<ChatMessage>): List<Content> {
        return messages
            .filter { !it.isLoading && !it.isError && it.text.isNotBlank() }
            .map { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                content(role) {
                    text(msg.text)
                }
            }
    }
}
