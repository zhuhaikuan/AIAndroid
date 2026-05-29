package com.zhk.aiandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhk.coroutine.AndroidCoroutineBestPractices
import com.zhk.coroutine.CoroutineOperatorsCookbook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoroutineDemoUiState(
    val isRunning: Boolean = false,
    val logs: List<String> = emptyList(),
)

class CoroutineDemoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CoroutineDemoUiState())
    val uiState: StateFlow<CoroutineDemoUiState> = _uiState.asStateFlow()

    fun runAllCoroutineDemos() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    logs = listOf("开始执行 Kotlin Coroutine 全功能演示...")
                )
            }
            CoroutineOperatorsCookbook.runAllOperatorDemos { line ->
                appendLog(line)
            }
            AndroidCoroutineBestPractices.logChecklist { line ->
                appendLog(line)
            }
            appendLog("演示完成：你可以滚动日志逐条验证协程行为。")
            _uiState.update { it.copy(isRunning = false) }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun appendLog(line: String) {
        _uiState.update { state ->
            state.copy(logs = state.logs + line)
        }
    }
}
