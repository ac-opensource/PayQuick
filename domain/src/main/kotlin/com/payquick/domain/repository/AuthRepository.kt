package com.payquick.domain.repository

import com.payquick.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun logout()
}
