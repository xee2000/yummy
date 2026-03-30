package com.pms_parkin_mobile.receiver;

import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pms_parkin_mobile.service.BleScanner;

import java.util.List;

/**
 * PendingIntent 기반 BLE 스캔 결과 수신기.
 * 앱이 백그라운드/프로세스 없음 상태여도 OS가 직접 이 리시버에 스캔 결과를 전달한다.
 */
public class BleScanReceiver extends BroadcastReceiver {
    private static final String TAG = "BleScanReceiver";

    // BluetoothLeScanner.EXTRA_* 상수가 SDK 버전에 따라 컴파일 타임 접근 불가한 경우가 있어 직접 정의
    private static final String EXTRA_LIST_OF_SCAN_RESULTS = "android.bluetooth.le.extra.LIST_SCAN_RESULT";
    private static final String EXTRA_ERROR_CODE           = "android.bluetooth.le.extra.ERROR_CODE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1);
        if (errorCode != -1) {
            Log.e(TAG, "PendingIntent 스캔 오류: errorCode=" + errorCode);
            return;
        }

        List<ScanResult> results = intent.getParcelableArrayListExtra(EXTRA_LIST_OF_SCAN_RESULTS);
        if (results == null || results.isEmpty()) return;

        BleScanner instance = BleScanner.runningInstance;
        if (instance == null) {
            Log.d(TAG, "BleScanner 인스턴스 없음 — 서비스 재시작 필요");
            return;
        }

        for (ScanResult result : results) {
            instance.onPendingIntentResult(result);
        }
    }
}
