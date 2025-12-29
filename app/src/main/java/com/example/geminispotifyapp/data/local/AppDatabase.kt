package com.example.geminispotifyapp.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.geminispotifyapp.data.remote.model.SpotifyTokenResponse
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
        val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
        val SCOPE_KEY = stringPreferencesKey("scope")

        val SEARCH_SIMILAR_NUM_KEY = intPreferencesKey("search_num")
        val GET_USER_DATA_NUM_KEY = intPreferencesKey("get_item_num")
        val CHECK_MARKET_IF_PLAYABLE_KEY = stringPreferencesKey("check_market_if_playable")

        val SHOW_CASE_SEARCH_NUM_KEY = intPreferencesKey("show_case_search_num")
        val SHOW_CASE_LANGUAGE_KEY = stringPreferencesKey("show_case_language")
        val SHOW_CASE_GENRE_KEY = stringPreferencesKey("show_case_genre")
        val SHOW_CASE_YEAR_KEY = stringPreferencesKey("show_case_year")
        val IS_RANDOM_YEAR_OF_SHOW_CASE_SELECTION = booleanPreferencesKey("is_random_year_of_show_case_selection")

        val LAST_UPDATED_EVERYDAY_RECOMMENDATION_DATE = stringPreferencesKey("last_updated_everyday_recommendation_date")
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

    suspend fun deleteCodeVerifier() {
        dataStore.edit { preferences ->
            preferences.remove(CODE_VERIFIER_KEY)
        }
    }

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

    suspend fun deleteAuthState() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_STATE_KEY)
        }
    }

    // Encrypted Data: Access Token, Refresh Token
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = encryptedPreferenceManager.encrypt(token)
        }
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
            //preferences[TOKEN_TYPE_KEY] = tokenResponse.tokenType
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

    fun getExpiresAtFlow(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[EXPIRES_AT_KEY]
        }
    }

    // Clear all auth data for the current user. (notice some user preference may be added in the future, so don't use it.clear())
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(EXPIRES_AT_KEY)
            preferences.remove(SCOPE_KEY)
            preferences.remove(CODE_VERIFIER_KEY)
            preferences.remove(AUTH_STATE_KEY)
            Log.d("AppDatabase", "All session tokens cleared.")
        }
    }


    // User Settings
    suspend fun saveSearchSimilarNum(searchNum: Int) {
        dataStore.edit { preferences ->
            preferences[SEARCH_SIMILAR_NUM_KEY] = searchNum
        }
    }
    val searchSimilarNumFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SEARCH_SIMILAR_NUM_KEY] ?: 20
    }


    suspend fun saveGetUserDataNum(dataNum: Int) {
        dataStore.edit { preferences ->
            preferences[GET_USER_DATA_NUM_KEY] = dataNum
        }
    }

    val getUserDataNumFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[GET_USER_DATA_NUM_KEY] ?: 20
    }

    suspend fun saveCheckMarketIfPlayableFlow(market: String?) {
        dataStore.edit { preferences ->
            preferences[CHECK_MARKET_IF_PLAYABLE_KEY] = market ?: "TW"
        }
    }

    val checkMarketIfPlayableFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[CHECK_MARKET_IF_PLAYABLE_KEY] ?: "TW"
    }

    // Showcase Search Settings
    val numOfShowCaseSearchFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SHOW_CASE_SEARCH_NUM_KEY] ?: 15
    }

    suspend fun saveNumOfShowCaseSearch(num: Int) {
        dataStore.edit { preferences ->
            preferences[SHOW_CASE_SEARCH_NUM_KEY] = num
        }
    }

    val languageOfShowCaseSearchFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[SHOW_CASE_LANGUAGE_KEY] ?: "English"
    }

    suspend fun saveLanguageOfShowCaseSearch(language: String) {
        dataStore.edit { preferences ->
            preferences[SHOW_CASE_LANGUAGE_KEY] = language
        }
    }

    val genreOfShowCaseSearchFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[SHOW_CASE_GENRE_KEY] ?: "" // Genre is not set by default
    }

    suspend fun saveGenreOfShowCaseSearch(genre: String) {
        dataStore.edit { preferences ->
            preferences[SHOW_CASE_GENRE_KEY] = genre
        }
    }

    val yearOfShowCaseSearchFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[SHOW_CASE_YEAR_KEY] ?: "2015"
    }

    suspend fun saveYearOfShowCaseSearch(year: String) {
        dataStore.edit { preferences ->
            preferences[SHOW_CASE_YEAR_KEY] = year
        }
    }

    val isRandomYearOfShowCaseSelectionFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_RANDOM_YEAR_OF_SHOW_CASE_SELECTION] ?: false
    }

    suspend fun saveIsRandomYearOfShowCaseSelection(isRandom: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_RANDOM_YEAR_OF_SHOW_CASE_SELECTION] = isRandom
        }
    }

    val lastUpdatedEverydayRecommendationDateFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_UPDATED_EVERYDAY_RECOMMENDATION_DATE] ?: ""
    }

    suspend fun saveLastUpdatedEverydayRecommendationDate(date: String) {
        dataStore.edit { preferences ->
            preferences[LAST_UPDATED_EVERYDAY_RECOMMENDATION_DATE] = date
        }
    }
}
