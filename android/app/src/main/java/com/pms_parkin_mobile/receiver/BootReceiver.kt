package com.pms_parkin_mobile.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pms_parkin_mobile.service.App
import com.pms_parkin_mobile.service.BluetoothService
import com.pms_parkin_mobile.service.SensorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getAction()
        if (Intent.ACTION_BOOT_COMPLETED != action && "android.intent.action.QUICKBOOT_POWERON" != action) return

        App.instance.init(context)

        if (!App.instance.isServiceFlag) {
            Log.d("BootReceiver", "ServiceFlag OFF - 서비스 미시작")
            return
        }

        Log.d("BootReceiver", "부팅 완료 - BluetoothService / SensorService 재시작")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, BluetoothService::class.java))
            context.startService(Intent(context, SensorService::class.java))
        } else {
            context.startService(Intent(context, BluetoothService::class.java))
            context.startService(Intent(context, SensorService::class.java))
        }
    }
}
