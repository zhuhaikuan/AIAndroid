package com.zhk.aiandroid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhk.datastore.DataStoreContract
import com.zhk.datastore.DataStoreProvider
import com.zhk.datastore.model.AppPreferences
import com.zhk.datastore.model.ProtoUserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DataStoreDemoUiState(
    val appPreferences: AppPreferences = AppPreferences(),
    val userSettings: ProtoUserSettings = ProtoUserSettings(),
    val statusMessage: String = "就绪",
)

class DataStoreDemoViewModel(
    private val repository: DataStoreContract,
) : ViewModel() {
    private val statusFlow = MutableStateFlow("就绪")

    val uiState: StateFlow<DataStoreDemoUiState> = combine(
        repository.appPreferencesFlow,
        repository.protoUserSettingsFlow,
        statusFlow,
    ) { appPreferences, userSettings, status ->
        DataStoreDemoUiState(
            appPreferences = appPreferences,
            userSettings = userSettings,
            statusMessage = status,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DataStoreDemoUiState(),
    )

    fun writeDemoData() {
        viewModelScope.launch {
            repository.saveUserId("demo_1001")
            repository.updateDarkMode(enabled = true)
            repository.incrementLaunchCount()
            repository.addSearchKeyword("datastore")
            repository.updateLastLoginAt(System.currentTimeMillis())

            repository.upsertProtoUserSettings(
                ProtoUserSettings(
                    userId = "demo_1001",
                    displayName = "DemoUser",
                    notificationsEnabled = true,
                    fontScalePercent = 110,
                    favoriteCategories = listOf("Android", "Compose"),
                ),
            )
            statusFlow.value = "已写入示例数据"
        }
    }

    fun addFavoriteCategory(category: String = "Jetpack") {
        viewModelScope.launch {
            repository.addFavoriteCategory(category)
            statusFlow.value = "已新增分类: $category"
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearPreferences()
            repository.clearProtoSettings()
            statusFlow.value = "已清空所有 DataStore 数据"
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repository = DataStoreProvider.createRepository(context)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DataStoreDemoViewModel::class.java)) {
                        return DataStoreDemoViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
