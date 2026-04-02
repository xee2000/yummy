package com.pms_parkin_mobile.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pms_parkin_mobile.service.BluetoothService

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_DISMISSED: String =
            "com.pms_parkin_mobile.NOTIFICATION_DISMISSED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("NotificationActionReceiver", "onReceive action: $action")

        if (ACTION_NOTIFICATION_DISMISSED == action) {
            Log.d("NotificationActionReceiver", "포그라운드 알림 삭제 감지 → 알림 복구 요청")
            val serviceIntent = Intent(context, BluetoothService::class.java)
            serviceIntent.action = "RESTORE_FOREGROUND"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}