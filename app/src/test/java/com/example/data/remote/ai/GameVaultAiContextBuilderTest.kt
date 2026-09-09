package com.example.data.remote.ai

import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.remote.rawg.RawgGameDto
import com.example.data.remote.rawg.RawgGenreDto
import com.example.data.remote.rawg.RawgPlatformDto
import com.example.data.remote.rawg.RawgPlatformSlotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameVaultAiContextBuilderTest {

    @Test
    fun testUserBacklogContextFormatting() {
        val games = listOf(
            Game(
                id = 1,
                title = "Elden Ring",
                platform = "PC",
                genre = "Action RPG",
                status = GameStatus.CURRENTLY_PLAYING,
                rating = 9
            )
        )

        val context = GameVaultAiContextBuilder.buildUserBacklogContext(games)

        assertTrue(context.contains("Elden Ring"))
        assertTrue(context.contains("PC"))
        assertTrue(context.contains("Action RPG"))
        assertTrue(context.contains("my rating: 9/10"))
    }

    @Test
    fun testEmptyBacklogReturnsEmptyString() {
        val context = GameVaultAiContextBuilder.buildUserBacklogContext(emptyList())
        assertEquals("", context)
    }

    @Test
    fun testRawgMetadataContextFormatting() {
        val rawgGames = listOf(
            RawgGameDto(
                id = 123,
                name = "Cyberpunk 2077",
                released = "2020-12-10",
                rating = 4.2,
                metacritic = 86,
                genres = listOf(RawgGenreDto(name = "RPG")),
                platforms = listOf(RawgPlatformSlotDto(platform = RawgPlatformDto(name = "PC")))
            )
        )

        val context = GameVaultAiContextBuilder.buildRawgMetadataContext(rawgGames)

        assertTrue(context.contains("Cyberpunk 2077"))
        assertTrue(context.contains("Rating 4.2/5.0"))
        assertTrue(context.contains("Metacritic: 86"))
        assertTrue(context.contains("Genres: RPG"))
    }

    @Test
    fun testEmptyRawgMetadataReturnsEmptyString() {
        val context = GameVaultAiContextBuilder.buildRawgMetadataContext(emptyList())
        assertEquals("", context)
    }
}
