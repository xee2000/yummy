package com.pms_parkin_mobile.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.BleScanner;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        App.getInstance().init(context);

        if (!App.getInstance().isServiceFlag()) {
            Log.d("BootReceiver", "ServiceFlag OFF - 서비스 미시작");
            return;
        }

        Log.d("BootReceiver", "부팅 완료 - BleScanner 재시작");
        Intent serviceIntent = new Intent(context, BleScanner.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
