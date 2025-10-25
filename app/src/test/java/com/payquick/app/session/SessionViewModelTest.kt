package com.payquick.app.session

import com.payquick.R
import com.payquick.app.testing.MainDispatcherRule
import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import com.payquick.domain.repository.AuthRepository
import com.payquick.domain.usecase.LogoutUseCase
import com.payquick.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SessionViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private fun createViewModel(): SessionViewModel =
        SessionViewModel(ObserveSessionUseCase(repository), LogoutUseCase(repository))

    @Test
    fun sessionUpdatesWhenFlowEmits() = runTest(dispatcherRule.dispatcher) {
        repository.sessionFlow.value = null
        repository.shouldFailLogout = false
        val viewModel = createViewModel()
        val session = Session(
            accessToken = "a",
            refreshToken = "b",
            expiresAt = Instant.DISTANT_FUTURE,
            user = User(id = "1", fullName = "Jamie", email = "jamie@example.com")
        )
        repository.sessionFlow.value = session
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(session, viewModel.state.value.session)
    }

    @Test
    fun logoutFailureEmitsFallbackMessage() = runTest(dispatcherRule.dispatcher) {
        repository.sessionFlow.value = null
        repository.shouldFailLogout = false
        val viewModel = createViewModel()
        repository.shouldFailLogout = true

        viewModel.logout()

        val event = viewModel.events.filterIsInstance<SessionEvent.Error>().first()
        assertEquals(R.string.session_error_logout, event.messageResId)
        assertNull(event.message)
    }

    private class FakeAuthRepository : AuthRepository {
        val sessionFlow = MutableStateFlow<Session?>(null)
        var shouldFailLogout: Boolean = false

        override val session: StateFlow<Session?> = sessionFlow
        override val isMfaEnrolled: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)

        override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)

        override suspend fun logout() {
            if (shouldFailLogout) throw IllegalStateException()
        }

        override suspend fun setMfaEnrollment(enrolled: Boolean) {}
    }
}
