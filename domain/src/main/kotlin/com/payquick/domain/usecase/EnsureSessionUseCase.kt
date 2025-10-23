package com.payquick.domain.usecase

import com.payquick.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Ensures a session is ready before entering the authenticated flow. If the cached session is
 * missing we surface a failure. If it exists but has expired we attempt a refresh using the
 * persisted refresh token.
 */
class EnsureSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val existing = authRepository.session.firstOrNull()
            ?: return Result.failure(IllegalStateException("No session available"))

        if (!existing.isExpired) return Result.success(Unit)

        return authRepository.refreshSession()
    }
}
