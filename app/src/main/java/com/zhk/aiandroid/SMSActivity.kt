package com.zhk.aiandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.zhk.aiandroid.ui.theme.ui.theme.AIAndroidTheme

data class SmsMessage(
    val id: String,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int
) {
    companion object {
        const val TYPE_INBOX = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3

        fun getTypeText(type: Int): String {
            return when (type) {
                TYPE_INBOX -> "收件箱"
                TYPE_SENT -> "已发送"
                TYPE_DRAFT -> "草稿"
                else -> "未知"
            }
        }
    }
}

class SMSActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadSmsMessages()
        } else {
            permissionDenied = true
        }
    }

    private var permissionDenied by mutableStateOf(false)

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SMSActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SMSContent(
                        modifier = Modifier.padding(innerPadding),
                        onRequestPermission = { checkAndRequestPermission() },
                        permissionDenied = permissionDenied
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadSmsMessages()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }

    private fun loadSmsMessages() {
        val messages = readSmsFromDevice(this)
        smsData = messages
    }

    private var smsData by mutableStateOf<List<SmsMessage>>(emptyList())

    @Composable
    fun SMSContent(
        modifier: Modifier = Modifier,
        onRequestPermission: () -> Unit,
        permissionDenied: Boolean
    ) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                loadSmsMessages()
            }
        }

        when {
            permissionDenied -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "需要短信权限才能查看",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRequestPermission) {
                            Text("授予权限")
                        }
                    }
                }
            }
            smsData.isEmpty() && !permissionDenied -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载中...")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestPermission) {
                            Text("检查权限并加载")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "共 ${smsData.size} 条短信",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(smsData) { message ->
                        SmsItem(message = message)
                    }
                }
            }
        }
    }

    @Composable
    fun SmsItem(message: SmsMessage, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (message.type) {
                    SmsMessage.TYPE_INBOX -> Color(0xFFE3F2FD)
                    SmsMessage.TYPE_SENT -> Color(0xFFF1F8E9)
                    else -> Color.White
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.address,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTimestamp(message.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SmsMessage.getTypeText(message.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
}

fun readSmsFromDevice(context: Context, maxCount: Int = 100): List<SmsMessage> {
    val smsList = mutableListOf<SmsMessage>()

    try {
        val uri = "content://sms/".toUri()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $maxCount"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val address = it.getString(addressIndex) ?: "未知号码"
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)

                smsList.add(
                    SmsMessage(
                        id = id,
                        address = address,
                        body = body,
                        date = date,
                        type = type
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return smsList
}
