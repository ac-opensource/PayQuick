package com.payquick.data.auth

import com.payquick.data.network.PayQuickApi
import com.payquick.data.network.model.LoginRequest
import com.payquick.data.network.model.LoginResponse
import com.payquick.data.network.model.RefreshTokenRequest
import com.payquick.data.session.SessionManager
import com.payquick.domain.repository.AuthRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: PayQuickApi,
    @Named("refreshApi") private val refreshApi: PayQuickApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val session = sessionManager.session

    override suspend fun login(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (normalizedEmail != EXPECTED_EMAIL || password != EXPECTED_PASSWORD) {
            return Result.failure(InvalidCredentialsException())
        }

        return try {
            val response = api.login(LoginRequest(email = EXPECTED_EMAIL, password = password))
            ensureSuccessfulLogin(response, EXPECTED_EMAIL)

            val payload = response.data
            storeSession(payload.accessToken, payload.refreshToken, payload.expiresIn) {
                SessionManager.SessionUserSnapshot(
                    id = payload.user.id,
                    fullName = payload.user.fullName,
                    email = payload.user.email
                )
            }

            Result.success(Unit)
        } catch (error: InvalidCredentialsException) {
            Result.failure(error)
        } catch (error: Throwable) {
            Result.failure(InvalidCredentialsException(cause = error))
        }
    }

    override suspend fun refreshSession(): Result<Unit> {
        val current = sessionManager.session.value
            ?: return Result.failure(IllegalStateException("No session available"))

        return try {
            val response = refreshApi.refreshToken(
                RefreshTokenRequest(refreshToken = current.refreshToken)
            )

            sessionManager.updateTokens(
                accessToken = response.data.accessToken,
                refreshToken = response.data.refreshToken,
                expiresInSeconds = response.data.expiresIn
            )

            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun ensureSuccessfulLogin(response: LoginResponse, expectedEmail: String) {
        if (!response.status.equals(SUCCESS_STATUS, ignoreCase = true)) {
            throw InvalidCredentialsException(message = response.message)
        }

        val payload = response.data
        val resolvedEmail = payload.user.email
        if (!resolvedEmail.equals(expectedEmail, ignoreCase = true)) {
            throw InvalidCredentialsException()
        }

        if (payload.accessToken.isBlank() || payload.refreshToken.isBlank()) {
            throw InvalidCredentialsException()
        }
    }

    private suspend fun storeSession(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userProvider: () -> SessionManager.SessionUserSnapshot
    ) {
        sessionManager.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresInSeconds,
            user = userProvider()
        )
    }

    class InvalidCredentialsException(
        message: String? = null,
        cause: Throwable? = null
    ) : IllegalArgumentException(message ?: "Invalid email or password", cause)

    override suspend fun logout() {
        sessionManager.clear()
    }

    companion object {
        private const val EXPECTED_EMAIL = "smith@example.com"
        private const val EXPECTED_PASSWORD = "pass123"
        private const val SUCCESS_STATUS = "success"
    }
}
