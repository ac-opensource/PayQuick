package com.payquick.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payquick.domain.model.Session
import com.payquick.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

@Singleton
class SessionPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }

    val session: Flow<Session?> = dataStore.data.map { preferences ->
        val accessToken = preferences[Keys.ACCESS_TOKEN]
        val refreshToken = preferences[Keys.REFRESH_TOKEN]
        val expiresAt = preferences[Keys.EXPIRES_AT]
        val userId = preferences[Keys.USER_ID]
        val userName = preferences[Keys.USER_NAME]
        val userEmail = preferences[Keys.USER_EMAIL]

        if (
            accessToken.isNullOrBlank() ||
            refreshToken.isNullOrBlank() ||
            expiresAt == null ||
            userId.isNullOrBlank() ||
            userName.isNullOrBlank() ||
            userEmail.isNullOrBlank()
        ) {
            null
        } else {
            Session(
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

    suspend fun saveSession(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.EXPIRES_AT] = session.expiresAt.epochSeconds
            prefs[Keys.USER_ID] = session.user.id
            prefs[Keys.USER_NAME] = session.user.fullName
            prefs[Keys.USER_EMAIL] = session.user.email
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
