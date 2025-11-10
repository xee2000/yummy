package com.pms_parkin_mobile.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.App;
import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.api.RestController;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class BleScanner extends Service implements BeaconConsumer, SensorEventListener {

    private static final String TAG = "BeaconScanService";
    private static final String CHANNEL_ID = "connect_beacon";

    // ===== Beacon =====
    private final Region beaconRegion = new Region(
            "all-beacons",
            Identifier.parse("20151005-8864-5654-3020-010400240902"),
            null, null
    );
    private BeaconManager beaconManager;
    private RestController controller;

    private boolean isInsideRegion = false;
    private int previousMinor = -1;

    // ===== Sensor =====
    private SensorManager sensorManager;
    private Sensor accel, gyro;

    // 리시버 등록 여부 추적
    private boolean bluetoothReceiverRegistered = false;

    // Bluetooth 상태 수신기
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Bluetooth state intent : "  + intent.getAction());
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON) {
                Log.d(TAG, "Bluetooth ON → 스캔 시작");
                startBeaconScan();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                Log.d(TAG, "Bluetooth OFF → 스캔 중지");
                stopBeaconScan();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        createNotificationChannel();
        startAsForeground();

        controller = new RestController();

        // ===== Sensor 등록 =====
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyro  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (accel != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
                Log.d(TAG, "Accelerometer registered");
            } else {
                Log.w(TAG, "No accelerometer sensor");
            }
            if (gyro != null) {
                sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
                Log.d(TAG, "Gyroscope registered");
            } else {
                Log.w(TAG, "No gyroscope sensor");
            }
        } else {
            Log.w(TAG, "SensorManager is null");
        }

        // ===== AltBeacon 설정 =====
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        );
        beaconManager.setForegroundScanPeriod(2000L);
        beaconManager.setForegroundBetweenScanPeriod(0L);
        beaconManager.setBackgroundScanPeriod(2000L);
        beaconManager.setBackgroundBetweenScanPeriod(0L);
        try {
            beaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // Bluetooth 상태 감지 리시버 등록
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
        bluetoothReceiverRegistered = true;

        // 블루투스가 이미 켜져 있다면 바로 스캔 시작
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null && bt.isEnabled()) {
            startBeaconScan();
        } else {
            Log.d(TAG, "Bluetooth is OFF at create-time");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        // 서비스 상태 true로 마킹
        App.getInstance().setServiceStatus(true);

        // 혹시라도 누락되었을 경우 스캔/센서 보강
        if (beaconManager != null && !beaconManager.isBound(this)) {
            startBeaconScan();
        }
        if (sensorManager != null) {
            // 이미 등록되어 있으면 재등록하지 않음
            // (필요 시 재등록 로직 추가 가능)
        }

        return START_STICKY;
    }

    private void startAsForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("비콘/센서 서비스 실행 중")
                .setContentText("비콘 탐지 및 센서 수집을 진행합니다.")
                .setSmallIcon(R.drawable.rn_edit_text_material)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Beacon 연결 서비스",
                    NotificationManager.IMPORTANCE_LOW
            );
            chan.setDescription("백그라운드 비콘 스캔 및 연결 서비스");
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        // 리시버 해제
        if (bluetoothReceiverRegistered) {
            try { unregisterReceiver(bluetoothReceiver); }
            catch (IllegalArgumentException ignore) {}
            bluetoothReceiverRegistered = false;
        }

        // 센서 해제
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Sensors unregistered");
        }

        // 비콘 스캔 중단
        stopBeaconScan();

        // 서비스 상태 off
        App.getInstance().setServiceStatus(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ===== BeaconConsumer 연결 시 =====
    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect()");

        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d(TAG, "didEnterRegion");
                isInsideRegion = true;
                previousMinor = -1;
            }

            @Override
            public void didExitRegion(Region region) {
                Log.d(TAG, "didExitRegion → 0 알림");
                isInsideRegion = false;
                previousMinor = 0;
                App.getInstance().setBeaconMajor(0);
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.d(TAG, "didDetermineStateForRegion: " + state);
            }
        });

        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "didRangeBeaconsInRegion: count=" + beacons.size());
                if (!isInsideRegion || beacons.isEmpty()) return;

                Beacon strongest = Collections.max(beacons,
                        Comparator.comparingInt(Beacon::getRssi)
                );
                int minor = strongest.getId3().toInt();

                if (minor != previousMinor) {
                    Log.d(TAG, "didRange: new minor=" + minor);
                    previousMinor = minor;
                    App.getInstance().setBeaconMinor(minor);
                    updateBeaconLocation(String.valueOf(minor));
                } else {
                    Log.d(TAG, "didRange: same minor, skip " + minor);
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(beaconRegion);
            beaconManager.startRangingBeaconsInRegion(beaconRegion);
            beaconManager.requestStateForRegion(beaconRegion);
            Log.d(TAG, "monitoring & ranging started");
        } catch (RemoteException e) {
            Log.e(TAG, "startMonitoring/Ranging 실패", e);
        }
    }

    private void startBeaconScan() {
        try {
            if (beaconManager == null) {
                Log.w(TAG, "startBeaconScan(): beaconManager is null");
                return;
            }
            Log.d(TAG, "startBeaconScan: isBound=" + beaconManager.isBound(this));
            if (!beaconManager.isBound(this)) {
                beaconManager.bind(this);
                Log.d(TAG, "startBeaconScan: bind() called");
            }
        } catch (Exception e) {
            Log.e(TAG, "startBeaconScan() 오류", e);
        }
    }

    private void stopBeaconScan() {
        try {
            if (beaconManager != null) {
                try { beaconManager.stopMonitoringBeaconsInRegion(beaconRegion); } catch (Exception ignore) {}
                try { beaconManager.stopRangingBeaconsInRegion(beaconRegion); } catch (Exception ignore) {}
                if (beaconManager.isBound(this)) {
                    beaconManager.unbind(this);
                    Log.d(TAG, "beaconManager unbound");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "stopBeaconScan() 오류", e);
        }
    }

    // 비콘 업데이트 시
    private void updateBeaconLocation(String minor) {
        // TODO: 필요 시 서버 업데이트 등
    }

    // ===== SensorEventListener =====
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] v = event.values;
//            Log.d(TAG, String.format("Accel: x=%.3f y=%.3f z=%.3f", v[0], v[1], v[2]));
            SensorTest.ReadAccel(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float[] v = event.values;
//            Log.d(TAG, String.format("Gyro : x=%.3f y=%.3f z=%.3f", v[0], v[1], v[2]));
            SensorTest.ReadGyro(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 필요 시 처리
    }
}