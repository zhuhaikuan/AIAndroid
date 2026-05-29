package com.zhk.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kotlin Coroutine 核心能力示例集合（按功能分组）。
 *
 * 设计目标：
 * 1) 覆盖 Android 业务最常见、最关键的协程操作。
 * 2) 每个分组都给出“可运行”的行为日志，方便在 Demo 页面验收。
 * 3) 注释里附带实战避坑建议，便于直接迁移到业务代码。
 *
 * 注意：
 * - 协程 API 很多，“全操作”在工程语义上通常指“核心并发能力完整覆盖”而非逐个 API 穷举。
 */
@OptIn(ExperimentalCoroutinesApi::class)
object CoroutineOperatorsCookbook {

    suspend fun runAllOperatorDemos(logger: (String) -> Unit = ::println) {
        logger("===== Coroutine 全功能演示开始 =====")
        basicBuildersAndStructuredConcurrency(logger)
        contextSwitchingAndDispatcherUsage(logger)
        jobLifecycleAndLazyStart(logger)
        cancellationAndCooperativeChecks(logger)
        timeoutAndFallback(logger)
        exceptionHandlingAndSupervisor(logger)
        synchronizationPrimitives(logger)
        channelAndSelect(logger)
        stateFlowAsUiState(logger)
        logger("===== Coroutine 全功能演示结束 =====")
    }

    /**
     * 1) launch / async / await / coroutineScope / join
     *
     * 避坑：
     * - 需要结果用 async + await；只做副作用用 launch。
     * - 不要在业务里滥用 GlobalScope，会丢失生命周期约束，难以取消。
     */
    private suspend fun basicBuildersAndStructuredConcurrency(logger: (String) -> Unit) {
        logger("[1] 基础构建器与结构化并发")
        coroutineScope {
            val jobA = launch(CoroutineName("job-A")) {
                delay(60)
                logger("launch job-A 完成")
            }
            val jobB = launch(CoroutineName("job-B")) {
                delay(90)
                logger("launch job-B 完成")
            }

            val deferredA = async { delay(40); 10 }
            val deferredB = async { delay(70); 20 }
            val deferredC = async { delay(20); 30 }
            val sum = awaitAll(deferredA, deferredB, deferredC).sum()
            logger("async/awaitAll 求和结果 = $sum")

            joinAll(jobA, jobB)
            logger("joinAll 等待 launch 子任务全部结束")
        }
    }

    /**
     * 2) Dispatchers + withContext
     *
     * 避坑：
     * - 计算密集任务用 Default；IO 密集任务用 IO。
     * - withContext 是“上下文切换 + 结果返回”的主力，不要手动开启过多嵌套协程。
     */
    private suspend fun contextSwitchingAndDispatcherUsage(logger: (String) -> Unit) {
        logger("[2] 调度器与上下文切换")
        val cpuResult = withContext(Dispatchers.Default) {
            (1..5000).sum()
        }
        logger("Dispatchers.Default 计算结果 = $cpuResult")

        val ioResult = withContext(Dispatchers.IO) {
            delay(50)
            "IO task done"
        }
        logger("Dispatchers.IO 执行结果 = $ioResult")
    }

    /**
     * 3) Job 生命周期与启动模式：LAZY / invokeOnCompletion
     *
     * 避坑：
     * - LAZY 任务如果忘记 start/await，会一直不执行。
     * - invokeOnCompletion 内只做轻量逻辑，复杂工作建议切回协程作用域。
     */
    private suspend fun jobLifecycleAndLazyStart(logger: (String) -> Unit) {
        logger("[3] Job 生命周期与延迟启动")
        coroutineScope {
            val lazyTask = async(start = CoroutineStart.LAZY) {
                delay(30)
                "lazy result"
            }
            logger("LAZY 任务创建后默认未执行")
            lazyTask.start()
            val result = lazyTask.await()
            logger("LAZY 任务显式启动后结果 = $result")

            val trackedJob = launch {
                delay(20)
            }
            trackedJob.invokeOnCompletion { throwable ->
                val message = throwable?.message ?: "normal completion"
                logger("invokeOnCompletion -> $message")
            }
            trackedJob.join()
        }
    }

    /**
     * 4) 协作式取消：isActive / ensureActive / yield / cancelAndJoin / NonCancellable
     *
     * 避坑：
     * - 协程取消是协作式，不是线程中断；长循环中要主动检查取消信号。
     * - finally 里的收尾逻辑如果还要调用挂起函数，需要 NonCancellable 包裹。
     */
    private suspend fun cancellationAndCooperativeChecks(logger: (String) -> Unit) {
        logger("[4] 取消与协作式检查")
        coroutineScope {
            val runningJob = launch {
                try {
                    var count = 0
                    while (isActive) {
                        ensureActive()
                        count++
                        if (count % 3 == 0) {
                            logger("任务处理中...count=$count")
                        }
                        delay(20)
                        yield()
                    }
                } finally {
                    withContext(NonCancellable) {
                        delay(10)
                        logger("取消后清理资源完成（NonCancellable）")
                    }
                }
            }

            delay(120)
            runningJob.cancelAndJoin()
            logger("job.cancelAndJoin() 已执行")
        }
    }

    /**
     * 5) 超时控制：withTimeout / withTimeoutOrNull
     *
     * 避坑：
     * - UI 请求请一定做超时保护，避免无上限等待。
     * - 业务允许降级时优先用 withTimeoutOrNull，避免异常分支泛滥。
     */
    private suspend fun timeoutAndFallback(logger: (String) -> Unit) {
        logger("[5] 超时控制与降级")
        val nullableResult = withTimeoutOrNull(80) {
            delay(120)
            "never reaches"
        }
        logger("withTimeoutOrNull 结果 = ${nullableResult ?: "timeout fallback"}")

        val timeoutResult = try {
            withTimeout(60) {
                delay(30)
                "ok"
            }
        } catch (_: CancellationException) {
            "timeout"
        }
        logger("withTimeout 结果 = $timeoutResult")
    }

    /**
     * 6) 异常处理：CoroutineExceptionHandler + supervisorScope
     *
     * 避坑：
     * - launch 的异常如果不处理会走到线程未捕获异常；async 需在 await 时处理。
     * - 父子隔离场景（并行加载多个卡片）优先 supervisorScope，避免“一处失败全盘取消”。
     */
    private suspend fun exceptionHandlingAndSupervisor(logger: (String) -> Unit) {
        logger("[6] 异常处理与监督作用域")
        val handler = CoroutineExceptionHandler { _, throwable ->
            logger("CoroutineExceptionHandler 捕获: ${throwable.message}")
        }

        supervisorScope {
            val childA = async(handler) {
                delay(20)
                error("childA failed")
            }
            val childB = async {
                delay(40)
                "childB success"
            }

            val aResult = runCatching { childA.await() }.getOrElse { "A-FALLBACK" }
            val bResult = runCatching { childB.await() }.getOrElse { "B-FALLBACK" }
            logger("supervisorScope 结果 => a=$aResult, b=$bResult")
        }
    }

    /**
     * 7) 同步原语：Mutex / Semaphore
     *
     * 避坑：
     * - 多协程写共享变量必须保护临界区，不能依赖“看起来顺序执行”。
     * - Semaphore 适用于“并发限流”，比如限制同时最多 3 个图片下载任务。
     */
    private suspend fun synchronizationPrimitives(logger: (String) -> Unit) {
        logger("[7] 同步原语（Mutex / Semaphore）")
        val mutex = Mutex()
        var safeCounter = 0
        coroutineScope {
            repeat(20) {
                launch {
                    repeat(5) {
                        mutex.withLock {
                            safeCounter++
                        }
                    }
                }
            }
        }
        logger("Mutex 保护后计数 = $safeCounter（预期 100）")

        val semaphore = Semaphore(permits = 3)
        val runningCount = AtomicInteger(0)
        coroutineScope {
            repeat(8) { idx ->
                launch {
                    semaphore.acquire()
                    try {
                        val current = runningCount.incrementAndGet()
                        logger("任务$idx 开始执行，并发中=$current")
                        delay(40)
                    } finally {
                        val current = runningCount.decrementAndGet()
                        logger("任务$idx 结束执行，并发中=$current")
                        semaphore.release()
                    }
                }
            }
        }
    }

    /**
     * 8) Channel + select
     *
     * 避坑：
     * - Channel 属于“热通道”，有背压语义；用完记得 close，避免消费者永久挂起。
     * - 多路抢占读取可以用 select，适合“谁先返回用谁”的并发策略。
     */
    private suspend fun channelAndSelect(logger: (String) -> Unit) {
        logger("[8] Channel 与 select")
        val channel = Channel<Int>(capacity = Channel.BUFFERED)
        coroutineScope {
            launch {
                repeat(3) {
                    delay(20)
                    channel.send(it + 1)
                }
                channel.close()
            }
            launch {
                for (value in channel) {
                    logger("Channel 接收 = $value")
                }
                logger("Channel 消费完成")
            }.join()
        }

        val fast = Channel<String>(capacity = Channel.RENDEZVOUS)
        val slow = Channel<String>(capacity = Channel.RENDEZVOUS)
        coroutineScope {
            launch {
                delay(30)
                fast.send("FAST_RESULT")
                fast.close()
            }
            launch {
                delay(80)
                slow.send("SLOW_RESULT")
                slow.close()
            }

            repeat(2) {
                val winner = select<String> {
                    fast.onReceiveCatching { result ->
                        result.getOrNull()?.let { "from fast=$it" } ?: "fast closed"
                    }
                    slow.onReceiveCatching { result ->
                        result.getOrNull()?.let { "from slow=$it" } ?: "slow closed"
                    }
                }
                logger("select winner -> $winner")
            }
        }
    }

    /**
     * 9) StateFlow 作为 UI 状态容器
     *
     * 避坑：
     * - StateFlow 用来描述“状态”，不要拿来表达“一次性事件”（比如 Toast / 导航）。
     * - 一次性事件建议 SharedFlow / Channel + receiveAsFlow。
     */
    private suspend fun stateFlowAsUiState(logger: (String) -> Unit) {
        logger("[9] StateFlow 状态更新")
        val state = MutableStateFlow(UiProgress(step = 0, message = "init"))
        state.update { it.copy(step = 1, message = "loading") }
        logger("StateFlow step=${state.value.step}, msg=${state.value.message}")
        state.update { it.copy(step = 2, message = "success") }
        logger("StateFlow step=${state.value.step}, msg=${state.value.message}")
    }

    private data class UiProgress(
        val step: Int,
        val message: String,
    )
}
