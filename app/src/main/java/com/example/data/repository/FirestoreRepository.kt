package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Game
import com.example.data.model.GameStatus
import com.example.data.model.MasterGame
import com.example.data.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

class FirestoreRepository(private val context: Context) {

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val USERS_COLLECTION = "users"
        private const val GAMES_SUBCOLLECTION = "games"
        private const val GAMES_DATABASE_COLLECTION = "games_database"
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder().build()
                        )
                        .build()
                    db.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.d(TAG, "Settings already set or exception: ${e.message}")
                }
                db
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not initialized: ${e.message}")
            null
        }
    }

    fun isFirestoreAvailable(): Boolean = firestore != null

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized. Please ensure google-services.json is configured."))
        try {
            val userRef = db.collection(USERS_COLLECTION).document(profile.uid)
            userRef.set(profile.toMap(), SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String, email: String, isEmailVerified: Boolean): Result<UserProfile?> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val userDoc = db.collection(USERS_COLLECTION).document(uid).get().awaitTask()
            if (userDoc.exists() && userDoc.data != null) {
                val profile = UserProfile.fromMap(userDoc.data!!, uid = uid, email = email, isEmailVerified = isEmailVerified)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateLastLogin(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            db.collection(USERS_COLLECTION).document(uid)
                .update("lastLoginAt", System.currentTimeMillis())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last login: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileFields(uid: String, fields: Map<String, Any?>): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            db.collection(USERS_COLLECTION).document(uid)
                .set(fields, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user fields: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncGamesToCloud(uid: String, games: List<Game>): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            // Write in batches of up to 500 documents (Firestore limit)
            val batch = db.batch()
            val userGamesRef = db.collection(USERS_COLLECTION).document(uid).collection(GAMES_SUBCOLLECTION)

            for (game in games) {
                val docRef = userGamesRef.document(game.id.toString())
                val gameMap = mapOf(
                    "id" to game.id,
                    "title" to game.title,
                    "coverUrl" to game.coverUrl,
                    "platform" to game.platform,
                    "genre" to game.genre,
                    "releaseYear" to game.releaseYear,
                    "status" to game.status.name,
                    "completionDate" to game.completionDate,
                    "playtimeHours" to game.playtimeHours,
                    "rating" to game.rating,
                    "notes" to game.notes,
                    "isFavorite" to game.isFavorite,
                    "createdAt" to game.createdAt,
                    "syncedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, gameMap, SetOptions.merge())
            }

            batch.commit().awaitTask()

            // Update games count on user profile
            val completedCount = games.count { it.status == GameStatus.COMPLETED }
            db.collection(USERS_COLLECTION).document(uid).set(
                mapOf(
                    "totalGamesCount" to games.size,
                    "completedGamesCount" to completedCount,
                    "lastSyncAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).awaitTask()

            Result.success(games.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing games to cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchGamesFromCloud(uid: String): Result<List<Game>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val snapshot = db.collection(USERS_COLLECTION).document(uid)
                .collection(GAMES_SUBCOLLECTION)
                .get()
                .awaitTask()

            val list = mutableListOf<Game>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val game = Game(
                    id = (data["id"] as? Number)?.toLong() ?: 0L,
                    title = data["title"] as? String ?: "Untitled",
                    coverUrl = data["coverUrl"] as? String ?: "",
                    platform = data["platform"] as? String ?: "PC",
                    genre = data["genre"] as? String ?: "Action RPG",
                    releaseYear = (data["releaseYear"] as? Number)?.toInt() ?: 2023,
                    status = GameStatus.fromString(data["status"] as? String ?: "BACKLOG"),
                    completionDate = data["completionDate"] as? String,
                    playtimeHours = (data["playtimeHours"] as? Number)?.toDouble() ?: 0.0,
                    rating = (data["rating"] as? Number)?.toInt() ?: 0,
                    notes = data["notes"] as? String ?: "",
                    isFavorite = data["isFavorite"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                list.add(game)
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games from cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch all games from the global Firestore Game Database (`games_database` collection).
     */
    suspend fun fetchMasterGameDatabase(): Result<List<MasterGame>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val snapshot = db.collection(GAMES_DATABASE_COLLECTION)
                .get()
                .awaitTask()

            val list = mutableListOf<MasterGame>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val masterGame = MasterGame.fromMap(doc.id, data)
                list.add(masterGame)
            }
            Log.d(TAG, "Fetched ${list.size} games from Firestore $GAMES_DATABASE_COLLECTION")
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching master game database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Seed or sync a list of curated master games into the Firestore `games_database` collection.
     */
    suspend fun seedMasterGameDatabase(games: List<MasterGame>): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val batch = db.batch()
            val collectionRef = db.collection(GAMES_DATABASE_COLLECTION)

            for (game in games) {
                val docId = if (game.id.isNotBlank()) game.id else game.title.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                val docRef = collectionRef.document(docId)
                batch.set(docRef, game.toMap(), SetOptions.merge())
            }

            batch.commit().awaitTask()
            Log.d(TAG, "Successfully seeded ${games.size} games into Firestore $GAMES_DATABASE_COLLECTION")
            Result.success(games.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding master game database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Add or update a single game in the Firestore `games_database` collection.
     */
    suspend fun addGameToMasterDatabase(game: MasterGame): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val docId = if (game.id.isNotBlank()) game.id else game.title.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
            db.collection(GAMES_DATABASE_COLLECTION).document(docId)
                .set(game.toMap(), SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding game to master database: ${e.message}", e)
            Result.failure(e)
        }
    }
}
