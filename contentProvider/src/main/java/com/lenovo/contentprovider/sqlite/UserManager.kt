package com.lenovo.contentprovider.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.util.Log
import androidx.core.database.sqlite.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserManager(context: Context) {
    private val dbHelper: DBHelper = DBHelper.getInstance(context)

    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBHelper.COLUMN_USER_NAME, user.name)
            put(DBHelper.COLUMN_USER_AGE, user.age)
            put(DBHelper.COLUMN_USER_EMAIL, user.email)
        }
        db.insert(DBHelper.TABLE_USERS, null, values)
    }

    /**
     * 批量插入：在**同一事务**内逐条执行；单行失败记入 [BatchInsertResult.failedUsers]，
     * 未抛出的情况下成功的行会**一起提交**。外层若整体失败（如无法开启事务），则全部记为失败。
     */
    suspend fun insertUsers(users: List<User>): BatchInsertResult = withContext(Dispatchers.IO) {
        if (users.isEmpty()) {
            return@withContext BatchInsertResult(mutableListOf(), mutableListOf(), 0)
        }
        val db = dbHelper.writableDatabase
        val sql = "INSERT INTO ${DBHelper.TABLE_USERS} (${DBHelper.COLUMN_USER_NAME}, ${DBHelper.COLUMN_USER_AGE}, ${DBHelper.COLUMN_USER_EMAIL}) VALUES (?, ?, ?)"
        try {
            val batchInsertResult = BatchInsertResult(mutableListOf(), mutableListOf(), users.size)
            db.transaction {
                compileStatement(sql).use { statement ->
                    users.forEach { user ->
                        try {
                            statement.clearBindings()
                            statement.bindString(1, user.name ?: "")
                            statement.bindLong(2, user.age.toLong())
                            statement.bindString(3, user.email ?: "")
                            val id = statement.executeInsert()
                            if (id != -1L) {
                                batchInsertResult.successIds.add(id)
                            } else {
                                batchInsertResult.failedUsers.add(user to "返回无效 ID")
                            }
                        } catch (e: SQLException) {
                            batchInsertResult.failedUsers.add(user to (e.message ?: "数据库错误"))
                            Log.e("UserManager", "插入用户失败: ${user.name}", e)
                        }
                    }
                }
                batchInsertResult
            }
        } catch (e: Exception) {
            Log.e("UserManager", "insertUsers failed", e)
            return@withContext BatchInsertResult(
                mutableListOf(),
                users.map { it to (e.message ?: "批量插入失败") }.toMutableList(),
                users.size
            )
        }
    }

    /**
     * 批量插入：**要么全部成功并提交，要么任一步失败则整批回滚**。
     * @return 按 [users] 顺序对应的新行 `_id` 列表
     * @throws SQLException 任一条插入失败（含 [android.database.sqlite.SQLiteStatement.executeInsert] 返回 -1）
     */
    suspend fun insertUsersAllOrNothing(users: List<User>): List<Long> = withContext(Dispatchers.IO) {
        if (users.isEmpty()) return@withContext emptyList()
        val db = dbHelper.writableDatabase
        val sql = "INSERT INTO ${DBHelper.TABLE_USERS} (${DBHelper.COLUMN_USER_NAME}, ${DBHelper.COLUMN_USER_AGE}, ${DBHelper.COLUMN_USER_EMAIL}) VALUES (?, ?, ?)"
        db.transaction {
            val ids = ArrayList<Long>(users.size)
            compileStatement(sql).use { statement ->
                users.forEach { user ->
                    statement.clearBindings()
                    statement.bindString(1, user.name ?: "")
                    statement.bindLong(2, user.age.toLong())
                    statement.bindString(3, user.email ?: "")
                    val id = statement.executeInsert()
                    if (id == -1L) {
                        throw SQLException("executeInsert 返回无效 ID: user=${user.name}")
                    }
                    ids.add(id)
                }
            }
            ids
        }
    }

    suspend fun deleteUser(id: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DBHelper.TABLE_USERS, "${DBHelper.COLUMN_USER_ID}=?", arrayOf(id.toString()))
    }

    suspend fun deleteAllUsers(needCount: Boolean): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        if (needCount) {
            db.delete(DBHelper.TABLE_USERS, "1", null)
        } else {
            db.delete(DBHelper.TABLE_USERS, null, null)
        }
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBHelper.COLUMN_USER_NAME, user.name)
            put(DBHelper.COLUMN_USER_AGE, user.age)
            put(DBHelper.COLUMN_USER_EMAIL, user.email)
        }
        db.update(DBHelper.TABLE_USERS, values, "${DBHelper.COLUMN_USER_ID}=?", arrayOf(user.id.toString()))
    }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DBHelper.TABLE_USERS, null, null, null, null, null, "id ASC")
        val users = mutableListOf<User>()
        cursor.use { cursor ->
            while (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_NAME))
                val age = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_AGE))
                val email = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_EMAIL))
                users.add(User(id, name, age, email))
            }
            users
        }
    }
}

class User(var id: Long, var name: String?, var age: Int, var email: String?)

data class BatchInsertResult(
    val successIds: MutableList<Long>,
    val failedUsers: MutableList<Pair<User, String>>,
    val totalCount: Int
) {
    val successCount: Int = successIds.size
    val failureCount: Int = failedUsers.size
    val isSuccess: Boolean = failureCount == 0
}
