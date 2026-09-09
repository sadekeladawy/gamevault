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
    @Json(name = "background_image_additional") val backgroundImageAdditional: String? = null,
    @Json(name = "rating") val rating: Double? = 0.0,
    @Json(name = "rating_top") val ratingTop: Int? = 5,
    @Json(name = "ratings_count") val ratingsCount: Int? = 0,
    @Json(name = "metacritic") val metacritic: Int? = null,
    @Json(name = "playtime") val playtime: Int? = 0,
    @Json(name = "esrb_rating") val esrbRating: RawgEsrbRatingDto? = null,
    @Json(name = "genres") val genres: List<RawgGenreDto>? = null,
    @Json(name = "platforms") val platforms: List<RawgPlatformSlotDto>? = null,
    @Json(name = "tags") val tags: List<RawgTagDto>? = null,
    @Json(name = "developers") val developers: List<RawgDeveloperDto>? = null,
    @Json(name = "publishers") val publishers: List<RawgPublisherDto>? = null,
    @Json(name = "stores") val stores: List<RawgStoreSlotDto>? = null,
    @Json(name = "short_screenshots") val shortScreenshots: List<RawgScreenshotDto>? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "description_raw") val descriptionRaw: String? = null,
    @Json(name = "website") val website: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgGenreDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgPlatformSlotDto(
    @Json(name = "platform") val platform: RawgPlatformDto? = null
)

@JsonClass(generateAdapter = true)
data class RawgPlatformDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgEsrbRatingDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgTagDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "language") val language: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgDeveloperDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "image_background") val imageBackground: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgPublisherDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "image_background") val imageBackground: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgStoreSlotDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "store") val store: RawgStoreDto? = null
)

@JsonClass(generateAdapter = true)
data class RawgStoreDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "domain") val domain: String? = null,
    @Json(name = "slug") val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class RawgScreenshotDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class RawgScreenshotResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "results") val results: List<RawgScreenshotDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgGenreListResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "results") val results: List<RawgGenreDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgPlatformListResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "results") val results: List<RawgPlatformDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgMovieResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "results") val results: List<RawgMovieDto>? = null
)

@JsonClass(generateAdapter = true)
data class RawgMovieDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "preview") val preview: String? = null,
    @Json(name = "data") val data: RawgMovieDataDto? = null
) {
    fun getVideoUrl(): String? {
        return data?.resolutionMax?.takeIf { it.isNotBlank() }
            ?: data?.resolution480?.takeIf { it.isNotBlank() }
    }
}

@JsonClass(generateAdapter = true)
data class RawgMovieDataDto(
    @Json(name = "480") val resolution480: String? = null,
    @Json(name = "max") val resolutionMax: String? = null
)

