package com.zhk.aiandroid

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zhk.aiandroid.ui.theme.AIAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier, context: Context = LocalContext.current) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Hello Tester, Welcome to AI Android"
        )
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show() }
        ) {
            Text(text = "Click Me")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { SMSActivity.start(context) }
        ) {
            Text(text = "SMS Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { LocationActivity.start(context) }
        ) {
            Text(text = "Location Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { NotificationActivity.start(context) }
        ) {
            Text(text = "Notification Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { AppinfoActivity.start(context) }
        ) {
            Text(text = "AppInfo Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("ClickMeButton"),
            onClick = { CalendarActivity.start(context) }
        ) {
            Text(text = "Calendar Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("DataStoreDemoEntryButton"),
            onClick = { DataStoreDemoActivity.start(context) }
        ) {
            Text(text = "DataStore Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("RoomDemoEntryButton"),
            onClick = { RoomDemoActivity.start(context) }
        ) {
            Text(text = "Room Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("FlowDemoEntryButton"),
            onClick = { FlowDemoActivity.start(context) }
        ) {
            Text(text = "Flow Demo")
        }
        Button(
            modifier = Modifier.padding(16.dp).testTag("CoroutineDemoEntryButton"),
            onClick = { CoroutineDemoActivity.start(context) }
        ) {
            Text(text = "Coroutine Demo")
        }
    }
}