package com.example.data.remote.rawg

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {
    /**
     * Search games using the RAWG Video Games Database API with advanced filtering.
     */
    @GET("api/games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") search: String? = null,
        @Query("search_precise") searchPrecise: Boolean? = null,
        @Query("genres") genres: String? = null,
        @Query("platforms") platforms: String? = null,
        @Query("developers") developers: String? = null,
        @Query("publishers") publishers: String? = null,
        @Query("tags") tags: String? = null,
        @Query("dates") dates: String? = null,
        @Query("metacritic") metacritic: String? = null,
        @Query("ordering") ordering: String? = "-rating",
        @Query("page") page: Int? = 1,
        @Query("page_size") pageSize: Int = 25
    ): RawgSearchResponse

    /**
     * Fetch games from RAWG Video Games Database API.
     */
    @GET("api/games")
    suspend fun getGames(
        @Query("key") apiKey: String,
        @Query("page_size") pageSize: Int = 25,
        @Query("ordering") ordering: String = "-rating",
        @Query("genres") genres: String? = null,
        @Query("platforms") platforms: String? = null
    ): RawgSearchResponse

    /**
     * Fetch full details for a specific game by ID or slug.
     * GET https://api.rawg.io/api/games/{id}?key=YOUR_API_KEY
     */
    @GET("api/games/{id}")
    suspend fun getGameDetails(
        @Path("id") gameId: String,
        @Query("key") apiKey: String
    ): RawgGameDto

    /**
     * Fetch screenshots for a specific game.
     * GET https://api.rawg.io/api/games/{id}/screenshots?key=YOUR_API_KEY
     */
    @GET("api/games/{id}/screenshots")
    suspend fun getGameScreenshots(
        @Path("id") gameId: String,
        @Query("key") apiKey: String
    ): RawgScreenshotResponse

    /**
     * Fetch official trailers/movies for a specific game.
     * GET https://api.rawg.io/api/games/{id}/movies?key=YOUR_API_KEY
     */
    @GET("api/games/{id}/movies")
    suspend fun getGameTrailers(
        @Path("id") gameId: String,
        @Query("key") apiKey: String
    ): RawgMovieResponse

    /**
     * Fetch list of available genres in RAWG.
     */
    @GET("api/genres")
    suspend fun getGenres(
        @Query("key") apiKey: String
    ): RawgGenreListResponse

    /**
     * Fetch list of available platforms in RAWG.
     */
    @GET("api/platforms")
    suspend fun getPlatforms(
        @Query("key") apiKey: String
    ): RawgPlatformListResponse
}
