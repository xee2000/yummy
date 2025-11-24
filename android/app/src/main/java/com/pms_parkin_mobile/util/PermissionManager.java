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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** 런타임 권한 유틸 (OS별 분기 + BG Location 분리) */
public final class PermissionManager {
    private PermissionManager() {}

    public static final int REQ_RUNTIME_PERMS = 1001;
    public static final int REQ_BG_LOCATION  = 1002;

    /* ---------- 현재 권한 상태 체크 ---------- */

    /** Android 13+ 에서만 의미 있음 */
    public static boolean hasPostNotifications(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** 위치(전경) 권한: FINE 또는 COARSE 둘 중 하나라도 OK면 true */
    public static boolean hasLocationPermissions(@NonNull Context ctx) {
        boolean fine = ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    /** 백그라운드 위치: API < 29 에서는 의미 없음(항상 true) */
    public static boolean hasBackgroundLocation(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Android 12+ 근거리 디바이스 스캔(블루투스 스캔) */
    public static boolean hasNearbyScan(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    /** (선택) Android 12+ 블루투스 연결 권한 */
    public static boolean hasNearbyConnect(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    /** 필요한 것만 OS 버전에 맞게 한 번에 요청(단, BG 위치는 제외) */
    public static void requestRuntimePermissions(@NonNull Activity activity) {
        List<String> req = new ArrayList<>();

        // 위치(전경)
        if (!hasLocationPermissions(activity)) {
            req.add(Manifest.permission.ACCESS_FINE_LOCATION);
            req.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // 알림(33+)
        if (!hasPostNotifications(activity)) {
            req.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // 블루투스 스캔/연결(31+)
        if (!hasNearbyScan(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            req.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!hasNearbyConnect(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            req.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!req.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    req.toArray(new String[0]),
                    REQ_RUNTIME_PERMS
            );
        }
    }

    /**
     * 백그라운드 위치는 **전경 위치 승인 후**에 별도로 요청해야 함.
     * - API 29(Q): 다이얼로그로 직접 요청 가능
     * - API 30+(R): 같은 화면에서 직접 승인되기 어려워 설정 이동이 필요할 수 있음
     */
    public static void requestBackgroundLocation(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return; // 의미 없음
        if (!hasLocationPermissions(activity)) return; // 전경 먼저!

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10: 다이얼로그 요청 가능
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION },
                    REQ_BG_LOCATION
            );
        } else {
            // Android 11+ : 설정 화면으로 유도 (권장)
            openAppLocationSettings(activity);
        }
    }

    /* ---------- 배터리 최적화 무시 ---------- */
    public static void requestIgnoreBatteryOptimization(@NonNull Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + context.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        } catch (Throwable ignored) {}
    }

    /* ---------- 설정 이동 헬퍼 ---------- */

    /** 앱 알림 설정 화면 */
    public static void openNotificationSettings(@NonNull Context ctx) {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    /** 앱의 위치 권한 상세 화면(설정) */
    public static void openAppLocationSettings(@NonNull Context ctx) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + ctx.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    /* ---------- 편의 메서드 ---------- */

    /** 전경 + (필요 시) 알림 + 블루투스까지 모두 허용되었는지 */
    public static boolean hasAllPrimaryPermissions(@NonNull Context ctx) {
        return hasLocationPermissions(ctx)
                && hasPostNotifications(ctx)
                && hasNearbyScan(ctx)
                && hasNearbyConnect(ctx);
    }
}