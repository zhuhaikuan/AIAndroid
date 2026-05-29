package com.zhk.mcp

import org.json.JSONObject
import kotlin.math.min
import kotlin.system.measureTimeMillis

/**
 * MCP 操作类型（覆盖 Android 客户端最常落地的全量操作）。
 */
enum class McpOperation(val method: String, val title: String) {
    INITIALIZE("initialize", "初始化会话"),
    PING("ping", "心跳检查"),
    TOOLS_LIST("tools/list", "工具列表"),
    TOOLS_CALL("tools/call", "调用工具"),
    RESOURCES_LIST("resources/list", "资源列表"),
    RESOURCES_READ("resources/read", "读取资源"),
    PROMPTS_LIST("prompts/list", "提示词模板列表"),
    PROMPTS_GET("prompts/get", "获取提示词模板"),
    COMPLETION_COMPLETE("completion/complete", "参数补全"),
    LOGGING_SET_LEVEL("logging/setLevel", "设置日志级别"),
    BEST_PRACTICE_FLOW("best-practice", "Android 最佳实践链路"),
}

/**
 * MCP 测试输入。
 *
 * 避坑建议：
 * 1) toolName/resourceUri/promptName 等如果为空，测试页会输出可执行建议而不是直接崩溃。
 * 2) toolArgumentsJson 必须是合法 JSON 对象字符串（如 {"query":"xxx"}）。
 */
data class McpTestInput(
    val toolName: String = "",
    val toolArgumentsJson: String = """{"query":"请给出 Android 架构建议"}""",
    val resourceUri: String = "",
    val promptName: String = "",
    val promptArgumentName: String = "topic",
    val promptArgumentValue: String = "android mcp",
    val completionRefType: String = "ref/prompt",
    val completionRefName: String = "",
    val loggingLevel: String = "info",
)

data class McpOperationResult(
    val operation: McpOperation,
    val success: Boolean,
    val costMs: Long,
    val summary: String,
    val payloadPreview: String = "",
)

/**
 * 统一执行 MCP 操作并返回可展示结果。
 *
 * 这里把“每个操作做什么、出现问题如何提示”都集中封装，便于在 Android UI 中做验证页。
 */
class McpOperationRunner(
    private val client: McpClient,
) {

    suspend fun runSingle(operation: McpOperation, input: McpTestInput): McpOperationResult {
        var summary = ""
        var preview = ""
        return try {
            val cost = measureTimeMillis {
                val result = when (operation) {
                    McpOperation.INITIALIZE -> client.initialize()
                    McpOperation.PING -> client.ping()
                    McpOperation.TOOLS_LIST -> client.listTools()
                    McpOperation.TOOLS_CALL -> runToolCall(input)
                    McpOperation.RESOURCES_LIST -> client.listResources()
                    McpOperation.RESOURCES_READ -> runResourceRead(input)
                    McpOperation.PROMPTS_LIST -> client.listPrompts()
                    McpOperation.PROMPTS_GET -> runPromptGet(input)
                    McpOperation.COMPLETION_COMPLETE -> runCompletion(input)
                    McpOperation.LOGGING_SET_LEVEL -> client.setLoggingLevel(input.loggingLevel)
                    McpOperation.BEST_PRACTICE_FLOW -> runBestPracticeFlow(input)
                }
                summary = buildSummary(operation, result)
                preview = compactPreview(result)
            }
            McpOperationResult(
                operation = operation,
                success = true,
                costMs = cost,
                summary = summary,
                payloadPreview = preview
            )
        } catch (e: Throwable) {
            McpOperationResult(
                operation = operation,
                success = false,
                costMs = 0L,
                summary = "失败: ${e.message ?: e::class.java.simpleName}",
                payloadPreview = ""
            )
        }
    }

    suspend fun runAll(input: McpTestInput): List<McpOperationResult> {
        val ordered = listOf(
            McpOperation.INITIALIZE,
            McpOperation.PING,
            McpOperation.TOOLS_LIST,
            McpOperation.TOOLS_CALL,
            McpOperation.RESOURCES_LIST,
            McpOperation.RESOURCES_READ,
            McpOperation.PROMPTS_LIST,
            McpOperation.PROMPTS_GET,
            McpOperation.COMPLETION_COMPLETE,
            McpOperation.LOGGING_SET_LEVEL,
            McpOperation.BEST_PRACTICE_FLOW,
        )
        return ordered.map { runSingle(it, input) }
    }

    private suspend fun runToolCall(input: McpTestInput): JSONObject {
        require(input.toolName.isNotBlank()) {
            "tools/call 需要 toolName。建议先执行 tools/list 获取可用工具。"
        }
        val arguments = parseArgs(input.toolArgumentsJson)
        return client.callTool(input.toolName, arguments)
    }

    private suspend fun runResourceRead(input: McpTestInput): JSONObject {
        require(input.resourceUri.isNotBlank()) {
            "resources/read 需要 resourceUri。建议先执行 resources/list 获取 uri。"
        }
        return client.readResource(input.resourceUri)
    }

    private suspend fun runPromptGet(input: McpTestInput): JSONObject {
        require(input.promptName.isNotBlank()) {
            "prompts/get 需要 promptName。建议先执行 prompts/list 获取名称。"
        }
        return client.getPrompt(
            name = input.promptName,
            arguments = JSONObject()
                .put(input.promptArgumentName, input.promptArgumentValue)
        )
    }

    private suspend fun runCompletion(input: McpTestInput): JSONObject {
        require(input.completionRefName.isNotBlank()) {
            "completion/complete 需要 completionRefName（通常是 prompt 或 resource 名称）。"
        }
        return client.complete(
            refType = input.completionRefType,
            refName = input.completionRefName,
            argumentName = input.promptArgumentName,
            argumentValue = input.promptArgumentValue
        )
    }

    /**
     * Android 中最常用的 MCP 最佳实践链路：
     * initialize -> tools/list -> tools/call。
     *
     * 适用于“用户提问 -> 选择工具 -> 获得结构化结果 -> 渲染 UI”的常见产品路径。
     */
    private suspend fun runBestPracticeFlow(input: McpTestInput): JSONObject {
        client.initialize()
        val tools = client.listTools()
        val firstToolName = tools.optJSONArray("tools")
            ?.optJSONObject(0)
            ?.optString("name")
            .orEmpty()

        val preferredTool = input.toolName.ifBlank { firstToolName }
        require(preferredTool.isNotBlank()) {
            "未找到可调用工具。请确认 MCP 服务端已正确暴露 tools/list。"
        }

        val toolResult = client.callTool(
            name = preferredTool,
            arguments = parseArgs(input.toolArgumentsJson)
        )
        return JSONObject()
            .put("selectedTool", preferredTool)
            .put("toolResult", toolResult)
    }

    private fun parseArgs(raw: String): JSONObject {
        return try {
            JSONObject(raw)
        } catch (_: Throwable) {
            JSONObject().put("query", raw)
        }
    }

    private fun buildSummary(operation: McpOperation, result: JSONObject): String {
        return when (operation) {
            McpOperation.TOOLS_LIST -> "获取工具数量=${result.optJSONArray("tools")?.length() ?: 0}"
            McpOperation.RESOURCES_LIST -> "获取资源数量=${result.optJSONArray("resources")?.length() ?: 0}"
            McpOperation.PROMPTS_LIST -> "获取模板数量=${result.optJSONArray("prompts")?.length() ?: 0}"
            McpOperation.BEST_PRACTICE_FLOW -> "最佳实践链路完成，选中工具=${result.optString("selectedTool")}"
            else -> "调用 ${operation.method} 成功"
        }
    }

    private fun compactPreview(result: JSONObject): String {
        val pretty = result.toString(2)
        val maxLen = 700
        if (pretty.length <= maxLen) return pretty
        return pretty.substring(0, min(pretty.length, maxLen)) + "\n...(省略)"
    }
}
