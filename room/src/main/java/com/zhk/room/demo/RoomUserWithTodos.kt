package com.zhk.room.demo

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 关系查询结果模型：1 个用户 + 多个 todo。
 */
data class RoomUserWithTodos(
    @Embedded
    val user: RoomUserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id",
    )
    val todos: List<RoomTodoEntity>,
)
