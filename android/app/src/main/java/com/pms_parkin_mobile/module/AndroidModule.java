package com.pms_parkin_mobile.module;

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
import com.pms_parkin_mobile.App;
import com.pms_parkin_mobile.service.BleScanner;

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
    public void SensorTestStart() {
        App.getInstance().setTestFlag(true);
        Log.d("TEST", "App.getInstance() : " + App.getInstance().isSensorStatus());
    }

    @ReactMethod
    public void SensorTestStop() {
        App.getInstance().setTestFlag(false);
        Log.d("TEST", "App.getInstance() : " + App.getInstance().isSensorStatus());
    }

    @ReactMethod
    public void SensorTestResult(Promise promise) {
        App.getInstance().setTestFlag(false);
        boolean result = App.getInstance().isSensorStatus();
        Log.d("TEST", "SensorTestResult result : " + result);
        promise.resolve(result); // ← 단일 값만 전달
    }

    @ReactMethod
    public void ServiceCheck(Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("sensor_status", App.getInstance().isSensorStatus());
        map.putBoolean("service_status", App.getInstance().isServiceStatus());
        promise.resolve(map);
    }

    @ReactMethod
    public void ServiceFlag(boolean flag) {
        App.getInstance().setServiceStatus(flag);

        final Context ctx = getReactApplicationContext();

        if (flag) {
            Log.d("SERVICE_FLAG", "서비스 시작 요청");
            Intent intent = new Intent(ctx, BleScanner.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.startService(intent);
            }
        } else {
            Log.d("SERVICE_FLAG", "서비스 중지 요청");
            Intent intent = new Intent(ctx, BleScanner.class);
            ctx.stopService(intent); // ✅ 시스템에 서비스 중지 요청
        }
    }
}