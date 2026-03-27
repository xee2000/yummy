package com.pms_parkin_mobile.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** 런타임 권한 유틸 (OS별 분기 + BG Location 분리) */
public final class PermissionManager {
    private PermissionManager() {}
    public static final int REQUEST_CODE_ALL = 1001;

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


    /** Android 12+ 근거리 디바이스 스캔(블루투스 스캔) */
    public static boolean hasNearbyScan(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestIgnoreBatteryOptimizations(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        if (isIgnoringBatteryOptimizations(ctx)) return;

        try {
            // 이 앱에 대한 직접 배터리 최적화 제외 다이얼로그 표시
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) {
            // 일부 기기에서 직접 다이얼로그 미지원 시 전체 목록 화면으로 fallback
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                Toast.makeText(ctx, "리스트에서 앱을 찾아 '최적화 안 함'으로 설정해주세요.", Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        }
    }



    public static boolean isIgnoringBatteryOptimizations(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return false;

        return pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
    }


    /** (선택) Android 12+ 블루투스 연결 권한 */
    public static boolean hasNearbyConnect(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isBluetoothEnabled(@NonNull Context ctx) {
        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) return false;

        BluetoothAdapter adapter = bm.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    @SuppressLint("MissingPermission") // 권한 체크 후 호출할 것이므로 억제
    public static void requestBluetoothEnable(@NonNull Activity activity) {
        // 이미 켜져 있다면 중단
        if (isBluetoothEnabled(activity)) return;

        // Android 12 이상에서 CONNECT 권한이 없는 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasNearbyConnect(activity)) {
            Toast.makeText(activity, "블루투스 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            requestRuntimePermissions(activity);
            return;
        }

        // 블루투스 켜기 요청 인텐트 실행
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        // activity.startActivityForResult를 써야 하지만,
        // 간단히 팝업만 띄우려면 아래와 같이 사용합니다.
        activity.startActivity(enableBtIntent);
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

    @NonNull
    public static String[] getRuntimePermissions() {
        List<String> list = new ArrayList<>();

        // 위치(런타임): 필요에 따라 FINE 또는 COARSE만 넣어도 됨.
        // BLE 스캔/정밀 위치가 필요하면 FINE 권장
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        // list.add(Manifest.permission.ACCESS_COARSE_LOCATION); // COARSE만 쓸 거면 FINE 대신 이걸로

        // Android 12+(S)부터 블루투스 런타임 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN);
            list.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Android 13+(T)부터 알림 런타임 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return list.toArray(new String[0]);
    }

    public static boolean hasAllPermissions(@NonNull Context context) {
        for (String perm : getRuntimePermissions()) {
            if (ContextCompat.checkSelfPermission(context, perm)
                    != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }
        return true;
    }


}