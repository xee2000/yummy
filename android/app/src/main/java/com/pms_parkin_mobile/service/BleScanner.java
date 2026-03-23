package com.pms_parkin_mobile.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.User;
import com.pms_parkin_mobile.receiver.NotificationActionReceiver;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BleScanner extends Service implements BeaconConsumer, SensorEventListener {

    private static final String TAG = "BleScanner";
    private static final String CHANNEL_ID = "connect_beacon";
    public static final String ACTION_RESTORE_NOTIFICATION = "com.pms_parkin_mobile.RESTORE_NOTIFICATION";
    private static final int NOTIFICATION_ID = 1;

    private static final String TARGET_UUID = "20151005-8864-5654-4159-013500201901";

//    private final Region beaconRegion = new Region(
//            "target-uuid-only",
//            org.altbeacon.beacon.Identifier.parse(TARGET_UUID),
//            null,
//            null
//    );

    private final Region allbeacon = new Region(
            "allbeacon",
            null,
            null,
            null
    );

    private BeaconManager beaconManager;
    private boolean isInsideRegion = false;
    private int previousMinor = -1;
    private BeaconFunction mbaconFunction;
    private SaveArrayListValue mSaveArrayListValue;

    private SensorManager sensorManager;
    private Sensor accel, gyro;
    private Context context;
    private boolean bluetoothReceiverRegistered = false;

    // Bluetooth 상태 수신기
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth ON → 스캔 시작");
                    startBeaconScan();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth OFF → 스캔 중지");
                    stopBeaconScan();
                    sendServerError("Bluetooth turned OFF by user");
                }
            } catch (Exception e) {
                sendServerError("BluetoothReceiver Error: " + e.getMessage());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Log.d(TAG, "onCreate()");
        try {
            createNotificationChannel();
            startAsForeground();

            App.getInstance().setServiceFlag(true);
            mbaconFunction = new BeaconFunction();
            mSaveArrayListValue = new SaveArrayListValue();

            setupSensors();
            setupBeaconManager();

            // 리시버 등록
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(bluetoothReceiver, filter);
            bluetoothReceiverRegistered = true;

            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            if (bt != null && bt.isEnabled()) {
                startBeaconScan();
            }
        } catch (Exception e) {
            sendServerError("onCreate Error: " + e.getMessage());
            scheduleServiceRestart();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.d(TAG, "onStartCommand()");
            if (intent != null && ACTION_RESTORE_NOTIFICATION.equals(intent.getAction())) {
                startAsForeground();
            }
            if (beaconManager != null && !beaconManager.isBound(this)) {
                startBeaconScan();
            }
        } catch (Exception e) {
            sendServerError("onStartCommand Error: " + e.getMessage());
        }
        return START_STICKY;
    }

    // ✅ 주차 서비스 시작 로직
    private void ParkingServiceStart() {
        try {
            Log.d(TAG, "ParkingServiceStart() 호출");
            TimerSingleton.getInstance().StartWholeTimer();
            String currentTime = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());
            App.getInstance().setTime(currentTime);
            App.getInstance().setParkingStartFlag(true);
        } catch (Exception e) {
            sendServerError("ParkingServiceStart Error: " + e.getMessage());
        }
    }

    // ✅ 알림 채널 생성 로직
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Beacon 연결 서비스",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            chan.setDescription("백그라운드 비콘 스캔 및 연결 서비스");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
        }
    }

    private void setupSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
                if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
            }
        } catch (Exception e) {
            sendServerError("setupSensors Error: " + e.getMessage());
        }
    }

    private void setupBeaconManager() {
        try {
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();

            // 1. iBeacon 레이아웃 (가장 일반적)
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

            // 2. AltBeacon 레이아웃
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24"));

            // 스캔 주기 설정 (반응 속도 향상)
            beaconManager.setForegroundScanPeriod(1100L); // 1.1초 스캔
            beaconManager.setForegroundBetweenScanPeriod(0L); // 휴식 없음

            beaconManager.updateScanPeriods();
        } catch (Exception e) {
            sendServerError("setupBeaconManager Error: " + e.getMessage());
        }
    }
    private void startBeaconScan() {
        try {
            if (beaconManager != null && !beaconManager.isBound(this)) {
                Log.d(TAG, "Binding BeaconManager...");
                beaconManager.bind(this);
            }
        } catch (Exception e) {
            sendServerError("startBeaconScan Error: " + e.getMessage());
            // 실패 시 재시도 예약
            new android.os.Handler().postDelayed(this::startBeaconScan, 5000);
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect()");
        beaconManager.addRangeNotifier((beacons, region) -> {
            try {
                if (beacons.isEmpty()) {
                    Log.d(TAG, "Scanning... No beacons found.");
                    return;
                }
                Beacon strongest = Collections.max(beacons, Comparator.comparingInt(Beacon::getRssi));
                processBeaconData(strongest);
            } catch (Exception e) {
                sendServerError("RangeNotifier Error: " + e.getMessage());
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(allbeacon);
            beaconManager.startRangingBeaconsInRegion(allbeacon);
            Log.d(TAG, "Scanning started successfully");
        } catch (RemoteException e) {
            sendServerError("onBeaconServiceConnect RemoteException: " + e.getMessage());
            startBeaconScan();
        }
    }

    private void processBeaconData(Beacon b) {
        try {
            int major = b.getId2() != null ? b.getId2().toInt() : -1;
            int minor = b.getId3() != null ? b.getId3().toInt() : -1;
            int rssi = b.getRssi();

            // 1. 자동 문열림
            if(major == 1 && rssi >= -90 && App.getInstance().isPassOpenLobbyFlag()){
                BeaconFunction.getInstance().OnlyOpenLobby(minor, major, rssi, context);
            }

            // 2. 주차 서비스
            if (major == 4 && rssi >= -90) {
                if (App.getInstance().isPassiveCheck()) {
                    // 수동 주차 수집 중 → 자동 주차 건너뜀
                    String id = String.valueOf(minor > 32768 ? minor - 32768 : minor);
                    PassiveParkingService.getInstance(context).parkingStart(String.format("%04X", Integer.valueOf(id)), String.valueOf(rssi));
                } else if (App.getInstance().isServiceFlag()) {
                    // 수동 주차 종료 후 10분 쿨다운 중이면 자동 주차 대기
                    if (App.getInstance().isPassiveCooldownActive()) {
                        Log.d(TAG, "수동 주차 쿨다운 중 — 자동 주차 대기");
                    } else if (!App.getInstance().isParkingStartFlag()) {
                        ParkingServiceStart();
                    } else {
                        handleParkingTracking(minor, rssi);
                    }
                }
            }
        } catch (Exception e) {
            sendServerError("processBeaconData Error: " + e.getMessage());
        }
    }

    private void handleParkingTracking(int minor, int rssi) {
        try {
            if (rssi >= -80) {
                String id = String.valueOf(minor > 32768 ? minor - 32768 : minor);
                String hexValue = String.format("%04X", Integer.valueOf(id));
                mSaveArrayListValue.SaveAccelBeacon(hexValue, String.valueOf(rssi), String.valueOf(App.getInstance().getmWholeTimerDelay()));
                mbaconFunction.AddAccelDelay(hexValue, rssi);
            }
        } catch (Exception e) {
            sendServerError("handleParkingTracking Error: " + e.getMessage());
        }
    }

    private void sendServerError(String msg) {
        try {

            RestController.getInstance().errorMessage(UserDataSingleton.getInstance().getUserId(), "[BleScanner] " + msg, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {

                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {

                }
            });
        } catch (Exception ignore) {}
    }

    private void scheduleServiceRestart() {
        try {
            Intent restartIntent = new Intent(getApplicationContext(), BleScanner.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 1, restartIntent,
                    PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 2000, pendingIntent);
                Log.d(TAG, "Service restart scheduled in 2s");
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleServiceRestart failed", e);
        }
    }

    private void startAsForeground() {
        try {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("주차 서비스 실행 중")
                    .setContentText("백그라운드에서 비콘 및 센서를 탐색 중입니다.")
                    .setSmallIcon(R.drawable.logo)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            sendServerError("startAsForeground Error: " + e.getMessage());
        }
    }

    private void stopBeaconScan() {
        try {
            if (beaconManager != null) {
                beaconManager.removeAllRangeNotifiers();
                if (beaconManager.isBound(this)) {
                    beaconManager.unbind(this);
                    Log.d(TAG, "BeaconManager unbound");
                }
            }
        } catch (Exception e) {
            sendServerError("stopBeaconScan Error: " + e.getMessage());
        }
    }

    @Override public void onSensorChanged(SensorEvent event) {
        try {
            if (event == null || App.getInstance().isPassiveCheck()) return;
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) SensorActive.ReadAccel(event.values);
            else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) SensorActive.ReadGyro(event.values);
        } catch (Exception ignore) {}
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (App.getInstance().isServiceFlag()) {
            sendServerError("Service destroyed unexpectedly");
            scheduleServiceRestart();
        }
        super.onDestroy();
        stopBeaconScan();
        if (bluetoothReceiverRegistered) {
            try { unregisterReceiver(bluetoothReceiver); } catch (Exception ignore) {}
        }
    }

    // ✅ 서비스 동작 여부 체크 메서드
    public static boolean isServiceRunning(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(Integer.MAX_VALUE);
            if (runningServices == null) return false;
            for (ActivityManager.RunningServiceInfo service : runningServices) {
                if (BleScanner.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }
}