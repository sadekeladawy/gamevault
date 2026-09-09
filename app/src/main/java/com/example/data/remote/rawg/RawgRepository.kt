package com.example.data.remote.rawg

import android.util.Log
import com.example.data.local.RawgCacheDao
import com.example.data.local.RawgCacheEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class RawgRepository(
    private val apiService: RawgApiService = RawgApiClient.apiService,
    val apiKey: String = RawgApiClient.HARDCODED_RAWG_API_KEY,
    private val rawgCacheDao: RawgCacheDao? = null
) {
    companion object {
        private const val TAG = "RawgRepository"
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 Hours
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun searchGames(query: String): Result<List<RawgGameDto>> = searchGamesWithFilters(query = query)

    suspend fun searchGamesWithFilters(
        query: String? = null,
        genres: String? = null,
        platforms: String? = null,
        developers: String? = null,
        publishers: String? = null,
        tags: String? = null,
        dates: String? = null,
        metacritic: String? = null,
        ordering: String? = "-rating",
        page: Int = 1,
        pageSize: Int = 25
    ): Result<List<RawgGameDto>> = withContext(Dispatchers.IO) {
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val cacheKey = "search_q=${trimmedQuery}_g=${genres}_p=${platforms}_page=${page}"

        if (rawgCacheDao != null) {
            try {
                val cached = rawgCacheDao.getCache(cacheKey)
                if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < CACHE_TTL_MS) {
                    val adapter = moshi.adapter(RawgSearchResponse::class.java)
                    val response = adapter.fromJson(cached.jsonPayload)
                    if (response?.results != null) {
                        Log.d(TAG, "RAWG search loaded from local Room cache (${response.results.size} games)")
                        return@withContext Result.success(response.results)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading search cache: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Searching RAWG with filters - query: \"$trimmedQuery\", genres: $genres, platforms: $platforms, dates: $dates, ordering: $ordering")
            val response = apiService.searchGames(
                apiKey = apiKey,
                search = trimmedQuery,
                genres = genres?.takeIf { it.isNotBlank() },
                platforms = platforms?.takeIf { it.isNotBlank() },
                developers = developers?.takeIf { it.isNotBlank() },
                publishers = publishers?.takeIf { it.isNotBlank() },
                tags = tags?.takeIf { it.isNotBlank() },
                dates = dates?.takeIf { it.isNotBlank() },
                metacritic = metacritic?.takeIf { it.isNotBlank() },
                ordering = ordering,
                page = page,
                pageSize = pageSize
            )
            val results = response.results ?: emptyList()
            Log.d(TAG, "RAWG search succeeded with ${results.size} games")

            if (rawgCacheDao != null && results.isNotEmpty()) {
                try {
                    val adapter = moshi.adapter(RawgSearchResponse::class.java)
                    val jsonPayload = adapter.toJson(response)
                    rawgCacheDao.insertCache(RawgCacheEntity(cacheKey = cacheKey, jsonPayload = jsonPayload))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed caching search response: ${e.message}")
                }
            }

            Result.success(results)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during RAWG search: ${e.message}", e)
            if (rawgCacheDao != null) {
                try {
                    val cached = rawgCacheDao.getCache(cacheKey)
                    if (cached != null) {
                        val adapter = moshi.adapter(RawgSearchResponse::class.java)
                        val response = adapter.fromJson(cached.jsonPayload)
                        if (response?.results != null) {
                            Log.d(TAG, "RAWG search served from offline cache fallback (${response.results.size} games)")
                            return@withContext Result.success(response.results)
                        }
                    }
                } catch (ce: Exception) {
                    Log.w(TAG, "Failed reading offline search cache: ${ce.message}")
                }
            }
            Result.failure(Exception("Network error connecting to RAWG API. Please check your internet connection.", e))
        } catch (e: HttpException) {
            val code = e.code()
            Log.e(TAG, "HTTP error $code during RAWG search: ${e.message}", e)
            val msg = when (code) {
                401 -> "RAWG API unauthorized (invalid API key). Please verify your RAWG key."
                429 -> "RAWG API rate limit reached. Please wait a moment and try again."
                else -> "RAWG server error ($code). Please try again shortly."
            }
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during RAWG search: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to search RAWG database.", e))
        }
    }

    suspend fun getGameDetails(gameIdOrSlug: String): Result<RawgGameDto> = withContext(Dispatchers.IO) {
        if (gameIdOrSlug.isBlank()) return@withContext Result.failure(IllegalArgumentException("Game ID or slug cannot be empty"))
        val cacheKey = "details_$gameIdOrSlug"

        if (rawgCacheDao != null) {
            try {
                val cached = rawgCacheDao.getCache(cacheKey)
                if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < CACHE_TTL_MS) {
                    val adapter = moshi.adapter(RawgGameDto::class.java)
                    val details = adapter.fromJson(cached.jsonPayload)
                    if (details != null) {
                        Log.d(TAG, "RAWG details loaded from Room cache for $gameIdOrSlug")
                        return@withContext Result.success(details)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading details cache: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Fetching game details for RAWG ID/slug: $gameIdOrSlug")
            val gameDetail = apiService.getGameDetails(
                gameId = gameIdOrSlug,
                apiKey = apiKey
            )

            if (rawgCacheDao != null) {
                try {
                    val adapter = moshi.adapter(RawgGameDto::class.java)
                    val jsonPayload = adapter.toJson(gameDetail)
                    rawgCacheDao.insertCache(RawgCacheEntity(cacheKey = cacheKey, jsonPayload = jsonPayload))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed caching details: ${e.message}")
                }
            }

            Result.success(gameDetail)
        } catch (e: IOException) {
            if (rawgCacheDao != null) {
                try {
                    val cached = rawgCacheDao.getCache(cacheKey)
                    if (cached != null) {
                        val adapter = moshi.adapter(RawgGameDto::class.java)
                        val details = adapter.fromJson(cached.jsonPayload)
                        if (details != null) {
                            Log.d(TAG, "RAWG details served from offline cache fallback for $gameIdOrSlug")
                            return@withContext Result.success(details)
                        }
                    }
                } catch (ce: Exception) {
                    Log.w(TAG, "Failed reading offline details cache: ${ce.message}")
                }
            }
            Result.failure(Exception("Network error connecting to RAWG API.", e))
        } catch (e: HttpException) {
            Result.failure(Exception("RAWG API error (${e.code()}).", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGameScreenshots(gameIdOrSlug: String): Result<List<RawgScreenshotDto>> = withContext(Dispatchers.IO) {
        if (gameIdOrSlug.isBlank()) return@withContext Result.success(emptyList())
        val cacheKey = "screenshots_$gameIdOrSlug"

        if (rawgCacheDao != null) {
            try {
                val cached = rawgCacheDao.getCache(cacheKey)
                if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < CACHE_TTL_MS) {
                    val adapter = moshi.adapter(RawgScreenshotResponse::class.java)
                    val response = adapter.fromJson(cached.jsonPayload)
                    if (response?.results != null) {
                        return@withContext Result.success(response.results)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading screenshot cache: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Fetching screenshots for RAWG ID/slug: $gameIdOrSlug")
            val response = apiService.getGameScreenshots(
                gameId = gameIdOrSlug,
                apiKey = apiKey
            )
            val list = response.results ?: emptyList()

            if (rawgCacheDao != null && list.isNotEmpty()) {
                try {
                    val adapter = moshi.adapter(RawgScreenshotResponse::class.java)
                    val jsonPayload = adapter.toJson(response)
                    rawgCacheDao.insertCache(RawgCacheEntity(cacheKey = cacheKey, jsonPayload = jsonPayload))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed caching screenshots: ${e.message}")
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch screenshots for $gameIdOrSlug from network: ${e.message}")
            if (rawgCacheDao != null) {
                try {
                    val cached = rawgCacheDao.getCache(cacheKey)
                    if (cached != null) {
                        val adapter = moshi.adapter(RawgScreenshotResponse::class.java)
                        val response = adapter.fromJson(cached.jsonPayload)
                        if (response?.results != null) {
                            Log.d(TAG, "Screenshots served from offline cache fallback for $gameIdOrSlug")
                            return@withContext Result.success(response.results)
                        }
                    }
                } catch (ce: Exception) {
                    Log.w(TAG, "Failed reading fallback screenshot cache: ${ce.message}")
                }
            }
            Result.success(emptyList())
        }
    }

    suspend fun getGameTrailers(gameIdOrSlug: String): Result<List<RawgMovieDto>> = withContext(Dispatchers.IO) {
        if (gameIdOrSlug.isBlank()) return@withContext Result.success(emptyList())
        val cacheKey = "trailers_$gameIdOrSlug"

        if (rawgCacheDao != null) {
            try {
                val cached = rawgCacheDao.getCache(cacheKey)
                if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < CACHE_TTL_MS) {
                    val adapter = moshi.adapter(RawgMovieResponse::class.java)
                    val response = adapter.fromJson(cached.jsonPayload)
                    if (response?.results != null) {
                        return@withContext Result.success(response.results)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading trailer cache: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Fetching trailers for RAWG ID/slug: $gameIdOrSlug")
            val response = apiService.getGameTrailers(
                gameId = gameIdOrSlug,
                apiKey = apiKey
            )
            val list = response.results ?: emptyList()

            if (rawgCacheDao != null && list.isNotEmpty()) {
                try {
                    val adapter = moshi.adapter(RawgMovieResponse::class.java)
                    val jsonPayload = adapter.toJson(response)
                    rawgCacheDao.insertCache(RawgCacheEntity(cacheKey = cacheKey, jsonPayload = jsonPayload))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed caching trailers: ${e.message}")
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch trailers for $gameIdOrSlug from network: ${e.message}")
            if (rawgCacheDao != null) {
                try {
                    val cached = rawgCacheDao.getCache(cacheKey)
                    if (cached != null) {
                        val adapter = moshi.adapter(RawgMovieResponse::class.java)
                        val response = adapter.fromJson(cached.jsonPayload)
                        if (response?.results != null) {
                            Log.d(TAG, "Trailers served from offline cache fallback for $gameIdOrSlug")
                            return@withContext Result.success(response.results)
                        }
                    }
                } catch (ce: Exception) {
                    Log.w(TAG, "Failed reading fallback trailer cache: ${ce.message}")
                }
            }
            Result.success(emptyList())
        }
    }

    suspend fun getGenres(): Result<List<RawgGenreDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGenres(apiKey = apiKey)
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlatforms(): Result<List<RawgPlatformDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlatforms(apiKey = apiKey)
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
