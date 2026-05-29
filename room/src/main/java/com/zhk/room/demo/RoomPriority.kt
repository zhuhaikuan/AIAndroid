package com.zhk.room.demo

/**
 * 任务优先级。
 *
 * Room 不建议直接把 Enum 按 ordinal 存库（数字顺序变了就会“脏数据”），
 * 所以我们在 Converter 里统一按 name 持久化，迁移更安全。
 */
enum class RoomPriority {
    LOW,
    MEDIUM,
    HIGH,
}
