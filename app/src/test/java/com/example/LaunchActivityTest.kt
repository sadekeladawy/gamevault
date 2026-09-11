package com.example

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.ui.viewmodel.NavDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaunchActivityTest {

    @Test
    fun testActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("Activity should launch successfully", activity)
            }
        }
    }

    @Test
    fun testRoomDatabaseOperations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = AppDatabase.getDatabase(context)
        val gameDao = db.gameDao()

        val testGame = Game(
            title = "Test Game",
            platform = "PC",
            genre = "RPG",
            releaseYear = 2024,
            status = GameStatus.BACKLOG,
            userId = "test_user_id",
            franchiseName = "Test Franchise",
            seriesOrder = 1
        )
        val id = gameDao.insertGame(testGame)
        assertTrue(id > 0)

        val retrieved = gameDao.getGameById(id).first()
        assertNotNull(retrieved)
        assertEquals("Test Game", retrieved?.title)
        assertEquals("Test Franchise", retrieved?.franchiseName)
        assertEquals(1, retrieved?.seriesOrder)
    }

    @Test
    fun testNavDestinationsEnum() {
        // Ensure all navigation destinations have titles and valid icons
        for (dest in NavDestination.entries) {
            assertTrue(dest.title.isNotBlank())
            assertNotNull(com.example.ui.getDestinationIcon(dest))
        }
    }
}
