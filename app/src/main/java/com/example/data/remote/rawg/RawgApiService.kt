package com.example.data.remote.rawg

import retrofit2.http.GET
import retrofit2.http.Query

interface RawgApiService {
    /**
     * Search games using the RAWG Video Games Database API.
     * GET https://api.rawg.io/api/games?key=YOUR_API_KEY&search={game_name}
     */
    @GET("api/games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") search: String,
        @Query("page_size") pageSize: Int = 25
    ): RawgSearchResponse
}
