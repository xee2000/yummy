package com.pms_parkin_mobile.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.pms_parkin_mobile.dto.AccelBeacon;
import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.BleScanner;
import com.pms_parkin_mobile.service.PassiveParkingService;
import com.pms_parkin_mobile.service.SensorActive;
import com.pms_parkin_mobile.service.TimerSingleton;
import com.pms_parkin_mobile.util.PermissionManager;

import java.util.HashMap;

public class AndroidModule extends ReactContextBaseJavaModule {

    private static final String TAG_TEST = "SensorTest";

    private final Handler handler = new Handler(Looper.getMainLooper());

    public AndroidModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "AndroidModule";
    }

    @ReactMethod
    public void StartApplication() {
        final Context ctx = getReactApplicationContext();
        ctx.startService(new Intent(ctx, UserIntent.class));
    }

    @ReactMethod
    public void startUserIntentService(String userData) {
        Intent intent = new Intent(getReactApplicationContext(), UserIntent.class);
        intent.putExtra("user", userData);
        getReactApplicationContext().startService(intent);
    }

    /* =========================
       ✅ 센서 테스트 관련 (수정/정리)
       ========================= */

    // (호환용) 시작만 켜는 기존 API
    @ReactMethod
    public void SensorTestStart() {
        App.getInstance().setTestStartFlag(true);
        Log.d(TAG_TEST, "SensorTestStart: startFlag=true (mode=" + App.getInstance().getSensorTestMode() + ")");
    }

    // (호환용) 모드만 세팅하는 기존 API
    @ReactMethod
    public void CenterSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_CENTER);
        Log.d(TAG_TEST, "CenterSensorTestStart: mode=중앙");
    }

    @ReactMethod
    public void LeftSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_LEFT);
        Log.d(TAG_TEST, "LeftSensorTestStart: mode=좌측");
    }

    @ReactMethod
    public void RightSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_RIGHT);
        Log.d(TAG_TEST, "RightSensorTestStart: mode=우측");
    }

    // ✅ 권장: 모드+시작을 “한번에”
    @ReactMethod
    public void SensorTestStartWithMode(String mode) {
        String normalized = normalizeMode(mode);

        Log.d(TAG_TEST,
                "[JS->Native] SensorTestStartWithMode 호출됨"
                        + " rawMode=" + mode
                        + " normalized=" + normalized
                        + " BEFORE {startFlag=" + App.getInstance().isTestStartFlag()
                        + ", lastStay=" + App.getInstance().isStayTestResult()
                        + ", lastLeft=" + App.getInstance().isLeftTestResult()
                        + ", lastRight=" + App.getInstance().isRightTestResult()
                        + "}"
        );

        // (권장) 강제 falling edge
        App.getInstance().setTestStartFlag(false);
        Log.d(TAG_TEST, "[Native] force stop: startFlag=false");
//        SensorActive.notifyTestFlagChanged(); // 있으면

        App.getInstance().setSensorTestMode(normalized);
        Log.d(TAG_TEST, "[Native] setSensorTestMode=" + normalized);

        App.getInstance().setTestStartFlag(true);
        Log.d(TAG_TEST, "[Native] set startFlag=true");

//        SensorActive.notifyTestFlagChanged(); // 있으면

        Log.d(TAG_TEST,
                "[Native] SensorTestStartWithMode 완료"
                        + " AFTER {startFlag=" + App.getInstance().isTestStartFlag()
                        + ", mode=" + App.getInstance().getSensorTestMode()
                        + "}"
        );
    }

    @ReactMethod
    public void SensorTestStop() {
        App.getInstance().setTestStartFlag(false);
        Log.d(TAG_TEST, "SensorTestStop: startFlag=false");
    }

    // ✅ “평가 끝날 때까지 기다렸다가” 해당 단계 결과 반환
    // - SensorActive가 평가 끝나면 startFlag를 false로 내려주는 구조(네가 고친 SensorActive 기준)
    @ReactMethod
    public void SensorTestWaitResult(String mode, int timeoutMs, Promise promise) {
        final String normalized = normalizeMode(mode);
        final long start = SystemClock.elapsedRealtime();
        final int safeTimeout = (timeoutMs <= 0) ? 4500 : timeoutMs;

        Log.d(TAG_TEST, "[Wait] start mode=" + normalized
                + ", timeoutMs=" + safeTimeout
                + ", startFlag=" + App.getInstance().isTestStartFlag()
                + ", appMode=" + App.getInstance().getSensorTestMode());

        final long[] lastLog = {0};

        Runnable poll = new Runnable() {
            @Override
            public void run() {
                boolean running = App.getInstance().isTestStartFlag();
                long elapsed = SystemClock.elapsedRealtime() - start;

                // 250ms마다 상태 로그
                if (elapsed - lastLog[0] >= 250) {
                    lastLog[0] = elapsed;
                    Log.d(TAG_TEST, "[Wait] polling..."
                            + " elapsed=" + elapsed
                            + ", startFlag=" + running
                            + ", appMode=" + App.getInstance().getSensorTestMode()
                            + ", stay=" + App.getInstance().isStayTestResult()
                            + ", left=" + App.getInstance().isLeftTestResult()
                            + ", right=" + App.getInstance().isRightTestResult());
                }

                if (!running) {
                    boolean r = getStepResult(normalized);
                    Log.d(TAG_TEST, "[Wait] done mode=" + normalized + ", result=" + r);
                    promise.resolve(r);
                    return;
                }

                if (elapsed > safeTimeout) {
                    Log.w(TAG_TEST, "[Wait] TIMEOUT mode=" + normalized
                            + " startFlag=" + running
                            + " appMode=" + App.getInstance().getSensorTestMode());
                    promise.reject("SENSOR_TEST_TIMEOUT", "timeout mode=" + normalized);
                    return;
                }

                handler.postDelayed(this, 50);
            }
        };

        handler.post(poll);
    }
    // 최종 결과 (3개 모두)
    @ReactMethod
    public void SensorTestResult(Promise promise) {
        boolean result = App.getInstance().isSensorTestAllPassed();
        Log.d(TAG_TEST, "SensorTestResult allPassed : " + result);
        promise.resolve(result);
    }

    // 단계별 결과 (즉시 조회용: “기다림 없음”)
    @ReactMethod
    public void StayResult(Promise promise) {
        boolean flag = App.getInstance().isStayTestResult();
        Log.d(TAG_TEST, "StayResult : " + flag);
        promise.resolve(flag);
    }

    @ReactMethod
    public void LeftResult(Promise promise) {
        boolean flag = App.getInstance().isLeftTestResult();
        Log.d(TAG_TEST, "LeftResult : " + flag);
        promise.resolve(flag);
    }

    @ReactMethod
    public void RightResult(Promise promise) {
        boolean flag = App.getInstance().isRightTestResult();
        Log.d(TAG_TEST, "RightResult : " + flag);
        promise.resolve(flag);
    }

    // (선택) JS에서 문자열을 어떻게 보내든 안전하게 맞춰주기
    private String normalizeMode(String mode) {
        if (mode == null) return SensorActive.TEST_MODE_CENTER;

        // 영문/키워드도 허용
        String m = mode.trim();
        if (m.equalsIgnoreCase("center") || m.equalsIgnoreCase("stay") || m.equals("정면") || m.equals("중앙")) {
            return SensorActive.TEST_MODE_CENTER; // "중앙"
        }
        if (m.equalsIgnoreCase("left") || m.equals("좌") || m.equals("좌측")) {
            return SensorActive.TEST_MODE_LEFT;   // "좌측"
        }
        if (m.equalsIgnoreCase("right") || m.equals("우") || m.equals("우측")) {
            return SensorActive.TEST_MODE_RIGHT;  // "우측"
        }
        // 기본
        return SensorActive.TEST_MODE_CENTER;
    }

    private boolean getStepResult(String normalizedMode) {
        if (SensorActive.TEST_MODE_LEFT.equals(normalizedMode)) {
            return App.getInstance().isLeftTestResult();
        }
        if (SensorActive.TEST_MODE_RIGHT.equals(normalizedMode)) {
            return App.getInstance().isRightTestResult();
        }
        return App.getInstance().isStayTestResult();
    }

    /* =========================
       이하 기존 코드 (그대로)
       ========================= */

    @ReactMethod
    public void ServiceCheck(Promise promise) {
        WritableMap map = Arguments.createMap();
        Context ctx = getReactApplicationContext();
        Boolean flag = BleScanner.isServiceRunning(ctx);
        Log.d("SensorTest", "sensorTest : " + App.getInstance().isSensorTestAllPassed());
        Log.d("SensorTest" , "ServiceFlag : " + App.getInstance().isServiceFlag());
        map.putBoolean("sensor_test", App.getInstance().isSensorTestAllPassed());
        map.putBoolean("service_flag", flag);

        Log.d("TEST", "map : " + map.toString());
        promise.resolve(map);
    }

    @ReactMethod
    public void ServiceRunningCheck(Promise promise) {
        final Context ctx = getReactApplicationContext();
        Boolean flag = BleScanner.isServiceRunning(ctx);
        promise.resolve(flag);
    }

    @ReactMethod
    public void ServiceFlag(boolean flag) {
        final Context ctx = getReactApplicationContext();

        if (flag) {
            Log.d("SERVICE_FLAG", "서비스 시작 요청");
            App.getInstance().setServiceFlag(true);
            Intent intent = new Intent(ctx, BleScanner.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.startService(intent);
            }
        } else {
            App.getInstance().setServiceFlag(false);
            Log.d("SERVICE_FLAG", "서비스 중지 요청");
            Intent intent = new Intent(ctx, BleScanner.class);
            ctx.stopService(intent);
        }
    }

    @ReactMethod
    public void requestAllPrimaryPermissions() {
        Activity activity = getCurrentActivity();
        if (activity == null) return;

        if (PermissionManager.hasAllPrimaryPermissions(activity)) return;
        PermissionManager.requestRuntimePermissions(activity);
    }

    @ReactMethod
    public void hasAllPrimaryPermissions(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.resolve(false);
            return;
        }
        boolean ok = PermissionManager.hasAllPrimaryPermissions(activity);
        promise.resolve(ok);
    }

    //수동주차를 진행할경우 차넘버를 받고서 처리하도록 한다.
    @ReactMethod
    public void passiveParking(String parkingCar) {
        App.getInstance().setPassiveCheck(true);
        App.getInstance().setParkingCar(parkingCar);
        App.getInstance().setmAccelBeaconMap(new HashMap<String, AccelBeacon>());
        App.getInstance().resetDelayList();
        TimerSingleton.getInstance().resetTimer();
    }

    @ReactMethod
    public void passiveParkingEnd(Promise promise) {
        try {
            App.getInstance().setPassiveCheck(false);

            Context ctx = getReactApplicationContext();
            AccelBeacon beacon = PassiveParkingService.getInstance(ctx).parkingEnd();

            if (beacon == null) {
                promise.resolve(null);
                return;
            }

            WritableMap map = Arguments.createMap();
            map.putString("beaconId", beacon.getBeaconId());
            map.putInt("rssi", Integer.parseInt(beacon.getRssi()));
            promise.resolve(map);

            App.getInstance().setmAccelBeaconMap(new HashMap<String, AccelBeacon>());
            App.getInstance().resetDelayList();
        } catch (Exception e) {
            promise.reject("PASSIVE_PARKING_END_ERROR", e);
        }
    }
}