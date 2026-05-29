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
import androidx.compose.material3.Button
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

class DataStoreDemoActivity : ComponentActivity() {
    private val dataStoreViewModel: DataStoreDemoViewModel by viewModels {
        DataStoreDemoViewModel.factory(applicationContext)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DataStoreDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DataStoreDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        dataStoreViewModel = dataStoreViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun DataStoreDemoScreen(
    modifier: Modifier,
    dataStoreViewModel: DataStoreDemoViewModel,
) {
    val uiState by dataStoreViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = "DataStore 演示页",
            fontWeight = FontWeight.Bold,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = "status: ${uiState.statusMessage}",
        )
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            text = "preferences.userId: ${uiState.appPreferences.userId ?: "null"}",
        )
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            text = "typed.displayName: ${uiState.userSettings.displayName}",
        )
        Text(
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            text = "typed.categories: ${uiState.userSettings.favoriteCategories.joinToString()}",
        )

        Button(
            modifier = Modifier.padding(12.dp).testTag("DataStoreWriteButton"),
            onClick = { dataStoreViewModel.writeDemoData() },
        ) {
            Text(text = "写入 DataStore 示例数据")
        }
        Button(
            modifier = Modifier.padding(12.dp).testTag("DataStoreAddCategoryButton"),
            onClick = { dataStoreViewModel.addFavoriteCategory() },
        ) {
            Text(text = "新增分类")
        }
        Button(
            modifier = Modifier.padding(12.dp).testTag("DataStoreClearButton"),
            onClick = { dataStoreViewModel.clearAll() },
        ) {
            Text(text = "清空 DataStore")
        }
    }
}
