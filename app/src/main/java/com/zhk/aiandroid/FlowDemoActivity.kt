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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.zhk.flow.FlowOperatorsCookbook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlowDemoUiState(
    val isRunning: Boolean = false,
    val logs: List<String> = emptyList(),
)

class FlowDemoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FlowDemoUiState())
    val uiState: StateFlow<FlowDemoUiState> = _uiState.asStateFlow()

    fun runAllOperators() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    logs = listOf("开始执行 Flow 全操作符演示...")
                )
            }
            FlowOperatorsCookbook.runAllOperatorDemos { line ->
                _uiState.update { state ->
                    state.copy(logs = state.logs + line)
                }
            }
            _uiState.update { it.copy(isRunning = false, logs = it.logs + "演示完成。") }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}

class FlowDemoActivity : ComponentActivity() {

    private val viewModel: FlowDemoViewModel by viewModels()

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FlowDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FlowDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowDemoScreen(
    modifier: Modifier,
    viewModel: FlowDemoViewModel,
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
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = "Flow 操作符演示页",
            fontWeight = FontWeight.Bold,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = "点击运行后会按分类输出操作符行为日志（创建/转换/组合/错误处理/背压等）"
        )
        Button(
            modifier = Modifier.padding(12.dp).testTag("FlowRunAllButton"),
            onClick = { viewModel.runAllOperators() },
            enabled = !uiState.isRunning
        ) {
            Text(if (uiState.isRunning) "演示运行中..." else "运行所有 Flow 操作符演示")
        }
        Button(
            modifier = Modifier.padding(4.dp).testTag("FlowClearLogButton"),
            onClick = { viewModel.clearLogs() },
            enabled = !uiState.isRunning
        ) {
            Text("清空日志")
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("FlowLogList")
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
