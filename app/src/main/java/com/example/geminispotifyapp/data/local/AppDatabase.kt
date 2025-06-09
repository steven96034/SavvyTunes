package com.example.geminispotifyapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.geminispotifyapp.EncryptedPreferenceManager
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppDatabase @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPreferenceManager: EncryptedPreferenceManager
) {
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
        val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
        val SCOPE_KEY = stringPreferencesKey("scope")
    }

    // Encrypted Data: Access Token, Refresh Token
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = encryptedPreferenceManager.encrypt(token)
        }
    }

    private suspend fun getAccessToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]?.let { encryptedPreferenceManager.decrypt(it) }
        }.first()
        //return encryptedPreferenceManager.readData(ACCESS_TOKEN_KEY).first()
    }

    fun getAccessTokenFlow(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]?.let { encryptedPreferenceManager.decrypt(it) }
        }
    }

    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = encryptedPreferenceManager.encrypt(token)
        }
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]?.let { encryptedPreferenceManager.decrypt(it) }
        }.first()
    }

    suspend fun saveTokenResponse(tokenResponse: SpotifyTokenResponse) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = encryptedPreferenceManager.encrypt(tokenResponse.accessToken)
            preferences[REFRESH_TOKEN_KEY] = encryptedPreferenceManager.encrypt(tokenResponse.refreshToken ?: "")
            preferences[TOKEN_TYPE_KEY] = tokenResponse.tokenType
            preferences[EXPIRES_AT_KEY] = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
            preferences[SCOPE_KEY] = tokenResponse.scope ?: ""
        }
    }

    // Non-Encrypted Data: Token Type, Expires At, Scope
    suspend fun saveExpiresAt(expiresAt: Long) {
        dataStore.edit { preferences ->
            preferences[EXPIRES_AT_KEY] = expiresAt
        }
    }

    // Always return "Bearer"
    private suspend fun getTokenType(): String? {
        return dataStore.data.map { preferences ->
            preferences[TOKEN_TYPE_KEY]// ?: "Bearer"
        }.first()
    }

    // Check if the token has expired
    suspend fun isTokenExpired(): Boolean {
        val expiresAt = dataStore.data.map { preferences ->
            preferences[EXPIRES_AT_KEY] ?: 0L
        }.first()
        return System.currentTimeMillis() > expiresAt
    }

    suspend fun getAuthorizationHeader(): String? {
        val tokenType = getTokenType()
        val accessToken = getAccessToken()
        return if (tokenType != null && accessToken != null) {
            "$tokenType $accessToken"
        } else {
            null // 或拋出一個自訂例外，表示 token 不可用
        }
    }
}