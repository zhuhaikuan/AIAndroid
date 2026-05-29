package com.zhk.aiandroid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhk.aiandroid.ui.theme.AIAndroidTheme
import com.zhk.mcp.HttpMcpClient
import com.zhk.mcp.McpClientConfig
import com.zhk.mcp.McpOperation
import com.zhk.mcp.McpOperationResult
import com.zhk.mcp.McpOperationRunner
import com.zhk.mcp.McpTestInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class McpDemoUiState(
    val endpoint: String = "http://10.0.2.2:8080/mcp",
    val toolName: String = "",
    val toolArgsJson: String = """{"query":"请给出 Android 中 MCP 落地建议"}""",
    val resourceUri: String = "",
    val promptName: String = "",
    val completionRefName: String = "",
    val isRunning: Boolean = false,
    val logs: List<String> = emptyList(),
)

class McpDemoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(McpDemoUiState())
    val uiState: StateFlow<McpDemoUiState> = _uiState.asStateFlow()

    fun updateEndpoint(value: String) = _uiState.update { it.copy(endpoint = value) }
    fun updateToolName(value: String) = _uiState.update { it.copy(toolName = value) }
    fun updateToolArgs(value: String) = _uiState.update { it.copy(toolArgsJson = value) }
    fun updateResourceUri(value: String) = _uiState.update { it.copy(resourceUri = value) }
    fun updatePromptName(value: String) = _uiState.update { it.copy(promptName = value) }
    fun updateCompletionRefName(value: String) = _uiState.update { it.copy(completionRefName = value) }

    fun clearLogs() = _uiState.update { it.copy(logs = emptyList()) }

    fun runSingle(operation: McpOperation) {
        runInternal(all = false, operation = operation)
    }

    fun runAll() {
        runInternal(all = true, operation = null)
    }

    private fun runInternal(all: Boolean, operation: McpOperation?) {
        if (_uiState.value.isRunning) return
        val snapshot = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    logs = it.logs + "开始执行 MCP ${if (all) "全操作验证" else "单项验证"}..."
                )
            }
            val client = HttpMcpClient(
                config = McpClientConfig(endpoint = snapshot.endpoint)
            )
            try {
                val runner = McpOperationRunner(client)
                val input = McpTestInput(
                    toolName = snapshot.toolName,
                    toolArgumentsJson = snapshot.toolArgsJson,
                    resourceUri = snapshot.resourceUri,
                    promptName = snapshot.promptName,
                    completionRefName = snapshot.completionRefName
                )
                if (all) {
                    val results = runner.runAll(input)
                    appendBatchLogs(results)
                } else {
                    val result = runner.runSingle(requireNotNull(operation), input)
                    appendSingleLog(result)
                }
            } finally {
                client.close()
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        logs = it.logs + "执行结束。"
                    )
                }
            }
        }
    }

    private fun appendBatchLogs(results: List<McpOperationResult>) {
        val newLines = mutableListOf<String>()
        results.forEach { result ->
            val status = if (result.success) "SUCCESS" else "FAILED"
            newLines += "[${result.operation.title}] $status - ${result.summary} (${result.costMs}ms)"
            if (result.payloadPreview.isNotBlank()) {
                newLines += result.payloadPreview
            }
        }
        _uiState.update { it.copy(logs = it.logs + newLines) }
    }

    private fun appendSingleLog(result: McpOperationResult) {
        val status = if (result.success) "SUCCESS" else "FAILED"
        val newLines = mutableListOf(
            "[${result.operation.title}] $status - ${result.summary} (${result.costMs}ms)"
        )
        if (result.payloadPreview.isNotBlank()) {
            newLines += result.payloadPreview
        }
        _uiState.update { it.copy(logs = it.logs + newLines) }
    }
}

class McpDemoActivity : ComponentActivity() {

    private val viewModel: McpDemoViewModel by viewModels()

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, McpDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    McpDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun McpDemoScreen(
    modifier: Modifier,
    viewModel: McpDemoViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
            text = "MCP 功能测试页",
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            text = "覆盖 initialize/ping/tools/resources/prompts/completion/logging 以及 Android 最佳实践链路。"
        )

        OutlinedTextField(
            value = uiState.endpoint,
            onValueChange = viewModel::updateEndpoint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpEndpointInput"),
            label = { Text("MCP Endpoint") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.toolName,
            onValueChange = viewModel::updateToolName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpToolNameInput"),
            label = { Text("Tool Name(用于 tools/call)") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.toolArgsJson,
            onValueChange = viewModel::updateToolArgs,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpToolArgsInput"),
            label = { Text("Tool Args JSON") }
        )
        OutlinedTextField(
            value = uiState.resourceUri,
            onValueChange = viewModel::updateResourceUri,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpResourceUriInput"),
            label = { Text("Resource Uri(用于 resources/read)") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.promptName,
            onValueChange = viewModel::updatePromptName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpPromptNameInput"),
            label = { Text("Prompt Name(用于 prompts/get)") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.completionRefName,
            onValueChange = viewModel::updateCompletionRefName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("McpCompletionRefNameInput"),
            label = { Text("Completion Ref Name") },
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                modifier = Modifier.testTag("McpRunAllButton"),
                onClick = viewModel::runAll,
                enabled = !uiState.isRunning
            ) {
                Text(if (uiState.isRunning) "执行中..." else "运行 MCP 全操作")
            }
            Button(
                modifier = Modifier.testTag("McpClearLogButton"),
                onClick = viewModel::clearLogs,
                enabled = !uiState.isRunning
            ) {
                Text("清空日志")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.runSingle(McpOperation.INITIALIZE) },
                enabled = !uiState.isRunning
            ) { Text("Initialize") }
            Button(
                onClick = { viewModel.runSingle(McpOperation.TOOLS_LIST) },
                enabled = !uiState.isRunning
            ) { Text("Tools") }
            Button(
                onClick = { viewModel.runSingle(McpOperation.BEST_PRACTICE_FLOW) },
                enabled = !uiState.isRunning
            ) { Text("最佳实践") }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("McpLogList")
        ) {
            itemsIndexed(uiState.logs) { index, line ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    text = "${index + 1}. $line"
                )
            }
        }
    }
}
