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
import com.pms_parkin_mobile.service.BleScanner


class MainApplication : Application(), ReactApplication {

  companion object {
    @Volatile private var instance: MainApplication? = null

    @JvmStatic
    fun getInstance(): MainApplication =
      instance ?: throw IllegalStateException("MainApplication is not initialized yet.")

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
    instance = this
    Log.d("MainApplication", "onCreate called")
    App.getInstance().init(applicationContext)

    // 프로세스 재시작(Android Studio 재시작, 시스템 kill 후 복구 등) 시
    // React Native 초기화 전에 serviceFlag=true이면 즉시 서비스 재기동
    if (App.getInstance().isServiceFlag()) {
      Log.d("MainApplication", "serviceFlag=true → BleScanner 자동 재시작")
      val serviceIntent = Intent(applicationContext, BleScanner::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
      } else {
        applicationContext.startService(serviceIntent)
      }
    }

    loadReactNative(this)
  }
}