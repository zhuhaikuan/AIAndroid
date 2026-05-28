package com.zhk.datastore

import com.zhk.datastore.model.ProtoUserSettings
import com.zhk.datastore.proto.UserSettingsSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * DataStore 强类型序列化测试模板。
 *
 * 覆盖点：
 * - 正常序列化/反序列化
 * - 版本不兼容时的兜底默认值
 */
class ExampleUnitTest {
    @Test
    fun serializer_roundTrip_shouldKeepFields() {
        runBlocking {
            val origin = ProtoUserSettings(
                userId = "u_1001",
                displayName = "ZHK",
                notificationsEnabled = true,
                fontScalePercent = 120,
                updatedAt = 1700000000000,
                favoriteCategories = listOf("Android", "Kotlin"),
            )
            val output = ByteArrayOutputStream()
            UserSettingsSerializer.writeTo(origin, output)

            val restored = UserSettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))
            assertEquals(origin, restored)
        }
    }

    @Test
    fun serializer_invalidVersion_shouldReturnDefault() {
        runBlocking {
            val invalidBytes = ByteArrayOutputStream().apply {
                write(byteArrayOf(0, 0, 0, 2))
            }
            val restored = UserSettingsSerializer.readFrom(ByteArrayInputStream(invalidBytes.toByteArray()))
            assertEquals(UserSettingsSerializer.defaultValue, restored)
            assertTrue(restored.favoriteCategories.isEmpty())
        }
    }
}