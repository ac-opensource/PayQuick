package com.payquick.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.delay

class SubmitMockTransferUseCase @Inject constructor() {
    suspend operator fun invoke(amount: Double, recipient: String, note: String?): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (recipient.isBlank()) {
            return Result.failure(IllegalArgumentException("Select a recipient"))
        }

        delay(600)
        return Result.success(Unit)
    }
}
