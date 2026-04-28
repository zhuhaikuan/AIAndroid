package com.zhk.aiandroid

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"

        // 用于存储最近的通知列表
        val recentNotifications = mutableListOf<NotificationInfo>()

        // 回调接口
        var onNotificationUpdate: ((List<NotificationInfo>) -> Unit)? = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { sbn -> onNotificationPosted(sbn) }
    }

    data class NotificationInfo(
        val packageName: String,
        val appName: String?,
        val title: String?,
        val text: String?,
        val postTime: Long,
        val id: Int
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val info = extractNotificationInfo(it)
            Log.d(TAG, "📬 新通知: ${info.title} from ${info.packageName}")

            // 添加到列表
            synchronized(recentNotifications) {
                recentNotifications.add(info)
                // 只保留最近50条
                if (recentNotifications.size > 50) {
                    recentNotifications.removeAt(0)
                }
            }

            // 通知 UI 更新
            onNotificationUpdate?.invoke(getRecentNotifications())
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            Log.d(TAG, "🗑️ 通知移除: ${it.packageName}")

            synchronized(recentNotifications) {
                recentNotifications.removeAll { info -> info.id == it.id && info.packageName == it.packageName }
            }

            onNotificationUpdate?.invoke(getRecentNotifications())
        }
    }

    private fun extractNotificationInfo(sbn: StatusBarNotification): NotificationInfo {
        val notification = sbn.notification
        val extras = notification.extras

        return NotificationInfo(
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            title = extras.getString(android.app.Notification.EXTRA_TITLE),
            text = extras.getString(android.app.Notification.EXTRA_TEXT),
            postTime = sbn.postTime,
            id = sbn.id
        )
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun getRecentNotifications(): List<NotificationInfo> {
        return synchronized(recentNotifications) {
            recentNotifications.toList()
        }
    }

    fun clearNotifications() {
        synchronized(recentNotifications) {
            recentNotifications.clear()
        }
        onNotificationUpdate?.invoke(emptyList())
    }
}