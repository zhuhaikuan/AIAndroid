package com.zhk.datastore

import android.content.Context
import com.zhk.datastore.model.ProtoUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 业务侧调用示例：
 * - 演示 Preferences DataStore + 强类型 DataStore 两种模式
 * - 演示完整的增删改查流程
 */
class DataStoreUsageExample(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository = DataStoreRepository(
        preferencesDataStore = DataStoreProvider.createPreferencesDataStore(context),
        protoDataStore = DataStoreProvider.createProtoDataStore(context),
    )

    fun demoCreateAndUpdate() {
        scope.launch {
            // Preferences DataStore: 新增/更新
            repository.saveUserId("u_1001")
            repository.updateDarkMode(enabled = true)
            repository.incrementLaunchCount()
            repository.addSearchKeyword("android datastore")
            repository.updateLastLoginAt(System.currentTimeMillis())

            // 强类型 DataStore: 新增/整体写入
            repository.upsertProtoUserSettings(
                ProtoUserSettings(
                    userId = "u_1001",
                    displayName = "ZHK",
                    notificationsEnabled = true,
                    fontScalePercent = 110,
                    favoriteCategories = listOf("Android", "Kotlin"),
                ),
            )
            // 强类型 DataStore: 局部更新
            repository.updateDisplayName("ZHK-Learning")
            repository.addFavoriteCategory("Jetpack")
        }
    }

    fun demoRead(observer: (String) -> Unit) {
        // 读取 Preferences + Proto，并在业务层进行合并展示
        combine(
            repository.appPreferencesFlow,
            repository.protoUserSettingsFlow,
        ) { preferences, proto ->
            "userId=${preferences.userId}, darkMode=${preferences.darkModeEnabled}, " +
                "launchCount=${preferences.launchCount}, displayName=${proto.displayName}, " +
                "favorites=${proto.favoriteCategories}"
        }
            .onEach(observer)
            .launchIn(scope)
    }

    fun demoDeleteAndClear() {
        scope.launch {
            // Preferences DataStore: 删除字段 + 清空
            repository.removeUserId()
            repository.removeSearchKeyword("android datastore")
            repository.clearPreferences()

            // 强类型 DataStore: 删除元素 + 清空
            repository.removeFavoriteCategory("Jetpack")
            repository.clearProtoSettings()
        }
    }
}
