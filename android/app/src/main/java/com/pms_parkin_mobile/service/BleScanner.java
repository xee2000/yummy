package com.pms_parkin_mobile.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import static android.view.KeyCharacterMap.ALPHA;
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
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pms_parkin_mobile.R;
import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.dataManager.DataManagerSingleton;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.GyroSensor;
import com.pms_parkin_mobile.dto.User;
import com.pms_parkin_mobile.receiver.NetworkStateReceiver;
import com.pms_parkin_mobile.receiver.NotificationActionReceiver;
import com.pms_parkin_mobile.util.KalmanFilter;

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
    int RollResultCount = 0, PitchResultCount = 0, YawResultCount = 0;

    int PreRollCount = 0;
    int PrePitchCount = 0;
    int PreYawCount = 0;

    float NextValue = 0;      // 현재 CVA 값
    float PreValue = 0;       // 이전 CVA 값
    int DefaultAbsValue = 4;  // 기본 Default PreValue-NextValue 절대값

    boolean BMatchValue = true;
    int IMatchValue = 0;
    boolean SR = false;
    boolean SP = false;
    boolean SY = false;
    int limitValue = 100;
    private KalmanFilter kalman_X;
    private KalmanFilter kalman_Y;
    private KalmanFilter kalman_Z;

    // jhlee 추가 시작
    private float accel_x;
    private float accel_y;
    private float accel_z;
    private int drive_accel_index = 0;
    private int drive_con_count = 0;
    private final float alpha = 0.8f;
    private final float[] gravity = new float[3];
    private final float[] linear_acceleration = new float[3];
    // jhlee 추가 끝 */
    private NetworkStateReceiver networkStateReceiver;
    private static boolean mSensorServiceRunning = false;

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
            networkStateReceiver = new NetworkStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            App.getInstance().setServiceFlag(true);
            mbaconFunction = new BeaconFunction();
            mSaveArrayListValue = new SaveArrayListValue();

            setupSensors();
            setupBeaconManager();

            // 리시버 등록

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

    @Override
    public void onSensorChanged(SensorEvent event) {

        //jhlee 0310 서비스 시작전이면 동작하지 않도록 제한 수정
        if(!TimerSingleton.getInstance().isWholeTimerStart()){
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float CVA;


            //jhlee 0310 서비스 시작전이면 동작하지 않도록 제한 수정끝

            // CVA 값 계산
            float[] accelData = new float[3];
            accelData = filter(event.values.clone(), accelData);
            CVA = (float) Math.sqrt(accelData[0] * accelData[0] + accelData[1] * accelData[1] + accelData[2] * accelData[2]);

            // 이전 값이 있을 경우에는 계산을 진행한다.
            if (PreValue != 0) {
                NextValue = CVA;

                float ABSValue = Math.abs(PreValue - NextValue);

                if (ABSValue >= DefaultAbsValue) {
                    int accel_count = DataManagerSingleton.getInstance().getAccelCount() + 1;
                    DataManagerSingleton.getInstance().setAccelCount(accel_count);
                }
                PreValue = NextValue;
            } else
                PreValue = CVA;

            if (!TimerSingleton.getInstance().isAccelTimerStart()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // only for gingerbread and newer versions
                    TimerSingleton.getInstance().AccelTimer();

                } else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> TimerSingleton.getInstance().AccelTimer());
                }
            }

            // jhlee 추가 시작
            // 운전중인지 확인하는 로직
            if (drive_accel_index >= 9) { //10 -> 0.1초
                float accel_x_value = accel_x / 10;
                float accel_y_value = accel_y / 10;
                float accel_z_value = accel_z / 10;

                if ((accel_x_value > 0.02 && accel_x_value < 0.4) || (accel_y_value > 0.02 && accel_y_value < 0.4) || (accel_z_value > 0.02 && accel_z_value < 0.4)) {
                    drive_con_count++;
                    // jhlee 2025.04.01 30 -> 10으로 단축
//                    if (drive_con_count > 10) // 1초 유지
//                    {
//                        //시작 타이머가 돌지 않고 있다면 stay 비컨이 수집되는지에 따라서 타이머가 시작되도록 한다.
//                        if (!TimerSingleton.getInstance().isWholeTimerStart()) {
//                            if (!TimerSingleton.getInstance().isAFTER_ACCEL_START_CALC()) {
//                                //jhlee 0708 수정 보내야하는 queue값이 있는 경우 시작을 보류한다
//                                //jhlee 0812 기존 시작조건을 sensor가 아닌 비콘으로만 해서 수정
////                              if (!DataManagerSingleton.getInstance().isWorkingState()){
////                                  Log.d("Lee", "서비스 시작전 after accel start timer");
////                                  TimerSingleton.getInstance().AFTER_ACCEL_START_TIMER(getApplicationContext());
////                              }
//                                //jhlee 0708 수정끝 보내야하는 queue값이 있는 경우 시작을 보류한다
//                            }
//                        } else { //시작되었다면 일반 비컨을 수집한다.
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                                // only for gingerbread and newer versions
////                                TimerSingleton.getInstance().GyroTimer();
//                                  TimerSingleton.getInstance().AFTER_ACCEL_START_TIMER(getApplicationContext());
//                            } else {
//                                Handler handler = new Handler(Looper.getMainLooper());
////                                handler.post(() -> TimerSingleton.getInstance().GyroTimer());
//                                handler.post(() -> TimerSingleton.getInstance().AFTER_ACCEL_START_TIMER(getApplicationContext()));
//                            }
//                        }
//                        drive_con_count = 0;
//                              }
                } else {
                    drive_con_count = 0;
                }

                accel_x = 0;
                accel_y = 0;
                accel_z = 0;
                drive_accel_index = 0;
            } else {
                //운전중을 확인하는 로직
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                //중력 성분 제거
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                accel_x += Math.abs(linear_acceleration[0]);
                accel_y += Math.abs(linear_acceleration[1]);
                accel_z += Math.abs(linear_acceleration[2]);
            }
            drive_accel_index++;
            //jhlee 추가 끝 */
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            final float LIMIT_MAX = 0.5f;
            final float LIMIT_MIN = -0.5f;

            double Roll = event.values[0];
            double Pitch = event.values[1];
            double Yaw = event.values[2];

            //1221 jhlee wholetimer가 시작했고 사용자가 빅데이터로 받기를 체크한경우
            //noformatVersion으로 넘겨준다 수정중
            //1231 기존 트리러가 false상태로 되잇어서 true로 수정
            if (TimerSingleton.getInstance().isWholeTimerStart()) {

                if(UserDataSingleton.getInstance().getBigDataSend()){
                    noFormatVersionGyroSensorResult((Roll), (Pitch), (Yaw));
                }else if(!UserDataSingleton.getInstance().getBigDataSend()){
                    if ((Roll > LIMIT_MAX || Roll < LIMIT_MIN) || (Pitch > LIMIT_MAX || Pitch < LIMIT_MIN) || (Yaw > LIMIT_MAX || Yaw < LIMIT_MIN)) {
                        kalman_X.Init();
                        kalman_Y.Init();
                        kalman_Z.Init();
                    } else {
                        double FilterX = kalman_X.Update(Roll);
                        double FilterY = kalman_Y.Update(Pitch);
                        double FilterZ = kalman_Z.Update(Yaw);
                        formatVersionGyroSensorResult((FilterX), (FilterY), (FilterZ));
                    }
                }
            }
            //noformatVersion으로 넘겨준다 수정
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //Sensor X,Y,Z 값 을 CVA 로 사용할 Data 변환 시켜주는 코드
    //중력제거
    private float[] filter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private void noFormatVersionGyroSensorResult(double roll, double pitch, double yaw) {

        // 혹시나 해서 들어온 값도 다시 한 번 절삭
        double cutRoll  = roll;
        double cutPitch = pitch;
        double cutYaw   = yaw;

        //한개는 기존처럼 똑같이 담아주고
        GyroSensor gyroSensor = new GyroSensor();
        gyroSensor.setDelay(String.valueOf(DataManagerSingleton.getInstance().getWholeTimerDelay()));
        gyroSensor.setX(String.valueOf(cutTo2(cutRoll)));
        gyroSensor.setY(String.valueOf(cutTo2(cutPitch)));
        gyroSensor.setZ(String.valueOf(cutTo2(cutYaw)));
        DataManagerSingleton.getInstance().getGyroSensorArrayList().add(gyroSensor);

        //압축버전의 큐과 file의 경우 file키값에는 원형의 값을 넣어주고 다른 필드에는 원형이 아닌 절삭된 값을 넣어준다.
        formatVersionGyroSensorResult((cutRoll), (cutPitch), (cutYaw));
    }

    //jhlee 변경버전 기존 방식대로 처리받는 함수 -> 함수명 수정
    private void formatVersionGyroSensorResult(double Roll, double Pitch, double Yaw) {
        final float LimitMinus = -0.025f;
        final float LimitPlus = 0.025f;

        DataManagerSingleton dataManagerSingleton = DataManagerSingleton.getInstance();
        if (dataManagerSingleton == null) return;

        Object[] RollA = dataManagerSingleton.getROLL_QUEUE().toArray();
        if (RollA.length == 4) {
            if (!(((double) RollA[0] < LimitPlus && (double) RollA[0] > LimitMinus) || ((double) RollA[1] < LimitPlus && (double) RollA[1] > LimitMinus) || ((double) RollA[2] < LimitPlus && (double) RollA[2] > LimitMinus) || ((double) RollA[3] < LimitPlus && (double) RollA[3] > LimitMinus))) {
                RollResultCount++;
            } else {
                if (RollResultCount >= limitValue) {
                    SR = true;
                    PreRollCount = RollResultCount;
                }
                RollResultCount = 0;
            }

            dataManagerSingleton.getROLL_QUEUE().poll();
            Double SECONDR = dataManagerSingleton.getROLL_QUEUE().poll();
            Double THIRDR = dataManagerSingleton.getROLL_QUEUE().poll();
            Double FORTHR = dataManagerSingleton.getROLL_QUEUE().poll();

            dataManagerSingleton.getROLL_QUEUE().offer(SECONDR == null ? 0.0 : SECONDR);
            dataManagerSingleton.getROLL_QUEUE().offer(THIRDR == null ? 0.0 : THIRDR);
            dataManagerSingleton.getROLL_QUEUE().offer(FORTHR == null ? 0.0 : FORTHR);
            dataManagerSingleton.getROLL_QUEUE().offer(Roll);
        } else {
            dataManagerSingleton.getROLL_QUEUE().offer(Roll);
        }

        Object[] PITCHA = dataManagerSingleton.getPITCH_QUEUE().toArray();
        if (PITCHA.length == 4) {
            if (!(((double) PITCHA[0] < LimitPlus && (double) PITCHA[0] > LimitMinus) || ((double) PITCHA[1] < LimitPlus && (double) PITCHA[1] > LimitMinus) || ((double) PITCHA[2] < LimitPlus && (double) PITCHA[2] > LimitMinus) || ((double) PITCHA[3] < LimitPlus && (double) PITCHA[3] > LimitMinus))) {
                PitchResultCount++;

            } else {
                if (PitchResultCount >= limitValue) {
                    SP = true;
                    PrePitchCount = PitchResultCount;
                }
                PitchResultCount = 0;
            }

            dataManagerSingleton.getPITCH_QUEUE().poll();
            Double SECONDP = dataManagerSingleton.getPITCH_QUEUE().poll();
            Double THIRDP = dataManagerSingleton.getPITCH_QUEUE().poll();
            Double FORTHP = dataManagerSingleton.getPITCH_QUEUE().poll();

            dataManagerSingleton.getPITCH_QUEUE().offer(SECONDP == null ? 0.0 : SECONDP);
            dataManagerSingleton.getPITCH_QUEUE().offer(THIRDP == null ? 0.0 : THIRDP);
            dataManagerSingleton.getPITCH_QUEUE().offer(FORTHP == null ? 0.0 : FORTHP);
            dataManagerSingleton.getPITCH_QUEUE().offer(Pitch);
        } else {
            dataManagerSingleton.getPITCH_QUEUE().offer(Pitch);
        }

        Object[] YAWA = dataManagerSingleton.getYAW_QUEUE().toArray();
        if (YAWA.length == 4) {
            if (!(((double) YAWA[0] < LimitPlus && (double) YAWA[0] > LimitMinus) ||
                    ((double) YAWA[1] < LimitPlus && (double) YAWA[1] > LimitMinus) ||
                    ((double) YAWA[2] < LimitPlus && (double) YAWA[2] > LimitMinus) ||
                    ((double) YAWA[3] < LimitPlus && (double) YAWA[3] > LimitMinus))) {
                YawResultCount++;
            } else {
                if (YawResultCount >= limitValue) {
                    SY = true;
                    PreYawCount = YawResultCount;
                }
                YawResultCount = 0;
            }

            dataManagerSingleton.getYAW_QUEUE().poll();
            Double SECONDY = dataManagerSingleton.getYAW_QUEUE().poll();
            Double THIRDY = dataManagerSingleton.getYAW_QUEUE().poll();
            Double FORTH = dataManagerSingleton.getYAW_QUEUE().poll();

            dataManagerSingleton.getYAW_QUEUE().offer(SECONDY == null ? 0.0 : SECONDY);
            dataManagerSingleton.getYAW_QUEUE().offer(THIRDY == null ? 0.0 : THIRDY);
            dataManagerSingleton.getYAW_QUEUE().offer(FORTH == null ? 0.0 : FORTH);
            dataManagerSingleton.getYAW_QUEUE().offer(Yaw);
        } else {
            dataManagerSingleton.getYAW_QUEUE().offer(Yaw);
        }

        if ((PreRollCount >= limitValue || PrePitchCount >= limitValue || PreYawCount >= limitValue) && (RollResultCount == 0 && PreRollCount != 0) || (PitchResultCount == 0 && PrePitchCount != 0) || (YawResultCount == 0 && PreYawCount != 0)) {
            IMatchValue++;
            BMatchValue = true;

            dataManagerSingleton.setSaveCountRoll(PreRollCount);
            dataManagerSingleton.setSaveCountPitch(PrePitchCount);
            dataManagerSingleton.setSaveCountYaw(PreYawCount);

            SR = false;
            SP = false;
            SY = false;
            PreRollCount = 0;
            PrePitchCount = 0;
            PreYawCount = 0;
        } else {
            BMatchValue = false;
            if (IMatchValue != 0) {
                TimerSingleton timerSingleton = TimerSingleton.getInstance();
                Log.d("TEST", "이구간오나");

                //기존과 다르게 StartWholeTimer의 조건은 Beacon감지로만 진행하기에 주석처리한다
//                if (dataManagerSingleton.isRESTART_BEACON() && !timerSingleton.isWholeTimerStart() && "T".equals(dataManagerSingleton.getPreState()) && timerSingleton.isStayRestartStart()) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        // only for gingerbread and newer versions
//                        timerSingleton.StartWholeTimer(getApplicationContext());
//                    } else {
//                        Handler handler = new Handler(Looper.getMainLooper());
//                        handler.post(() -> timerSingleton.StartWholeTimer(getApplicationContext()));
//                    }
//
//                    //jhlee 0306 코드 이동 TimerSingleTon으로 변경
////                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
////                    String currentDate = simpleDateFormat.format(Calendar.getInstance().getTime());
////                    dataManagerSingleton.setInputTime("move_" + currentDate);
//                    //jhlee 0306 코드 이동 TimerSingleTon으로 변경끝
//                    if (timerSingleton.isStayRestartStart()) {
//                        try {
//                            timerSingleton.getStayRestartStartTimer().onFinish();
//                            timerSingleton.getStayRestartStartTimer().cancel();
//                        } catch (RuntimeException e) {
//                            Timber.e("RuntimeException - STY RESTART START onFinish : %s", e.getMessage());
//                        }
//                    }
//                }

//                if (!timerSingleton.isWholeTimerStart()) {
//                    if (!timerSingleton.isAFTER_GYRO_START_CALC()) {
//                        timerSingleton.AFTER_GYRO_START_TIMER(getApplicationContext());
//                    }
//                }

                if (timerSingleton.isWholeTimerStart()) {
                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();

                    if(UserDataSingleton.getInstance().getBigDataSend()){
                        //압축버전의 경우 원형데이터는 file필드에 넣어주고 기존 방식의 필드에는 절삭된 데이터를 넣어준다.
                        Log.d("TEST", "압축버전의 경우 원형데이터는 file필드에 넣어주고 기존 방식의 필드에는 절삭된 데이터를 넣어준다.");
                        saveArrayListValue.SaveGyro2();
                    }else{
                        saveArrayListValue.SaveGyro();
                    }
                }

                BMatchValue = false;
                IMatchValue = 0;

                PitchResultCount = 0;
                RollResultCount = 0;
                YawResultCount = 0;
            }
        }
    }

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

    //jhlee 소수점 둘째 자르기 함수
    private double cutTo2(double value) {
        double factor = 100.0;
        long tmp = (long) (value * factor);
        return tmp / factor;
    }
}