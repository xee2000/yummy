package com.pms_parkin_mobile.service;

import android.app.ActivityManager;
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

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;

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

public class BleScanner extends Service implements BeaconConsumer, SensorEventListener {

    private static final String TAG = "BleScanner";
    private static final String CHANNEL_ID = "connect_beacon";

    // ===== Beacon =====
    private static final String TARGET_UUID = "20151005-8864-5654-3020-010400240902";

    private final Region beaconRegion = new Region(
            "target-uuid-only",
            org.altbeacon.beacon.Identifier.parse(TARGET_UUID), // UUID 고정
            null,                                               // Major 무관
            null                                                // Minor 무관
    );
    private BeaconManager beaconManager;
    private RestController controller;
    private boolean isInsideRegion = false;
    private int previousMinor = -1;
    private BeaconFunction mbaconFunction;
    private SaveArrayListValue mSaveArrayListValue;
    // ===== Sensor =====
    private SensorManager sensorManager;
    private Sensor accel, gyro;
    private Context context;

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
        context = this;
        startAsForeground();
        App.getInstance().setServiceFlag(true);
        mbaconFunction = new BeaconFunction(getApplicationContext());
        mSaveArrayListValue = new SaveArrayListValue();
        // ===== Sensor 등록 =====
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyro  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (accel != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "Accelerometer registered");
            } else {
                Log.w(TAG, "No accelerometer sensor");
            }
            if (gyro != null) {
                sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
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
        beaconManager.getBeaconParsers().clear();

        // iBeacon
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // AltBeacon
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24"));

        // Eddystone UID
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));

        // Eddystone URL
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41"));

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
        App.getInstance().setServiceFlag(false);
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

        // ✅ RangeNotifier 등록 (중복 onBeaconServiceConnect 제거, @Override 위치 수정)
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon b : beacons) {
                    Log.d(TAG,
                            "Beacon: uuid=" + b.getId1() +
                                    ", major=" + b.getId2() +
                                    ", minor=" + b.getId3() +
                                    ", rssi=" + b.getRssi() +
                                    ", txPower=" + b.getTxPower());
                }

                if (beacons.isEmpty()) return;

                Beacon strongest = Collections.max(beacons, Comparator.comparingInt(Beacon::getRssi));
                int major = strongest.getId2() != null ? strongest.getId2().toInt() : -1;
                int minor = strongest.getId3() != null ? strongest.getId3().toInt() : -1;
                int rssi = strongest.getRssi();


                Log.d(TAG, "App.getInstance().isServiceFlag(): " + App.getInstance().isServiceFlag());
                //2번으로 시작하고 서비스사용여부가 켜짐이며 시작한적이 없을경우


                //수동주차위치를 누르지 않은 상태일때만 들어오도록 한다.
                if (minor == 2 && rssi >= -90 && App.getInstance().isServiceFlag() && !App.getInstance().isPassiveCheck()) {
                    Log.d(TAG, "Parking Service Start - major: " + major + ", minor: " + minor);
                    ParkingServiceStart();
                }

                //수동 주차위치를 처리한경우 이곳으로 들어온다.
                if(minor == 2 && rssi >= -90 && App.getInstance().isPassiveCheck()){
                    Log.d("BleScanner" , "수동주차위치 설정중에는 시작하지 않음");
                    String id;

                    if (minor > 32768) {
                        id = String.valueOf(minor - 32768);
                    } else {
                        id = String.valueOf(minor);

                    }
                    String hexValue = String.format("%04X", Integer.valueOf(id));
                    PassiveParkingService.getInstance(context).parkingStart(hexValue, String.valueOf(rssi));
                }

                if(App.getInstance().isServiceFlag() && App.getInstance().isParkingStartFlag() && !App.getInstance().isPassiveCheck()){
                    String id;
                      Log.d("BleScanner" , "Start 이후에 데이터 확인");
                    if (minor > 32768) {
                        id = String.valueOf(minor - 32768);
                    } else {
                        id = String.valueOf(minor);
                    }

                    if(rssi >= -80){
                        Log.d("BleScanner" , "신호세기 충분충족");

                        String hexValue = String.format("%04X", Integer.valueOf(id));
                        mSaveArrayListValue.SaveAccelBeacon(hexValue, String.valueOf(rssi), String.valueOf(App.getInstance().getmWholeTimerDelay()));
                        // 2025.02.17 by jhlee
                        mbaconFunction.AddAccelDelay(hexValue, rssi);
                        int gyroCount = App.getInstance().getmAfterGyroCount();
                        int accelCount = App.getInstance().getmAfterAccelCount();
                        int startCount = App.getInstance().getmAfterStartCount();
                        int CalcBeacon = App.getInstance().getmCollectStartCalcBeacon();

                        Log.d("TimerSingleton", "WholeTimer onTick - GyroCount: " + gyroCount + ", AccelCount: " + accelCount + ", StartCount: " + startCount + ", CalcBeacon: " + CalcBeacon);
                        App.getInstance().setmAfterGyroCount(gyroCount + 1);
                        App.getInstance().setmAfterAccelCount(accelCount + 1);
                        App.getInstance().setmAfterStartCount(startCount + 1);
                        App.getInstance().setmCollectStartCalcBeacon(CalcBeacon + 1);
                    }
                }

                if (minor != previousMinor) {
                    previousMinor = minor;
                    Log.d(TAG, "didRange: new minor=" + minor);
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

    private void ParkingServiceStart() {
        //서비스가 시작현경우 상태를 true로 활성화해준다
        Log.d(TAG, "ParkingServiceStart() 호출");
        TimerSingleton.getInstance().StartWholeTimer();
        String currentTime = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());
        App.getInstance().setTime(currentTime);
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
        if(App.getInstance().isPassiveCheck()){
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float[] v = event.values;
             Log.d(TAG, String.format("Accel: x=%.3f y=%.3f z=%.3f", v[0], v[1], v[2]));

            SensorActive.ReadAccel(event.values);

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float[] v = event.values;

            // Log.d(TAG, String.format("Gyro : x=%.3f y=%.3f z=%.3f", v[0], v[1], v[2]));
            SensorActive.ReadGyro(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 필요 시 처리
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (BleScanner.class.getName().equals(s.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}