package com.payquick.domain.usecase

import com.payquick.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Convenience use case that ensures a session exists by performing a login with the supplied
 * credentials when one is not already available. Consumers may provide empty credentials to skip
 * the fallback behaviour.
 */
class EnsureSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val existing = authRepository.session.firstOrNull()
        if (existing != null) return Result.success(Unit)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalStateException("No session and no credentials provided"))
        }
        return authRepository.login(email, password)
    }
}
