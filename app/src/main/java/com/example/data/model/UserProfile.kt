package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val gamerTag: String? = null,
    val bio: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val totalGamesCount: Int = 0,
    val completedGamesCount: Int = 0,
    val isEmailVerified: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "photoUrl" to photoUrl,
            "gamerTag" to gamerTag,
            "bio" to bio,
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt,
            "totalGamesCount" to totalGamesCount,
            "completedGamesCount" to completedGamesCount
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>, uid: String, email: String, isEmailVerified: Boolean = false): UserProfile {
            return UserProfile(
                uid = uid,
                fullName = (map["fullName"] as? String) ?: "",
                email = (map["email"] as? String) ?: email,
                photoUrl = map["photoUrl"] as? String,
                gamerTag = map["gamerTag"] as? String,
                bio = map["bio"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastLoginAt = (map["lastLoginAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                totalGamesCount = (map["totalGamesCount"] as? Number)?.toInt() ?: 0,
                completedGamesCount = (map["completedGamesCount"] as? Number)?.toInt() ?: 0,
                isEmailVerified = isEmailVerified
            )
        }
    }
}
