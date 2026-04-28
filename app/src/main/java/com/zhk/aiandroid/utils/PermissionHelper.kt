package com.zhk.aiandroid.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun hasPermission(context: Context, permission: String): Boolean { // 检查是否具有指定权限
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean { // 检查是否具有所有权限
        if (permissions.isEmpty()) return true

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(activity: Activity, requestCode: Int, vararg permissions: String) {
        if (permissions.isEmpty()) return

        val missingPermissions = permissions.filter { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) return

        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), requestCode)

    }

    fun isAllPermissionsGranted(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) return false

        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false
        }

        return true
    }
}