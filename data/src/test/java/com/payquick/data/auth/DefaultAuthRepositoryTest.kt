package com.payquick.data.auth

import com.payquick.data.network.PayQuickApi
import com.payquick.data.network.model.LoginData
import com.payquick.data.network.model.LoginResponse
import com.payquick.data.network.model.NetworkUser
import com.payquick.data.network.model.RefreshTokenData
import com.payquick.data.network.model.RefreshTokenRequest
import com.payquick.data.network.model.RefreshTokenResponse
import com.payquick.data.session.SessionManager
import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.coJustRun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DefaultAuthRepositoryTest {

    @MockK
    private lateinit var api: PayQuickApi

    @MockK
    private lateinit var refreshApi: PayQuickApi

    @MockK(relaxed = true)
    private lateinit var sessionManager: SessionManager

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private val mfaFlow = MutableStateFlow(false)

    private lateinit var repository: DefaultAuthRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { sessionManager.session } returns sessionFlow
        every { sessionManager.isMfaEnrolled } returns mfaFlow
        repository = DefaultAuthRepository(api, refreshApi, sessionManager)
    }

    @Test
    fun loginStoresSessionOnSuccess() = runTest {
        val response = LoginResponse(
            status = "success",
            message = "ok",
            data = LoginData(
                accessToken = "access",
                expiresIn = 3600,
                refreshToken = "refresh",
                tokenType = "bearer",
                user = NetworkUser(id = "1", fullName = "Jamie Rivera", email = "smith@example.com")
            )
        )
        coEvery { api.login(any()) } returns response
        coJustRun {
            sessionManager.saveTokens(
                accessToken = any(),
                refreshToken = any(),
                expiresInSeconds = any(),
                user = any()
            )
        }

        val result = repository.login("smith@example.com", "pass123")

        assertTrue(result.isSuccess)
        coVerify {
            sessionManager.saveTokens(
                accessToken = "access",
                refreshToken = "refresh",
                expiresInSeconds = 3600,
                user = any()
            )
        }
    }

    @Test
    fun loginFailsForUnexpectedCredentials() = runTest {
        val result = repository.login("wrong@example.com", "pass123")
        assertTrue(result.isFailure)
    }

    @Test
    fun refreshSessionUpdatesTokens() = runTest {
        sessionFlow.value = Session(
            accessToken = "old",
            refreshToken = "refresh",
            expiresAt = Instant.parse("2024-01-01T00:00:00Z"),
            user = User(id = "1", fullName = "Jamie", email = "jamie@example.com")
        )
        val refreshResponse = RefreshTokenResponse(
            status = "success",
            message = "ok",
            data = RefreshTokenData(
                accessToken = "new",
                refreshToken = "new-refresh",
                expiresIn = 7200,
                tokenType = "bearer",
                user = NetworkUser(id = "1", fullName = "Jamie Rivera", email = "jamie@example.com")
            )
        )
        coEvery { refreshApi.refreshToken(any()) } returns refreshResponse
        coJustRun { sessionManager.updateTokens(any(), any(), any()) }

        val result = repository.refreshSession()

        assert(result.isSuccess)
        coVerify { refreshApi.refreshToken(RefreshTokenRequest(refreshToken = "refresh")) }
        coVerify { sessionManager.updateTokens("new", "new-refresh", 7200) }
    }

    @Test
    fun setMfaEnrollmentDelegatesToSessionManager() = runTest {
        repository.setMfaEnrollment(true)
        coVerify { sessionManager.setMfaEnrolled(true) }
    }

    @Test
    fun logoutClearsSession() = runTest {
        repository.logout()
        coVerify { sessionManager.clear() }
    }
}
