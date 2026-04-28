package com.zhk.aiandroid

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zhk.aiandroid.ui.theme.AIAndroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CalendarActivity"

        fun start(context: Context) {
            val intent = Intent(context, CalendarActivity::class.java)
            context.startActivity(intent)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "日历权限已授予")
            loadCalendarEvents()
        } else {
            Log.e(TAG, "日历权限被拒绝")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                CalendarScreen()
            }
        }

        checkAndRequestPermission()
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "日历权限已获得")
                loadCalendarEvents()
            }
            else -> {
                Log.d(TAG, "请求日历权限")
                requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    private fun loadCalendarEvents() {
        Log.d(TAG, "========== 开始加载用户事件 ==========")

        val allCalendars = getAllCalendars(this)
        Log.d(TAG, "设备上的日历列表:")
        allCalendars.forEach { calendar ->
            Log.d(TAG, "  - ${calendar.name} (账户: ${calendar.accountType}, 可见: ${calendar.visible})")
        }

        val userEvents = getUserEvents(this)
        Log.d(TAG, "获取到 ${userEvents.size} 个用户事件")

        userEvents.forEach { event ->
            val duration = if (event.isAllDay) {
                "全天"
            } else {
                val durationMs = event.endTime - event.startTime
                val durationHours = durationMs / (1000 * 60 * 60)
                val durationMinutes = (durationMs / (1000 * 60)) % 60
                if (durationHours > 0) {
                    "${durationHours}小时${durationMinutes}分钟"
                } else {
                    "${durationMinutes}分钟"
                }
            }

            Log.d(TAG, "  ✅ ${event.title}")
            Log.d(TAG, "     日历: ${event.calendarName}")
            Log.d(TAG, "     时间: ${formatTimestamp(event.startTime)} ~ ${formatTimestamp(event.endTime)}")
            Log.d(TAG, "     时长: $duration")
            if (event.location.isNotEmpty()) {
                Log.d(TAG, "     地点: ${event.location}")
            }
        }

        Log.d(TAG, "========== 加载完成 ==========")
    }

    @Composable
    fun CalendarScreen() {
        var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "我的日历事件",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (events.isEmpty()) {
                    Text(
                        text = "暂无事件",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text(
                        text = "共 ${events.size} 个事件",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn {
                        items(events) { event ->
                            CalendarEventCard(event)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        events = getUserEvents(this)
    }

    @Composable
    fun CalendarEventCard(event: CalendarEvent) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "日历: ${event.calendarName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (event.isAllDay) {
                    Text(
                        text = "📅 全天事件",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = " ${formatTimestamp(event.startTime)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "   ~ ${formatTimestamp(event.endTime)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val durationMs = event.endTime - event.startTime
                    val durationMinutes = durationMs / (1000 * 60)
                    if (durationMinutes < 60) {
                        Text(
                            text = "⏱️ 时长: ${durationMinutes}分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        val hours = durationMinutes / 60
                        val mins = durationMinutes % 60
                        Text(
                            text = "⏱️ 时长: ${hours}小时${mins}分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (event.hasRecurrence) {
                    Text(
                        text = "🔄 重复事件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (event.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${event.location}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val location: String,
    val calendarName: String,
    val isAllDay: Boolean = false,
    val hasRecurrence: Boolean = false,
    val organizer: String = ""
)

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountType: String,
    val visible: Boolean
)

fun getAllCalendars(context: Context): List<CalendarInfo> {
    val calendars = mutableListOf<CalendarInfo>()
    val contentResolver = context.contentResolver

    try {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1) ?: "未知"
                val accountType = it.getString(2) ?: "unknown"
                val visible = it.getInt(3) == 1

                calendars.add(CalendarInfo(id, name, accountType, visible))
            }
        }
    } catch (e: Exception) {
        Log.e("CalendarHelper", "获取日历列表失败", e)
    }

    return calendars
}

fun getUserEvents(context: Context): List<CalendarEvent> {
    val allEvents = getAllCalendarEvents(context)

    val userEvents = allEvents.filter { event ->
        !isHolidayEvent(event)
    }

    Log.d("CalendarHelper", "总事件数: ${allEvents.size}, 用户事件数: ${userEvents.size}, 过滤掉: ${allEvents.size - userEvents.size}")

    return userEvents
}

private fun isHolidayEvent(event: CalendarEvent): Boolean {
    val holidayCalendarKeywords = listOf(
        "holiday",
        "节假日",
        "法定节假日",
        "china holidays",
        "中国节假日"
    )

    val calendarNameLower = event.calendarName.lowercase(Locale.getDefault())

    val isHolidayCalendar = holidayCalendarKeywords.any { keyword ->
        calendarNameLower.contains(keyword)
    }

    if (isHolidayCalendar) {
        Log.d("CalendarFilter", "过滤节假日日历事件: ${event.title}")
        return true
    }

    return false
}

fun getAllCalendarEvents(context: Context): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val contentResolver: ContentResolver = context.contentResolver

    try {
        val uri: Uri = CalendarContract.Events.CONTENT_URI

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.ORGANIZER
        )

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        if (cursor == null) {
            Log.e("CalendarQuery", "查询返回 null")
            return events
        }

        Log.d("CalendarQuery", "查询到 ${cursor.count} 条事件记录")

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
            val calendarIdIndex = it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
            val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val recurrenceIndex = it.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            val organizerIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ORGANIZER)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val title = it.getString(titleIndex) ?: "无标题"
                val description = it.getString(descIndex) ?: ""
                val startTime = it.getLong(startIndex)
                val endTime = it.getLong(endIndex)
                val location = it.getString(locationIndex) ?: ""
                val calendarId = it.getLong(calendarIdIndex)
                val isAllDay = it.getInt(allDayIndex) == 1
                val recurrenceRule = it.getString(recurrenceIndex)
                val hasRecurrence = !recurrenceRule.isNullOrEmpty()
                val organizer = it.getString(organizerIndex) ?: ""

                val (calendarName, _) = getCalendarInfo(contentResolver, calendarId)

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        location = location,
                        calendarName = calendarName,
                        isAllDay = isAllDay,
                        hasRecurrence = hasRecurrence,
                        organizer = organizer
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("CalendarHelper", "读取日历事件失败", e)
    }

    return events
}

private fun getCalendarInfo(contentResolver: ContentResolver, calendarId: Long): Pair<String, String> {
    val uri = CalendarContract.Calendars.CONTENT_URI

    val projection = arrayOf(
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE
    )

    val selection = "${CalendarContract.Calendars._ID} = ?"
    val selectionArgs = arrayOf(calendarId.toString())

    val cursor = contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null
    )

    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountTypeIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)

            val name = it.getString(nameIndex) ?: "未知日历"
            val accountType = it.getString(accountTypeIndex) ?: "unknown"

            return Pair(name, accountType)
        }
    }

    return Pair("未知日历", "unknown")
}

//@Preview(showBackground = true)
//@Composable
//fun CalendarScreenPreview() {
//    AIAndroidTheme {
//        CalendarScreen()
//    }
//}

