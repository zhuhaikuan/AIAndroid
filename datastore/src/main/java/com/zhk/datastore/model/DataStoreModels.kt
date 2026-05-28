package com.zhk.datastore.model

/**
 * Preferences DataStore 的领域模型（适合轻量 KV 配置）。
 */
data class AppPreferences(
    val userId: String? = null,
    val darkModeEnabled: Boolean = false,
    val launchCount: Int = 0,
    val searchHistory: Set<String> = emptySet(),
    val lastLoginAt: Long = 0L,
)

/**
 * 强类型 DataStore 的领域模型（可对应 Proto/自定义二进制序列化）。
 */
data class ProtoUserSettings(
    val userId: String = "",
    val displayName: String = "",
    val notificationsEnabled: Boolean = true,
    val fontScalePercent: Int = 100,
    val updatedAt: Long = 0L,
    val favoriteCategories: List<String> = emptyList(),
)
