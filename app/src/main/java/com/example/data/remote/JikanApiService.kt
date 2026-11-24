package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
  @GET("anime")
  suspend fun searchAnime(
    @Query("q") query: String,
    @Query("limit") limit: Int = 6,
    @Query("sfw") sfw: Boolean = true
  ): JikanSearchResponse
}
