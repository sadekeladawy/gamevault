package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Game
import com.example.data.model.GameStatus
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

    /**
     * Creates a document in users/{uid} with basic info: uid, email, displayName, and createdAt timestamp.
     */
    suspend fun createUserDocument(
        uid: String,
        email: String,
        displayName: String?,
        createdAt: Long = System.currentTimeMillis()
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val profile = UserProfile(
                uid = uid,
                displayName = displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                fullName = displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email,
                createdAt = createdAt,
                lastLoginAt = System.currentTimeMillis(),
                isEmailVerified = false
            )
            val userRef = db.collection(USERS_COLLECTION).document(uid)
            userRef.set(profile.toMap(), SetOptions.merge()).awaitTask()
            Log.i(TAG, "Successfully created Firestore document at users/$uid")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user document at users/$uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Loads the user's Firestore document from users/{uid}.
     * If it doesn't exist, creates it automatically with uid, email, displayName, and createdAt timestamp.
     */
    suspend fun loadOrCreateUserDocument(
        uid: String,
        email: String,
        displayName: String?,
        isEmailVerified: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val userDoc = db.collection(USERS_COLLECTION).document(uid).get().awaitTask()
            if (userDoc.exists() && userDoc.data != null) {
                val profile = UserProfile.fromMap(userDoc.data!!, uid = uid, email = email, isEmailVerified = isEmailVerified)
                Log.d(TAG, "Loaded existing Firestore user document for $uid")
                Result.success(profile)
            } else {
                Log.i(TAG, "Firestore user document not found for $uid. Creating automatically...")
                val effectiveName = displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val newProfile = UserProfile(
                    uid = uid,
                    displayName = effectiveName,
                    fullName = effectiveName,
                    email = email,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis(),
                    isEmailVerified = isEmailVerified
                )
                db.collection(USERS_COLLECTION).document(uid).set(newProfile.toMap(), SetOptions.merge()).awaitTask()
                Log.i(TAG, "Automatically created Firestore document at users/$uid")
                Result.success(newProfile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadOrCreateUserDocument for $uid: ${e.message}", e)
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

    /**
     * Saves or updates an individual game under users/{uid}/games/{game.id}.
     */
    suspend fun saveUserGame(uid: String, game: Game): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val userGamesRef = db.collection(USERS_COLLECTION).document(uid).collection(GAMES_SUBCOLLECTION)
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
                "rawgRating" to game.rawgRating,
                "metacriticScore" to game.metacriticScore,
                "developer" to game.developer,
                "publisher" to game.publisher,
                "notes" to game.notes,
                "isFavorite" to game.isFavorite,
                "isArchived" to game.isArchived,
                "createdAt" to game.createdAt,
                "userId" to uid,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(gameMap, SetOptions.merge()).awaitTask()
            Log.d(TAG, "Saved game ${game.id} to users/$uid/games/${game.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user game under users/$uid/games/${game.id}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an individual game document from users/{uid}/games/{gameId}.
     */
    suspend fun deleteUserGame(uid: String, gameId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            db.collection(USERS_COLLECTION).document(uid)
                .collection(GAMES_SUBCOLLECTION).document(gameId.toString())
                .delete()
                .awaitTask()
            Log.d(TAG, "Deleted game $gameId from users/$uid/games/$gameId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user game at users/$uid/games/$gameId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Completely wipes all game documents under users/{uid}/games for this user.
     */
    suspend fun clearAllUserGamesInCloud(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val snapshot = db.collection(USERS_COLLECTION).document(uid)
                .collection(GAMES_SUBCOLLECTION)
                .get()
                .awaitTask()

            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitTask()
            }

            // Reset user counts
            db.collection(USERS_COLLECTION).document(uid).set(
                mapOf(
                    "totalGamesCount" to 0,
                    "completedGamesCount" to 0,
                    "lastSyncAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).awaitTask()

            Log.d(TAG, "Successfully cleared all games in users/$uid/games")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cloud games for users/$uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncGamesToCloud(uid: String, games: List<Game>): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            val userGamesRef = db.collection(USERS_COLLECTION).document(uid).collection(GAMES_SUBCOLLECTION)
            val existingSnapshot = userGamesRef.get().awaitTask()

            val localGameIds = games.map { it.id.toString() }.toSet()
            val batch = db.batch()

            // Delete remote games that no longer exist locally
            for (doc in existingSnapshot.documents) {
                if (doc.id !in localGameIds) {
                    batch.delete(doc.reference)
                }
            }

            // Write / update all current games
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
                    "rawgRating" to game.rawgRating,
                    "metacriticScore" to game.metacriticScore,
                    "developer" to game.developer,
                    "publisher" to game.publisher,
                    "notes" to game.notes,
                    "isFavorite" to game.isFavorite,
                    "createdAt" to game.createdAt,
                    "userId" to uid,
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

            Log.d(TAG, "Synced ${games.size} games to users/$uid/games")
            Result.success(games.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing games to cloud for users/$uid: ${e.message}", e)
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
                    rawgRating = (data["rawgRating"] as? Number)?.toDouble() ?: 0.0,
                    metacriticScore = (data["metacriticScore"] as? Number)?.toInt(),
                    developer = data["developer"] as? String ?: "",
                    publisher = data["publisher"] as? String ?: "",
                    notes = data["notes"] as? String ?: "",
                    isFavorite = data["isFavorite"] as? Boolean ?: false,
                    isArchived = data["isArchived"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    userId = uid
                )
                list.add(game)
            }
            Log.d(TAG, "Fetched ${list.size} games from users/$uid/games")
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games from cloud for users/$uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Store future user-specific data under users/{uid}/{subcollection}/{docId}.
     */
    suspend fun saveUserSpecificData(
        uid: String,
        subcollection: String,
        docId: String,
        data: Map<String, Any?>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            db.collection(USERS_COLLECTION).document(uid)
                .collection(subcollection).document(docId)
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user-specific data under users/$uid/$subcollection/$docId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete user-specific data under users/{uid}/{subcollection}/{docId}.
     */
    suspend fun deleteUserSpecificData(
        uid: String,
        subcollection: String,
        docId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized."))
        try {
            db.collection(USERS_COLLECTION).document(uid)
                .collection(subcollection).document(docId)
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user-specific data under users/$uid/$subcollection/$docId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
