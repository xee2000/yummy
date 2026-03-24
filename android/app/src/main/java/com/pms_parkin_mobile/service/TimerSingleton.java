package com.pms_parkin_mobile.service;

import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.pms_parkin_mobile.api.RestController;
import com.pms_parkin_mobile.dataManager.DataManagerSingleton;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.AccelSensor;
import com.pms_parkin_mobile.dto.AccelSensor2;
import com.pms_parkin_mobile.dto.Total;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class TimerSingleton {
    private final DataManagerSingleton mDataManagerSingleton = DataManagerSingleton.getInstance();
    private boolean mLobbyTimerStart = false;
    private boolean mWholeTimerStart = false;
    private boolean mAccelTimerStart = false;
    private boolean mGyroTimerStart = false;
    // 시작 이후 수집하는 타이머
    private boolean COLLECT_START_BEACON_CALC = false;
    private boolean mNotRestart = false;
    private boolean mCollectAccelBeaconStart = false;
    private boolean mCollectLobbyStart = false;
    private boolean mStayRestartStart = false;
    private boolean mNotStartBeaconStart = false;
    /**
     * Time out 관련 Data
     **/
    private boolean mTimeoutTimerStart = false;
    private int mFirstTimeout = 60 * 1000;
    private int mSecondTimeout = 120 * 1000;
    private int mThirdTimeout = 180 * 1000;
    private CountDownTimer mTimeoutCountDownTimer;
    private CountDownTimer mLobbyTimer;
    private CountDownTimer mWholeTimer;
    private CountDownTimer mAccelTimer;
    private CountDownTimer mGyroTimer;
    private CountDownTimer mCollectAccelBeaconTimer;
    private CountDownTimer mCollectLobbyTimer;
    private CountDownTimer mStayRestartStartTimer;
    // 시작 이후 10초간 일반 비컨이 몇개들어오는지 셀 Count
    private CountDownTimer mAfterStartCountDownTimer;
    private boolean mAfterStart = false;
    // 자이로 발생이후 일반 비컨을 수집하는 타이머
    private boolean AFTER_GYRO_START_CALC = false;
    private CountDownTimer AFTER_GYRO_START_COUNTDOWN_TIMER;

    // jhlee 추가 시작
    private boolean AFTER_ACCEL_START_CALC = false;
    private CountDownTimer AFTER_ACCEL_START_COUNTDOWN_TIMER;
    //jhlee 추가 끝 */

    // 시작을 하였으나 로비비컨만 받고 엘리베이터 비컨을 못받았을때 상황을 확인하는 타이머
    private boolean AFTER_LOBBY_ELEVATOR_CHECK = false;
    private CountDownTimer AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER;
    private TimerTask mNotStartBeaconStartTimerTask;
    private Timer mNotStartBeaconStartTimer;
    private TimerTask Collect_START_CALC_TIMER_TASK;
    private Timer Collect_START_CALC_TIMER;

    private TimerSingleton() {
    }

    public static TimerSingleton getInstance() {
        return TimerSingletonHolder.instance;
    }

    public boolean isLobbyTimerStart() {
        return mLobbyTimerStart;
    }

    public void setLobbyTimerStart(boolean lobbyTimerStart) {
        mLobbyTimerStart = lobbyTimerStart;
    }

    public boolean isWholeTimerStart() {
        return mWholeTimerStart;
    }

    public void setWholeTimerStart(boolean wholeTimerStart) {
        mWholeTimerStart = wholeTimerStart;
    }

    public boolean isAccelTimerStart() {
        return mAccelTimerStart;
    }

    public void setAccelTimerStart(boolean accelTimerStart) {
        mAccelTimerStart = accelTimerStart;
    }

    public boolean isGyroTimerStart() {
        return mGyroTimerStart;
    }

    public void setGyroTimerStart(boolean gyroTimerStart) {
        mGyroTimerStart = gyroTimerStart;
    }

    public boolean isCOLLECT_START_BEACON_CALC() {
        return COLLECT_START_BEACON_CALC;
    }

    public void setCOLLECT_START_BEACON_CALC(boolean COLLECT_START_BEACON_CALC) {
        this.COLLECT_START_BEACON_CALC = COLLECT_START_BEACON_CALC;
    }

    public boolean isNotRestart() {
        return mNotRestart;
    }

    public void setNotRestart(boolean notRestart) {
        mNotRestart = notRestart;
    }

    public boolean isCollectAccelBeaconStart() {
        return mCollectAccelBeaconStart;
    }

    public void setCollectAccelBeaconStart(boolean collectAccelBeaconStart) {
        mCollectAccelBeaconStart = collectAccelBeaconStart;
    }

    public boolean isCollectLobbyStart() {
        return mCollectLobbyStart;
    }

    public void setCollectLobbyStart(boolean collectLobbyStart) {
        mCollectLobbyStart = collectLobbyStart;
    }

    public boolean isStayRestartStart() {
        return mStayRestartStart;
    }

    public void setStayRestartStart(boolean stayRestartStart) {
        mStayRestartStart = stayRestartStart;
    }

    public boolean isNotStartBeaconStart() {
        return mNotStartBeaconStart;
    }

    public void setNotStartBeaconStart(boolean notStartBeaconStart) {
        mNotStartBeaconStart = notStartBeaconStart;
    }

    public boolean isTimeoutTimerStart() {
        return mTimeoutTimerStart;
    }

    public void setTimeoutTimerStart(boolean timeoutTimerStart) {
        mTimeoutTimerStart = timeoutTimerStart;
    }

    public int getFirstTimeout() {
        return mFirstTimeout;
    }

    public void setFirstTimeout(int firstTimeout) {
        mFirstTimeout = firstTimeout;
    }

    public int getSecondTimeout() {
        return mSecondTimeout;
    }

    public void setSecondTimeout(int secondTimeout) {
        mSecondTimeout = secondTimeout;
    }

    public int getThirdTimeout() {
        return mThirdTimeout;
    }

    public void setThirdTimeout(int thirdTimeout) {
        mThirdTimeout = thirdTimeout;
    }

    public CountDownTimer getTimeoutCountDownTimer() {
        return mTimeoutCountDownTimer;
    }

    public void setTimeoutCountDownTimer(CountDownTimer timeoutCountDownTimer) {
        mTimeoutCountDownTimer = timeoutCountDownTimer;
    }

    public CountDownTimer getLobbyTimer() {
        return mLobbyTimer;
    }

    public void setLobbyTimer(CountDownTimer lobbyTimer) {
        mLobbyTimer = lobbyTimer;
    }

    public CountDownTimer getWholeTimer() {
        return mWholeTimer;
    }

    public void setWholeTimer(CountDownTimer wholeTimer) {
        mWholeTimer = wholeTimer;
    }

    public CountDownTimer getAccelTimer() {
        return mAccelTimer;
    }

    public void setAccelTimer(CountDownTimer accelTimer) {
        mAccelTimer = accelTimer;
    }

    public CountDownTimer getGyroTimer() {
        return mGyroTimer;
    }

    public void setGyroTimer(CountDownTimer gyroTimer) {
        mGyroTimer = gyroTimer;
    }

    public CountDownTimer getCollectAccelBeaconTimer() {
        return mCollectAccelBeaconTimer;
    }

    public void setCollectAccelBeaconTimer(CountDownTimer collectAccelBeaconTimer) {
        mCollectAccelBeaconTimer = collectAccelBeaconTimer;
    }

    public CountDownTimer getCollectLobbyTimer() {
        return mCollectLobbyTimer;
    }

    public void setCollectLobbyTimer(CountDownTimer collectLobbyTimer) {
        mCollectLobbyTimer = collectLobbyTimer;
    }

    public CountDownTimer getStayRestartStartTimer() {
        return mStayRestartStartTimer;
    }

    public void setStayRestartStartTimer(CountDownTimer stayRestartStartTimer) {
        mStayRestartStartTimer = stayRestartStartTimer;
    }

    public CountDownTimer getAfterStartCountDownTimer() {
        return mAfterStartCountDownTimer;
    }

    public void setAfterStartCountDownTimer(CountDownTimer afterStartCountDownTimer) {
        mAfterStartCountDownTimer = afterStartCountDownTimer;
    }

    public boolean isAfterStart() {
        return mAfterStart;
    }

    public void setAfterStart(boolean afterStart) {
        mAfterStart = afterStart;
    }

    public boolean isAFTER_GYRO_START_CALC() {
        return AFTER_GYRO_START_CALC;
    }

    public void setAFTER_GYRO_START_CALC(boolean AFTER_GYRO_START_CALC) {
        this.AFTER_GYRO_START_CALC = AFTER_GYRO_START_CALC;
    }

    // jhlee 추가 시작
    public boolean isAFTER_ACCEL_START_CALC() {
        return AFTER_ACCEL_START_CALC;
    }

    public void setAFTER_ACCEL_START_CALC(boolean AFTER_ACCEL_START_CALC) {
        this.AFTER_ACCEL_START_CALC = AFTER_ACCEL_START_CALC;
    }
    //jhlee 추가 끝 */

    public CountDownTimer getAFTER_GYRO_START_COUNTDOWN_TIMER() {
        return AFTER_GYRO_START_COUNTDOWN_TIMER;
    }

    public void setAFTER_GYRO_START_COUNTDOWN_TIMER(CountDownTimer AFTER_GYRO_START_COUNTDOWN_TIMER) {
        this.AFTER_GYRO_START_COUNTDOWN_TIMER = AFTER_GYRO_START_COUNTDOWN_TIMER;
    }

    public boolean isAFTER_LOBBY_ELEVATOR_CHECK() {
        return AFTER_LOBBY_ELEVATOR_CHECK;
    }

    public void setAFTER_LOBBY_ELEVATOR_CHECK(boolean AFTER_LOBBY_ELEVATOR_CHECK) {
        this.AFTER_LOBBY_ELEVATOR_CHECK = AFTER_LOBBY_ELEVATOR_CHECK;
    }

    public CountDownTimer getAFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER() {
        return AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER;
    }

    public void setAFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER(CountDownTimer AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER) {
        this.AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER = AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER;
    }

    public TimerTask getNotStartBeaconStartTimerTask() {
        return mNotStartBeaconStartTimerTask;
    }

    public void setNotStartBeaconStartTimerTask(TimerTask notStartBeaconStartTimerTask) {
        mNotStartBeaconStartTimerTask = notStartBeaconStartTimerTask;
    }

    public Timer getNotStartBeaconStartTimer() {
        return mNotStartBeaconStartTimer;
    }

    public void setNotStartBeaconStartTimer(Timer notStartBeaconStartTimer) {
        mNotStartBeaconStartTimer = notStartBeaconStartTimer;
    }

    public TimerTask getCollect_START_CALC_TIMER_TASK() {
        return Collect_START_CALC_TIMER_TASK;
    }

    public void setCollect_START_CALC_TIMER_TASK(TimerTask collect_START_CALC_TIMER_TASK) {
        Collect_START_CALC_TIMER_TASK = collect_START_CALC_TIMER_TASK;
    }

    public Timer getCollect_START_CALC_TIMER() {
        return Collect_START_CALC_TIMER;
    }

    public void setCollect_START_CALC_TIMER(Timer collect_START_CALC_TIMER) {
        Collect_START_CALC_TIMER = collect_START_CALC_TIMER;
    }

    /**
     * 로비 시작 Timer
     * 엘리베이터 비컨 -> 로비 비컨 을 받은 후 자이로가 들어오기 전까지 대기하도록 하는 Timer
     * 15분간 동작하며 해당 시간안에 자이로가 발생 안할 시에 종료 시켜버린다.
     **/
    public void StartLobbyTimer() {
        int Delay = 3 * 1000;

        if (mLobbyTimer != null)
            mLobbyTimer = null;

        mLobbyTimerStart = true;

        mLobbyTimer = new CountDownTimer(Delay, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mLobbyTimerStart = false;
            }
        }.start();
    }

    /**
     * 전체 Timer
     * 주차 시스템 측정 시작
     * 해당 타이머가 돌경우에 정보 수집을 한다.
     * 15분간 동작하며 타이머가 끝날시 서버로 데이터를 보낸다.
     **/
    public void StartWholeTimer(final Context context) {
        //jhlee 0805 재시작을 막기위해 true처리
        DataManagerSingleton.getInstance().setWorkingApiCheck(true);
        //jhlee 0805 재시작을 막기위해 true처리

        Timber.d("START WHOLE TIMER");

        AfterStartTIMER();

        //jhlee 0305 시작메시지 수정 및 시작시간 추가
        if (!COLLECT_START_BEACON_CALC) {
            RestController.getInstance().GyroInformation(context, "15분 시작타이머 가동");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDate = simpleDateFormat.format(Calendar.getInstance().getTime());
            DataManagerSingleton.getInstance().setInputTime(currentDate);
            START_CALC_TIMER(context);
        }
        //jhlee 0305 시작메시지 수정 및 시작시간 추가끝
        if (mNotStartBeaconStart) {
            mNotStartBeaconStartTimerTask.cancel();
            mDataManagerSingleton.getNO_START_BEACON().clear();
        }

        int Delay = 900 * 1000;
        if (mWholeTimer != null) mWholeTimer = null;

        mWholeTimerStart = true;

        mWholeTimer = new CountDownTimer(Delay, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int Time = mDataManagerSingleton.getWholeTimerDelay();
                Time++;
            Log.d("TEST", "WHOLE TIMER TICK: " + Time + " seconds");
                mDataManagerSingleton.setWholeTimerDelay(Time);
            }

            @Override
            public void onFinish() {
                try {
                    if (COLLECT_START_BEACON_CALC) {
                        RestController.getInstance().GyroInformation(context, "start whole timer finish");
                        Collect_START_CALC_TIMER_TASK.cancel();
                        Collect_START_CALC_TIMER.cancel();
                        mDataManagerSingleton.setCollectStartCalcBeacon(0);
                        TimerSingleton.getInstance().setCOLLECT_START_BEACON_CALC(false);
                    }else{
                        RestController.getInstance().GyroInformation(context, "start whole timer finish");
                    }

                    mWholeTimerStart = false;
                    COLLECT_START_BEACON_CALC = false;

                    if (mAccelTimerStart) {
                        if (mAccelTimer != null) {
                            try {
                                mAccelTimer.onFinish();
                                mAccelTimer.cancel();
                            } catch (RuntimeException e) {
                                Timber.d("RunTimeException - ACCEL TIMER FINISH: %s", e.getMessage());
                            }
                        }
                    }

                    //jhlee 0106 자이로 타이머를 주석했기 때문에 해당 코드는 의미없음
//                    if (mGyroTimerStart) {
//                        if (mGyroTimer != null) {
//                            try {
//                                mGyroTimer.onFinish();
//                                mGyroTimer.cancel();
//                            } catch (RuntimeException e) {
//                                Timber.d("RunTimeException - GYRO TIMER FINISH : %s", e.getMessage());
//                            }
//                        }
//                    }
                    String ParingState = mDataManagerSingleton.getParingStateValue();
                    if (mDataManagerSingleton.isABNORMAL_END()) {
                        ParingState += "-end";
                    }

                    if (mDataManagerSingleton.isLobbyBeaconEnd()) {
                        ParingState += "-lobby";
                    }

                    if (mDataManagerSingleton.isOutParking() && !mDataManagerSingleton.isABNORMAL_END()) {
                        ParingState += "-out";
                        Timber.d("isOutParking && !isABNORMAL_END");
                    }

                    Timber.d("Test After Start - COUNT: " + mDataManagerSingleton.getAfterStartCount() + ", PARING STATE: " + ParingState);

                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                    saveArrayListValue.SaveAccelBeacon();

                    // jhlee 0806 api전송시 네트워크 검사는 api전송단에서 처리하고 여긴 바로 보내도록 수정
                    final Total total = new Total();
                    total.setPhoneInfo("");
                    total.setBeaconList(mDataManagerSingleton.getBeaconArrayList());
                    total.setGyroList(mDataManagerSingleton.getGyroSensorArrayList());
                    total.setGyroList2(mDataManagerSingleton.getGyroSensorArrayList2());
                    total.setSensorList(mDataManagerSingleton.getAccelSensorArrayList());
                    // 0421 jhlee sensor2추가
                    total.setSensorList2(mDataManagerSingleton.getAccelSensorArrayList2());
                    // 0421 jhlee sensor2추가끝
                    total.setAccelBeaconList(mDataManagerSingleton.getAccelBeaconArrayList());
                    total.setInputDate(mDataManagerSingleton.getInputTime());
                    //0422 jhlee paringstate ANDROID 고정
                    total.setParingState(ParingState);
                    //0422 jhlee paringstate ANDROID 고정끝
                    // mDataManagerSingleton.getTotalArrayList().add(total);
                    Log.d("TEST", "WHOLE TIMER FINISH - Total: " + total);
                    mDataManagerSingleton.setSAVE_DELAY(mDataManagerSingleton.getWholeTimerDelay());
//                    ParkingServiceApi.getInstance().ParkingComplete(context, total);
                    RestController.getInstance().Parking(context, total);
                    // ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    // NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

                    // boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();


                    // if (isConnected) {
                    //     // Send Parking Complete message to server
                    //     ParkingServiceApi.getInstance().ParkingComplete(context, total);

                    //     mDataManagerSingleton.setParingStateValue("non-paring");
                    // } else {
                    //     //0715 jhlee 별도로 타이머를 구동하도록 진행
                    //     //해당 타이머가 도는동안 서비스가 재시작하면 안됨.
                    //     new Handler(Looper.getMainLooper()).post(() -> {
                    //         parkingApiTimer(context, total); // 여기 안에서 start()가 있어야 함
                    //     });
                    // }
                    //jhlee 0806 api전송시 네트워크 검사는 api전송단에서 처리하고 여긴 바로 보내도록 수정끝


                    mDataManagerSingleton.setWholeTimerDelay(0);
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        }.start();
    }

    public void AccelTimer() {
        // 1. 이미 타이머가 실행 중이라면 중복 실행 방지
        if (mAccelTimerStart && mAccelTimer != null) {
            return;
        }

        int Delay = 1000;
        mAccelTimerStart = true;

        mAccelTimer = new CountDownTimer(Delay, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 주기적인 작업이 필요 없다면 비워둠
            }

            @Override
            public void onFinish() {
                try {
                    // 핵심 조건: WholeTimer가 실행 중일 때만 로직 수행 및 반복
                    if (TimerSingleton.getInstance().isWholeTimerStart()) {

                        SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                        String accelResult = saveArrayListValue.AccelSensorResult();
                        int accelResult2 = DataManagerSingleton.getInstance().getAccelCount();

                        // [상태 비교 및 로직 처리 부분 - 기존과 동일]
                        handlePreState(accelResult);

                        // 1. 데이터 적재 (현재 시퀀스 번호 사용)
                        int currentSeq = mDataManagerSingleton.getAccelSequence();

                        AccelSensor accelSensor = new AccelSensor();
                        accelSensor.setState(accelResult);
                        accelSensor.setSeq(String.valueOf(currentSeq));
                        accelSensor.setDelay(String.valueOf(mDataManagerSingleton.getWholeTimerDelay()));

                        AccelSensor2 accelSensor2 = new AccelSensor2();
                        accelSensor2.setState(String.valueOf(accelResult2));
                        accelSensor2.setSeq(String.valueOf(currentSeq));
                        accelSensor2.setDelay(String.valueOf(mDataManagerSingleton.getWholeTimerDelay()));

                        mDataManagerSingleton.getAccelSensorArrayList().add(accelSensor);
                        mDataManagerSingleton.getAccelSensorArrayList2().add(accelSensor2);

                        // 2. 다음 바퀴를 위해 상태 업데이트
                        mDataManagerSingleton.setAccelSequence(currentSeq + 1);
                        mDataManagerSingleton.setAccelCount(0);

                        // 3. ⭐ 재귀 호출: 1초 뒤에 다시 실행되도록 함
                        mAccelTimerStart = false; // 상태 해제 후 재시작
                        AccelTimer();
                    } else {
                        // WholeTimer가 꺼져있으면 반복 중단
                        mAccelTimerStart = false;
                    }
                } catch (Exception ex) {
                    mAccelTimerStart = false;
                }
            }
        }.start();
    }

    // 가독성을 위해 상태 처리 로직 분리
    private void handlePreState(String accelResult) {
        String preState = mDataManagerSingleton.getPreState();
        if (preState == null) {
            mDataManagerSingleton.setPreState(accelResult);
        } else {
            if (preState.equals("T") && (accelResult.equals("S") || accelResult.equals("W"))) {
                if (!mCollectAccelBeaconStart) {
                    StartCollectAccelBeacon();
                }
            }
            mDataManagerSingleton.setPreState(accelResult);
        }
    }

    //jhlee 0105 해당 코드 주석
//    public void GyroTimer() {
//        int delay = 1000;   // 1 Second
//
//        if (mGyroTimer != null) mGyroTimer = null;
//
//        mGyroTimerStart = true;
//
//        mGyroTimer = new CountDownTimer(delay, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//            }
//
//            @Override
//            public void onFinish() {
//                mGyroTimerStart = false;
//            }
//        }.start();
//    }
    //jhlee 0105 해당 코드 주석끝

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

    public void StartCollectLobby(final Context context) {
        mCollectLobbyStart = true;

        mCollectLobbyTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    mCollectLobbyStart = false;

                    if (!mNotRestart) {
                        if (mDataManagerSingleton.getINOUT_DATA_MAJOR().isEmpty()) {
                            Timber.d("END VALUE : %s", mDataManagerSingleton.getLAST_BEACON());
                            if (mDataManagerSingleton.getLAST_BEACON() == 3) {
                                if (isWholeTimerStart()) {
                                    try {
                                        mWholeTimer.onFinish();
                                        mWholeTimer.cancel();
                                    } catch (RuntimeException e) {
                                        Timber.e("RuntimeException - Whole Timer Finish: %s", e.getMessage());
                                    }
                                }
                            } else {
                                mDataManagerSingleton.getINOUT_DATA_MAJOR().clear();
                                StartCollectLobby(context);
                            }
                        } else {
                            int Last = mDataManagerSingleton.getINOUT_DATA_MAJOR().get(mDataManagerSingleton.getINOUT_DATA_MAJOR().size() - 1);

                            mDataManagerSingleton.setLAST_BEACON(Last);
                            mDataManagerSingleton.getINOUT_DATA_MAJOR().clear();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // only for gingerbread and newer versions
                                StartCollectLobby(context);
                            } else {
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> StartCollectLobby(context));
                            }
                        }
                    } else {
                        mDataManagerSingleton.getINOUT_DATA_MAJOR().clear();
                    }
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        }.start();
    }

    public void StartStayRestart() {
        mStayRestartStart = true;

        mStayRestartStartTimer = new CountDownTimer(900 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    mStayRestartStart = false;

                    mDataManagerSingleton.setRESTART_BEACON(false);
                    mDataManagerSingleton.setEnd1Beacon(false);
                    mDataManagerSingleton.setEnd2Beacon(false);
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        }.start();
    }

    public void NotStartBeacon() {
        mNotStartBeaconStart = true;

        mNotStartBeaconStartTimerTask = new TimerTask() {
            @Override
            public void run() {
                Timber.d("TAG_TEST_NOT_START - VALUE BEACON : %s", mDataManagerSingleton.getNO_START_BEACON().size());
                mDataManagerSingleton.getNO_START_BEACON().clear();
            }
        };

        mNotStartBeaconStartTimer = new Timer();
        mNotStartBeaconStartTimer.schedule(mNotStartBeaconStartTimerTask, 10000, 10000);
    }

    public void START_CALC_TIMER(final Context context) {
        if (COLLECT_START_BEACON_CALC) {
            return;
        }
        COLLECT_START_BEACON_CALC = true;

        Collect_START_CALC_TIMER_TASK = new TimerTask() {
            @Override
            public void run() {
                Timber.e("TEST_TIMER_SINGLETON - 30초");
                if (mDataManagerSingleton.getCollectStartCalcBeacon() != 0) {
                    Timber.d("TEST_TIMER_SINGLETON_VAL - ACCEL_BEACON 0개 아님 종료 처리 X : %s", mDataManagerSingleton.getCollectStartCalcBeacon());
                    mDataManagerSingleton.setCollectStartCalcBeacon(0);
                } else {
                    //jhlee 0401 안드로이드에서 accel start 와 queue가 너무 빈번하게 일어나는 케이스로 비콘이 없어서 발생된 현상 맞는지 체크용 gyroinfo 추가
                    RestController.getInstance().GyroInformation(context, "30초 검사시 비콘 없음 확인");
                    //jhlee 0401 안드로이드에서 accel start 와 queue가 너무 빈번하게 일어나는 케이스로 비콘이 없어서 발생된 현상 맞는지 체크용 gyroinfo 추가 수정끝

                    // 1번 로비비콘을 통해서 끝날 시 mOutParking = true가 되는데 이곳은 비컨이 30초 동안 들어오지 않은 상태이므로 false로 변경
                    mDataManagerSingleton.setOutParking(false);
                    // 전체 타이머가 돌고있을 경우 전체 타이머 종료
                    if (isWholeTimerStart()) {
                        try {
                            RestController.getInstance().GyroInformation(context, "30초 검사가 끝나서 없는경우 홀타이머 종료직전 시점");
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
                    mDataManagerSingleton.setCollectStartCalcBeacon(0);
                }
            }
        };

        try {
            Collect_START_CALC_TIMER = new Timer();
            Collect_START_CALC_TIMER.schedule(Collect_START_CALC_TIMER_TASK, 30000, 30000);
        } catch (IllegalStateException ex) {
            RestController.getInstance().GyroInformation(context, Integer.toString(3031));
        }
    }

    public void AfterStartTIMER() {
        if (!mAfterStart) {
            mAfterStart = true;
            mAfterStartCountDownTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    mAfterStart = false;

                    try {
                        mDataManagerSingleton.setParingStateValue("ANDROID");
                    } catch (Exception ex) {
                        // ignore exception
                    }
                }
            }.start();
        }
    }

    public void AFTER_LOBBY_ELEVATOR_TIMER() {
        AFTER_LOBBY_ELEVATOR_CHECK = true;
        AFTER_LOBBY_ELEVATOR_CHECK_COUNTDOWN_TIMER = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    if (mDataManagerSingleton.getAfterLobbyEleCount() == 0) {
                        if (mWholeTimerStart && !mDataManagerSingleton.isElevatorBeaconGet()) {
                            Timber.d("TAG_BEACON_FUNCTION - END LOBBY");
                            mDataManagerSingleton.setLobbyBeaconEnd(true);
                            try {
                                mWholeTimer.onFinish();
                                mWholeTimer.cancel();
                            } catch (RuntimeException e) {
                                Timber.e("RuntimeException - Whole Timer Finish: %s", e.getMessage());
                            }
                            AFTER_LOBBY_ELEVATOR_CHECK = false;
                        } else {
                            AFTER_LOBBY_ELEVATOR_CHECK = false;
                            Timber.d("TAG_BEACON_FUNCTION - END LOBBY NO MATCH");
                        }
                    } else {
                        AFTER_LOBBY_ELEVATOR_CHECK = false;
                        AFTER_LOBBY_ELEVATOR_TIMER();
                        Timber.d("TAG_BEACON_FUNCTION - WHOLE_TIMER: " + mWholeTimerStart + " , " + mDataManagerSingleton.isElevatorBeaconGet() + " , COUNT : " + mDataManagerSingleton.getAfterLobbyEleCount());
                        mDataManagerSingleton.setAfterLobbyEleCount(0);
                    }
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        }.start();
    }

    private static class TimerSingletonHolder {
        private static final TimerSingleton instance = new TimerSingleton();
    }
}
