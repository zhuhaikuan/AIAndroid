package com.zhk.coroutine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 中最常用的 Coroutine 最佳实践模板。
 *
 * 模板覆盖点：
 * - Repository 层：只暴露 suspend / Flow，不暴露线程细节。
 * - ViewModel 层：用 viewModelScope 承接生命周期；UI 状态和一次性事件分离。
 * - Dispatcher 注入：方便单元测试替换为 TestDispatcher。
 * - 错误处理：runCatching + 明确错误状态，避免静默失败。
 */
object AndroidCoroutineBestPractices {

    data class Article(
        val id: Long,
        val title: String,
    )

    data class UiState(
        val isLoading: Boolean = false,
        val data: List<Article> = emptyList(),
        val errorMessage: String? = null,
    )

    sealed interface UiEvent {
        data class ShowToast(val message: String) : UiEvent
    }

    interface DispatcherProvider {
        val main: CoroutineDispatcher
        val io: CoroutineDispatcher
        val default: CoroutineDispatcher
    }

    object DefaultDispatcherProvider : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
    }

    class ArticleRepository(
        private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    ) {
        suspend fun fetchArticles(): List<Article> = withContext(dispatcherProvider.io) {
            // 真实项目里这里一般是 Retrofit/Room 等数据源访问。
            listOf(
                Article(1, "Structured Concurrency"),
                Article(2, "Cancellation & Timeout"),
                Article(3, "SupervisorScope in Android"),
            )
        }
    }

    class ArticleViewModel(
        private val repository: ArticleRepository = ArticleRepository(),
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<UiEvent>()
        val events = _events.asSharedFlow()

        /**
         * 推荐写法：
         * - 进入加载态 -> 执行业务 -> 成功/失败都回收加载态。
         * - 不抛异常到 UI 层，统一映射为状态和事件。
         */
        fun loadArticles() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching { repository.fetchArticles() }
                    .onSuccess { list ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                data = list,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val message = throwable.message ?: "unknown error"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = message,
                            )
                        }
                        _events.emit(UiEvent.ShowToast("加载失败: $message"))
                    }
            }
        }
    }

    /**
     * 可直接在 Demo 页打印的最佳实践速记（用于人工验收）。
     */
    suspend fun logChecklist(logger: (String) -> Unit = ::println) {
        logger("===== Android Coroutine 最佳实践清单 =====")
        logger("1) 业务协程统一从 ViewModel 的 viewModelScope 发起，避免 Activity 旋转泄漏。")
        logger("2) Dispatcher 不要写死在业务类里，推荐通过 DispatcherProvider 注入，方便测试。")
        logger("3) CPU 密集任务 -> Dispatchers.Default；IO 密集任务 -> Dispatchers.IO。")
        logger("4) UI 状态使用 StateFlow；一次性事件使用 SharedFlow/Channel。")
        logger("5) 并行子任务默认使用 coroutineScope；要隔离失败用 supervisorScope。")
        logger("6) 网络/磁盘请求必须配合超时与取消，避免长时间占用资源。")
        logger("7) 尽量在数据层做线程切换（withContext），UI 层只关心状态渲染。")
        logger("8) 异常不要吞掉，至少更新错误态并记录日志，便于线上排查。")
    }

    /**
     * 供 Compose/Activity 示例使用：将上游 StateFlow 转换成可缓存热流。
     */
    fun ArticleViewModel.cachedUiState(): StateFlow<UiState> {
        return uiState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(isLoading = true),
        )
    }
}
