package com.zhk.room.demo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户表。
 *
 * 实战建议：
 * - 明确主键策略，避免后期改主键导致全表迁移。
 * - createdAt/updatedAt 在业务里始终维护，方便排查“最后一次写入”问题。
 */
@Entity(tableName = "room_user")
data class RoomUserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val age: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
