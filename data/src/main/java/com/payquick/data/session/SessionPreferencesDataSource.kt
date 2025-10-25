package com.payquick.data.session

import android.content.SharedPreferences
import androidx.core.content.edit
import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Singleton
@OptIn(ExperimentalTime::class)
class SessionPreferencesDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    private object Keys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val EXPIRES_AT = "expires_at"
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val MFA_ENROLLED = "mfa_enrolled"
    }

    private val sessionState = MutableStateFlow(readSession())
    private val mfaState = MutableStateFlow(sharedPreferences.getBoolean(Keys.MFA_ENROLLED, false))

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Keys.ACCESS_TOKEN,
            Keys.REFRESH_TOKEN,
            Keys.EXPIRES_AT,
            Keys.USER_ID,
            Keys.USER_NAME,
            Keys.USER_EMAIL -> sessionState.value = readSession()
            Keys.MFA_ENROLLED -> mfaState.value = sharedPreferences.getBoolean(Keys.MFA_ENROLLED, false)
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    val session: Flow<Session?> = sessionState.asStateFlow()
    val mfaEnrolled: Flow<Boolean> = mfaState.asStateFlow()

    suspend fun saveSession(session: Session) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(Keys.ACCESS_TOKEN, session.accessToken)
                putString(Keys.REFRESH_TOKEN, session.refreshToken)
                putLong(Keys.EXPIRES_AT, session.expiresAt.epochSeconds)
                putString(Keys.USER_ID, session.user.id)
                putString(Keys.USER_NAME, session.user.fullName)
                putString(Keys.USER_EMAIL, session.user.email)
            }
            sessionState.value = session
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                remove(Keys.ACCESS_TOKEN)
                remove(Keys.REFRESH_TOKEN)
                remove(Keys.EXPIRES_AT)
                remove(Keys.USER_ID)
                remove(Keys.USER_NAME)
                remove(Keys.USER_EMAIL)
            }
            sessionState.value = null
        }
    }

    suspend fun setMfaEnrolled(value: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putBoolean(Keys.MFA_ENROLLED, value)
            }
            mfaState.value = value
        }
    }

    private fun readSession(): Session? {
        val accessToken = sharedPreferences.getString(Keys.ACCESS_TOKEN, null)
        val refreshToken = sharedPreferences.getString(Keys.REFRESH_TOKEN, null)
        val expiresAt = sharedPreferences.getLong(Keys.EXPIRES_AT, 0L)
        val userId = sharedPreferences.getString(Keys.USER_ID, null)
        val userName = sharedPreferences.getString(Keys.USER_NAME, null)
        val userEmail = sharedPreferences.getString(Keys.USER_EMAIL, null)

        if (
            accessToken.isNullOrBlank() ||
            refreshToken.isNullOrBlank() ||
            expiresAt == 0L ||
            userId.isNullOrBlank() ||
            userName.isNullOrBlank() ||
            userEmail.isNullOrBlank()
        ) {
            return null
        }

        return Session(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.fromEpochSeconds(expiresAt),
            user = User(
                id = userId,
                fullName = userName,
                email = userEmail
            )
        )
    }
}
