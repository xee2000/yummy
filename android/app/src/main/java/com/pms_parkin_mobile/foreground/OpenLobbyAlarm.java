package com.pms_parkin_mobile.foreground;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.service.UserDataSingleton;

public class OpenLobbyAlarm extends Service {

    private static final String TAG = "OpenLobbyAlarm";
    private static final int NOTIFICATION_ID = 13;
    private static final String CHANNEL_ID = "lobby_open_channel";
    private static final String CHANNEL_NAME = "Lobby Open Notification";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OpenLobbyAlarm onCreate()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 사용하지 않을 경우 null 반환이 일반적입니다.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OpenLobbyAlarm onStartCommand()");

        // 1. 알림 채널 생성 (Android O 이상)
        createNotificationChannel();

        // 2. 알림 빌드 (문법 오류 수정 완료)
        String userName = UserDataSingleton.getInstance().getUserName();
        if (userName == null) userName = "입주민";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // 유효한 아이콘으로 변경 권장
                .setContentTitle("공동현관문 열림")
                .setContentText(userName + "님 공동현관문이 열렸습니다.") // ✅ 괄호 위치 수정
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(false) // 문 열림 알림이므로 계속 떠 있을 필요가 없다면 false
                .setAutoCancel(true)
                .build();

        // 3. 포그라운드 서비스 시작 (Android 14+ 대응)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Manifest에 선언된 type과 일치해야 함 (예: specialUse 또는 remoteMessaging 등)
            // 여기서는 일반 알림용이므로 특정 타입을 지정하거나 manifest 설정을 따릅니다.
            startForeground(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 알림 후 서비스 자동 종료 (문만 열고 알림만 주면 되므로)
        // 만약 계속 유지해야 한다면 아래 라인을 삭제하세요.
        // stopSelf();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("공동현관문 열림 알림을 위한 채널입니다.");

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OpenLobbyAlarm destroyed");
    }
}