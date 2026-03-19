package com.pms_parkin_mobile.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.pms_parkin_mobile.service.BleScanner;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_NOTIFICATION_DISMISSED = "com.pms_parkin_mobile.NOTIFICATION_DISMISSED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        Log.d("NotificationActionReceiver", "onReceive action: " + action);

        if (ACTION_NOTIFICATION_DISMISSED.equals(action)) {
            Log.d("NotificationActionReceiver", "포그라운드 알림 삭제 감지 → 알림 복구 요청");
            Intent serviceIntent = new Intent(context, BleScanner.class);
            serviceIntent.setAction(BleScanner.ACTION_RESTORE_NOTIFICATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
