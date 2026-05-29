package com.zhk.room.demo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待办表。
 *
 * 实战避坑：
 * - 给高频查询字段建索引（这里 user_id + done）。
 * - 使用外键约束保障数据一致性；删除用户时自动级联删除 todo。
 */
@Entity(
    tableName = "room_todo",
    foreignKeys = [
        ForeignKey(
            entity = RoomUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["done"]),
    ],
)
data class RoomTodoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "todo_id")
    val todoId: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val title: String,
    val description: String,
    val priority: RoomPriority = RoomPriority.MEDIUM,
    val done: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
