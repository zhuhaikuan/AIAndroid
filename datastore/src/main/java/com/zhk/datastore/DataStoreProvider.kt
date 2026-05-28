package com.zhk.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.zhk.datastore.model.ProtoUserSettings
import com.zhk.datastore.proto.UserSettingsSerializer

private const val PREFERENCES_DATASTORE_NAME = "app_preferences"
private const val PROTO_DATASTORE_FILE_NAME = "user_settings.pb"

/**
 * 统一维护 DataStore 实例，避免重复创建造成额外 I/O 和并发问题。
 */
object DataStoreProvider {
    fun createPreferencesDataStore(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_DATASTORE_NAME) },
        )
    }

    fun createProtoDataStore(context: Context): DataStore<ProtoUserSettings> {
        return DataStoreFactory.create(
            serializer = UserSettingsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { ProtoUserSettings() },
            produceFile = { context.dataDir.resolve(PROTO_DATASTORE_FILE_NAME) },
        )
    }

    /**
     * 对业务层暴露的统一入口，避免上层直接依赖 DataStore 底层类型。
     */
    fun createRepository(context: Context): DataStoreContract {
        val appContext = context.applicationContext
        return DataStoreRepository(
            preferencesDataStore = createPreferencesDataStore(appContext),
            protoDataStore = createProtoDataStore(appContext),
        )
    }
}
