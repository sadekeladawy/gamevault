package com.example.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// RAWG Search API Response Models
data class RawgSearchResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("results") val results: List<RawgGame>
)

data class RawgGame(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("background_image") val backgroundImage: String?
)

// Retrofit Interface
interface RawgApiService {
    @GET("api/games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String
    ): RawgSearchResponse

    companion object {
        private const val BASE_URL = "https://api.rawg.io/"

        fun create(): RawgApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RawgApiService::class.java)
        }
    }
}