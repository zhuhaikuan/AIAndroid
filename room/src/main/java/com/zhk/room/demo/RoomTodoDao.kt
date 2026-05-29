package com.zhk.room.demo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO：覆盖最常见操作。
 *
 * 关键实践：
 * - 查询列表用 Flow，让 UI 自动响应数据库变更。
 * - 批量写操作或“先删后插”必须放进 @Transaction，避免中间状态泄露给业务层。
 * - 不要把复杂拼接 SQL 暴露给 UI 层，统一封装在 DAO/Repository 内。
 */
@Dao
interface RoomTodoDao {

    // region Insert / Upsert
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserIfAbsent(user: RoomUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceUser(user: RoomUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<RoomTodoEntity>): List<Long>
    // endregion

    // region Update / Delete
    @Update
    suspend fun updateTodo(todo: RoomTodoEntity): Int

    @Delete
    suspend fun deleteTodo(todo: RoomTodoEntity): Int

    @Query("DELETE FROM room_todo WHERE todo_id = :todoId")
    suspend fun deleteTodoById(todoId: Long): Int

    @Query("DELETE FROM room_todo WHERE done = 1")
    suspend fun deleteCompletedTodos(): Int

    @Query("DELETE FROM room_todo")
    suspend fun clearTodos(): Int
    // endregion

    // region Query
    @Query("SELECT * FROM room_todo ORDER BY updated_at DESC")
    fun observeAllTodos(): Flow<List<RoomTodoEntity>>

    @Query("SELECT * FROM room_todo WHERE todo_id = :todoId LIMIT 1")
    suspend fun queryTodoById(todoId: Long): RoomTodoEntity?

    @Query("SELECT * FROM room_todo WHERE title LIKE '%' || :keyword || '%' ORDER BY updated_at DESC")
    fun observeByKeyword(keyword: String): Flow<List<RoomTodoEntity>>

    @Query("SELECT COUNT(*) FROM room_todo")
    fun observeTodoCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM room_todo WHERE title = :title LIMIT 1)")
    suspend fun existsByTitle(title: String): Boolean

    @Transaction
    @Query("SELECT * FROM room_user WHERE user_id = :userId LIMIT 1")
    suspend fun queryUserWithTodos(userId: String): RoomUserWithTodos?
    // endregion

    // region RawQuery
    @RawQuery(observedEntities = [RoomTodoEntity::class])
    fun observeByRawQuery(query: SupportSQLiteQuery): Flow<List<RoomTodoEntity>>

    fun observePriorityAtLeast(priority: RoomPriority): Flow<List<RoomTodoEntity>> {
        val sql = when (priority) {
            RoomPriority.LOW -> "SELECT * FROM room_todo ORDER BY updated_at DESC"
            RoomPriority.MEDIUM ->
                "SELECT * FROM room_todo WHERE priority IN ('MEDIUM','HIGH') ORDER BY updated_at DESC"
            RoomPriority.HIGH ->
                "SELECT * FROM room_todo WHERE priority = 'HIGH' ORDER BY updated_at DESC"
        }
        return observeByRawQuery(SimpleSQLiteQuery(sql))
    }
    // endregion

    // region Transaction
    @Transaction
    suspend fun replaceAllTodos(todos: List<RoomTodoEntity>): List<Long> {
        clearTodos()
        return insertTodos(todos)
    }
    // endregion
}
