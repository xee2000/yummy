package com.pms_parkin_mobile.foreground;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.UserDataSingleton;


public class AppRunning3 extends Service {

    private static final String TAG = AppRunning3.class.getSimpleName();
    private final int NOTIFICATION_ID = 13;
    private final String CHANNEL_ID = "balem_activity";
    private final String CHANNEL_NAME = "Balem Activity";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ActivityService onCreate()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rn_edit_text_material)
                .setContentTitle("주차위치서비스")
                .setContentText(UserDataSingleton.getInstance().getUserName() +"님 주차가 완료되었습니다.")
                .setOngoing(true)
                .setAutoCancel(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("앱 실행중 알림 채널");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        startForeground(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "ActivityService started with foreground notification.");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ActivityService destroyed");
    }
}
