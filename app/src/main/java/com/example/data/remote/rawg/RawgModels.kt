package com.example.data.remote.rawg

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RawgSearchResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "next") val next: String? = null,
    @Json(name = "previous") val previous: String? = null,
    @Json(name = "results") val results: List<RawgGameDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgGameDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "released") val released: String? = null,
    @Json(name = "background_image") val backgroundImage: String? = null,
    @Json(name = "rating") val rating: Double? = 0.0,
    @Json(name = "rating_top") val ratingTop: Int? = 5,
    @Json(name = "ratings_count") val ratingsCount: Int? = 0,
    @Json(name = "metacritic") val metacritic: Int? = null,
    @Json(name = "playtime") val playtime: Int? = 0,
    @Json(name = "genres") val genres: List<RawgGenreDto>? = null,
    @Json(name = "platforms") val platforms: List<RawgPlatformSlotDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgGenreDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgPlatformSlotDto(
    @Json(name = "platform") val platform: RawgPlatformDto? = null
)

@JsonClass(generateAdapter = true)
data class RawgPlatformDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null
)
