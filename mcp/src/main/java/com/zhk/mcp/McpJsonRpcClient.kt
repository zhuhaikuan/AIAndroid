package com.zhk.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * MCP 客户端配置。
 *
 * 避坑建议：
 * 1) endpoint 建议使用网关地址（如 Cloudflare Worker/Nginx 转发），不要在移动端直接暴露内网敏感地址。
 * 2) requestTimeoutMs 不要过短，MCP 工具调用常包含联网/检索，建议 >= 15s。
 * 3) maxRetries 建议只用于幂等请求（如 list/read/ping），tools/call 是否重试要结合业务幂等性判断。
 */
data class McpClientConfig(
    val endpoint: String,
    val requestTimeoutMs: Long = 20_000L,
    val connectTimeoutMs: Long = 10_000L,
    val readTimeoutMs: Long = 30_000L,
    val writeTimeoutMs: Long = 10_000L,
    val maxRetries: Int = 2,
    val retryDelayMs: Long = 600L,
    val protocolVersion: String = "2025-03-26",
    val clientName: String = "aiandroid-mcp-client",
    val clientVersion: String = "1.0.0",
)

/**
 * MCP 统一异常。
 */
class McpException(
    message: String,
    val code: Int? = null,
    val data: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Android 上可直接复用的 MCP 操作接口（覆盖常用全量操作）。
 */
interface McpClient : Closeable {
    suspend fun initialize(): JSONObject
    suspend fun ping(): JSONObject
    suspend fun listTools(): JSONObject
    suspend fun callTool(name: String, arguments: JSONObject = JSONObject()): JSONObject
    suspend fun listResources(): JSONObject
    suspend fun readResource(uri: String): JSONObject
    suspend fun listPrompts(): JSONObject
    suspend fun getPrompt(name: String, arguments: JSONObject = JSONObject()): JSONObject
    suspend fun complete(
        refType: String,
        refName: String,
        argumentName: String,
        argumentValue: String,
    ): JSONObject
    suspend fun setLoggingLevel(level: String): JSONObject
}

/**
 * 基于 HTTP(JSON-RPC 2.0) 的 MCP 客户端实现。
 *
 * 关键实践：
 * - 自动维护 MCP Session Header，避免服务端要求会话时出现“首次成功、后续失败”问题。
 * - 在 Android 主线程外执行 IO。
 * - 使用轻量重试，降低弱网下偶发失败率。
 */
class HttpMcpClient(
    private val config: McpClientConfig,
    providedClient: OkHttpClient? = null,
) : McpClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val requestIdGenerator = AtomicLong(1L)

    private val okHttpClient: OkHttpClient = providedClient ?: OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var sessionId: String? = null

    @Volatile
    private var initialized = false

    override suspend fun initialize(): JSONObject {
        val params = JSONObject()
            .put("protocolVersion", config.protocolVersion)
            .put(
                "capabilities",
                JSONObject()
                    .put("roots", JSONObject().put("listChanged", false))
                    .put("sampling", JSONObject())
                    .put("elicitation", JSONObject())
            )
            .put(
                "clientInfo",
                JSONObject()
                    .put("name", config.clientName)
                    .put("version", config.clientVersion)
            )
        val result = request("initialize", params = params, requireInitialized = false)
        initialized = true
        return result
    }

    override suspend fun ping(): JSONObject = request("ping")

    override suspend fun listTools(): JSONObject = request("tools/list")

    override suspend fun callTool(name: String, arguments: JSONObject): JSONObject {
        val params = JSONObject()
            .put("name", name)
            .put("arguments", arguments)
        return request("tools/call", params = params)
    }

    override suspend fun listResources(): JSONObject = request("resources/list")

    override suspend fun readResource(uri: String): JSONObject {
        val params = JSONObject().put("uri", uri)
        return request("resources/read", params = params)
    }

    override suspend fun listPrompts(): JSONObject = request("prompts/list")

    override suspend fun getPrompt(name: String, arguments: JSONObject): JSONObject {
        val params = JSONObject()
            .put("name", name)
            .put("arguments", arguments)
        return request("prompts/get", params = params)
    }

    override suspend fun complete(
        refType: String,
        refName: String,
        argumentName: String,
        argumentValue: String,
    ): JSONObject {
        val params = JSONObject()
            .put(
                "ref",
                JSONObject()
                    .put("type", refType)
                    .put("name", refName)
            )
            .put(
                "argument",
                JSONObject()
                    .put("name", argumentName)
                    .put("value", argumentValue)
            )
        return request("completion/complete", params = params)
    }

    override suspend fun setLoggingLevel(level: String): JSONObject {
        val params = JSONObject().put("level", level)
        return request("logging/setLevel", params = params)
    }

    private suspend fun request(
        method: String,
        params: JSONObject? = null,
        requireInitialized: Boolean = true,
    ): JSONObject = withContext(Dispatchers.IO) {
        if (requireInitialized && !initialized) {
            throw McpException("MCP client has not been initialized. Call initialize() first.")
        }
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", requestIdGenerator.getAndIncrement())
            .put("method", method)
            .apply { params?.let { put("params", it) } }
            .toString()

        var attempt = 0
        var lastError: Throwable? = null
        while (attempt <= config.maxRetries) {
            try {
                return@withContext executeOnce(payload)
            } catch (throwable: Throwable) {
                lastError = throwable
                if (attempt == config.maxRetries) break
                attempt += 1
                delay(config.retryDelayMs * attempt)
            }
        }
        throw McpException(
            message = "MCP request failed after retries. method=$method",
            cause = lastError
        )
    }

    private fun executeOnce(payload: String): JSONObject {
        val body = payload.toRequestBody(jsonMediaType)
        val requestBuilder = Request.Builder()
            .url(config.endpoint)
            .post(body)
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")

        sessionId?.let { requestBuilder.addHeader("Mcp-Session-Id", it) }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body.string()

            // 许多 MCP 服务器要求后续请求携带会话 ID；不保存会导致请求随机失败。
            val newSessionId = response.header("Mcp-Session-Id")
            if (!newSessionId.isNullOrBlank()) {
                sessionId = newSessionId
            }

            if (!response.isSuccessful) {
                throw McpException(
                    message = "HTTP ${response.code} for MCP request.",
                    code = response.code,
                    data = responseBody
                )
            }

            val envelope = try {
                JSONObject(responseBody)
            } catch (e: Throwable) {
                throw McpException(
                    message = "MCP response is not valid JSON.",
                    data = responseBody,
                    cause = e
                )
            }

            if (envelope.has("error")) {
                val error = envelope.optJSONObject("error")
                throw McpException(
                    message = error?.optString("message").orEmpty().ifBlank { "Unknown MCP error" },
                    code = error?.optInt("code"),
                    data = error?.opt("data")?.toString()
                )
            }

            return envelope.optJSONObject("result") ?: JSONObject()
        }
    }

    override fun close() {
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}
