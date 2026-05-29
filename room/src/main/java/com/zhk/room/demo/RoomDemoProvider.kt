package com.zhk.room.demo

import android.content.Context

/**
 * Room 对外入口。
 *
 * app 模块只依赖 Provider，不直接感知 DB 创建细节。
 */
object RoomDemoProvider {
    fun createRepository(context: Context): RoomDemoRepository {
        val db = RoomDemoDatabase.getInstance(context)
        return RoomDemoRepository(db.roomTodoDao())
    }
}
