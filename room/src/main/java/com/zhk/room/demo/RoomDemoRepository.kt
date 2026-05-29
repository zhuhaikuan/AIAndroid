package com.zhk.room.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Room 最常用“仓库层”示例。
 *
 * 为什么要有 Repository：
 * - 屏蔽底层 SQL/DAO 细节，UI 只关心业务动作。
 * - 把多 DAO、多表事务、容错等集中管理，便于测试与复用。
 * - 统一补充“实践中的坑位处理”。
 */
class RoomDemoRepository(
    private val dao: RoomTodoDao,
) {
    val todosFlow: Flow<List<RoomTodoEntity>> = dao.observeAllTodos()
    val todoCountFlow: Flow<Int> = dao.observeTodoCount()
    val completedCountFlow: Flow<Int> = todosFlow.map { list -> list.count { it.done } }

    suspend fun prepareDefaultUser(userId: String = DEFAULT_USER_ID) {
        val now = System.currentTimeMillis()
        dao.insertUserIfAbsent(
            RoomUserEntity(
                userId = userId,
                displayName = "RoomDemoUser",
                age = 20,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun seedInitialTodos(userId: String = DEFAULT_USER_ID): List<Long> {
        prepareDefaultUser(userId)
        val now = System.currentTimeMillis()
        val todos = listOf(
            RoomTodoEntity(
                userId = userId,
                title = "学习 Room CRUD",
                description = "验证 insert / query / update / delete",
                priority = RoomPriority.HIGH,
                createdAt = now,
                updatedAt = now,
            ),
            RoomTodoEntity(
                userId = userId,
                title = "实践事务与关系查询",
                description = "验证 @Transaction + @Relation",
                priority = RoomPriority.MEDIUM,
                createdAt = now,
                updatedAt = now,
            ),
            RoomTodoEntity(
                userId = userId,
                title = "测试 RawQuery",
                description = "验证动态 SQL 的封装方式",
                priority = RoomPriority.LOW,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return dao.replaceAllTodos(todos)
    }

    suspend fun addTodo(
        title: String,
        description: String,
        priority: RoomPriority = RoomPriority.MEDIUM,
        userId: String = DEFAULT_USER_ID,
    ): Long {
        prepareDefaultUser(userId)
        val now = System.currentTimeMillis()
        return dao.insertTodos(
            listOf(
                RoomTodoEntity(
                    userId = userId,
                    title = title,
                    description = description,
                    priority = priority,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        ).first()
    }

    suspend fun toggleTodo(todoId: Long, done: Boolean): Boolean {
        val todo = dao.queryTodoById(todoId) ?: return false
        val updatedRows = dao.updateTodo(
            todo.copy(
                done = done,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return updatedRows > 0
    }

    suspend fun updateTodoTitle(todoId: Long, newTitle: String): Boolean {
        val todo = dao.queryTodoById(todoId) ?: return false
        val updatedRows = dao.updateTodo(
            todo.copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return updatedRows > 0
    }

    suspend fun deleteTodoById(todoId: Long): Boolean = dao.deleteTodoById(todoId) > 0

    suspend fun deleteCompletedTodos(): Int = dao.deleteCompletedTodos()

    suspend fun clearAllTodos(): Int = dao.clearTodos()

    suspend fun queryTodoById(todoId: Long): RoomTodoEntity? = dao.queryTodoById(todoId)

    fun observeByKeyword(keyword: String): Flow<List<RoomTodoEntity>> = dao.observeByKeyword(keyword)

    fun observePriorityAtLeast(priority: RoomPriority): Flow<List<RoomTodoEntity>> =
        dao.observePriorityAtLeast(priority)

    suspend fun existsByTitle(title: String): Boolean = dao.existsByTitle(title)

    suspend fun queryUserWithTodos(userId: String = DEFAULT_USER_ID): RoomUserWithTodos? =
        dao.queryUserWithTodos(userId)

    /**
     * 一键跑完常见操作，返回“可直接展示给 UI 的日志”。
     *
     * 覆盖点：
     * 1) 插入/替换 2) 列表查询 3) 单条查询 4) 更新 5) 条件删除
     * 6) 关系查询 7) exists 8) RawQuery 9) 清空
     */
    suspend fun runFullDemo(): List<String> {
        val logs = mutableListOf<String>()
        val ids = seedInitialTodos()
        logs += "1) replaceAll + insert: 写入 ${ids.size} 条记录"

        val all = todosFlow.first()
        logs += "2) query all: 当前总数 ${all.size}"

        val firstId = all.firstOrNull()?.todoId
        if (firstId != null) {
            val found = queryTodoById(firstId)
            logs += "3) query by id: id=$firstId, title=${found?.title}"
            toggleTodo(firstId, true)
            logs += "4) update: id=$firstId 已标记 done=true"
        }

        val exists = existsByTitle("测试 RawQuery")
        logs += "5) exists: '测试 RawQuery' = $exists"

        val relation = queryUserWithTodos()
        logs += "6) relation query: user=${relation?.user?.displayName}, todos=${relation?.todos?.size ?: 0}"

        val highPriorityCount = observePriorityAtLeast(RoomPriority.HIGH).first().size
        logs += "7) raw query: HIGH 及以上优先级数量 $highPriorityCount"

        val removed = deleteCompletedTodos()
        logs += "8) delete completed: 删除 $removed 条"

        val left = todoCountFlow.first()
        logs += "9) count flow: 剩余 $left 条"

        return logs
    }

    companion object {
        private const val DEFAULT_USER_ID = "room_demo_user"
    }
}
