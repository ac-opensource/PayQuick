package com.payquick.domain.usecase

import com.payquick.domain.model.Session
import com.payquick.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Session?> = authRepository.session
}
