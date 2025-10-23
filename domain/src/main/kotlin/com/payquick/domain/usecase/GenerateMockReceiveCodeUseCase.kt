package com.payquick.domain.usecase

import javax.inject.Inject
import kotlin.random.Random

class GenerateMockReceiveCodeUseCase @Inject constructor() {
    operator fun invoke(): String {
        val digits = List(6) { Random.nextInt(0, 10) }.joinToString(separator = "")
        return "PQ-$digits"
    }
}
