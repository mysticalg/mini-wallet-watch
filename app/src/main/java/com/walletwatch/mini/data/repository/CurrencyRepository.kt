package com.walletwatch.mini.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.walletwatch.mini.data.remote.FxApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class CurrencyRepository {
    private val api: FxApi by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FxApi::class.java)
    }

    suspend fun getUsdRates(): Map<String, Double> {
        val response = api.latest(from = "USD")
        return response.rates + mapOf("USD" to 1.0)
    }
}
