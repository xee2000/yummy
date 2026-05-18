package com.pms_parkin_mobile.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * ForegroundServiceStartNotAllowedException (Android 12+, SDK 31+) 방어용 유틸
 * 앱이 백그라운드 상태일 때 startForegroundService() 호출 시 발생.
 * try-catch 로 감싸 크래시 방지.
 */
fun Context.safeStartForegroundService(intent: Intent) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    } catch (e: Exception) {
        // ForegroundServiceStartNotAllowedException: 백그라운드 제한으로 시작 불가 — 무시
        Log.w("ServiceStartUtil", "startForegroundService 불가 (백그라운드 제한): ${e.message}")
    }
}
