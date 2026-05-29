package com.zhk.aiandroid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhk.room.demo.RoomDemoProvider
import com.zhk.room.demo.RoomDemoRepository
import com.zhk.room.demo.RoomPriority
import com.zhk.room.demo.RoomTodoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomDemoUiState(
    val todos: List<RoomTodoEntity> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val statusMessage: String = "就绪",
    val operationLogs: List<String> = emptyList(),
)

class RoomDemoViewModel(
    private val repository: RoomDemoRepository,
) : ViewModel() {
    private val statusFlow = MutableStateFlow("就绪")
    private val logsFlow = MutableStateFlow(emptyList<String>())

    val uiState: StateFlow<RoomDemoUiState> = combine(
        repository.todosFlow,
        repository.todoCountFlow,
        repository.completedCountFlow,
        statusFlow,
        logsFlow,
    ) { todos, totalCount, completedCount, status, logs ->
        RoomDemoUiState(
            todos = todos,
            totalCount = totalCount,
            completedCount = completedCount,
            statusMessage = status,
            operationLogs = logs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoomDemoUiState(),
    )

    fun runAllRoomOperations() {
        viewModelScope.launch {
            val logs = repository.runFullDemo()
            logsFlow.value = logs
            statusFlow.value = "已完成 Room 全量操作演示"
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val inserted = repository.seedInitialTodos().size
            statusFlow.value = "已初始化数据，写入 $inserted 条"
        }
    }

    fun addRandomTodo() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = repository.addTodo(
                title = "动态新增任务 $now",
                description = "用于验证 insert + Flow 自动刷新",
                priority = RoomPriority.MEDIUM,
            )
            statusFlow.value = "新增成功，todoId=$id"
        }
    }

    fun markFirstAsDone() {
        viewModelScope.launch {
            val first = uiState.value.todos.firstOrNull()
            if (first == null) {
                statusFlow.value = "没有可更新的任务"
                return@launch
            }
            val success = repository.toggleTodo(first.todoId, done = true)
            statusFlow.value = if (success) "更新成功，id=${first.todoId} 已完成" else "更新失败"
        }
    }

    fun updateFirstTitle() {
        viewModelScope.launch {
            val first = uiState.value.todos.firstOrNull()
            if (first == null) {
                statusFlow.value = "没有可更新标题的任务"
                return@launch
            }
            val success = repository.updateTodoTitle(
                todoId = first.todoId,
                newTitle = "已更新标题 ${System.currentTimeMillis()}",
            )
            statusFlow.value = if (success) "标题更新成功" else "标题更新失败"
        }
    }

    fun deleteFirst() {
        viewModelScope.launch {
            val first = uiState.value.todos.firstOrNull()
            if (first == null) {
                statusFlow.value = "没有可删除的任务"
                return@launch
            }
            val success = repository.deleteTodoById(first.todoId)
            statusFlow.value = if (success) "删除成功，id=${first.todoId}" else "删除失败"
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch {
            val removed = repository.deleteCompletedTodos()
            statusFlow.value = "已删除完成任务 $removed 条"
        }
    }

    fun queryAdvancedInfo() {
        viewModelScope.launch {
            val exists = repository.existsByTitle("测试 RawQuery")
            val relation = repository.queryUserWithTodos()
            val rawCount = repository.observePriorityAtLeast(RoomPriority.HIGH).first().size
            statusFlow.value = "exists=$exists, relationTodos=${relation?.todos?.size ?: 0}, highPriority=$rawCount"
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val removed = repository.clearAllTodos()
            logsFlow.value = emptyList()
            statusFlow.value = "已清空，删除 $removed 条"
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repository = RoomDemoProvider.createRepository(context)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RoomDemoViewModel::class.java)) {
                        return RoomDemoViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
