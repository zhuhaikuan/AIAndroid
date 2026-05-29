package com.zhk.room.demo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库定义。
 *
 * 最佳实践：
 * - 只暴露 DAO，不在 DB 类里写业务逻辑。
 * - 单例持有并使用 applicationContext，避免 Activity 泄漏。
 * - 生产环境优先写 Migration，不建议依赖 fallbackToDestructiveMigration。
 */
@Database(
    entities = [RoomUserEntity::class, RoomTodoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class RoomDemoDatabase : RoomDatabase() {
    abstract fun roomTodoDao(): RoomTodoDao

    companion object {
        @Volatile
        private var INSTANCE: RoomDemoDatabase? = null

        fun getInstance(context: Context): RoomDemoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoomDemoDatabase::class.java,
                    "room_demo.db",
                )
                    // 实战建议：开发期可以开启，发布前建议关闭并通过 Migration 保证升级可靠。
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
