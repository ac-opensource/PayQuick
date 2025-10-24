package com.payquick.app.auth

import com.payquick.R
import com.payquick.app.testing.MainDispatcherRule
import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import com.payquick.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    @Test
    fun blankEmailDisplaysValidationError() = runTest(dispatcherRule.dispatcher) {
        val viewModel = LoginViewModel(repository)

        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("pass123")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.login_error_email_required, viewModel.state.value.emailErrorResId)
    }

    @Test
    fun successfulLoginNavigatesToMfaEnrollWhenNotEnrolled() = runTest(dispatcherRule.dispatcher) {
        val viewModel = LoginViewModel(repository)
        repository.loginResult = Result.success(Unit)
        repository.isMfaEnrolledFlow.value = false

        viewModel.onEmailChanged("smith@example.com")
        viewModel.onPasswordChanged("pass123")

        viewModel.submit()

        val event = viewModel.events.filterIsInstance<LoginEvent.NavigateToMfaEnroll>().first()
        assertTrue(event is LoginEvent.NavigateToMfaEnroll)
    }

    @Test
    fun successfulLoginNavigatesToMfaVerifyWhenEnrolled() = runTest(dispatcherRule.dispatcher) {
        val viewModel = LoginViewModel(repository)
        repository.loginResult = Result.success(Unit)
        repository.isMfaEnrolledFlow.value = true

        viewModel.onEmailChanged("smith@example.com")
        viewModel.onPasswordChanged("pass123")

        viewModel.submit()

        val event = viewModel.events.filterIsInstance<LoginEvent.NavigateToMfaVerify>().first()
        assertTrue(event is LoginEvent.NavigateToMfaVerify)
    }

    private class FakeAuthRepository : AuthRepository {
        val sessionFlow = MutableStateFlow<Session?>(null)
        val isMfaEnrolledFlow = MutableStateFlow(false)
        var loginResult: Result<Unit> = Result.success(Unit)

        override val session: StateFlow<Session?> = sessionFlow
        override val isMfaEnrolled: StateFlow<Boolean> = isMfaEnrolledFlow

        override suspend fun login(email: String, password: String): Result<Unit> = loginResult

        override suspend fun refreshSession(): Result<Unit> = Result.success(Unit)

        override suspend fun logout() {}

        override suspend fun setMfaEnrollment(enrolled: Boolean) {
            isMfaEnrolledFlow.value = enrolled
        }
    }
}
