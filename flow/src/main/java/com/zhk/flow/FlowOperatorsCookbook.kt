package com.zhk.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

/**
 * Kotlin Flow 操作符大全示例（按类型分类）。
 *
 * 说明：
 * 1) 这个文件重点是“覆盖 + 分类 + 注释”，用于学习和复制到业务中。
 * 2) 某些操作符需要实验性注解（FlowPreview / ExperimentalCoroutinesApi）。
 * 3) 示例日志都通过 [logger] 输出，便于在单元测试或 Demo 页面中观察。
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
object FlowOperatorsCookbook {

    /**
     * 实战避坑：
     * - SharedFlow 默认无重放，晚到订阅者拿不到历史值；如需要“最近一次值”，用 replay = 1 或改用 StateFlow。
     * - extraBufferCapacity 只缓冲“临时突发”，不是无限队列，仍需控制发送频率。
     */
    private val hotEventBus = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 实战避坑：
     * - StateFlow 一定有初始值，适合 UI 状态；不要拿它发一次性事件（如 Toast/导航）。
     */
    private val hotState = MutableStateFlow("init")

    suspend fun runAllOperatorDemos(logger: (String) -> Unit = ::println) {
        creationOperators(logger)
        transformOperators(logger)
        filterAndLimitOperators(logger)
        combineAndFlattenOperators(logger)
        sideEffectAndErrorOperators(logger)
        contextAndBackpressureOperators(logger)
        terminalOperators(logger)
        hotFlowOperators(logger)
    }

    // ------------------------------ 1. 创建类 ------------------------------
    private suspend fun creationOperators(logger: (String) -> Unit) {
        logger("===== [创建类操作符] =====")

        // flowOf: 直接从固定值创建
        val fromFlowOf = flowOf(1, 2, 3)

        // asFlow: 集合/序列转 Flow
        val fromCollection = listOf(4, 5, 6).asFlow()

        // flow { }: 自定义发射逻辑，支持挂起函数
        val fromBuilder = flow {
            repeat(3) {
                delay(30)
                emit(it + 7)
            }
        }

        // callbackFlow: 回调 API -> Flow（务必 awaitClose 释放资源，防泄漏）
        val fromCallback = callbackFlow {
            val worker = CoroutineScope(Dispatchers.Default).launch {
                repeat(2) {
                    trySend("callback-$it")
                    delay(20)
                }
                close()
            }
            awaitClose {
                worker.cancel()
            }
        }

        merge(fromFlowOf, fromCollection, fromBuilder.map { it * 10 })
            .onEach { logger("create -> $it") }
            .collect()

        fromCallback.onEach { logger("callbackFlow -> $it") }.collect()
    }

    // ------------------------------ 2. 转换类 ------------------------------
    private suspend fun transformOperators(logger: (String) -> Unit) {
        logger("===== [转换类操作符] =====")
        val source = flowOf("1", "2", "x", "3")

        source
            .map { it.toIntOrNull() ?: -1 } // 一进一出转换
            .mapNotNull { it.takeIf { v -> v > 0 } } // 过滤 null
            .transform { value ->
                // transform 可一进多出/零出，比 map 更灵活
                emit("value=$value")
                emit("double=${value * 2}")
            }
            .onEach { logger("transform -> $it") }
            .collect()

        flowOf(1, 2, 3, 4)
            .runningFold(0) { acc, item -> acc + item } // 每步累计值，含初始值
            .onEach { logger("runningFold -> $it") }
            .collect()

        flowOf(1, 2, 3, 4)
            .runningReduce { acc, item -> acc + item } // 每步累计值，无初始值
            .onEach { logger("runningReduce -> $it") }
            .collect()

        flowOf(1, 2, 3)
            .scan(100) { acc, value -> acc + value } // scan 与 runningFold 语义接近
            .onEach { logger("scan -> $it") }
            .collect()
    }

    // ------------------------------ 3. 过滤/截断类 ------------------------------
    private suspend fun filterAndLimitOperators(logger: (String) -> Unit) {
        logger("===== [过滤/截断类操作符] =====")

        flowOf(1, 2, 3, 4, 5, 6)
            .filter { it % 2 == 0 }
            .filterNot { it == 4 }
            .take(2)
            .onEach { logger("filter/take -> $it") }
            .collect()

        flowOf(1, 2, 3, 4, 5, 6)
            .drop(2)
            .takeWhile { it < 6 }
            .dropWhile { it == 3 }
            .onEach { logger("drop/takeWhile -> $it") }
            .collect()

        flowOf("A", "A", "B", "B", "C", "C")
            .distinctUntilChanged()
            .onEach { logger("distinctUntilChanged -> $it") }
            .collect()

        data class User(val id: Long, val name: String)
        flowOf(
            User(1, "Tom"),
            User(1, "Tom 2"),
            User(2, "Jerry")
        )
            .distinctUntilChangedBy { it.id }
            .onEach { logger("distinctBy(id) -> $it") }
            .collect()

        flowOf<Any>(1, "A", 2, "B")
            .filterIsInstance<String>()
            .onEach { logger("filterIsInstance<String> -> $it") }
            .collect()
    }

    // ------------------------------ 4. 组合/扁平化类 ------------------------------
    private suspend fun combineAndFlattenOperators(logger: (String) -> Unit) {
        logger("===== [组合/扁平化类操作符] =====")

        val f1 = flow {
            emit(1)
            delay(40)
            emit(2)
        }
        val f2 = flow {
            delay(20)
            emit("A")
            delay(40)
            emit("B")
        }

        // zip: 严格一一配对，任一结束即停止
        f1.zip(f2) { n, s -> "$n-$s" }
            .onEach { logger("zip -> $it") }
            .collect()

        // combine: 任一上游更新都会组合“最新值”
        f1.combine(f2) { n, s -> "$n+$s" }
            .onEach { logger("combine -> $it") }
            .collect()

        // merge: 并发合并，不做配对
        merge(f1.map { "L$it" }, f2.map { "R$it" })
            .onEach { logger("merge -> $it") }
            .collect()

        val nested = flowOf(
            flowOf("A1", "A2"),
            flowOf("B1", "B2")
        )

        // flattenConcat: 子流串行，顺序稳定
        nested.flattenConcat().onEach { logger("flattenConcat -> $it") }.collect()

        // flattenMerge: 子流并发，吞吐高但顺序不稳定
        nested.flattenMerge(concurrency = 2).onEach { logger("flattenMerge -> $it") }.collect()

        val queryFlow = flowOf("k", "ko", "kot")
        queryFlow
            .flatMapConcat { q ->
                flowOf("$q-1", "$q-2")
            }
            .onEach { logger("flatMapConcat -> $it") }
            .collect()

        queryFlow
            .flatMapMerge { q ->
                flow {
                    emit("$q-M1")
                    delay(15)
                    emit("$q-M2")
                }
            }
            .onEach { logger("flatMapMerge -> $it") }
            .collect()

        // flatMapLatest: 新值到来会取消旧任务，搜索输入场景最常用
        queryFlow
            .flatMapLatest { q ->
                flow {
                    emit("latest-start-$q")
                    delay(35)
                    emit("latest-end-$q")
                }
            }
            .onEach { logger("flatMapLatest -> $it") }
            .collect()
    }

    // ------------------------------ 5. 副作用/错误处理类 ------------------------------
    private suspend fun sideEffectAndErrorOperators(logger: (String) -> Unit) {
        logger("===== [副作用/错误处理类操作符] =====")

        flow {
            emit(1)
            emit(2)
            error("network failed")
        }
            .onStart { logger("onStart: 开始加载") }
            .onEach { logger("onEach: value=$it") } // 仅做副作用，不改数据
            .catch { e ->
                // catch 只捕获其上游异常，放置位置非常关键
                logger("catch: ${e.message}")
                emit(-1) // 可发降级值，避免 UI 中断
            }
            .onCompletion { cause ->
                logger("onCompletion: cause=${cause?.message}")
            }
            .onEmpty { emit(0) }
            .collect { logger("collect -> $it") }

        var retryCounter = 0
        flow {
            if (retryCounter++ < 2) error("transient error")
            emit("success after retry")
        }
            .retry(2) { e ->
                // retry: 简单重试；不要无脑重试 4xx 之类确定性错误
                e is IllegalStateException
            }
            .catch { emit("fallback: ${it.message}") }
            .collect { logger("retry -> $it") }

        var retryWhenCounter = 0
        flow {
            if (retryWhenCounter++ < 2) error("server busy")
            emit("retryWhen success")
        }
            .retryWhen { cause, attempt ->
                val shouldRetry = cause is IllegalStateException && attempt < 3
                if (shouldRetry) delay(50L * (attempt + 1)) // 简易退避
                shouldRetry
            }
            .catch { emit("retryWhen fallback: ${it.message}") }
            .collect { logger("retryWhen -> $it") }
    }

    // ------------------------------ 6. 上下文/背压/节流类 ------------------------------
    private suspend fun contextAndBackpressureOperators(logger: (String) -> Unit) {
        logger("===== [上下文/背压/节流类] =====")

        val fastProducer = flow {
            repeat(8) {
                emit(it)
                delay(10)
            }
        }.flowOn(Dispatchers.Default) // 只改变上游发射上下文

        fastProducer
            .buffer(capacity = 4) // 生产和消费解耦；容量过大会增内存占用
            .onEach {
                delay(20)
                logger("buffer -> $it")
            }
            .collect()

        fastProducer
            .conflate() // 只保留最新值，适合“只关心最新 UI 状态”
            .onEach {
                delay(20)
                logger("conflate -> $it")
            }
            .collect()

        fastProducer.collectLatest { value ->
            // collectLatest: 新值到来会取消上一条处理（例如取消旧渲染）
            delay(25)
            logger("collectLatest -> $value")
        }

        flow {
            emit("k")
            delay(30)
            emit("ko")
            delay(30)
            emit("kot")
            delay(120)
            emit("kotlin")
        }
            .debounce(80) // 防抖：静默时间后才发射
            .onEach { logger("debounce -> $it") }
            .collect()

        flow {
            repeat(10) {
                emit(it)
                delay(25)
            }
        }
            .sample(80) // 采样：固定窗口取最新值
            .onEach { logger("sample -> $it") }
            .collect()
    }

    // ------------------------------ 7. 终止/收集类 ------------------------------
    private suspend fun terminalOperators(logger: (String) -> Unit) {
        logger("===== [终止/收集类] =====")

        val numbers = flowOf(2, 4, 6, 8)
        logger("first = ${numbers.first()}")
        logger("firstOrNull = ${flowOf<Int>().firstOrNull()}")
        logger("last = ${numbers.last()}")
        logger("single = ${flowOf(7).single()}")
        logger("singleOrNull = ${flowOf(1, 2).singleOrNull()}")
        logger("count = ${numbers.count()}")
        logger("reduce = ${numbers.reduce { acc, i -> acc + i }}")
        logger("fold = ${numbers.fold(10) { acc, i -> acc + i }}")
        logger("toList = ${numbers.toList()}")
        logger("toSet = ${flowOf(1, 1, 2, 2).toSet()}")

        // launchIn: 适合“声明式订阅”；记得由外部 scope 控制生命周期
        val localScope = CoroutineScope(Dispatchers.Default)
        flowOf("A", "B", "C")
            .onEach { logger("launchIn -> $it") }
            .launchIn(localScope)
        delay(80)
        localScope.cancel()
    }

    // ------------------------------ 8. 热流/共享类 ------------------------------
    private suspend fun hotFlowOperators(logger: (String) -> Unit) {
        logger("===== [热流/共享类] =====")

        // StateFlow: 保存当前状态
        hotState.value = "loading"
        hotState.value = "success"
        logger("stateFlow current=${hotState.value}")

        // SharedFlow: 事件广播
        hotEventBus.emit("open_detail")
        hotEventBus.emit("show_toast")

        val appScope = CoroutineScope(Dispatchers.Default)
        val upstream = flow {
            emit("remote-1")
            delay(30)
            emit("remote-2")
        }

        // stateIn/shareIn: 冷流转热流，避免多订阅重复请求
        val state = upstream.stateIn(
            scope = appScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = "initial"
        )
        val shared = upstream.shareIn(
            scope = appScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            replay = 1
        )

        logger("stateIn current=${state.value}")
        shared.take(2).collect { logger("shareIn -> $it") }
        appScope.cancel()
    }
}
