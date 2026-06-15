package com.zhk.aiandroid

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenovo.contentprovider.CustomContentProvider
import com.lenovo.contentprovider.sqlite.DBHelper
import com.zhk.aiandroid.ui.theme.AIAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 使用 [android.content.ContentResolver] 调用 [CustomContentProvider] 暴露的 `users` 表能力。
 */
class ContentProviderDemoActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ContentProviderDemoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContentProviderDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

private data class UserListItem(
    val id: Long,
    val name: String?,
    val email: String?,
    val age: Int?,
)

@Composable
private fun ContentProviderDemoScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var status by remember { mutableStateOf("") }

    fun loadUsers() {
        scope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val uri = CustomContentProvider.CONTENT_URI
            val list = mutableListOf<UserListItem>()
            try {
                resolver.query(
                    uri,
                    null,
                    null,
                    null,
                    "${DBHelper.COLUMN_USER_ID} ASC",
                )?.use { c ->
                    val idIdx = c.getColumnIndexOrThrow(DBHelper.COLUMN_USER_ID)
                    val nameIdx = c.getColumnIndex(DBHelper.COLUMN_USER_NAME)
                    val emailIdx = c.getColumnIndex(DBHelper.COLUMN_USER_EMAIL)
                    val ageIdx = c.getColumnIndex(DBHelper.COLUMN_USER_AGE)
                    while (c.moveToNext()) {
                        list.add(
                            UserListItem(
                                id = c.getLong(idIdx),
                                name = if (nameIdx >= 0 && !c.isNull(nameIdx)) c.getString(nameIdx) else null,
                                email = if (emailIdx >= 0 && !c.isNull(emailIdx)) c.getString(emailIdx) else null,
                                age = if (ageIdx >= 0 && !c.isNull(ageIdx)) c.getInt(ageIdx) else null,
                            ),
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    users = list
                    status = "已加载 ${list.size} 条"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "查询失败: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "ContentResolver + ContentProvider",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "authority: ${CustomContentProvider.AUTHORITY}",
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(text = status, modifier = Modifier.padding(bottom = 12.dp))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(DBHelper.COLUMN_USER_NAME, "user_${System.currentTimeMillis() % 100000}")
                        put(DBHelper.COLUMN_USER_EMAIL, "demo@example.com")
                        put(DBHelper.COLUMN_USER_AGE, (20..55).random())
                    }
                    val inserted = context.contentResolver.insert(
                        CustomContentProvider.CONTENT_URI,
                        values,
                    )
                    withContext(Dispatchers.Main) {
                        status = if (inserted != null) "已插入: $inserted" else "插入失败"
                    }
                    loadUsers()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("插入一条（ContentResolver.insert）")
        }

        Button(
            onClick = { loadUsers() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("刷新列表（ContentResolver.query）")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    if (users.isEmpty()) {
                        withContext(Dispatchers.Main) { status = "没有可删除的行" }
                        return@launch
                    }
                    val first = users.first()
                    val rowUri = ContentUris.withAppendedId(CustomContentProvider.CONTENT_URI, first.id)
                    val n = context.contentResolver.delete(rowUri, null, null)
                    withContext(Dispatchers.Main) { status = "已删除 $n 条（首行 id=${first.id}）" }
                    loadUsers()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("删除第一条（ContentResolver.delete + 行 URI）")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val n = context.contentResolver.delete(
                        CustomContentProvider.CONTENT_URI,
                        null,
                        null,
                    )
                    withContext(Dispatchers.Main) { status = "已清空 $n 条" }
                    loadUsers()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("清空 users 表（delete 集合 URI）")
        }

        if (users.isNotEmpty()) {
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val u = users.first()
                        val values = ContentValues().apply {
                            put(DBHelper.COLUMN_USER_NAME, "${u.name}_upd")
                            put(DBHelper.COLUMN_USER_EMAIL, u.email)
                            put(DBHelper.COLUMN_USER_AGE, (u.age ?: 0) + 1)
                        }
                        val rowUri = ContentUris.withAppendedId(CustomContentProvider.CONTENT_URI, u.id)
                        val n = context.contentResolver.update(rowUri, values, null, null)
                        withContext(Dispatchers.Main) { status = "已更新 $n 条（id=${u.id}）" }
                        loadUsers()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("更新第一条（ContentResolver.update + 行 URI）")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(users, key = { it.id }) { row ->
                Text(
                    text = "_id=${row.id}  name=${row.name}  email=${row.email}  age=${row.age}",
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
