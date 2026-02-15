package com.walletwatch.mini.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface FxApi {
    @GET("latest")
    suspend fun latest(@Query("from") from: String = "USD"): FxResponse
}

@Serializable
data class FxResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
