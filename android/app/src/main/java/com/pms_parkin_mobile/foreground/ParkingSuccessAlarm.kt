package com.pms_parkin_mobile.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.service.UserDataSingleton

class ParkingSuccessAlarm : Service() {
    private val NOTIFICATION_ID = 13
    private val CHANNEL_ID = "balem_activity"
    private val CHANNEL_NAME = "Balem Activity"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ActivityService onCreate()")
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.rn_edit_text_material)
            .setContentTitle("주차위치서비스")
            .setContentText(UserDataSingleton.instance.getUserName() + "님 주차가 완료되었습니다.")
            .setOngoing(true)
            .setAutoCancel(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setDescription("앱 실행중 알림 채널")
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
        Log.d(TAG, "ActivityService started with foreground notification.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ActivityService destroyed")
    }

    companion object {
        private val TAG: String = ParkingSuccessAlarm::class.java.getSimpleName()
    }
}
