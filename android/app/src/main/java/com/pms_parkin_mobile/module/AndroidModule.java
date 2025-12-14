package com.pms_parkin_mobile.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import com.pms_parkin_mobile.service.SensorActive;
import com.pms_parkin_mobile.util.PermissionManager;

import java.util.HashMap;
import java.util.Map;

public class AndroidModule extends ReactContextBaseJavaModule {

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

    @ReactMethod
    public void SensorTestStart() {
        // ❗ 여기서는 모드 절대 건들지 말고 flag만 ON
        App.getInstance().setTestFlag(true);
    }


    @ReactMethod
    public void CenterSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_CENTER);
    }

    @ReactMethod
    public void LeftSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_LEFT);
    }

    @ReactMethod
    public void RightSensorTestStart() {
        App.getInstance().setSensorTestMode(SensorActive.TEST_MODE_RIGHT);
    }


    @ReactMethod
    public void SensorTestStop() {
        App.getInstance().setTestFlag(false);
        App.getInstance().setSensorStatus(false);
        Log.d("TEST", "App.getInstance() : " + App.getInstance().isSensorStatus());
    }

    //최종적인 센서 테스트 결과값으로 sensor_status로 처리
    @ReactMethod
    public void SensorTestResult(Promise promise) {
        boolean result = App.getInstance().isSensorStatus();
        Log.d("TEST", "SensorTestResult result : " + result);
        promise.resolve(result); // ← 단일 값만 전달
    }

    //정면 휴대폰 테스트 결과값 출력
    @ReactMethod
    public void StayResult(Promise promise) {
        Boolean flag = App.getInstance().isStayTestResult();
        promise.resolve(flag);
        Log.d("TEST", "StayResult : " + flag);
    }

    @ReactMethod
    public void LeftResult(Promise promise) {
        Boolean flag = App.getInstance().isLeftTestResult();
        promise.resolve(flag);
        Log.d("TEST", "LeftResult : " + flag);
    }

    @ReactMethod
    public void RightResult(Promise promise) {
        Boolean flag = App.getInstance().isRightTestResult();
        promise.resolve(flag);
        Log.d("TEST", "RightResult : " + flag);
    }


    @ReactMethod
    public void ServiceCheck(Promise promise) {
        WritableMap map = Arguments.createMap();
        Log.d("SERVICE_CHECK", "sensor_status : " + App.getInstance().isSensorStatus() + "__" +  App.getInstance().isServiceStatus());

        map.putBoolean("sensor_status", App.getInstance().isSensorStatus());
        map.putBoolean("service_flag", App.getInstance().isServiceFlag());

        Log.d("TEST", "map : " + map.toString());
        promise.resolve(map);
    }

    @ReactMethod
    public void ServiceRunningCheck(Promise promise) {

        Boolean flag = BleScanner.isRunning();
        promise.resolve(flag);
    }

    @ReactMethod
    public void ServiceFlag(boolean flag) {

        final Context ctx = getReactApplicationContext();


        if (flag) {
            Log.d("SERVICE_FLAG", "서비스 시작 요청");
            App.getInstance().setStartFlag(true);
            Intent intent = new Intent(ctx, BleScanner.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.startService(intent);
            }
        } else {
            App.getInstance().setServiceFlag(false);
            App.getInstance().setStartFlag(false);
            Log.d("SERVICE_FLAG", "서비스 중지 요청");
            Intent intent = new Intent(ctx, BleScanner.class);
            ctx.stopService(intent); // ✅ 시스템에 서비스 중지 요청
        }
    }

    @ReactMethod
    public void requestAllPrimaryPermissions() {
        Activity activity = getCurrentActivity();
        if (activity == null) return;

        // 이미 모두 허용됨 → 바로 종료
        if (PermissionManager.hasAllPrimaryPermissions(activity)) {
            return;
        }

        // 실제 요청 함수 (PermissionManager 내부의 분기 로직 그대로 활용)
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


    //수동주차를 위해 필요한것 객체 초기화
    @ReactMethod
    public void passiveParking() {
        App.getInstance().setPassiveCheck(true);
        App.getInstance().setmAccelBeaconMap(new HashMap<String, AccelBeacon>());
        App.getInstance().resetDelayList();

    }

    @ReactMethod
    public void passiveParkingEnd() {
        App.getInstance().setPassiveCheck(false);

    }
}