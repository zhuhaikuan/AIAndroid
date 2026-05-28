package com.zhk.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.zhk.datastore.model.AppPreferences
import com.zhk.datastore.model.ProtoUserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private object PreferenceKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    val LAUNCH_COUNT = intPreferencesKey("launch_count")
    val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
}

/**
 * 对外提供统一的数据读写入口：
 * - Preferences DataStore：轻量级 KV 配置
 * - 强类型 DataStore：结构化对象（可替换为 Proto Serializer）
 */
class DataStoreRepository(
    private val preferencesDataStore: DataStore<Preferences>,
    private val protoDataStore: DataStore<ProtoUserSettings>,
) : DataStoreContract {
    // ---------------------- Preferences DataStore（CRUD） ----------------------
    override val appPreferencesFlow: Flow<AppPreferences> = preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                userId = preferences[PreferenceKeys.USER_ID],
                darkModeEnabled = preferences[PreferenceKeys.DARK_MODE_ENABLED] ?: false,
                launchCount = preferences[PreferenceKeys.LAUNCH_COUNT] ?: 0,
                searchHistory = preferences[PreferenceKeys.SEARCH_HISTORY] ?: emptySet(),
                lastLoginAt = preferences[PreferenceKeys.LAST_LOGIN_AT] ?: 0L,
            )
        }

    override suspend fun saveUserId(userId: String) {
        preferencesDataStore.edit { prefs ->
            prefs[PreferenceKeys.USER_ID] = userId
        }
    }

    override suspend fun removeUserId() {
        preferencesDataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.USER_ID)
        }
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        preferencesDataStore.edit { prefs ->
            prefs[PreferenceKeys.DARK_MODE_ENABLED] = enabled
        }
    }

    override suspend fun incrementLaunchCount() {
        preferencesDataStore.edit { prefs ->
            val current = prefs[PreferenceKeys.LAUNCH_COUNT] ?: 0
            prefs[PreferenceKeys.LAUNCH_COUNT] = current + 1
        }
    }

    override suspend fun addSearchKeyword(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return
        preferencesDataStore.edit { prefs ->
            val history = (prefs[PreferenceKeys.SEARCH_HISTORY] ?: emptySet()).toMutableSet()
            history.add(normalized)
            prefs[PreferenceKeys.SEARCH_HISTORY] = history
        }
    }

    override suspend fun removeSearchKeyword(keyword: String) {
        preferencesDataStore.edit { prefs ->
            val history = (prefs[PreferenceKeys.SEARCH_HISTORY] ?: emptySet()).toMutableSet()
            history.remove(keyword.trim())
            prefs[PreferenceKeys.SEARCH_HISTORY] = history
        }
    }

    override suspend fun updateLastLoginAt(timestampMs: Long) {
        preferencesDataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_LOGIN_AT] = timestampMs
        }
    }

    override suspend fun clearPreferences() {
        preferencesDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // ---------------------- 强类型 DataStore（CRUD） ----------------------
    override val protoUserSettingsFlow: Flow<ProtoUserSettings> = protoDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(ProtoUserSettings())
            } else {
                throw exception
            }
        }

    override suspend fun upsertProtoUserSettings(model: ProtoUserSettings) {
        protoDataStore.updateData { _ -> model }
    }

    override suspend fun updateDisplayName(displayName: String) {
        protoDataStore.updateData { current ->
            current.copy(
                displayName = displayName,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        protoDataStore.updateData { current ->
            current.copy(
                notificationsEnabled = enabled,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun updateFontScalePercent(percent: Int) {
        val safePercent = percent.coerceIn(80, 200)
        protoDataStore.updateData { current ->
            current.copy(
                fontScalePercent = safePercent,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun addFavoriteCategory(category: String) {
        val normalized = category.trim()
        if (normalized.isEmpty()) return
        protoDataStore.updateData { current ->
            val updatedCategories = if (current.favoriteCategories.contains(normalized)) {
                current.favoriteCategories
            } else {
                current.favoriteCategories + normalized
            }
            current.copy(
                favoriteCategories = updatedCategories,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun removeFavoriteCategory(category: String) {
        val normalized = category.trim()
        protoDataStore.updateData { current ->
            current.copy(
                favoriteCategories = current.favoriteCategories.filterNot { it == normalized },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun clearProtoSettings() {
        protoDataStore.updateData {
            ProtoUserSettings()
        }
    }
}
