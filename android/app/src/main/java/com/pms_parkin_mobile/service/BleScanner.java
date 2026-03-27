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
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.receiver.NotificationActionReceiver;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.util.List;

public class BleScanner extends Service implements SensorEventListener {

    private static final String TAG = "BleScanner";
    private static final String CHANNEL_ID = "connect_beacon";
    public static final String ACTION_RESTORE_NOTIFICATION = "com.pms_parkin_mobile.RESTORE_NOTIFICATION";
    private static final int NOTIFICATION_ID = 1;

    private static final String BANSUKTARGET_UUID = "20151005-8864-5654-3020-013900202001";

    private BeaconManager beaconManager;
    private Region beaconRegion;
    private boolean isScanning = false;

    private BeaconFunction mbaconFunction;
    private SaveArrayListValue mSaveArrayListValue;

    private SensorManager sensorManager;
    private Sensor accel, gyro;
    private Context context;
    private boolean bluetoothReceiverRegistered = false;

    private android.os.Handler handler;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth ON → 스캔 시작");
                    updateForegroundNotification(true);
                    startBeaconScan();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth OFF → 스캔 중지");
                    updateForegroundNotification(false);
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

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            boolean isBtOn = (btAdapter != null && btAdapter.isEnabled());
            startAsForeground(isBtOn);

            App.getInstance().setServiceFlag(true);
            mbaconFunction = new BeaconFunction();
            mSaveArrayListValue = new SaveArrayListValue();

            setupSensors();

            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(bluetoothReceiver, filter);
            bluetoothReceiverRegistered = true;

            handler = new android.os.Handler(android.os.Looper.getMainLooper());

            checkBatteryOptimization();

            // BeaconManager 초기화 (iBeacon 파서 등록)
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(
                    new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
            );
            beaconManager.setEnableScheduledScanJobs(false);
            beaconRegion = new Region("iBeaconRegion", Identifier.parse(BANSUKTARGET_UUID), null, null);

            if (isBtOn) {
                startBeaconScan();
            }
        } catch (Exception e) {
            sendServerError("onCreate Error: " + e.getMessage());
            scheduleServiceRestart();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                boolean ignored = pm.isIgnoringBatteryOptimizations(getPackageName());
                Log.d(TAG, "배터리 최적화 예외 여부: " + ignored
                        + (ignored ? " → 정상" : " → BLE 스캔 제한될 수 있음! 설정에서 배터리 최적화 제외 필요"));
                if (!ignored) {
                    sendServerError("배터리 최적화 예외 미적용 상태 - BLE 스캔 제한 가능");
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.d(TAG, "onStartCommand()");
            if (intent != null && ACTION_RESTORE_NOTIFICATION.equals(intent.getAction())) {
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                startAsForeground(bt != null && bt.isEnabled());
            }
            if (!isScanning) {
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                if (bt != null && bt.isEnabled()) {
                    startBeaconScan();
                }
            }
        } catch (Exception e) {
            sendServerError("onStartCommand Error: " + e.getMessage());
        }
        return START_STICKY;
    }

    private void startBeaconScan() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                Log.d(TAG, "startBeaconScan - Bluetooth 비활성화 상태");
                return;
            }

            beaconManager.removeAllRangeNotifiers();
            beaconManager.addRangeNotifier((beacons, region) -> {
                for (Beacon b : beacons) {
                    int major = b.getId2().toInt();
                    int minor = b.getId3().toInt();
                    int rssi  = b.getRssi();
                    Log.d(TAG, "SCAN - 비콘 감지: major=" + major + " minor=" + minor + " rssi=" + rssi);
                    processBeaconData(major, minor, rssi);
                }
            });

            beaconManager.startRangingBeacons(beaconRegion);
            isScanning = true;
            Log.d(TAG, "startBeaconScan - AltBeacon 스캔 시작");
        } catch (Exception e) {
            isScanning = false;
            sendServerError("startBeaconScan Error: " + e.getMessage());
        }
    }

    private void stopBeaconScan() {
        try {
            if (beaconManager != null && beaconRegion != null) {
                beaconManager.stopRangingBeacons(beaconRegion);
                Log.d(TAG, "stopBeaconScan - 스캔 중지");
            }
        } catch (Exception e) {
            sendServerError("stopBeaconScan Error: " + e.getMessage());
        } finally {
            isScanning = false;
        }
    }

    private void processBeaconData(int major, int minor, int rssi) {
        try {
            Log.d(TAG, "PROCESS - major=" + major + " minor=" + minor + " rssi=" + rssi
                    + " | passOpenLobbyFlag=" + App.getInstance().isPassOpenLobbyFlag());

            switch (major){
                case (1):
                    if(rssi >= -90 && App.getInstance().isPassOpenLobbyFlag()){
                        BeaconFunction.getInstance().OnlyOpenLobby(minor, major, rssi, context);
                    }
                    break;
                case (4):
                    if (App.getInstance().isPassiveCheck()) {
                        // 수동 주차 중 — 자동 시작은 막고 비콘 데이터만 수집
                        handleParkingTracking(minor, rssi);
                        break;
                    }
                    if (rssi >= -90 && !App.getInstance().isParkingStartFlag()) {
                        ParkingServiceStart();
                    } else {
                        handleParkingTracking(minor, rssi);
                    }
                    break;
            }
        } catch (Exception e) {
            sendServerError("processBeaconData Error: " + e.getMessage());
        }
    }

    private void ParkingServiceStart() {
        try {
            Log.d(TAG, "ParkingServiceStart() 호출");
            TimerSingleton.getInstance().StartWholeTimer(App.getInstance().getContext());
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

    private void setupSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                gyro  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
                if (gyro  != null) sensorManager.registerListener(this, gyro,  SensorManager.SENSOR_DELAY_UI);
            }
        } catch (Exception e) {
            sendServerError("setupSensors Error: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Beacon 연결 서비스",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            chan.setDescription("백그라운드 비콘 스캔 및 연결 서비스");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(chan);
        }
    }

    private void updateForegroundNotification(boolean isBluetoothOn) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, createNotification(isBluetoothOn));
    }

    private Notification createNotification(boolean isBluetoothOn) {
        String text = isBluetoothOn
                ? "백그라운드에서 비콘 및 센서를 탐색 중입니다."
                : "블루투스가 OFF되어 자동으로 현관문이 열리지 않습니다.";

        Intent dismissedIntent = new Intent(NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED);
        dismissedIntent.setPackage(getPackageName());
        PendingIntent deleteIntent = PendingIntent.getBroadcast(
                this, 0, dismissedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("스마트파킹")
                .setContentText(text)
                .setSmallIcon(R.drawable.logo)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDeleteIntent(deleteIntent)
                .build();
    }

    private void startAsForeground(boolean isBluetoothOn) {
        try {
            Notification notification = createNotification(isBluetoothOn);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            sendServerError("startAsForeground Error: " + e.getMessage());
        }
    }

    private void scheduleServiceRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), BleScanner.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // setExactAndAllowWhileIdle은 API 31+에서 SCHEDULE_EXACT_ALARM 권한 필요
            // 재시작 2초 딜레이는 정확한 타이밍 불필요 → setAndAllowWhileIdle 사용
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 2000, pendingIntent);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved() - 서비스 재시작 예약");
        scheduleServiceRestart();
    }

    private void sendServerError(String msg) {
        try {
            RestController.getInstance().errorMessage(UserDataSingleton.getInstance().getUserId(), "[BleScanner] " + msg);
        } catch (Exception ignore) {}
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() - 서비스 재시작 예약");
        if (handler != null) handler.removeCallbacksAndMessages(null);
        scheduleServiceRestart();
        stopBeaconScan();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (bluetoothReceiverRegistered) {
            try { unregisterReceiver(bluetoothReceiver); } catch (Exception ignore) {}
        }
        super.onDestroy();
    }

    public static boolean isServiceRunning(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
            if (services == null) return false;
            for (ActivityManager.RunningServiceInfo s : services) {
                if (BleScanner.class.getName().equals(s.service.getClassName())) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }
}
