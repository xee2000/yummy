package com.pms_parkin_mobile.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 권한 관리 유틸리티 클래스 (Health Connect 통합)
 */
public class PermissionManager {
    private PermissionManager() {}

    public static final int REQUEST_CODE_ALL = 1001;

    @SuppressLint("InlinedApi")
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN

    };


    /** Android 런타임 권한 요청 */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void requestAllPermissions(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_ALL
        );
    }

    /** 모든 런타임 권한이 허용됐는지 확인 */
    public static boolean hasAllPermissions(@NonNull Context context) {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }



    /** 배터리 최적화 무시 요청 */
    public static void requestIgnoreBatteryOptimization(@NonNull Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    private static final String TAG = "PermissionManager";
    /**
     * Health Connect 앱 설치 및 권한 요청, 이후 데이터 읽기(or onReady 콜백)까지 한번에 처리합니다.
     * - 설치 필요 시 Play 스토어 다이얼로그
     * - 권한 필요 시 권한 대화상자
     * - 권한 이미 있으면 바로 onReady.run()
     */
    /** 앱의 알림 설정 화면으로 이동 (Activity 없어도 가능) */
    public static void openNotificationSettings(@NonNull Context ctx) {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

}