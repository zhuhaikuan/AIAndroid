package com.lenovo.contentprovider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.lenovo.contentprovider.sqlite.DBHelper

/**
 * 对 [CustomContentProvider] 的封装：通过 [ContentResolver] 完成 `users` 表的增删改查。
 *
 * URI 与 [CustomContentProvider] 保持一致，见 [usersContentUri]。
 */
class CustomContentResolver(private val context: Context) {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    /** 与 [CustomContentProvider.CONTENT_URI]、[USERS_CONTENT_URI] 相同 */
    val usersContentUri: Uri = USERS_CONTENT_URI

    fun userRowUri(id: Long): Uri = ContentUris.withAppendedId(usersContentUri, id)

    /**
     * 插入一行用户；成功返回带 `_id` 的行 URI（与 Provider 约定一致）。
     */
    fun insertUser(name: String?, email: String?, age: Int?): Uri? {
        val values = ContentValues().apply {
            put(DBHelper.COLUMN_USER_NAME, name)
            put(DBHelper.COLUMN_USER_EMAIL, email)
            if (age != null) put(DBHelper.COLUMN_USER_AGE, age)
        }
        return contentResolver.insert(usersContentUri, values)
    }

    fun updateUser(id: Long, name: String?, email: String?, age: Int?): Int {
        val values = ContentValues().apply {
            put(DBHelper.COLUMN_USER_NAME, name)
            put(DBHelper.COLUMN_USER_EMAIL, email)
            if (age != null) put(DBHelper.COLUMN_USER_AGE, age)
        }
        return contentResolver.update(userRowUri(id), values, null, null)
    }

    /**
     * 按集合 URI 条件更新；[selection] / [selectionArgs] 语义同 [ContentResolver.update]。
     */
    fun updateUsers(
        values: ContentValues,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = contentResolver.update(usersContentUri, values, selection, selectionArgs)

    fun deleteUser(id: Long): Int =
        contentResolver.delete(userRowUri(id), null, null)

    fun deleteUsers(selection: String?, selectionArgs: Array<String>?): Int =
        contentResolver.delete(usersContentUri, selection, selectionArgs)

    fun queryUsers(
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): Cursor? = contentResolver.query(
        usersContentUri,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )

    fun queryUserById(id: Long): Cursor? =
        contentResolver.query(userRowUri(id), null, null, null, null)

    fun getType(uri: Uri): String? = contentResolver.getType(uri)

    /**
     * 读取游标为列表（调用方负责关闭 [cursor]）。
     */
    fun readUsers(cursor: Cursor?): List<UserRow> {
        if (cursor == null) return emptyList()
        return buildList {
            while (cursor.moveToNext()) {
                add(cursor.readUserRow())
            }
        }
    }

    fun queryAllUsers(sortOrder: String? = null): List<UserRow> =
        queryUsers(sortOrder = sortOrder)?.use { readUsers(it) } ?: emptyList()

    fun queryUserByIdAsRow(id: Long): UserRow? =
        queryUserById(id)?.use { c ->
            if (!c.moveToFirst()) null else c.readUserRow()
        }

    data class UserRow(
        val id: Long,
        val name: String?,
        val email: String?,
        val age: Int?
    )

    companion object {
        /** 与 [CustomContentProvider.CONTENT_URI] 相同；无需 [Context] 即可在客户端引用 */
        val USERS_CONTENT_URI: Uri = CustomContentProvider.CONTENT_URI
    }
}

private fun Cursor.readUserRow(): CustomContentResolver.UserRow {
    val idIdx = getColumnIndexOrThrow(DBHelper.COLUMN_USER_ID)
    val nameIdx = getColumnIndex(DBHelper.COLUMN_USER_NAME)
    val emailIdx = getColumnIndex(DBHelper.COLUMN_USER_EMAIL)
    val ageIdx = getColumnIndex(DBHelper.COLUMN_USER_AGE)
    return CustomContentResolver.UserRow(
        id = getLong(idIdx),
        name = if (nameIdx >= 0 && !isNull(nameIdx)) getString(nameIdx) else null,
        email = if (emailIdx >= 0 && !isNull(emailIdx)) getString(emailIdx) else null,
        age = if (ageIdx >= 0 && !isNull(ageIdx)) getInt(ageIdx) else null
    )
}
