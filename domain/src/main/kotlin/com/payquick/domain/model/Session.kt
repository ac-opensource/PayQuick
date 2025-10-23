package com.payquick.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val user: User
) {
    val isExpired: Boolean
        get() = Clock.System.now() >= expiresAt
}
