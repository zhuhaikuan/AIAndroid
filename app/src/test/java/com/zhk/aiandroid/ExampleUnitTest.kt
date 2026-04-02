package com.zhk.aiandroid

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

        val options = UiAutomator2Options()
    }

    @Test
    fun testAppiumClient() {
        val options = UiAutomator2Options()
            .setUdid("N0OR180136")
            .setApp("C:\\D\\app-debug.apk")
        // 关键：跳过设备初始化和服务器安装
//            .setSkipDeviceInitialization(true)
//            .setSkipServerInstallation(true)
//        // 禁用应用签名验证
//            .setNoReset(true)
        val driver = AndroidDriver( // The default URL in Appium 1 is http://127.0.0.1:4723/wd/hub
            URI("http://10.46.56.64:4723").toURL(), options
        )
        try {
            val el = driver.findElement(AppiumBy.xpath("//android.widget.TextView[@text='Hello Android!']"))
            el.click()
            driver.pageSource
        } finally {
            driver.quit()
        }
    }
}