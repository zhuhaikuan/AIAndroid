package com.lenovo.contentprovider

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * 监听 [CustomContentProvider] 对应 URI 上的 [android.content.ContentResolver.notifyChange]。
 *
 * @param handler 决定 [onChange] 在哪个线程执行；默认主线程，便于更新 UI。
 * @param onContentChange `selfChange` 为 true 时表示 URI 自身元数据变化；`uri` 为触发通知的 URI（可能为 null）。
 */
class CustomContentObserver(
    handler: Handler = Handler(Looper.getMainLooper()),
    private val onContentChange: (selfChange: Boolean, uri: Uri?) -> Unit,
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        onContentChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        onContentChange(selfChange, uri)
    }
}
