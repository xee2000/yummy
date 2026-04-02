package com.pms_parkin_mobile.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.service.UserDataSingleton

class OpenLobbyAlarm : Service() {

    companion object {
        private const val TAG = "OpenLobbyAlarm"
        private const val NOTIFICATION_ID = 13
        private const val CHANNEL_ID = "lobby_open_channel"
        private const val CHANNEL_NAME = "Lobby Open Notification"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OpenLobbyAlarm onCreate()")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 사용하지 않을 경우 null 반환이 일반적입니다.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OpenLobbyAlarm onStartCommand()")

        // 1. 알림 채널 생성 (Android O 이상)
        createNotificationChannel()

        // 2. 알림 빌드
        val userName = UserDataSingleton.instance.getUserName() ?: "입주민"

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 유효한 아이콘으로 변경 권장
            .setContentTitle("공동현관문 열림")
            .setContentText("${userName}님 공동현관문이 열렸습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        // 3. 포그라운드 서비스 시작
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}")
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "공동현관문 열림 알림을 위한 채널입니다."
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OpenLobbyAlarm destroyed")
    }
}