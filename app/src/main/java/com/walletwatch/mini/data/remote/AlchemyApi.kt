package com.walletwatch.mini.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AlchemyApi {
    @POST("/")
    suspend fun getBalance(@Body request: JsonRpcBalanceRequest): JsonRpcBalanceResponse
}

@Serializable
data class JsonRpcBalanceRequest(
    val id: Int = 1,
    val jsonrpc: String = "2.0",
    val method: String = "eth_getBalance",
    val params: List<String>
)

@Serializable
data class JsonRpcBalanceResponse(
    val id: Int,
    val jsonrpc: String,
    val result: String? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    @SerialName("data")
    val extraData: String? = null
)
