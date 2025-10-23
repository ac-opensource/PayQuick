package com.payquick.domain.repository

import com.payquick.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>
    val isMfaEnrolled: Flow<Boolean>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun refreshSession(): Result<Unit>

    suspend fun logout()

    suspend fun setMfaEnrollment(enrolled: Boolean)
}
