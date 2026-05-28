package com.zhk.datastore.proto

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zhk.datastore.model.ProtoUserSettings
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * 强类型 DataStore 序列化器。
 *
 * 注意：这里采用手写二进制协议，避免额外插件依赖，同时保留强类型能力。
 */
object UserSettingsSerializer : Serializer<ProtoUserSettings> {
    override val defaultValue: ProtoUserSettings = ProtoUserSettings()

    override suspend fun readFrom(input: InputStream): ProtoUserSettings {
        try {
            val source = DataInputStream(input)
            val schemaVersion = source.readInt()
            if (schemaVersion != 1) return defaultValue

            val userId = source.readUTF()
            val displayName = source.readUTF()
            val notificationsEnabled = source.readBoolean()
            val fontScalePercent = source.readInt()
            val updatedAt = source.readLong()
            val categorySize = source.readInt()
            val categories = buildList {
                repeat(categorySize) {
                    add(source.readUTF())
                }
            }

            return ProtoUserSettings(
                userId = userId,
                displayName = displayName,
                notificationsEnabled = notificationsEnabled,
                fontScalePercent = fontScalePercent,
                updatedAt = updatedAt,
                favoriteCategories = categories,
            )
        } catch (_: EOFException) {
            return defaultValue
        } catch (exception: Exception) {
            throw CorruptionException("Cannot read typed UserSettings.", exception)
        }
    }

    override suspend fun writeTo(t: ProtoUserSettings, output: OutputStream) {
        val sink = DataOutputStream(output)
        sink.writeInt(1)
        sink.writeUTF(t.userId)
        sink.writeUTF(t.displayName)
        sink.writeBoolean(t.notificationsEnabled)
        sink.writeInt(t.fontScalePercent)
        sink.writeLong(t.updatedAt)
        sink.writeInt(t.favoriteCategories.size)
        t.favoriteCategories.forEach { sink.writeUTF(it) }
        sink.flush()
    }
}
