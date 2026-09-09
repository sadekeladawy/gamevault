package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rawg_cache")
data class RawgCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val jsonPayload: String,
    val cachedAt: Long = System.currentTimeMillis()
)
