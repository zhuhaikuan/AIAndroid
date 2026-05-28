package com.zhk.datastore

import com.zhk.datastore.model.AppPreferences
import com.zhk.datastore.model.ProtoUserSettings
import kotlinx.coroutines.flow.Flow

/**
 * 面向业务层的数据访问抽象，避免暴露底层 DataStore 细节。
 */
interface DataStoreContract {
    val appPreferencesFlow: Flow<AppPreferences>
    val protoUserSettingsFlow: Flow<ProtoUserSettings>

    suspend fun saveUserId(userId: String)
    suspend fun removeUserId()
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun incrementLaunchCount()
    suspend fun addSearchKeyword(keyword: String)
    suspend fun removeSearchKeyword(keyword: String)
    suspend fun updateLastLoginAt(timestampMs: Long)
    suspend fun clearPreferences()

    suspend fun upsertProtoUserSettings(model: ProtoUserSettings)
    suspend fun updateDisplayName(displayName: String)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateFontScalePercent(percent: Int)
    suspend fun addFavoriteCategory(category: String)
    suspend fun removeFavoriteCategory(category: String)
    suspend fun clearProtoSettings()
}
