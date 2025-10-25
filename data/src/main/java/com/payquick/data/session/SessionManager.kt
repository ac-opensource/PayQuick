package com.payquick.data.session

import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalTime::class)
class SessionManager @Inject constructor(
    private val preferences: SessionPreferencesDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val session: StateFlow<Session?> = preferences.session.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val isMfaEnrolled: StateFlow<Boolean> = preferences.mfaEnrolled.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    suspend fun saveSession(session: Session) {
        preferences.saveSession(session)
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        user: SessionUserSnapshot
    ) {
        val expiresAt = Clock.System.now().plus(expiresInSeconds.seconds)
        val session = Session(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            user = user.toUser()
        )
        saveSession(session)
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        val current = session.value ?: return
        val expiresAt = Clock.System.now().plus(expiresInSeconds.seconds)
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
        saveSession(updated)
    }

    suspend fun clear() {
        preferences.clear()
    }

    suspend fun setMfaEnrolled(value: Boolean) {
        preferences.setMfaEnrolled(value)
    }

    data class SessionUserSnapshot(
        val id: String,
        val fullName: String,
        val email: String
    )

    private fun SessionUserSnapshot.toUser(): User = User(
        id = id,
        fullName = fullName,
        email = email
    )
}
