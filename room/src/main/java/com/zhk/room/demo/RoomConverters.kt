package com.zhk.room.demo

import androidx.room.TypeConverter

/**
 * Room 类型转换器。
 *
 * 实战避坑：
 * 1) 避免把复杂对象直接序列化为“不可读字符串”，后续迁移会非常痛苦。
 * 2) Enum 推荐存 name，不存 ordinal。
 */
class RoomConverters {
    @TypeConverter
    fun priorityToString(value: RoomPriority?): String? = value?.name

    @TypeConverter
    fun stringToPriority(value: String?): RoomPriority? =
        value?.let { runCatching { RoomPriority.valueOf(it) }.getOrNull() } ?: RoomPriority.MEDIUM
}
