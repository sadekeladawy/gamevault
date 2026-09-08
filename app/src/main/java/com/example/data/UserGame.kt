package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class UserGame(
    val id: String = "",
    val name: String = "",
    val hoursPlayed: Double = 0.0,
    val completed: Boolean = false,
    val imageUrl: String = ""
)

suspend fun saveGameToFirestore(
    game: UserGame,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        onFailure(IllegalStateException("User is not authenticated"))
        return
    }

    val userId = currentUser.uid
    val db = FirebaseFirestore.getInstance()

    val gamesCollection = db.collection("users").document(userId).collection("games")

    val docRef = if (game.id.isNotEmpty()) {
        gamesCollection.document(game.id)
    } else {
        gamesCollection.document()
    }

    val gameToSave = game.copy(id = docRef.id)

    try {
        docRef.set(gameToSave).await()
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}