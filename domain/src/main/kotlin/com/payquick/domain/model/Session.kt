package com.payquick.domain.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Session @OptIn(ExperimentalTime::class) constructor(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val user: User
) {
    @OptIn(ExperimentalTime::class)
    val isExpired: Boolean
        get() = Clock.System.now() >= expiresAt
}
