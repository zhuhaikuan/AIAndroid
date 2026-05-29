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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

class RoomDemoActivity : ComponentActivity() {
    private val roomDemoViewModel: RoomDemoViewModel by viewModels {
        RoomDemoViewModel.factory(applicationContext)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RoomDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RoomDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = roomDemoViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomDemoScreen(
    modifier: Modifier,
    viewModel: RoomDemoViewModel,
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
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = "Room 功能测试页",
            fontWeight = FontWeight.Bold,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = "status: ${uiState.statusMessage}",
        )
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            text = "total=${uiState.totalCount}, completed=${uiState.completedCount}",
        )

        Button(
            modifier = Modifier.padding(top = 12.dp).testTag("RoomRunAllButton"),
            onClick = viewModel::runAllRoomOperations,
        ) { Text(text = "一键执行 Room 全量操作") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomSeedButton"),
            onClick = viewModel::seedData,
        ) { Text(text = "初始化测试数据") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomInsertButton"),
            onClick = viewModel::addRandomTodo,
        ) { Text(text = "新增一条任务（Insert）") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomUpdateDoneButton"),
            onClick = viewModel::markFirstAsDone,
        ) { Text(text = "标记首条任务完成（Update）") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomUpdateTitleButton"),
            onClick = viewModel::updateFirstTitle,
        ) { Text(text = "更新首条任务标题（Update）") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomDeleteFirstButton"),
            onClick = viewModel::deleteFirst,
        ) { Text(text = "删除首条任务（Delete）") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomDeleteCompletedButton"),
            onClick = viewModel::deleteCompleted,
        ) { Text(text = "删除已完成任务（Delete by condition）") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomQueryAdvancedButton"),
            onClick = viewModel::queryAdvancedInfo,
        ) { Text(text = "验证 RawQuery / Relation / Exists") }

        Button(
            modifier = Modifier.padding(top = 8.dp).testTag("RoomClearButton"),
            onClick = viewModel::clearAll,
        ) { Text(text = "清空全部（Clear）") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(text = "操作日志：")
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(uiState.operationLogs) { log ->
                Text(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    text = log,
                )
            }
        }
    }
}
