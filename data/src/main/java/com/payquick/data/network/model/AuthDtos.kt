package com.payquick.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val status: String,
    val message: String,
    val data: LoginData
)

@Serializable
data class LoginData(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: NetworkUser
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val status: String,
    val message: String,
    val data: RefreshTokenData
)

@Serializable
data class RefreshTokenData(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: NetworkUser
)

@Serializable
data class NetworkUser(
    @SerialName("user_id") val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String
)
