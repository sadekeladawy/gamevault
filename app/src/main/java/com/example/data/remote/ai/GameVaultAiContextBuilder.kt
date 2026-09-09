package com.example.data.remote.ai

import com.example.data.model.Game
import com.example.data.remote.rawg.RawgGameDto
import java.util.Locale

object GameVaultAiContextBuilder {

    const val SYSTEM_INSTRUCTION = """
You are GameVault AI, a knowledgeable, unbiased, and helpful gaming assistant inside the GameVault app.
Your job is to help gamers discover games, decide what to play next, analyze their backlog, compare titles, and answer gaming questions.
Always rely on the RAWG Game Database and user Vault information provided in the context for accurate metadata.
Do not invent fake release dates or ratings.

STRICT FORMATTING RULES:
1. NEVER use star emojis (⭐, ★, ☆, etc.) anywhere for ratings or decoration.
2. ALWAYS format game ratings as "Rating: X.X/5.0" for RAWG ratings, or "My Rating: X/10" for personal ratings.
3. If Metacritic score is available, format it as "Metacritic: XX".
4. Keep responses clean, concise, professional, well-structured, and easy to scan using bullet points and bold headers.
"""

    fun buildUserBacklogContext(userBacklog: List<Game>): String {
        if (userBacklog.isEmpty()) return ""
        val backlogList = userBacklog.take(15).joinToString("\n") { g ->
            "- ${g.title} (${g.platform}, ${g.genre}, status: ${g.status.displayName}${if (g.rating > 0) ", my rating: ${g.rating}/10" else ""})"
        }
        return "\n\n[USER'S CURRENT VAULT / BACKLOG GAMES]:\n$backlogList\n[END USER BACKLOG]\n"
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
}
