package com.pms_parkin_mobile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.pms_parkin_mobile.module.ParkingPackage
import com.pms_parkin_mobile.service.App
import com.pms_parkin_mobile.service.BluetoothService
import com.pms_parkin_mobile.service.SensorService

class MainApplication : Application(), ReactApplication {

  companion object {
    @Volatile private var _instance: MainApplication? = null

    @JvmStatic
    fun getInstance(): MainApplication =
      _instance ?: throw IllegalStateException("MainApplication is not initialized yet.")

    @JvmStatic
    fun getAppContext(): Context = getInstance().applicationContext
  }

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList = PackageList(this).packages.apply {
        add(ParkingPackage())
      },
    )
  }

  override fun onCreate() {
    super.onCreate()
    _instance = this
    Log.d("MainApplication", "onCreate called")

    // 정상적으로 스펠링이 입력된 부분
    App.instance.init(applicationContext)

    // 💡 수정 완료: instace -> instance 오타 수정 및 괄호() 제거
    if (App.instance.isServiceFlag) {
      Log.d("MainApplication", "serviceFlag=true → BluetoothService / SensorService 자동 시작")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, BluetoothService::class.java))
        applicationContext.startService(Intent(applicationContext, SensorService::class.java))
      } else {
        applicationContext.startService(Intent(applicationContext, BluetoothService::class.java))
        applicationContext.startService(Intent(applicationContext, SensorService::class.java))
      }
    }

    loadReactNative(this)
  }
}