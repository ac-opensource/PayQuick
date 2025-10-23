package com.payquick.data.network

import com.payquick.data.network.model.LoginRequest
import com.payquick.data.network.model.LoginResponse
import com.payquick.data.network.model.RefreshTokenRequest
import com.payquick.data.network.model.RefreshTokenResponse
import com.payquick.data.network.model.TransactionEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PayQuickApi {

    @POST("v1/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("v1/token/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    @GET("v1/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int
    ): TransactionEnvelope
}
