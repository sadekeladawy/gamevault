package com.example.data.repository

import android.util.Log
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.ai.ChatMessage
import com.example.data.model.ai.MessageSender
import com.example.data.remote.ai.GameVaultAiContextBuilder
import com.example.data.remote.ai.GameVaultAiRepository
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.remote.rawg.RawgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale

class AiChatRepository(
    private val rawgRepository: RawgRepository = RawgRepository(),
    private val aiRepository: GameVaultAiRepository = GameVaultAiRepository()
) {
    companion object {
        private const val TAG = "AiChatRepository"
    }

    private fun sanitizeAiTextResponse(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(Regex("[⭐★☆✨🌟]\\s*([0-9]+(\\.[0-9]+)?)\\s*/\\s*5(\\.0)?"), "Rating: $1/5.0")
            .replace(Regex("[⭐★☆✨🌟]\\s*([0-9]+(\\.[0-9]+)?)\\s*/\\s*10(\\.0)?"), "Rating: $1/10.0")
            .replace(Regex("[⭐★☆✨🌟]\\s*([0-9]+(\\.[0-9]+)?)"), "Rating: $1/5.0")
            .replace(Regex("[⭐★☆✨🌟]"), "")
            .replace(Regex("\\*{3,}"), "") // Remove *** decorative dividers
    }

    suspend fun processUserMessage(
        userText: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        userBacklog: List<Game> = emptyList()
    ): ChatMessage = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) {
            return@withContext ChatMessage(
                sender = MessageSender.AI,
                text = "Please enter a question or ask for game recommendations!"
            )
        }

        Log.d(TAG, "Processing user message via Firebase AI Logic: \"$trimmed\"")

        val lower = trimmed.lowercase()
        val isBacklogQuery = lower.contains("backlog") || lower.contains("what should i play") || lower.contains("play tonight") || lower.contains("choose from my games")

        // Step 1: Detect intent & extract RAWG search criteria
        val intent = extractSearchIntent(trimmed)

        // Step 2: Fetch actual game metadata from RAWG API
        var rawgGames: List<RawgGameDto> = emptyList()
        var rawgQueryTag: String? = null

        if (intent.shouldQueryRawg && !isBacklogQuery) {
            Log.d(TAG, "Intent requires RAWG query: query=${intent.query}, genre=${intent.genre}, platform=${intent.platform}, dates=${intent.dates}, ordering=${intent.ordering}")
            val rawgResult = rawgRepository.searchGamesWithFilters(
                query = intent.query,
                genres = intent.genre,
                platforms = intent.platform,
                dates = intent.dates,
                ordering = intent.ordering,
                pageSize = 6
            )
            rawgResult.onSuccess { games ->
                rawgGames = games.take(6)
                rawgQueryTag = intent.query ?: intent.genre ?: "RAWG Search"
                Log.d(TAG, "RAWG returned ${rawgGames.size} games for AI context")
            }.onFailure { err ->
                Log.w(TAG, "RAWG search failed for AI context: ${err.message}")
            }
        }

        // Step 3: Generate response with Firebase AI Logic
        try {
            val promptWithContext = buildPromptWithContext(trimmed, conversationHistory, rawgGames, userBacklog)
            val responseText = aiRepository.generateContent(promptWithContext)
            if (!responseText.isNullOrBlank()) {
                val sanitized = sanitizeAiTextResponse(responseText)
                return@withContext ChatMessage(
                    sender = MessageSender.AI,
                    text = sanitized,
                    recommendedGames = rawgGames,
                    rawgQueryUsed = rawgQueryTag
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic exception: ${e.message}", e)
        }

        // Step 4: Smart Local AI Fallback Engine
        val fallbackText = generateSmartFallbackResponse(trimmed, intent, rawgGames, userBacklog)
        val sanitizedFallback = sanitizeAiTextResponse(fallbackText)
        return@withContext ChatMessage(
            sender = MessageSender.AI,
            text = sanitizedFallback,
            recommendedGames = rawgGames,
            rawgQueryUsed = rawgQueryTag
        )
    }

    fun processUserMessageStream(
        userText: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        userBacklog: List<Game> = emptyList(),
        onRawgMetaDataFetched: (List<RawgGameDto>, String?) -> Unit
    ): Flow<String> = flow {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) {
            emit("Please enter a question or ask for game recommendations!")
            return@flow
        }

        val lower = trimmed.lowercase()
        val isBacklogQuery = lower.contains("backlog") || lower.contains("what should i play") || lower.contains("play tonight") || lower.contains("choose from my games")

        val intent = extractSearchIntent(trimmed)
        var rawgGames: List<RawgGameDto> = emptyList()
        var rawgQueryTag: String? = null

        if (intent.shouldQueryRawg && !isBacklogQuery) {
            val rawgResult = rawgRepository.searchGamesWithFilters(
                query = intent.query,
                genres = intent.genre,
                platforms = intent.platform,
                dates = intent.dates,
                ordering = intent.ordering,
                pageSize = 6
            )
            rawgResult.onSuccess { games ->
                rawgGames = games.take(6)
                rawgQueryTag = intent.query ?: intent.genre ?: "RAWG Search"
                onRawgMetaDataFetched(rawgGames, rawgQueryTag)
            }
        }

        val promptWithContext = buildPromptWithContext(trimmed, conversationHistory, rawgGames, userBacklog)

        var accumulatedText = ""
        try {
            aiRepository.generateContentStream(promptWithContext).collect { chunk ->
                accumulatedText += chunk
                emit(sanitizeAiTextResponse(accumulatedText))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream failed: ${e.message}", e)
            if (accumulatedText.isBlank()) {
                val fallbackText = generateSmartFallbackResponse(trimmed, intent, rawgGames, userBacklog)
                emit(sanitizeAiTextResponse(fallbackText))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPromptWithContext(
        userText: String,
        history: List<ChatMessage>,
        rawgGames: List<RawgGameDto>,
        userBacklog: List<Game>
    ): String {
        val sb = StringBuilder()

        // Include user backlog context if relevant
        val backlogContext = GameVaultAiContextBuilder.buildUserBacklogContext(userBacklog)
        if (backlogContext.isNotBlank()) {
            sb.append(backlogContext)
        }

        // Include RAWG metadata context if relevant
        val rawgContext = GameVaultAiContextBuilder.buildRawgMetadataContext(rawgGames)
        if (rawgContext.isNotBlank()) {
            sb.append(rawgContext)
        }

        // Include recent conversation history (last 4 turns)
        val recentHistory = history.takeLast(4)
        if (recentHistory.isNotEmpty()) {
            sb.append("[CONVERSATION HISTORY]:\n")
            for (msg in recentHistory) {
                val senderLabel = if (msg.sender == MessageSender.USER) "User" else "GameVault AI"
                sb.append("$senderLabel: ${msg.text}\n")
            }
            sb.append("[END CONVERSATION HISTORY]\n\n")
        }

        sb.append("Current User Request: ").append(userText)
        return sb.toString()
    }

    private data class SearchIntent(
        val shouldQueryRawg: Boolean,
        val query: String? = null,
        val genre: String? = null,
        val platform: String? = null,
        val dates: String? = null,
        val ordering: String? = "-rating"
    )

    private fun extractSearchIntent(userText: String): SearchIntent {
        val lower = userText.lowercase()

        // Detect genres
        var genreSlug: String? = null
        if (lower.contains("rpg") || lower.contains("role-playing") || lower.contains("role playing")) genreSlug = "5"
        else if (lower.contains("action")) genreSlug = "4"
        else if (lower.contains("shooter") || lower.contains("fps")) genreSlug = "2"
        else if (lower.contains("adventure")) genreSlug = "3"
        else if (lower.contains("strategy") || lower.contains("rts")) genreSlug = "10"
        else if (lower.contains("indie")) genreSlug = "51"
        else if (lower.contains("horror")) genreSlug = "horror"
        else if (lower.contains("racing")) genreSlug = "1"
        else if (lower.contains("sports")) genreSlug = "15"
        else if (lower.contains("puzzle")) genreSlug = "7"

        // Detect platform
        var platformId: String? = null
        if (lower.contains("pc") || lower.contains("computer") || lower.contains("steam")) platformId = "4"
        else if (lower.contains("playstation") || lower.contains("ps5") || lower.contains("ps4")) platformId = "187,18"
        else if (lower.contains("xbox")) platformId = "186,1"
        else if (lower.contains("switch") || lower.contains("nintendo")) platformId = "7"
        else if (lower.contains("android") || lower.contains("mobile")) platformId = "21"

        // Detect dates / years
        var dateRange: String? = null
        if (lower.contains("after 2020") || lower.contains("since 2020") || lower.contains("2021")) {
            dateRange = "2020-01-01,2026-12-31"
        } else if (lower.contains("after 2015") || lower.contains("2015 to 2025")) {
            dateRange = "2015-01-01,2025-12-31"
        } else if (lower.contains("2023")) {
            dateRange = "2023-01-01,2023-12-31"
        } else if (lower.contains("2024")) {
            dateRange = "2024-01-01,2024-12-31"
        }

        // Clean query terms by removing common intent phrases
        val cleanQuery = userText
            .replace(Regex("(?i)recommend( me)?|find( me)?|top|best|games like|similar to|tell me about|what is|search for|games released"), "")
            .replace(Regex("(?i)rpg|action|shooter|horror|strategy|pc|playstation|xbox|switch|after 2020|since 2020|with high ratings"), "")
            .trim()
            .takeIf { it.length >= 2 }

        val isQuestionAboutGames = lower.contains("recommend") || lower.contains("game") ||
                lower.contains("best") || lower.contains("top") || lower.contains("like") ||
                lower.contains("similar") || lower.contains("played") || lower.contains("compare") ||
                lower.contains("elden ring") || lower.contains("cyberpunk") || lower.contains("witcher") ||
                lower.contains("gtav") || lower.contains("gta") || cleanQuery != null

        val shouldQuery = isQuestionAboutGames || genreSlug != null || platformId != null

        val searchQuery = when {
            lower.contains("elden ring") -> "Elden Ring"
            lower.contains("cyberpunk") -> "Cyberpunk 2077"
            lower.contains("witcher") -> "The Witcher 3"
            lower.contains("gta") || lower.contains("grand theft auto") -> "Grand Theft Auto V"
            lower.contains("red dead") -> "Red Dead Redemption 2"
            lower.contains("god of war") -> "God of War"
            lower.contains("zelda") -> "Zelda"
            lower.contains("baldur") -> "Baldur's Gate 3"
            else -> cleanQuery
        }

        return SearchIntent(
            shouldQueryRawg = shouldQuery,
            query = searchQuery,
            genre = genreSlug,
            platform = platformId,
            dates = dateRange,
            ordering = if (lower.contains("newest") || lower.contains("latest")) "-released" else "-rating"
        )
    }

    private fun generateSmartFallbackResponse(
        userText: String,
        intent: SearchIntent,
        rawgGames: List<RawgGameDto>,
        userBacklog: List<Game>
    ): String {
        val lower = userText.lowercase()

        if (lower.contains("backlog") || lower.contains("what should i play") || lower.contains("play tonight")) {
            if (userBacklog.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append("🎯 **GameVault Backlog Recommendations**\n\n")
                sb.append("Here are top picks from your personal backlog:\n\n")
                val picks = userBacklog.filter { it.status == GameStatus.BACKLOG || it.status == GameStatus.WISHLIST }.take(5)
                    .ifEmpty { userBacklog.take(5) }

                picks.forEachIndexed { idx, game ->
                    val myRatingStr = if (game.rating > 0) " | My Rating: ${game.rating}/10" else ""
                    sb.append("${idx + 1}. **${game.title}** (${game.platform})\n")
                    sb.append("   • **Genre:** ${game.genre}$myRatingStr\n")
                    sb.append("   • **Status:** ${game.status.displayName}\n\n")
                }
                sb.append("💡 *Tip: Open My Vault to update status or track your playtime!*")
                return sb.toString()
            }
        }

        if (rawgGames.isNotEmpty()) {
            val sb = StringBuilder()
            if (lower.contains("compare")) {
                sb.append("Game Comparison Summary\n\n")
                sb.append("Here is how these titles compare based on RAWG database records:\n\n")
            } else if (lower.contains("tell me about") || lower.contains("what is")) {
                sb.append("Game Intelligence Report\n\n")
            } else {
                sb.append("GameVault AI Recommendations\n\n")
                sb.append("Based on your preferences and the official RAWG database, here are top handpicked titles:\n\n")
            }

            rawgGames.forEachIndexed { idx, game ->
                val ratingStr = if (game.rating != null && game.rating > 0) "Rating: ${String.format(Locale.US, "%.1f", game.rating)}/5.0" else "Rating: N/A"
                val metacriticStr = if (game.metacritic != null) " | Metacritic: ${game.metacritic}" else ""
                val releaseYear = game.released?.take(4) ?: "N/A"
                val genres = game.genres?.mapNotNull { it.name }?.joinToString(", ") ?: "Action"
                val platforms = game.platforms?.mapNotNull { it.platform?.name }?.take(3)?.joinToString(", ") ?: "PC/Console"

                sb.append("${idx + 1}. ${game.name} ($releaseYear)\n")
                sb.append("   • Scores: $ratingStr$metacriticStr\n")
                sb.append("   • Genres: $genres\n")
                sb.append("   • Platforms: $platforms\n\n")
            }

            sb.append("Tip: Tap any game card below to view full details, screenshots, or add it to your Vault!")
            return sb.toString()
        }

        // Generic friendly response if no RAWG games found
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I am GameVault AI, powered by Firebase AI Logic. Ask me to recommend games, compare titles, help pick from your backlog, or search RAWG for top games!"
            lower.contains("help") ->
                "How I Can Help You:\n\n" +
                "• Backlog Assistant: Ask 'What should I play tonight?' or 'Pick 5 games from my backlog'.\n" +
                "• Discover Games: Ask for RPGs released after 2020, horror games on PlayStation, or top PC titles.\n" +
                "• Game Info: Ask 'Tell me about Cyberpunk 2077' or 'What is Elden Ring?'.\n" +
                "• Compare Titles: Ask 'Compare Witcher 3 and Skyrim'."
            else ->
                "I searched the database for \"$userText\". Try searching by genre (e.g., RPG, Shooter), platform (PC, PS5), or ask me to recommend top rated games released after 2020!"
        }
    }
}
