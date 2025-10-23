package com.payquick.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.delay

class SubmitMockTransferUseCase @Inject constructor() {
    suspend operator fun invoke(amount: BigDecimal, recipient: String, note: String?): Result<Unit> {
        if (amount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (recipient.isBlank()) {
            return Result.failure(IllegalArgumentException("Select a recipient"))
        }

        delay(600)
        return Result.success(Unit)
    }
}
