package com.pms_parkin_mobile.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.AccelBeacon;
import com.pms_parkin_mobile.util.KalmanFilter;

import java.util.Map;

public class PassiveParkingService {

    private static final String TAG = "PassiveParkingService";

    // 🔹 싱글톤 인스턴스
    private static PassiveParkingService instance;

    // 🔹 내부에서 쓸 것들 (필요하면 점점 채워나가면 됨)
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final SaveArrayListValue saveArrayListValue;

    // 🔹 외부에서 new 못하게 private 생성자
    private PassiveParkingService(Context context) {
        Context appCtx = context.getApplicationContext();
        sensorManager = (SensorManager) appCtx.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                : null;

        saveArrayListValue = new SaveArrayListValue();

        Log.d(TAG, "PassiveParkingService created");
    }

    /**
     * 🔹 외부에서 호출할 싱글톤 접근자
     *  - 최초 1번만 생성, 이후에는 같은 인스턴스 재사용
     */
    public static synchronized PassiveParkingService getInstance(Context context) {
        if (instance == null) {
            instance = new PassiveParkingService(context);
        }
        return instance;
    }
    /**
     * 🔹 서비스 시작 (센서 등록 등)
     *   - 추후 센서 리스너 등록 로직 추가
     */
    public void start() {
        Log.d(TAG, "PassiveParkingService start()");
        // TODO: SensorEventListener 구현 후 여기에서 registerListener 호출
        // sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * 🔹 서비스 중지 (센서 해제 등)
     */
    public void stop() {
        Log.d(TAG, "PassiveParkingService stop()");
        // TODO: sensorManager.unregisterListener(listener);
    }

    //수동 주차의 경우 비컨값중 신호세기가 좋은 값만 받아서 처리하도록 한다.
    //해당 함수에서는 적재만 하고 끝나는 시점에 꺼낸다.
    public void parkingStart(String id, String rssi) {


        saveArrayListValue.SaveAccelBeacon(id ,rssi, String.valueOf(App.getInstance().getmWholeTimerDelay()));
        Log.d(TAG, "Parking started at minor: " + id + ", rssi: " + rssi);

    }

    public AccelBeacon parkingEnd() {
        Map<String, AccelBeacon> beaconMap = App.getInstance().getmAccelBeaconMap();

        if (beaconMap == null || beaconMap.isEmpty()) {
            Log.d(TAG, "parkingEnd: beaconMap is null or empty");
            return null;
        }

        AccelBeacon strongest = null;
        int bestRssi = Integer.MIN_VALUE;  // 제일 큰 RSSI 저장용

        for (String key : beaconMap.keySet()) {
            AccelBeacon beacon = beaconMap.get(key);
            if (beacon == null) continue;

            String beaconId = beacon.getBeaconId();
            if (beaconId == null) continue; // id 없는 건 스킵

            // 🔹 RSSI 를 숫자로 변환 (String → int)
            int rssi;
            try {
                rssi = Integer.parseInt(beacon.getRssi());  // getRssi() 가 String 인 경우
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid RSSI for beacon " + beaconId + " : " + beacon.getRssi());
                continue;
            }

            // 가장 큰 RSSI 선택
            if (strongest == null || rssi > bestRssi) {
                bestRssi = rssi;
                strongest = beacon;
            }

            // 디버그용 로그
            Log.d(TAG,
                    "Beacon ID: " + beaconId +
                            ", RSSI: " + rssi +
                            ", Delay: " + beacon.getDelay() +
                            ", Count: " + beacon.getCount());
            beacon.getDelayList();
        }

        if (strongest != null) {
            Log.d(TAG, "parkingEnd: strongest beacon = " +
                    strongest.getBeaconId() + ", RSSI=" + bestRssi);

        } else {
            Log.d(TAG, "parkingEnd: no beacon with valid RSSI");
        }

        Log.d(TAG, "Parking ended");
        return strongest;
    }
}