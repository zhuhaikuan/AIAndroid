package com.zhk.flow

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Android 中最常用的 Flow 最佳实践模板（可直接拷贝到业务代码）。
 *
 * 覆盖点：
 * - Repository 返回冷流（Cold Flow）
 * - ViewModel 将冷流组合为 StateFlow（UI 可订阅）
 * - 一次性事件使用 SharedFlow，而不是 StateFlow
 * - UI 层使用 repeatOnLifecycle 安全收集，避免泄漏和后台浪费
 */
object AndroidFlowBestPractices {

    data class Article(
        val id: Long,
        val title: String,
    )

    data class UiState(
        val isLoading: Boolean = false,
        val keyword: String = "",
        val articles: List<Article> = emptyList(),
        val errorMessage: String? = null,
    )

    sealed interface UiEvent {
        data object NavigateBack : UiEvent
        data class ShowToast(val message: String) : UiEvent
    }

    /**
     * Repository 层：
     * - 返回 Flow 而不是直接返回值，方便上层组合/取消/重试。
     * - 把 IO 放进 flowOn(Dispatchers.IO)，避免阻塞主线程。
     */
    class ArticleRepository {
        fun observeRecommendArticles(): Flow<List<Article>> = flow {
            emit(
                listOf(
                    Article(1, "Kotlin Flow 入门"),
                    Article(2, "StateFlow 与 SharedFlow"),
                    Article(3, "repeatOnLifecycle 实战")
                )
            )
        }.flowOn(Dispatchers.IO)

        fun searchArticles(keyword: String): Flow<List<Article>> = flow {
            delay(180) // 模拟网络耗时
            val all = listOf(
                Article(1, "Kotlin Flow 入门"),
                Article(2, "StateFlow 与 SharedFlow"),
                Article(3, "repeatOnLifecycle 实战"),
                Article(4, "flatMapLatest 取消旧请求"),
            )
            emit(all.filter { it.title.contains(keyword, ignoreCase = true) })
        }.flowOn(Dispatchers.IO)
    }

    /**
     * ViewModel 层：
     * - UI 状态统一暴露为 StateFlow，便于 Compose/传统 View 统一消费。
     * - 搜索关键词使用 debounce + distinctUntilChanged + flatMapLatest，减少无效请求。
     * - 通过 stateIn 把组合流变热，避免每个 Collector 都触发一遍上游请求。
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    class ArticleViewModel(
        private val repository: ArticleRepository = ArticleRepository()
    ) : ViewModel() {

        private val keyword = MutableStateFlow("")
        private val loading = MutableStateFlow(false)
        private val localError = MutableStateFlow<String?>(null)

        private val _events = MutableSharedFlow<UiEvent>()
        val events: Flow<UiEvent> = _events

        private val searchResult = keyword
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.observeRecommendArticles()
                } else {
                    repository.searchArticles(query)
                }
            }
            .catch { e ->
                // 避坑：catch 只抓上游异常，尽量就地降级并记录业务错误状态。
                localError.value = e.message ?: "unknown error"
                emit(emptyList())
            }

        val uiState = combine(
            loading,
            keyword,
            searchResult,
            localError
        ) { isLoading, kw, list, error ->
            UiState(
                isLoading = isLoading,
                keyword = kw,
                articles = list,
                errorMessage = error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UiState(isLoading = true)
        )

        fun onKeywordChanged(newKeyword: String) {
            keyword.value = newKeyword
        }

        fun refresh() {
            viewModelScope.launch {
                loading.value = true
                localError.value = null
                delay(150)
                loading.value = false
            }
        }

        fun onBackClicked() {
            viewModelScope.launch {
                _events.emit(UiEvent.NavigateBack)
            }
        }

        fun clearError() {
            localError.update { null }
        }
    }

    /**
     * Activity/Fragment 层收集模板：
     * - 必须放到 repeatOnLifecycle，避免页面不可见时仍在 collect。
     * - UI 状态与一次性事件分开收集，职责清晰。
     */
    fun AppCompatActivity.collectFlowInUi(viewModel: ArticleViewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        // 这里更新列表、Loading、错误文案等 UI
                        // render(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            UiEvent.NavigateBack -> onBackPressedDispatcher.onBackPressed()
                            is UiEvent.ShowToast -> {
                                // showToast(event.message)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 常见避坑速记：
     * 1) 不要在 UI 层直接 collect 冷流且不做生命周期绑定（会泄漏）。
     * 2) 不要用 StateFlow 发一次性事件（旋转屏幕后会重放旧状态语义问题）。
     * 3) 不要把重网络请求直接放在 map 里，用 flatMapLatest + flowOn 更可控。
     * 4) 不要在 catch 里吞掉所有错误后什么也不做，至少更新错误态并打日志。
     */
}
