package com.pms_parkin_mobile

import android.app.Application
import android.content.Context
import android.util.Log // ✅ 추가
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.pms_parkin_mobile.module.ParkingPackage
import com.pms_parkin_mobile.service.App


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
    loadReactNative(this)

  }
}