package com.example.data.remote.rawg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Intelligent search relevance ranker for game queries.
 * Solves the critical issue where searching "GTA 5" ranks "Persona 5" above "Grand Theft Auto V".
 *
 * Employs:
 * 1. Alias & gaming abbreviation expansion (GTA -> Grand Theft Auto, RDR -> Red Dead Redemption, COD -> Call of Duty).
 * 2. Roman numeral vs Hindu-Arabic numeral equivalence (V <-> 5, IV <-> 4, II <-> 2, etc.).
 * 3. Exact title, exact acronym, and token sequence scoring (+100,000 pts).
 * 4. Token match ratio with harsh penalties for unrelated games sharing only single generic characters like "5".
 * 5. Popularity/Metacritic as a strictly secondary tie-breaker (max 50 pts) so popularity never overrides relevance.
 */
object GameSearchRanker {

    private val KNOWN_ALIASES = mapOf(
        "gta" to "grand theft auto",
        "rdr" to "red dead redemption",
        "cod" to "call of duty",
        "re" to "resident evil",
        "gow" to "god of war",
        "ac" to "assassins creed",
        "bg" to "baldurs gate",
        "cp2077" to "cyberpunk 2077",
        "botw" to "breath of the wild",
        "totk" to "tears of the kingdom",
        "ff" to "final fantasy",
        "tes" to "the elder scrolls",
        "ds" to "dark souls",
        "me" to "mass effect",
        "mgs" to "metal gear solid",
        "hl" to "half life",
        "cs" to "counter strike",
        "wow" to "world of warcraft",
        "lotr" to "lord of the rings"
    )

    private val ROMAN_TO_DECIMAL = mapOf(
        "i" to "1",
        "ii" to "2",
        "iii" to "3",
        "iv" to "4",
        "v" to "5",
        "vi" to "6",
        "vii" to "7",
        "viii" to "8",
        "ix" to "9",
        "x" to "10"
    )

    /**
     * Expands query abbreviations (e.g. "GTA 5" -> "grand theft auto 5")
     */
    fun expandQueryAliases(query: String): String {
        val tokens = query.trim().split(Regex("\\s+"))
        val expandedTokens = tokens.map { token ->
            val clean = token.lowercase()
            KNOWN_ALIASES[clean] ?: token
        }
        return expandedTokens.joinToString(" ")
    }

    /**
     * Ranks a list of RAWG games against the user's search query on [Dispatchers.Default].
     */
    suspend fun rank(query: String, games: List<RawgGameDto>): List<RawgGameDto> = withContext(Dispatchers.Default) {
        if (query.isBlank() || games.isEmpty()) return@withContext games

        val normalizedQuery = normalizeForSearch(query)
        val expandedQuery = normalizeForSearch(expandQueryAliases(query))
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        val expandedTokens = expandedQuery.split(" ").filter { it.isNotBlank() }

        // Deduplicate games by id first
        val distinctGames = games.distinctBy { it.id }

        distinctGames
            .map { game ->
                val score = calculateScore(game, normalizedQuery, expandedQuery, queryTokens, expandedTokens)
                game to score
            }
            .sortedWith(
                compareByDescending<Pair<RawgGameDto, Double>> { it.second }
                    .thenByDescending { it.first.ratingsCount ?: 0 }
            )
            .map { it.first }
    }

    private fun calculateScore(
        game: RawgGameDto,
        normQuery: String,
        expandedQuery: String,
        queryTokens: List<String>,
        expandedTokens: List<String>
    ): Double {
        val normTitle = normalizeForSearch(game.name)
        val normTitleNoThe = normTitle.removePrefix("the ").trim()
        val titleTokens = normTitle.split(" ").filter { it.isNotBlank() }
        val acronym = generateAcronym(game.name)

        var score = 0.0

        // 1. EXACT TITLE MATCH (Top priority)
        if (normTitle == normQuery || normTitleNoThe == normQuery) {
            score += 150000.0
        } else if (normTitle == expandedQuery || normTitleNoThe == expandedQuery) {
            score += 130000.0
        }

        // 2. EXACT ACRONYM MATCH (e.g. "gta 5" matches "Grand Theft Auto V" acronym "gta 5")
        if (acronym.isNotBlank()) {
            if (acronym == normQuery) {
                score += 120000.0
            } else if (acronym.replace(" ", "") == normQuery.replace(" ", "")) {
                score += 115000.0
            }
        }

        // 3. TITLE STARTS WITH QUERY
        if (normTitle.startsWith(normQuery) || normTitleNoThe.startsWith(normQuery)) {
            score += 60000.0
        } else if (normTitle.startsWith(expandedQuery) || normTitleNoThe.startsWith(expandedQuery)) {
            score += 55000.0
        }

        // 4. TITLE CONTAINS FULL QUERY AS SUBSTRING
        if (normTitle.contains(normQuery)) {
            score += 40000.0
        } else if (normTitle.contains(expandedQuery)) {
            score += 35000.0
        }

        // 5. TOKEN MATCHING & PENALTIES
        val activeTargetTokens = if (expandedTokens.size > queryTokens.size) expandedTokens else queryTokens
        var matchedTokensCount = 0
        var genericNumberOnlyMatch = false

        activeTargetTokens.forEach { qToken ->
            val matchFound = titleTokens.any { tToken ->
                tToken == qToken ||
                (ROMAN_TO_DECIMAL[tToken] != null && ROMAN_TO_DECIMAL[tToken] == qToken) ||
                (ROMAN_TO_DECIMAL[qToken] != null && ROMAN_TO_DECIMAL[qToken] == tToken)
            }

            if (matchFound) {
                matchedTokensCount++
                // Detect if the only token matching is a single digit (like "5")
                if (qToken.all { it.isDigit() } && qToken.length <= 2) {
                    genericNumberOnlyMatch = true
                }
            }
        }

        // Token match percentage bonus
        val matchRatio = if (activeTargetTokens.isNotEmpty()) matchedTokensCount.toDouble() / activeTargetTokens.size else 0.0
        score += matchRatio * 25000.0

        // If all tokens matched
        if (matchedTokensCount == activeTargetTokens.size && activeTargetTokens.isNotEmpty()) {
            score += 20000.0
        }

        // CRITICAL PENALTY:
        // If query has multiple tokens (e.g. "gta 5"), but the game ONLY matched the generic number "5"
        // (e.g. Persona 5, Tekken 5, Far Cry 5), heavily penalize!
        if (activeTargetTokens.size > 1 && matchedTokensCount == 1 && genericNumberOnlyMatch) {
            score -= 50000.0
        }

        // Penalty for missing tokens in multi-token queries
        val missingCount = activeTargetTokens.size - matchedTokensCount
        if (missingCount > 0) {
            score -= (missingCount * 8000.0)
        }

        // 6. SECONDARY TIE-BREAKERS (Rating & Popularity)
        // Kept intentionally small (max 50 pts) so popularity NEVER overrides a relevant title match
        val rating = game.rating ?: 0.0
        score += (rating * 6.0) // 0 - 30 pts

        val metacritic = game.metacritic ?: 0
        score += (metacritic * 0.1) // 0 - 10 pts

        val ratingsCount = (game.ratingsCount ?: 0).coerceAtMost(10000)
        score += (ratingsCount * 0.001) // 0 - 10 pts

        return score
    }

    /**
     * Generates standard gamer acronym for game title (e.g. "Grand Theft Auto V" -> "gta 5")
     */
    private fun generateAcronym(title: String): String {
        val normalized = normalizeForSearch(title)
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        if (tokens.size < 2) return ""

        val sb = StringBuilder()
        for (token in tokens) {
            if (token == "the" || token == "of" || token == "and" || token == "in") continue
            val decimal = ROMAN_TO_DECIMAL[token]
            if (decimal != null) {
                sb.append(" ").append(decimal)
            } else if (token.all { it.isDigit() }) {
                sb.append(" ").append(token)
            } else if (token.isNotEmpty()) {
                sb.append(token[0])
            }
        }
        return sb.toString().trim()
    }

    private fun normalizeForSearch(input: String): String {
        var clean = input.lowercase(Locale.ROOT)
            .replace("&", "and")
            .replace(":", " ")
            .replace("-", " ")
            .replace("'", "")
            .replace("’", "")
            .replace("\"", "")
            .replace(".", " ")
            .replace(",", " ")
            .replace("!", "")
            .replace("?", "")
            .replace("/", " ")
            .trim()

        // Replace Roman numerals surrounded by boundaries with decimals
        ROMAN_TO_DECIMAL.forEach { (roman, dec) ->
            clean = clean.replace(Regex("\\b$roman\\b"), dec)
        }

        return clean.replace(Regex("\\s+"), " ")
    }
}
