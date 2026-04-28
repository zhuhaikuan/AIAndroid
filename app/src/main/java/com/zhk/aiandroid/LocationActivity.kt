package com.zhk.aiandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zhk.aiandroid.ui.theme.AIAndroidTheme
import com.zhk.aiandroid.utils.PermissionUtils

class LocationActivity : ComponentActivity() {
    lateinit var locationManager: LocationManager

    private val _locationInfo: MutableState<String> = mutableStateOf("waiting for location...")

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    companion object {
        // 定位更新最小时间：1秒（1000ms）
        const val MIN_TIME: Long = 1000
        // 定位更新最小距离：1米
        const val MIN_DISTANCE: Float = 1f

        const val REQUEST_CODE_LOCATION: Int = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocationActivity::class.java)
            context.startActivity(intent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAndroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "TopAppBar") }
                        )
                    },
                    bottomBar = {
                        BottomAppBar {
                            Text(text = "BottomAppBar", modifier = Modifier.padding(1.dp))
                        }
                    },
                    floatingActionButton = {
                        Text(text = "Sensor")
                    },
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
                ) { innerPadding ->
                    Greeting2(
                        locationInfo = _locationInfo.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if(PermissionUtils.hasPermissions(this, *locationPermissions)) {
            getLocation(this)
        } else {
            PermissionUtils.requestPermissions(this, REQUEST_CODE_LOCATION, *locationPermissions)
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLocation(context: Context) {
        // 获取最后一次已知位置（最快，无需等待）
        val lastLocation: Location? = getLastKnownLocation()
        if (lastLocation != null) {
            showLocation(lastLocation)
        }

        // 监听实时位置更新
        requestLocationUpdates()
    }

    /**
     * 获取设备缓存的最后一次位置
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLastKnownLocation(): Location? {
        var gpsLocation: Location? = null
        var networkLocation: Location? = null

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        // 优先返回 GPS 高精度位置
        return gpsLocation ?: networkLocation
    }

    /**
     * 注册实时位置监听
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        // 同时监听 GPS 和 网络 定位
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME,
                MIN_DISTANCE,
                locationListener
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME,
                MIN_DISTANCE,
                locationListener
            )
        }
    }

    /**
     * 位置监听器
     */
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.e("LocationActivity", "onLocationChanged: $location")
            showLocation(location)
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
    }

    /**
     * 展示位置信息
     */
    private fun showLocation(location: Location) {
        val latitude = location.latitude // 纬度
        val longitude = location.longitude // 经度
        val accuracy = location.accuracy // 精度（米）
        val provider = location.provider // 定位来源

        val info = "定位来源：" + provider +
                "\n纬度：" + latitude +
                "\n经度：" + longitude +
                "\n精度：" + accuracy + "米"

        _locationInfo.value = info

        Toast.makeText(this, info, Toast.LENGTH_LONG).show()
    }

    /**
     * 权限申请结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (PermissionUtils.isAllPermissionsGranted(grantResults)) {
                recreate()
            } else {
                Toast.makeText(this, "请开启定位权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        // 页面销毁时停止定位，节省电量
        locationManager.removeUpdates(locationListener)
    }
}

@Composable
fun Greeting2(locationInfo: String, modifier: Modifier = Modifier) {
    Text(
        text = locationInfo,
        modifier = modifier.padding(15.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    AIAndroidTheme {
        Greeting2("locationInfo")
    }
}