package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RawgCacheDao {
    @Query("SELECT * FROM rawg_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCache(key: String): RawgCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: RawgCacheEntity)

    @Query("DELETE FROM rawg_cache WHERE cachedAt < :expiryTimestamp")
    suspend fun clearExpiredCache(expiryTimestamp: Long)
}
