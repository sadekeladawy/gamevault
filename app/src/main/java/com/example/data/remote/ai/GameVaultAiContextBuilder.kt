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
