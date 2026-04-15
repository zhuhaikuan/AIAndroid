package com.zhk.aiandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun testAppiumClient() {
        val options = UiAutomator2Options()
            .setPlatformName("Android")
            .setAutomationName("UiAutomator2")
            .setUdid("N0OR180136")
            .setApp("C:\\D\\app-debug.apk")
            .setAppPackage("com.zhk.aiandroid")
            .setAppActivity("com.zhk.aiandroid.MainActivity")

        // 使用 OkHttp 的 Appium 驱动
        val driver = AndroidDriver(
            URI("http://10.46.56.203:4723").toURL(),
            options
        )

        try {
            val button = driver.findElement(AppiumBy.xpath("//android.widget.TextView[@text=\"Click Me\"]"))
            button.click()
            Thread.sleep(1000)
            val pageSource = driver.pageSource
            assertTrue("Toast 应该在页面源中", pageSource?.contains("Clicked") ?: false)
        } finally {
            driver.quit()
        }
    }
}