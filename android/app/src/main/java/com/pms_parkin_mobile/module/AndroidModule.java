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
import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.BleScanner;
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


        Log.d("SERVICE_FLAG", "App : " + App.getInstance().isServiceFlag());
        Intent intent = new Intent(ctx, BleScanner.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(ctx, intent);
        } else {
            ctx.startService(intent);
        }
    }

    @ReactMethod
    public void startUserIntentService(String userData) {
        Intent intent = new Intent(getReactApplicationContext(), UserIntent.class);
        intent.putExtra("user", userData);
        getReactApplicationContext().startService(intent);
    }


    @ReactMethod
    public void SensorTestStart() {
        App.getInstance().setTestFlag(true);
        Log.d("TEST", "App.getInstance() : " + App.getInstance().isSensorStatus());
    }

    @ReactMethod
    public void SensorTestStop() {
        App.getInstance().setTestFlag(false);
        App.getInstance().setSensorStatus(false);
        Log.d("TEST", "App.getInstance() : " + App.getInstance().isSensorStatus());
    }

    @ReactMethod
    public void SensorTestResult(Promise promise) {
        boolean result = App.getInstance().isSensorStatus();
        Log.d("TEST", "SensorTestResult result : " + result);
        promise.resolve(result); // ← 단일 값만 전달
    }

    @ReactMethod
    public void ServiceCheck(Promise promise) {
        WritableMap map = Arguments.createMap();
        Log.d("SERVICE_CHECK", "sensor_status : " + App.getInstance().isSensorStatus() + "__" +  App.getInstance().isServiceStatus());

        map.putBoolean("sensor_status", App.getInstance().isSensorStatus());
        map.putBoolean("service_status", App.getInstance().isServiceStatus());
        promise.resolve(map);
    }

    @ReactMethod
    public void ServiceRunningCheck(Promise promise) {
        boolean flag = App.getInstance().isServiceStatus();
        promise.resolve(flag);
    }

    @ReactMethod
    public void ServiceFlag(boolean flag) {

        final Context ctx = getReactApplicationContext();

        if (flag) {
            Log.d("SERVICE_FLAG", "서비스 시작 요청");
            App.getInstance().setServiceFlag(flag);
            App.getInstance().setStartFlag(false);
            Intent intent = new Intent(ctx, BleScanner.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.startService(intent);
            }
        } else {
            App.getInstance().setServiceFlag(flag);
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
}