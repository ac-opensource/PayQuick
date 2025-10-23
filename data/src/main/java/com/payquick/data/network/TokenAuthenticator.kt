package com.payquick.data.network

import com.payquick.data.network.model.RefreshTokenRequest
import com.payquick.data.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    @Named("refreshApi") private val refreshApi: PayQuickApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            Timber.w("Canceling auth retry after multiple failures")
            return null
        }

        val currentSession = sessionManager.session.value ?: return null

        val refreshResult = runBlocking {
            try {
                val response = refreshApi.refreshToken(
                    RefreshTokenRequest(refreshToken = currentSession.refreshToken)
                )
                Result.success(response)
            } catch (throwable: Throwable) {
                Result.failure(throwable)
            }
        }

        val refreshed = refreshResult.getOrElse { error ->
            Timber.e(error, "Token refresh failed")
            runBlocking { sessionManager.clear() }
            return null
        }

        runBlocking {
            sessionManager.updateTokens(
                accessToken = refreshed.data.accessToken,
                refreshToken = refreshed.data.refreshToken,
                expiresInSeconds = refreshed.data.expiresIn
            )
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshed.data.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var current = response.priorResponse
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }
}
