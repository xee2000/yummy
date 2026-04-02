package com.pms_parkin_mobile.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** 런타임 권한 유틸 (OS별 분기 + BG Location 분리)  */
object PermissionManager {
    const val REQUEST_CODE_ALL: Int = 1001
    const val REQ_RUNTIME_PERMS: Int = 1001
    const val REQ_BG_LOCATION: Int = 1002

    /* ---------- 현재 권한 상태 체크 ---------- */

    /** Android 13+ 에서만 의미 있음  */
    fun hasPostNotifications(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 위치(전경) 권한: FINE 또는 COARSE 둘 중 하나라도 OK면 true  */
    fun hasLocationPermissions(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Android 12+ 근거리 디바이스 스캔(블루투스 스캔)  */
    fun hasNearbyScan(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Android 12+ 블루투스 연결 권한  */
    fun hasNearbyConnect(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** ✅ Android 12+ 정확한 알람(Exact Alarm) 권한 체크 */
    fun hasExactAlarmPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /* ---------- 권한 요청 및 설정 화면 이동 ---------- */

    /** ✅ 정확한 알람 권한 설정 화면으로 이동 (Android 12+) */
    fun requestExactAlarmPermission(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasExactAlarmPermission(ctx)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                    Toast.makeText(ctx, "정확한 알람 권한을 '허용'으로 설정해주세요.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            }
        }
    }

    fun requestIgnoreBatteryOptimizations(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations(ctx)) return

        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:" + ctx.getPackageName()))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                Toast.makeText(ctx, "리스트에서 앱을 찾아 '최적화 안 함'으로 설정해주세요.", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        }
    }

    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
    }

    fun isBluetoothEnabled(ctx: Context): Boolean {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        val adapter = bm?.adapter
        return adapter != null && adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    fun requestBluetoothEnable(activity: Activity) {
        if (isBluetoothEnabled(activity)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasNearbyConnect(activity)) {
            Toast.makeText(activity, "블루투스 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            requestRuntimePermissions(activity)
            return
        }

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivity(enableBtIntent)
    }

    /** 필요한 것만 OS 버전에 맞게 한 번에 요청(단, BG 위치 및 Exact Alarm은 별도 처리)  */
    fun requestRuntimePermissions(activity: Activity) {
        val req: MutableList<String> = ArrayList()

        // 1. 위치(전경)
        if (!hasLocationPermissions(activity)) {
            req.add(Manifest.permission.ACCESS_FINE_LOCATION)
            req.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // 2. 알림(33+)
        if (!hasPostNotifications(activity)) {
            req.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. 블루투스 스캔/연결(31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasNearbyScan(activity)) req.add(Manifest.permission.BLUETOOTH_SCAN)
            if (!hasNearbyConnect(activity)) req.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (req.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                req.toTypedArray(),
                REQ_RUNTIME_PERMS
            )
        }

        // 💡 4. 정확한 알람(Exact Alarm) 권한 체크 및 유도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission(activity)) {
            requestExactAlarmPermission(activity)
        }
    }

    /** 앱의 위치 권한 상세 화면(설정)  */
    fun openAppLocationSettings(ctx: Context) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:" + ctx.getPackageName()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    /* ---------- 편의 메서드 ---------- */

    /** 전경 + 알림 + 블루투스 + 정확한 알람까지 모두 허용되었는지  */
    fun hasAllPrimaryPermissions(ctx: Context): Boolean {
        return hasLocationPermissions(ctx)
                && hasPostNotifications(ctx)
                && hasNearbyScan(ctx)
                && hasNearbyConnect(ctx)
                && hasExactAlarmPermission(ctx)
    }

    val runtimePermissions: Array<String>
        get() {
            val list: MutableList<String> = ArrayList()
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_SCAN)
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            return list.toTypedArray()
        }

    fun hasAllPermissions(context: Context): Boolean {
        for (perm in runtimePermissions) {
            if (ContextCompat.checkSelfPermission(context, perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}