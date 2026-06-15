package com.lenovo.contentprovider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.lenovo.contentprovider.sqlite.DBHelper
import androidx.core.net.toUri

/**
 * 通过 ContentProvider 暴露 [DBHelper] 中的 `users` 表。
 *
 * - 集合：`content://com.lenovo.contentprovider.customcontentprovider/users`
 * - 单行：`content://.../users/{_id}`
 */
class CustomContentProvider : ContentProvider() {

    private var dbHelper: DBHelper? = null

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        dbHelper = DBHelper.getInstance(ctx)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val helper = dbHelper ?: return null
        val db = helper.readableDatabase
        val (sel, args) = when (URI_MATCHER.match(uri)) {
            USERS -> selection to selectionArgs
            USER_ID -> combineIdWithSelection(ContentUris.parseId(uri), selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        val cursor = db.query(
            DBHelper.TABLE_USERS,
            projection,
            sel,
            args,
            null,
            null,
            sortOrder
        )
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (URI_MATCHER.match(uri) != USERS) {
            throw IllegalArgumentException("Insert not supported for URI: $uri")
        }
        val helper = dbHelper ?: return null
        val id = helper.writableDatabase.insert(DBHelper.TABLE_USERS, null, values)
        if (id == -1L) return null
        val rowUri = ContentUris.withAppendedId(CONTENT_URI, id)
        // 只 notify 一次：同时 notify 行 URI 与集合 URI 会让 registerContentObserver(集合, notifyDescendants=true) 收到两次回调
        context?.contentResolver?.notifyChange(rowUri, null)
        return rowUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val helper = dbHelper ?: return 0
        val db = helper.writableDatabase
        val (sel, args) = when (URI_MATCHER.match(uri)) {
            USERS -> selection to selectionArgs
            USER_ID -> combineIdWithSelection(ContentUris.parseId(uri), selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        val count = db.update(DBHelper.TABLE_USERS, values, sel, args)
        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val helper = dbHelper ?: return 0
        val db = helper.writableDatabase
        val (sel, args) = when (URI_MATCHER.match(uri)) {
            USERS -> selection to selectionArgs
            USER_ID -> combineIdWithSelection(ContentUris.parseId(uri), selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        val count = db.delete(DBHelper.TABLE_USERS, sel, args)
        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (URI_MATCHER.match(uri)) {
            USERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.users"
            USER_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.users"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    companion object {
        const val AUTHORITY = "com.lenovo.contentprovider.customcontentprovider"
        private const val PATH_USERS = "users"

        /** 对外访问 `users` 表的 content URI */
        val CONTENT_URI: Uri = "content://$AUTHORITY/$PATH_USERS".toUri()

        private const val USERS = 1
        private const val USER_ID = 2

        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_USERS, USERS)
            addURI(AUTHORITY, "$PATH_USERS/#", USER_ID)
        }

        private fun combineIdWithSelection(
            id: Long,
            selection: String?,
            selectionArgs: Array<String>?
        ): Pair<String, Array<String>?> {
            val idClause = "${DBHelper.COLUMN_USER_ID} = ?"
            val idOnly = arrayOf(id.toString())
            return when {
                selection.isNullOrEmpty() -> idClause to idOnly
                else -> {
                    val mergedArgs = idOnly + (selectionArgs ?: emptyArray())
                    "($idClause) AND ($selection)" to mergedArgs
                }
            }
        }
    }
}
