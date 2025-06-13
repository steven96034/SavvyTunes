package com.example.geminispotifyapp.data.local

import android.util.Log
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
import javax.inject.Singleton

@Singleton
class AppDatabase @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPreferenceManager: EncryptedPreferenceManager
) {
    companion object {
        val CODE_VERIFIER_KEY = stringPreferencesKey("encrypted_code_verifier")
        val AUTH_STATE_KEY = stringPreferencesKey("encrypted_auth_state")

        val ACCESS_TOKEN_KEY = stringPreferencesKey("encrypted_access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("encrypted_refresh_token")
        val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
        val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
        val SCOPE_KEY = stringPreferencesKey("scope")
    }

    suspend fun saveCodeVerifier(codeVerifier: String) {
        dataStore.edit { preferences ->
            preferences[CODE_VERIFIER_KEY] = encryptedPreferenceManager.encrypt(codeVerifier)
        }
    }

    suspend fun getCodeVerifier(): String? {
        return dataStore.data.map { preferences ->
            preferences[CODE_VERIFIER_KEY]?.let { encryptedPreferenceManager.decrypt(it) }
        }.first()
    }

//    suspend fun clearCodeVerifier() {
//        dataStore.edit { preferences ->
//            preferences.remove(CODE_VERIFIER_KEY)
//        }
//    }

    suspend fun saveAuthState(state: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_STATE_KEY] = encryptedPreferenceManager.encrypt(state)
        }
    }

    suspend fun getAuthState(): String? {
        return dataStore.data.map { preferences ->
            preferences[AUTH_STATE_KEY]?.let { encryptedPreferenceManager.decrypt(it) }
        }.first()
    }

//    suspend fun clearAuthState() {
//        dataStore.edit { preferences ->
//            preferences.remove(AUTH_STATE_KEY)
//        }
//    }

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

    // TODO: Test if encrypted refresh token is working.
    suspend fun saveRefreshToken(token: String?) {
        if (!token.isNullOrBlank()) {
            dataStore.edit { preferences ->
                preferences[REFRESH_TOKEN_KEY] = token//encryptedPreferenceManager.encrypt(token)
            }
        }
    }

    suspend fun getRefreshToken(): String? {
        return try {
            dataStore.data.map { preferences ->
                preferences[REFRESH_TOKEN_KEY]//?.let { encryptedPreferenceManager.decrypt(it) }
            }.first()
        } catch (e: Exception) {
            Log.e("AppDatabase", "Error getting refresh token", e)
            null
        }
    }

    suspend fun saveTokenResponse(tokenResponse: SpotifyTokenResponse) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = encryptedPreferenceManager.encrypt(tokenResponse.accessToken)
            preferences[TOKEN_TYPE_KEY] = tokenResponse.tokenType
            preferences[EXPIRES_AT_KEY] = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
            preferences[SCOPE_KEY] = tokenResponse.scope ?: ""
            if (!tokenResponse.refreshToken.isNullOrBlank()) {
                preferences[REFRESH_TOKEN_KEY] = tokenResponse.refreshToken//encryptedPreferenceManager.encrypt(tokenResponse.refreshToken)
                Log.d("AppDatabase", "New refresh token saved.")
            }
            else Log.d("AppDatabase", "No new refresh token in the response. Existing refresh token (if any) will be kept.")
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
            // Handle the case where tokenType or accessToken is null
            null
        }
    }
}