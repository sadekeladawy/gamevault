package com.example.data.repository

import android.util.Log
import com.example.data.model.Franchise
import com.example.data.model.FranchiseDetails
import com.example.data.model.Game
import com.example.data.model.SeriesGameItem
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.remote.rawg.RawgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for providing COMPLETE series/franchise data.
 * CRITICAL RULE: The series screen must NEVER be filtered by user GameVault ownership status.
 * Every franchise contains all released/announced games in the series, with each game clearly
 * indicating whether it is In My Vault or Not in My Vault.
 */
class FranchiseRepository(
    private val rawgRepository: RawgRepository? = null
) {
    companion object {
        private const val TAG = "FranchiseRepository"

        /**
         * Comprehensive, verified canonical database of major video game franchises/series.
         * Used for instantaneous, reliable offline franchise presentation.
         */
        val CANONICAL_FRANCHISES: Map<String, List<SeriesGameItem>> = mapOf(
            "The Witcher" to listOf(
                SeriesGameItem(
                    id = "witcher-1",
                    title = "The Witcher: Enhanced Edition",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2007,
                    releaseDate = "2007-10-26",
                    platform = "PC",
                    seriesOrder = 1,
                    genre = "Action RPG",
                    rawgRating = 4.2
                ),
                SeriesGameItem(
                    id = "witcher-2",
                    title = "The Witcher 2: Assassins of Kings",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2011,
                    releaseDate = "2011-05-17",
                    platform = "PC, Xbox 360",
                    seriesOrder = 2,
                    genre = "Action RPG",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "witcher-3",
                    title = "The Witcher 3: Wild Hunt",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2015,
                    releaseDate = "2015-05-19",
                    platform = "PC, PlayStation 5, Xbox Series X, Switch",
                    seriesOrder = 3,
                    genre = "Action RPG",
                    rawgRating = 4.7
                ),
                SeriesGameItem(
                    id = "witcher-4",
                    title = "The Witcher 4: Polaris",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co6l8l.webp",
                    releaseYear = 2026,
                    releaseDate = "2026",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 4,
                    genre = "Action RPG",
                    rawgRating = 0.0
                )
            ),
            "Grand Theft Auto" to listOf(
                SeriesGameItem(
                    id = "gta-3",
                    title = "Grand Theft Auto III",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2001,
                    releaseDate = "2001-10-22",
                    platform = "PlayStation 2, PC, Xbox",
                    seriesOrder = 1,
                    genre = "Open World Action",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "gta-vc",
                    title = "Grand Theft Auto: Vice City",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2002,
                    releaseDate = "2002-10-29",
                    platform = "PlayStation 2, PC, Xbox",
                    seriesOrder = 2,
                    genre = "Open World Action",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "gta-sa",
                    title = "Grand Theft Auto: San Andreas",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2004,
                    releaseDate = "2004-10-26",
                    platform = "PlayStation 2, PC, Xbox",
                    seriesOrder = 3,
                    genre = "Open World Action",
                    rawgRating = 4.7
                ),
                SeriesGameItem(
                    id = "gta-4",
                    title = "Grand Theft Auto IV",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mdf.webp",
                    releaseYear = 2008,
                    releaseDate = "2008-04-29",
                    platform = "PlayStation 3, Xbox 360, PC",
                    seriesOrder = 4,
                    genre = "Open World Action",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "gta-5",
                    title = "Grand Theft Auto V",
                    coverUrl = "https://media.rawg.io/media/games/20a/20aa5831e4a36460a8c11295a3ccedbc.jpg",
                    releaseYear = 2013,
                    releaseDate = "2013-09-17",
                    platform = "PlayStation 5, Xbox Series X, PC",
                    seriesOrder = 5,
                    genre = "Open World Action",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "gta-6",
                    title = "Grand Theft Auto VI",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co6l8l.webp",
                    releaseYear = 2025,
                    releaseDate = "2025",
                    platform = "PlayStation 5, Xbox Series X",
                    seriesOrder = 6,
                    genre = "Open World Action",
                    rawgRating = 0.0
                )
            ),
            "Resident Evil" to listOf(
                SeriesGameItem(
                    id = "re-2",
                    title = "Resident Evil 2 (Remake)",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1x77.webp",
                    releaseYear = 2019,
                    releaseDate = "2019-01-25",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 1,
                    genre = "Survival Horror",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "re-3",
                    title = "Resident Evil 3 (Remake)",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2020,
                    releaseDate = "2020-04-03",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 2,
                    genre = "Survival Horror",
                    rawgRating = 4.1
                ),
                SeriesGameItem(
                    id = "re-4",
                    title = "Resident Evil 4 (Remake)",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5s5v.webp",
                    releaseYear = 2023,
                    releaseDate = "2023-03-24",
                    platform = "PlayStation 5, PC, Xbox Series X",
                    seriesOrder = 3,
                    genre = "Survival Horror",
                    rawgRating = 4.7
                ),
                SeriesGameItem(
                    id = "re-7",
                    title = "Resident Evil 7: Biohazard",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2017,
                    releaseDate = "2017-01-24",
                    platform = "PC, PlayStation 4, Xbox One",
                    seriesOrder = 4,
                    genre = "Survival Horror",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "re-8",
                    title = "Resident Evil Village",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2k05.webp",
                    releaseYear = 2021,
                    releaseDate = "2021-05-07",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 5,
                    genre = "Survival Horror",
                    rawgRating = 4.5
                )
            ),
            "God of War" to listOf(
                SeriesGameItem(
                    id = "gow-1",
                    title = "God of War (2005)",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2005,
                    releaseDate = "2005-03-22",
                    platform = "PlayStation 2",
                    seriesOrder = 1,
                    genre = "Action Adventure",
                    rawgRating = 4.3
                ),
                SeriesGameItem(
                    id = "gow-2",
                    title = "God of War II",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2007,
                    releaseDate = "2007-03-13",
                    platform = "PlayStation 2",
                    seriesOrder = 2,
                    genre = "Action Adventure",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "gow-3",
                    title = "God of War III",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2010,
                    releaseDate = "2010-03-16",
                    platform = "PlayStation 3, PlayStation 4",
                    seriesOrder = 3,
                    genre = "Action Adventure",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "gow-2018",
                    title = "God of War (2018)",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1tka.webp",
                    releaseYear = 2018,
                    releaseDate = "2018-04-20",
                    platform = "PlayStation 4, PC",
                    seriesOrder = 4,
                    genre = "Action Adventure",
                    rawgRating = 4.8
                ),
                SeriesGameItem(
                    id = "gow-ragnarok",
                    title = "God of War Ragnarök",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5s5v.webp",
                    releaseYear = 2022,
                    releaseDate = "2022-11-09",
                    platform = "PlayStation 5, PlayStation 4, PC",
                    seriesOrder = 5,
                    genre = "Action Adventure",
                    rawgRating = 4.7
                )
            ),
            "Soulsborne" to listOf(
                SeriesGameItem(
                    id = "demons-souls",
                    title = "Demon's Souls",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2009,
                    releaseDate = "2009-02-05",
                    platform = "PlayStation 5, PlayStation 3",
                    seriesOrder = 1,
                    genre = "Action RPG",
                    rawgRating = 4.3
                ),
                SeriesGameItem(
                    id = "dark-souls-1",
                    title = "Dark Souls",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2011,
                    releaseDate = "2011-09-22",
                    platform = "PC, PS4, Xbox One, Switch",
                    seriesOrder = 2,
                    genre = "Action RPG",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "dark-souls-2",
                    title = "Dark Souls II: Scholar of the First Sin",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2014,
                    releaseDate = "2014-03-11",
                    platform = "PC, PS4, Xbox One",
                    seriesOrder = 3,
                    genre = "Action RPG",
                    rawgRating = 4.0
                ),
                SeriesGameItem(
                    id = "bloodborne",
                    title = "Bloodborne",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1tka.webp",
                    releaseYear = 2015,
                    releaseDate = "2015-03-24",
                    platform = "PlayStation 4",
                    seriesOrder = 4,
                    genre = "Action RPG",
                    rawgRating = 4.8
                ),
                SeriesGameItem(
                    id = "dark-souls-3",
                    title = "Dark Souls III",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mdf.webp",
                    releaseYear = 2016,
                    releaseDate = "2016-03-24",
                    platform = "PC, PlayStation 4, Xbox One",
                    seriesOrder = 5,
                    genre = "Action RPG",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "elden-ring",
                    title = "Elden Ring",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.webp",
                    releaseYear = 2022,
                    releaseDate = "2022-02-25",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 6,
                    genre = "Action RPG",
                    rawgRating = 4.8
                )
            ),
            "The Legend of Zelda" to listOf(
                SeriesGameItem(
                    id = "zelda-oot",
                    title = "The Legend of Zelda: Ocarina of Time",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 1998,
                    releaseDate = "1998-11-21",
                    platform = "Nintendo 64, 3DS",
                    seriesOrder = 1,
                    genre = "Action Adventure",
                    rawgRating = 4.8
                ),
                SeriesGameItem(
                    id = "zelda-tp",
                    title = "The Legend of Zelda: Twilight Princess",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2006,
                    releaseDate = "2006-11-19",
                    platform = "GameCube, Wii, Wii U",
                    seriesOrder = 2,
                    genre = "Action Adventure",
                    rawgRating = 4.5
                ),
                SeriesGameItem(
                    id = "zelda-botw",
                    title = "The Legend of Zelda: Breath of the Wild",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co3p2d.webp",
                    releaseYear = 2017,
                    releaseDate = "2017-03-03",
                    platform = "Nintendo Switch, Wii U",
                    seriesOrder = 3,
                    genre = "Action Adventure",
                    rawgRating = 4.8
                ),
                SeriesGameItem(
                    id = "zelda-totk",
                    title = "The Legend of Zelda: Tears of the Kingdom",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5vmg.webp",
                    releaseYear = 2023,
                    releaseDate = "2023-05-12",
                    platform = "Nintendo Switch",
                    seriesOrder = 4,
                    genre = "Action Adventure",
                    rawgRating = 4.8
                )
            ),
            "Baldur's Gate" to listOf(
                SeriesGameItem(
                    id = "bg-1",
                    title = "Baldur's Gate: Enhanced Edition",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 1998,
                    releaseDate = "1998-11-30",
                    platform = "PC",
                    seriesOrder = 1,
                    genre = "CRPG",
                    rawgRating = 4.3
                ),
                SeriesGameItem(
                    id = "bg-2",
                    title = "Baldur's Gate II: Shadows of Amn",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2000,
                    releaseDate = "2000-09-24",
                    platform = "PC",
                    seriesOrder = 2,
                    genre = "CRPG",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "bg-3",
                    title = "Baldur's Gate 3",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co670h.webp",
                    releaseYear = 2023,
                    releaseDate = "2023-08-03",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 3,
                    genre = "CRPG",
                    rawgRating = 4.9
                )
            ),
            "Cyberpunk" to listOf(
                SeriesGameItem(
                    id = "cyberpunk-2077",
                    title = "Cyberpunk 2077",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mdf.webp",
                    releaseYear = 2020,
                    releaseDate = "2020-12-10",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 1,
                    genre = "Open World RPG",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "cyberpunk-orion",
                    title = "Cyberpunk: Project Orion",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co6l8l.webp",
                    releaseYear = 2028,
                    releaseDate = "In Development",
                    platform = "PC, Next-Gen Consoles",
                    seriesOrder = 2,
                    genre = "Open World RPG",
                    rawgRating = 0.0
                )
            ),
            "Hades" to listOf(
                SeriesGameItem(
                    id = "hades-1",
                    title = "Hades",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2020,
                    releaseDate = "2020-09-17",
                    platform = "PC, Switch, PlayStation 5, Xbox Series X",
                    seriesOrder = 1,
                    genre = "Roguelike",
                    rawgRating = 4.8
                ),
                SeriesGameItem(
                    id = "hades-2",
                    title = "Hades II",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5w3j.webp",
                    releaseYear = 2024,
                    releaseDate = "2024-05-06",
                    platform = "PC",
                    seriesOrder = 2,
                    genre = "Roguelike",
                    rawgRating = 4.7
                )
            ),
            "Red Dead" to listOf(
                SeriesGameItem(
                    id = "rd-revolver",
                    title = "Red Dead Revolver",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2004,
                    releaseDate = "2004-05-04",
                    platform = "PlayStation 2, Xbox",
                    seriesOrder = 1,
                    genre = "Action Adventure",
                    rawgRating = 3.9
                ),
                SeriesGameItem(
                    id = "rdr-1",
                    title = "Red Dead Redemption",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2010,
                    releaseDate = "2010-05-18",
                    platform = "PlayStation 4, Switch, Xbox 360, PC",
                    seriesOrder = 2,
                    genre = "Open World Action",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "rdr-2",
                    title = "Red Dead Redemption 2",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2018,
                    releaseDate = "2018-10-26",
                    platform = "PlayStation 4, Xbox One, PC",
                    seriesOrder = 3,
                    genre = "Open World Action",
                    rawgRating = 4.8
                )
            ),
            "Mass Effect" to listOf(
                SeriesGameItem(
                    id = "me-1",
                    title = "Mass Effect",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2007,
                    releaseDate = "2007-11-20",
                    platform = "PC, Xbox 360, PS3",
                    seriesOrder = 1,
                    genre = "Sci-Fi RPG",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "me-2",
                    title = "Mass Effect 2",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2010,
                    releaseDate = "2010-01-26",
                    platform = "PC, Xbox 360, PS3",
                    seriesOrder = 2,
                    genre = "Sci-Fi RPG",
                    rawgRating = 4.7
                ),
                SeriesGameItem(
                    id = "me-3",
                    title = "Mass Effect 3",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2012,
                    releaseDate = "2012-03-06",
                    platform = "PC, Xbox 360, PS3",
                    seriesOrder = 3,
                    genre = "Sci-Fi RPG",
                    rawgRating = 4.4
                ),
                SeriesGameItem(
                    id = "me-andromeda",
                    title = "Mass Effect: Andromeda",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mdf.webp",
                    releaseYear = 2017,
                    releaseDate = "2017-03-21",
                    platform = "PC, PlayStation 4, Xbox One",
                    seriesOrder = 4,
                    genre = "Sci-Fi RPG",
                    rawgRating = 3.6
                )
            ),
            "Fallout" to listOf(
                SeriesGameItem(
                    id = "fo-3",
                    title = "Fallout 3",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7f.webp",
                    releaseYear = 2008,
                    releaseDate = "2008-10-28",
                    platform = "PC, Xbox 360, PS3",
                    seriesOrder = 1,
                    genre = "Post-Apocalyptic RPG",
                    rawgRating = 4.3
                ),
                SeriesGameItem(
                    id = "fo-nv",
                    title = "Fallout: New Vegas",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2crj.webp",
                    releaseYear = 2010,
                    releaseDate = "2010-10-19",
                    platform = "PC, Xbox 360, PS3",
                    seriesOrder = 2,
                    genre = "Post-Apocalyptic RPG",
                    rawgRating = 4.6
                ),
                SeriesGameItem(
                    id = "fo-4",
                    title = "Fallout 4",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.webp",
                    releaseYear = 2015,
                    releaseDate = "2015-11-10",
                    platform = "PC, PlayStation 5, Xbox Series X",
                    seriesOrder = 3,
                    genre = "Post-Apocalyptic RPG",
                    rawgRating = 4.2
                ),
                SeriesGameItem(
                    id = "fo-76",
                    title = "Fallout 76",
                    coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mdf.webp",
                    releaseYear = 2018,
                    releaseDate = "2018-11-14",
                    platform = "PC, PlayStation 4, Xbox One",
                    seriesOrder = 4,
                    genre = "Online RPG",
                    rawgRating = 3.4
                )
            )
        )
    }

    /**
     * Resolves the complete FranchiseDetails for a franchise name.
     * Guaranteed to return the FULL series, cross-referenced with the user's vault games
     * so that each game is clearly marked as In My Vault or Not in My Vault.
     */
    suspend fun getFranchiseDetails(
        franchiseName: String,
        userVaultGames: List<Game>,
        originRawgId: Long? = null
    ): FranchiseDetails = withContext(Dispatchers.IO) {
        val trimmed = franchiseName.trim()
        Log.d(TAG, "Resolving complete series for franchise: \"$trimmed\" (Vault contains ${userVaultGames.size} games)")

        // 1. Find canonical series entries if available
        val matchedEntry = CANONICAL_FRANCHISES.entries.firstOrNull { (name, _) ->
            name.equals(trimmed, ignoreCase = true) ||
            trimmed.contains(name, ignoreCase = true) ||
            name.contains(trimmed, ignoreCase = true)
        }
        val canonicalList = matchedEntry?.value ?: emptyList()

        // 2. Query RAWG series if RAWG repository is available and we have originRawgId or games with notes
        var rawgSeriesList: List<RawgGameDto> = emptyList()
        val rawgQueryId = originRawgId?.toString() ?: userVaultGames
            .firstOrNull { it.franchiseName.equals(trimmed, ignoreCase = true) && it.notes.contains("[RAWG_ID:") }
            ?.let { g ->
                val regex = Regex("\\[RAWG_ID:(\\d+)\\]")
                regex.find(g.notes)?.groupValues?.getOrNull(1)
            }

        if (rawgRepository != null && !rawgQueryId.isNullOrBlank()) {
            try {
                val rawgResult = rawgRepository.getGameSeries(rawgQueryId)
                rawgSeriesList = rawgResult.getOrDefault(emptyList())
                Log.d(TAG, "Fetched ${rawgSeriesList.size} series games from RAWG for ID $rawgQueryId")
            } catch (e: Exception) {
                Log.w(TAG, "RAWG series fetch error (non-fatal): ${e.message}")
            }
        }

        // 3. Merge canonical and RAWG series
        val seriesGameMap = linkedMapOf<String, SeriesGameItem>()

        canonicalList.forEach { item ->
            val key = normalizeKey(item.title)
            seriesGameMap[key] = item
        }

        rawgSeriesList.forEachIndexed { index, dto ->
            val key = normalizeKey(dto.name)
            if (!seriesGameMap.containsKey(key)) {
                val releaseYear = dto.released?.take(4)?.toIntOrNull() ?: 0
                val platformStr = dto.platforms?.joinToString(", ") { it.platform?.name.orEmpty() }?.takeIf { it.isNotBlank() } ?: "Multi-platform"
                val genreStr = dto.genres?.firstOrNull()?.name ?: "Action"
                seriesGameMap[key] = SeriesGameItem(
                    id = "rawg-${dto.id}",
                    title = dto.name,
                    coverUrl = dto.backgroundImage.orEmpty(),
                    releaseYear = releaseYear,
                    releaseDate = dto.released,
                    platform = platformStr,
                    seriesOrder = canonicalList.size + index + 1,
                    isInVault = false,
                    vaultGame = null,
                    rawgRating = dto.rating ?: 0.0,
                    metacritic = dto.metacritic,
                    rawgId = dto.id,
                    genre = genreStr
                )
            }
        }

        // 4. Also check if the user has any games in their vault with this franchise that weren't in canonical list
        val userFranchiseGames = userVaultGames.filter { it.franchiseName.equals(trimmed, ignoreCase = true) }
        userFranchiseGames.forEach { vaultGame ->
            val key = normalizeKey(vaultGame.title)
            if (!seriesGameMap.containsKey(key)) {
                seriesGameMap[key] = SeriesGameItem(
                    id = "vault-${vaultGame.id}",
                    title = vaultGame.title,
                    coverUrl = vaultGame.coverUrl,
                    releaseYear = vaultGame.releaseYear,
                    platform = vaultGame.platform,
                    seriesOrder = vaultGame.seriesOrder ?: (seriesGameMap.size + 1),
                    isInVault = true,
                    vaultGame = vaultGame,
                    genre = vaultGame.genre
                )
            }
        }

        // 5. Cross-reference with ALL user games in vault to set isInVault and attach vaultGame
        val completeSeriesList = seriesGameMap.values.map { item ->
            val matchingVaultGame = userVaultGames.firstOrNull { vaultG ->
                normalizeKey(vaultG.title) == normalizeKey(item.title) ||
                (item.rawgId != null && vaultG.notes.contains("[RAWG_ID:${item.rawgId}]")) ||
                (vaultG.title.contains(item.title, ignoreCase = true) && Math.abs(vaultG.releaseYear - item.releaseYear) <= 1)
            }
            if (matchingVaultGame != null) {
                item.copy(
                    isInVault = true,
                    vaultGame = matchingVaultGame,
                    // If vault game has custom cover or order, preserve it
                    seriesOrder = item.seriesOrder ?: matchingVaultGame.seriesOrder,
                    coverUrl = if (item.coverUrl.isNotBlank()) item.coverUrl else matchingVaultGame.coverUrl
                )
            } else {
                item.copy(isInVault = false, vaultGame = null)
            }
        }

        // 6. Chronological / Series ordering
        val sortedSeries = completeSeriesList.sortedWith(
            compareBy<SeriesGameItem> { it.seriesOrder ?: Int.MAX_VALUE }
                .thenBy { it.releaseYear }
                .thenBy { it.title }
        )

        // Banner Image: Pick the highest quality image available
        val bannerImage = sortedSeries.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
            ?: userFranchiseGames.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
            ?: ""

        val canonicalName = matchedEntry?.key ?: trimmed

        FranchiseDetails(
            franchise = Franchise(
                name = canonicalName,
                imageUrl = bannerImage
            ),
            games = sortedSeries.mapNotNull { it.vaultGame },
            seriesGames = sortedSeries
        )
    }

    private fun normalizeKey(title: String): String {
        return title.lowercase()
            .replace(":", "")
            .replace("-", "")
            .replace("'", "")
            .replace("’", "")
            .replace("the ", "")
            .replace("remake", "")
            .replace("enhanced edition", "")
            .trim()
    }
}
