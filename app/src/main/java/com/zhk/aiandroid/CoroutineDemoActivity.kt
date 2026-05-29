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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhk.aiandroid.ui.theme.AIAndroidTheme

class CoroutineDemoActivity : ComponentActivity() {

    private val viewModel: CoroutineDemoViewModel by viewModels()

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CoroutineDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CoroutineDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun CoroutineDemoScreen(
    modifier: Modifier,
    viewModel: CoroutineDemoViewModel,
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
            text = "Coroutine 功能测试页",
            fontWeight = FontWeight.Bold,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = "一键运行后会按模块输出协程日志：构建器、上下文、取消、超时、异常、同步原语、Channel、最佳实践。"
        )

        Button(
            modifier = Modifier.padding(12.dp).testTag("CoroutineRunAllButton"),
            onClick = viewModel::runAllCoroutineDemos,
            enabled = !uiState.isRunning
        ) {
            Text(if (uiState.isRunning) "运行中..." else "运行 Coroutine 全功能验证")
        }

        Button(
            modifier = Modifier.padding(4.dp).testTag("CoroutineClearLogButton"),
            onClick = viewModel::clearLogs,
            enabled = !uiState.isRunning
        ) {
            Text("清空日志")
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("CoroutineLogList")
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
