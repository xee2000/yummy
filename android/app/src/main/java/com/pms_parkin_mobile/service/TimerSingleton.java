package com.pms_parkin_mobile.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.api.SimpleCallback;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.AccelSensor;
import com.pms_parkin_mobile.dto.AccelSensor2;
import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.foreground.AppRunning3;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class TimerSingleton {
    private CountDownTimer mWholeTimer;
    private static final String TAG = "TimerSingleton";
    private CountDownTimer mAccelTimer;
    private CountDownTimer mAfterStartCountDownTimer;
    private CountDownTimer mCollectAccelBeaconTimer;
    private boolean mLobbyTimerStart = false;
    private boolean mWholeTimerStart = false;
    private boolean mAccelTimerStart = false;
    private boolean mGyroTimerStart = false;
    // 시작 이후 수집하는 타이머
    private boolean COLLECT_START_BEACON_CALC = false;
    private TimerTask Collect_START_CALC_TIMER_TASK;
    private boolean mNotRestart = false;
    private boolean mCollectAccelBeaconStart = false;
    private boolean mCollectLobbyStart = false;
    private boolean mStayRestartStart = false;
    private boolean mNotStartBeaconStart = false;
    private static TimerSingleton instance;
    private Timer Collect_START_CALC_TIMER;
    public static TimerSingleton getInstance() {
        if (instance == null) {
            instance = new TimerSingleton();
        }
        return instance;
    }

    public void StartWholeTimer(){


        App.getInstance().setStartFlag(true);
        Log.d("TIMER","타이머 시작");
        int Delay = 900 * 1000;
        AfterStartTIMER();
        AccelTimer();
        if (mWholeTimer != null) mWholeTimer = null;
        //서비스가 시작했으면 처음은 false니까 진입
        if (!COLLECT_START_BEACON_CALC) {

            START_CALC_TIMER();
        }
        //total 15min timer
        mWholeTimer = new CountDownTimer(Delay, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {


                int Time = App.getInstance().getmWholeTimerDelay();
                Time++;
                App.getInstance().setmWholeTimerDelay(Time);





            }

            @Override
            public void onFinish() {

                if (App.getInstance().isServiceFlag()) {

                    if (COLLECT_START_BEACON_CALC) {
                        Collect_START_CALC_TIMER_TASK.cancel();
                        Collect_START_CALC_TIMER.cancel();
                        App.getInstance().setmCollectStartCalcBeacon(0);
                        TimerSingleton.getInstance().setCOLLECT_START_BEACON_CALC(false);
                    }
                    mWholeTimerStart = false;
                    COLLECT_START_BEACON_CALC = false;

                    if (mAccelTimerStart) {
                        if (mAccelTimer != null) {
                            try {
                                mAccelTimer.onFinish();
                                mAccelTimer.cancel();
                            } catch (RuntimeException e) {
                                Log.d("RunTimeException - ACCEL TIMER FINISH: %s", e.getMessage());
                            }
                        }
                    }

                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                    //0421 jhlee sensor 실측값으로 전송추가
                    saveArrayListValue.SaveAccelBeacon();
                    Total total = new Total();
                    total.setPhoneInfo("TEST");
                    total.setBeaconList(App.getInstance().getmBeaconArrayList());
                    total.setGyroList(App.getInstance().getmGyroSensorArrayList());
                    total.setSensorList(App.getInstance().getmAccelSensorArrayList());
                    total.setSensorList2(App.getInstance().getmAccelSensorArrayList2());
                    total.setAccelBeaconList(App.getInstance().getmAccelBeaconArrayList());
                    total.setInputDate(App.getInstance().getTime());
                    total.setParingState(App.getInstance().getmParingStateValue());
                    App.getInstance().setSAVE_DELAY(App.getInstance().getmWholeTimerDelay());


                    RestController.getInstance().parking(App.getInstance().getUserId(), App.getInstance().getDong(), App.getInstance().getHo(), total, new Callback<Void>(){
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {
                            Log.d("Success", "response : " + response.message());
                            App.getInstance().setStartFlag(false);
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.d("ERROR", "gyroinfo Error : " + t.getMessage());
                        }
                    });
//                    String accelResult = saveArrayListValue.AccelSensorResult();
//                    AccelSensor accelSensor = new AccelSensor();
//                    accelSensor.setState(accelResult);
//                    accelSensor.setSeq(String.valueOf(App.getInstance().getmAccelSequence()));
//                    accelSensor.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));
//
//                    AccelSensor2 accelSensor2 = new AccelSensor2();
//                    accelSensor2.setState(String.valueOf(App.getInstance().getmAccelCount()));
//                    accelSensor2.setSeq(String.valueOf(App.getInstance().getmAccelSequence()));
//                    accelSensor2.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));
//
//                    Log.d("TEST","accelSensor - ACCEL : " + accelSensor);
//                    Log.d("TEST","accelSensor2 - ACCEL : " + accelSensor2);
//
//
//                    App.getInstance().getmAccelSensorArrayList().add(accelSensor);
//                    App.getInstance().getmAccelSensorArrayList2().add(accelSensor2);
                }
            }
        }.start();
    }

    public void START_CALC_TIMER() {
        if (COLLECT_START_BEACON_CALC) {
            return;
        }
        COLLECT_START_BEACON_CALC = true;

        Collect_START_CALC_TIMER_TASK = new TimerTask() {
            @Override
            public void run() {
                Log.d("TimerSingleton","TEST_TIMER_SINGLETON - 30초");
                if (App.getInstance().getmCollectStartCalcBeacon() != 0) {
                    Log.d("TimerSingleton", "TEST_TIMER_SINGLETON_VAL - ACCEL_BEACON 0개 아님 종료 처리 : " + App.getInstance().getmCollectStartCalcBeacon());
                    App.getInstance().setmCollectStartCalcBeacon(0);
                } else {

                    Log.d("TimerSingleton", "TEST_TIMER_SINGLETON_VAL - ACCEL_BEACON 0개 종료 처리 : " + App.getInstance().getmCollectStartCalcBeacon());
                    //jhlee 0401 안드로이드에서 accel start 와 queue가 너무 빈번하게 일어나는 케이스로 비콘이 없어서 발생된 현상 맞는지 체크용 gyroinfo 추가
                    RestController.getInstance().gyroinfo(App.getInstance().getUserId(), 3030, new Callback<Void>(){
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {
                            Log.d("TimerSingleton", "response :  : " + response.message());


                            SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                            //0421 jhlee sensor 실측값으로 전송추가
                            saveArrayListValue.SaveAccelBeacon();
                            Total total = new Total();
                            total.setPhoneInfo("TEST");
                            total.setBeaconList(App.getInstance().getmBeaconArrayList());
                            total.setGyroList(App.getInstance().getmGyroSensorArrayList());
                            total.setSensorList(App.getInstance().getmAccelSensorArrayList());
                            total.setSensorList2(App.getInstance().getmAccelSensorArrayList2());
                            total.setAccelBeaconList(App.getInstance().getmAccelBeaconArrayList());
                            total.setInputDate(App.getInstance().getTime());
                            total.setParingState(App.getInstance().getmParingStateValue());
                            App.getInstance().setSAVE_DELAY(App.getInstance().getmWholeTimerDelay());
                            Log.d("TimerSingleton", "total : " + total);


                            RestController.getInstance().parking(App.getInstance().getUserId(), App.getInstance().getDong(), App.getInstance().getHo(), total, new Callback<Void>(){
                                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                    Log.d("Success", "response : " + response.message());
                                    ParkingAlarm(App.getInstance().getContext());
                                    App.getInstance().setStartFlag(false);
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Log.d("ERROR", "gyroinfo Error : " + t.getMessage());
                                }
                            });

                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.d("ERROR", "gyroinfo Error : " + t.getMessage());
                        }
                    });
                    //jhlee 0401 안드로이드에서 accel start 와 queue가 너무 빈번하게 일어나는 케이스로 비콘이 없어서 발생된 현상 맞는지 체크용 gyroinfo 추가 수정끝

                    // 1번 로비비콘을 통해서 끝날 시 mOutParking = true가 되는데 이곳은 비컨이 30초 동안 들어오지 않은 상태이므로 false로 변경
                    // 전체 타이머가 돌고있을 경우 전체 타이머 종료
                    if (App.getInstance().isStartFlag()) {
                        try {
                            mWholeTimer.onFinish();
                            mWholeTimer.cancel();
                        } catch (RuntimeException e) {
                            Timber.e("RuntimeException - Whole Timer Finish: %s", e.getMessage());
                        }
                    }

                    Collect_START_CALC_TIMER_TASK.cancel();
                    Collect_START_CALC_TIMER.cancel();

                    /*
                    if (mWholeTimerStart) {
                        if (!mCollectLobbyStart) {
                            if (!mDataManagerSingleton.isEnd1Beacon()) {
                                mDataManagerSingleton.setABNORMAL_END(true);

                                Timber.d("TEST_TIMER_SINGLETON_VAL - ACCEL_BEACON 0개 종료 처리");
                                mDataManagerSingleton.setOutParking(true);

                                if (isWholeTimerStart()) {
                                    try {
                                        mWholeTimer.onFinish();
                                        mWholeTimer.cancel();
                                    } catch (RuntimeException e) {
                                        Timber.e("RuntimeException - Whole Timer Finish: %s", e.getMessage());
                                    }
                                }
                                Collect_START_CALC_TIMER_TASK.cancel();
                                Collect_START_CALC_TIMER.cancel();
                            }
                        }
                    }
                    */
                    // 초기화
                    App.getInstance().setmCollectStartCalcBeacon(0);
                }
            }
        };

        try {
            Collect_START_CALC_TIMER = new Timer();
            Collect_START_CALC_TIMER.schedule(Collect_START_CALC_TIMER_TASK, 30000, 30000);
        } catch (IllegalStateException ex) {
            RestController.getInstance().gyroinfo(App.getInstance().getUserId(), 3031, new Callback<Void>(){
                @Override public void onResponse(Call<Void> call, Response<Void> response) {

                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.d("ERROR", "gyroinfo Error : " + t.getMessage());
                }
            });
        }
    }



    public void AfterStartTIMER() {
        if (!App.getInstance().ismAfterStart()) {
            App.getInstance().setmAfterStart(true);
            mAfterStartCountDownTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    App.getInstance().setmAfterStart(false);

                    try {
                        App.getInstance().setmParingStateValue("ANDROID");
                    } catch (Exception ex) {
                        // ignore exception
                    }
                }
            }.start();
        }
    }

    public void StartCollectAccelBeacon() {
        mCollectAccelBeaconStart = true;

        mCollectAccelBeaconTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mCollectAccelBeaconStart = false;
            }
        }.start();
    }


    public void AccelTimer() {
        // jhlee 0410 Delay 시간 2초 -> 1초로 수정
        int Delay = 1000;
        // jhlee 0410 Delay 시간 2초 -> 1초로 수정끝

        if (mAccelTimer != null) mAccelTimer = null;

        mAccelTimerStart = true;

        mAccelTimer = new CountDownTimer(Delay, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    mAccelTimerStart = false;

                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                    //0421 jhlee sensor 실측값으로 전송추가
                    String accelResult = saveArrayListValue.AccelSensorResult();
                    int accelResult2 = App.getInstance().getInstance().getmAccelCount();

                    String preState = App.getInstance().getInstance().getmPreState();
                    if (preState == null) {
                        App.getInstance().setmPreState(accelResult);
                    } else {
                        // 이전값이 T 이고 현재 값이 S/W 일경우 타이머를 돌린다.
                        if (preState.equals("T") && (accelResult.equals("S") || accelResult.equals("W"))) {
                            if (!mCollectAccelBeaconStart) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // only for gingerbread and newer versions
                                    StartCollectAccelBeacon();
                                } else {
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(() -> StartCollectAccelBeacon());
                                }
                            }
                        }

                        App.getInstance().setmPreState(accelResult);
                    }

                    if (App.getInstance().isStartFlag()) {
                        AccelSensor accelSensor = new AccelSensor();
                        accelSensor.setState(accelResult);
                        accelSensor.setSeq(String.valueOf(App.getInstance().getmAccelSequence()));
                        accelSensor.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));

                        AccelSensor2 accelSensor2 = new AccelSensor2();
                        accelSensor2.setState(String.valueOf(accelResult2));
                        accelSensor2.setSeq(String.valueOf(App.getInstance().getmAccelSequence()));
                        accelSensor2.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));
                        App.getInstance().getmAccelSensorArrayList().add(accelSensor);
                        App.getInstance().getmAccelSensorArrayList2().add(accelSensor2);
                    }
                    //0421 jhlee sensor 실측값으로 전송추가끝

                    int accelSensorSequence = App.getInstance().getmAccelSequence() + 1;
                    App.getInstance().setmAccelSequence(accelSensorSequence);

                    App.getInstance().setmAccelCount(0);
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        }.start();
    }




    public boolean isCOLLECT_START_BEACON_CALC() {
        return COLLECT_START_BEACON_CALC;
    }

    public void setCOLLECT_START_BEACON_CALC(boolean COLLECT_START_BEACON_CALC) {
        this.COLLECT_START_BEACON_CALC = COLLECT_START_BEACON_CALC;
    }

    public static void ParkingAlarm(Context context) {
        Intent AppRunning3 = new Intent(context, AppRunning3.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(AppRunning3);
        } else {
            context.startService(AppRunning3);
        }
    }
}
