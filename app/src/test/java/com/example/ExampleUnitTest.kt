package com.example

import com.example.data.model.UserProfile
import com.example.ui.viewmodel.NavDestination
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNavDestinationContainsAuth() {
    val destinations = NavDestination.entries
    assertTrue(destinations.contains(NavDestination.AUTH))
    assertEquals("Account", NavDestination.AUTH.title)
  }

  @Test
  fun testUserProfileCreation() {
    val user = UserProfile(
      uid = "test-uid-123",
      email = "commander@gamevault.io",
      fullName = "Commander Shepard",
      gamerTag = "Spectre#001",
      isEmailVerified = true
    )
    assertEquals("test-uid-123", user.uid)
    assertEquals("commander@gamevault.io", user.email)
    assertEquals("Commander Shepard", user.fullName)
    assertEquals("Spectre#001", user.gamerTag)
    assertTrue(user.isEmailVerified)
  }
}
