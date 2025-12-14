package com.pms_parkin_mobile.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.util.Log;

import com.pms_parkin_mobile.dto.AccelBeacon;
import com.pms_parkin_mobile.dto.AccelSensor;
import com.pms_parkin_mobile.dto.AccelSensor2;
import com.pms_parkin_mobile.dto.Beacon;
import com.pms_parkin_mobile.dto.GyroSensor;
import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.dto.User;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class App {
    // ======== 싱글톤 인스턴스 ========
    private static App instance;

    // ======== SharedPreferences ========
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_pref";

    // ======== 사용자 필드 ========
    private int id;
    private String userId;
    private String userPwd;
    private Context appContext;
    private String phone;
    private String userName;
    private String dong;
    private String ho;
    private int buildingId;
    private double latitude;
    private double longitude;
    private boolean sensorStatus;
    private boolean testFlag;
    private boolean serviceStatus;
    private int beaconMajor;
    private String mPreState = null;
    private int beaconMinor;
    private boolean mCollectAccelBeaconStart = false;
    private int mSaveCountRoll = 0;
    private int mSaveCountPitch = 0;
    private boolean TestCompleteFlag = false;
    private int SAVE_DELAY = 0;
    private boolean StayTestResult = false;
    private boolean LeftTestResult = false;
    private boolean RightTestResult = false;
    private String TestSensorMode = "";

    public int getSAVE_DELAY() {
        return SAVE_DELAY;
    }

    public void setSAVE_DELAY(int SAVE_DELAY) {
        this.SAVE_DELAY = SAVE_DELAY;
    }

    public ArrayList<GyroSensor> getmGyroSensorArrayList() {
        return mGyroSensorArrayList;
    }

    public void setmGyroSensorArrayList(ArrayList<GyroSensor> mGyroSensorArrayList) {
        this.mGyroSensorArrayList = mGyroSensorArrayList;
    }

    public boolean isTestCompleteFlag() {
        TestCompleteFlag = sharedPreferences.getBoolean("test_complete_flag", false);
        return TestCompleteFlag;
    }

    public void setTestCompleteFlag(boolean TestCompleteFlag) {
        sharedPreferences.edit().putBoolean("test_complete_flag", TestCompleteFlag).apply();
        this.TestCompleteFlag = TestCompleteFlag;
    }

    public boolean ismCollectAccelBeaconStart() {
        return mCollectAccelBeaconStart;
    }

    public void setmCollectAccelBeaconStart(boolean mCollectAccelBeaconStart) {
        this.mCollectAccelBeaconStart = mCollectAccelBeaconStart;
    }

    private int mSaveCountYaw = 0;
    private boolean serviceFlag = false;
    private boolean startFlag = false;
    private String time;
    private ArrayList<Beacon> mBeaconArrayList;
    private ArrayList<GyroSensor> mGyroSensorArrayList;
    private ArrayList<AccelSensor> mAccelSensorArrayList;
    private int mAfterAccelCount = 0;
    private int mAccelCount = 0;
    private int mAfterStartCount = 0;
    private int mAfterGyroCount = 0;
    private Total CAN_NOT_SEND_TOTAL_SAVE;
    private int mCollectStartCalcBeacon = 0;
    private LinkedHashSet<String> saveDelayList = null;
    private ArrayList<Total> mTotalArrayList;
    private Total pendingTotal;
    private boolean passiveCheck;

    public LinkedHashSet<String> SetDelayList(String value){
        saveDelayList.add(value);
        return saveDelayList;
    }
    public int getmCollectStartCalcBeacon() {
        return mCollectStartCalcBeacon;
    }

    public void setmCollectStartCalcBeacon(int mCollectStartCalcBeacon) {
        this.mCollectStartCalcBeacon = mCollectStartCalcBeacon;
    }

    public boolean ismAfterStart() {
        return mAfterStart;
    }

    public void setmAfterStart(boolean mAfterStart) {
        this.mAfterStart = mAfterStart;
    }

    public int getmAfterGyroCount() {
        return mAfterGyroCount;
    }

    public void setmAfterGyroCount(int mAfterGyroCount) {
        this.mAfterGyroCount = mAfterGyroCount;
    }

    public int getmAfterStartCount() {
        return mAfterStartCount;
    }

    public void setmAfterStartCount(int mAfterStartCount) {
        this.mAfterStartCount = mAfterStartCount;
    }

    public int getmAfterAccelCount() {
        return mAfterAccelCount;
    }

    public void setmAfterAccelCount(int mAfterAccelCount) {
        this.mAfterAccelCount = mAfterAccelCount;
    }

    public String getmPreState() {
        return mPreState;
    }

    public void setmPreState(String mPreState) {
        this.mPreState = mPreState;
    }

    private int mAccelSequence = 0;
    private String mParingStateValue = "non-paring";

    public boolean isStartFlag() {
        startFlag = sharedPreferences.getBoolean("start_flag", false);
        return startFlag;
    }

    public void setStartFlag(boolean startFlag) {
        sharedPreferences.edit().putBoolean("start_flag", startFlag).apply();
        this.startFlag = startFlag;
    }

    public String getmParingStateValue() {
        return mParingStateValue;
    }

    public void setmParingStateValue(String mParingStateValue) {
        this.mParingStateValue = mParingStateValue;
    }

    // jhlee 0421 AccelSensor2클래스 추가
    private ArrayList<AccelSensor2> mAccelSensorArrayList2;

    public ArrayList<AccelSensor> getmAccelSensorArrayList() {
        return mAccelSensorArrayList;
    }

    public void setmAccelSensorArrayList(ArrayList<AccelSensor> mAccelSensorArrayList) {
        this.mAccelSensorArrayList = mAccelSensorArrayList;
    }

    public ArrayList<AccelSensor2> getmAccelSensorArrayList2() {
        return mAccelSensorArrayList2;
    }

    public void setmAccelSensorArrayList2(ArrayList<AccelSensor2> mAccelSensorArrayList2) {
        this.mAccelSensorArrayList2 = mAccelSensorArrayList2;
    }

    public int getmAccelSequence() {
        return mAccelSequence;
    }

    public void setmAccelSequence(int mAccelSequence) {
        this.mAccelSequence = mAccelSequence;
    }

    public int getmAccelCount() {
        return mAccelCount;
    }

    public void setmAccelCount(int mAccelCount) {
        this.mAccelCount = mAccelCount;
    }

    public ArrayList<Total> getmTotalArrayList() {
        return mTotalArrayList;
    }

    public void setmTotalArrayList(ArrayList<Total> mTotalArrayList) {
        this.mTotalArrayList = mTotalArrayList;
    }



    public boolean isStayTestResult() {
        return StayTestResult;
    }

    public void setStayTestResult(boolean stayTestResult) {
        StayTestResult = stayTestResult;
    }

    private CountDownTimer mAccelTimer;
    private Queue<Double> ROLL_QUEUE;
    private Queue<Double> PITCH_QUEUE;
    private Queue<Double> YAW_QUEUE;
    private Map<String, AccelBeacon> mAccelBeaconMap;
    private ArrayList<Integer> mAccelBeaconDelayArray;
    private Map<String, LinkedHashSet<String>> mAccelBeaconDelayMap;
    private CountDownTimer mAfterStartCountDownTimer;
    private boolean mAfterStart = false;
    private int mBeaconSequence = 0;

    public ArrayList<Beacon> getmBeaconArrayList() {
        return mBeaconArrayList;
    }

    public void setmBeaconArrayList(ArrayList<Beacon> mBeaconArrayList) {
        this.mBeaconArrayList = mBeaconArrayList;
    }

    public ArrayList<Integer> getmAccelBeaconDelayArray() {
        return mAccelBeaconDelayArray;
    }

    public void setmAccelBeaconDelayArray(ArrayList<Integer> mAccelBeaconDelayArray) {
        this.mAccelBeaconDelayArray = mAccelBeaconDelayArray;
    }

    public int getmBeaconSequence() {
        return mBeaconSequence;
    }

    public void setmBeaconSequence(int mBeaconSequence) {
        this.mBeaconSequence = mBeaconSequence;
    }

    private int mWholeTimerDelay = 0;

    public int getmWholeTimerDelay() {
        return mWholeTimerDelay;
    }

    public void setmWholeTimerDelay(int mWholeTimerDelay) {
        this.mWholeTimerDelay = mWholeTimerDelay;
    }

    private ArrayList<AccelBeacon> mAccelBeaconArrayList;

    public ArrayList<AccelBeacon> getmAccelBeaconArrayList() {
        return mAccelBeaconArrayList;
    }

    public void setmAccelBeaconArrayList(ArrayList<AccelBeacon> mAccelBeaconArrayList) {
        this.mAccelBeaconArrayList = mAccelBeaconArrayList;
    }

    public Map<String, LinkedHashSet<String>> getmAccelBeaconDelayMap() {
        return mAccelBeaconDelayMap;
    }

    public void setmAccelBeaconDelayMap(Map<String, LinkedHashSet<String>> mAccelBeaconDelayMap) {
        this.mAccelBeaconDelayMap = mAccelBeaconDelayMap;
    }

    public boolean isLeftTestResult() {
        return LeftTestResult;
    }

    public void setLeftTestResult(boolean leftTestResult) {
        LeftTestResult = leftTestResult;
    }

    public boolean isRightTestResult() {
        return RightTestResult;
    }

    public void setRightTestResult(boolean rightTestResult) {
        RightTestResult = rightTestResult;
    }

    // ======== private 생성자 ========
    public void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.appContext = context.getApplicationContext();
        loadUserInfo(); // 초기화 시 기존 저장된 값 불러오기
    }

    public Context getContext() {
        return appContext;
    }

    private App(){
        mBeaconArrayList = new ArrayList<>();
        mGyroSensorArrayList = new ArrayList<>();
        mAccelSensorArrayList = new ArrayList<>();
        // jhlee 0421 mAccelSensorArrayList2 초기화 추가
        mAccelSensorArrayList2 = new ArrayList<>();
        // jhlee 0421 mAccelSensorArrayList2 초기화 추가끝
        mTotalArrayList = new ArrayList<>();
        mAccelBeaconArrayList = new ArrayList<>();
        mAccelBeaconDelayArray = new ArrayList<>();

        pendingTotal = null;

        mAccelBeaconMap = new HashMap<>();
        mAccelBeaconDelayMap = new HashMap<>();

        CAN_NOT_SEND_TOTAL_SAVE = new Total();
        ROLL_QUEUE = new LinkedList<>();
        PITCH_QUEUE = new LinkedList<>();
        YAW_QUEUE = new LinkedList<>();

        saveDelayList = new LinkedHashSet<String>();
        startFlag = false;
        ROLL_QUEUE.clear();
        PITCH_QUEUE.clear();
        YAW_QUEUE.clear();

    }

    // ======== 전역 싱글톤 접근자 (context 불필요) ========
    public static App getInstance() {
        if (instance == null) {
            instance = new App(); // <-- 이 시점에 내부 ArrayList들이 모두 new 로 초기화됨
        }
        return instance;
    }


    // ======== SharedPreference 저장 ========
    public void setUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", user.getUserId());
        editor.putString("user_name", user.getUserName());
        editor.putString("user_phone", user.getPhone());
        editor.putInt("id", user.getId());
        editor.putString("user_pwd", user.getUserPwd());
        editor.putString("dong", user.getDong());
        editor.putString("ho", user.getHo());
        editor.putInt("building_id", user.getBuildingId());
        editor.putFloat("latitude", (float) user.getLatitude());
        editor.putFloat("longitude", (float) user.getLongitude());
        editor.apply();

        // 메모리 반영
        this.id = user.getId();
        this.userId = user.getUserId();
        this.userPwd = user.getUserPwd();
        this.phone = user.getPhone();
        this.userName = user.getUserName();
        this.dong = user.getDong();
        this.ho = user.getHo();
        this.buildingId = user.getBuildingId();
        this.latitude = user.getLatitude();
        this.longitude = user.getLongitude();
    }

    // ======== SharedPreference에서 로드 ========
    private void loadUserInfo() {
        id = sharedPreferences.getInt("id", -1);
        userId = sharedPreferences.getString("user_id", null);
        userPwd = sharedPreferences.getString("user_pwd", null);
        phone = sharedPreferences.getString("user_phone", null);
        userName = sharedPreferences.getString("user_name", null);
        dong = sharedPreferences.getString("dong", null);
        ho = sharedPreferences.getString("ho", null);
        buildingId = sharedPreferences.getInt("building_id", 0);
        latitude = sharedPreferences.getFloat("latitude", 0f);
        longitude = sharedPreferences.getFloat("longitude", 0f);
        startFlag = sharedPreferences.getBoolean("start_flag", false);
        serviceFlag = sharedPreferences.getBoolean("service_flag", false);
    }

    // ======== 로그아웃 or 초기화 ========
    public void clearUserInfo() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        id = 0;
        userId = null;
        userPwd = null;
        phone = null;
        userName = null;
        dong =  null;
        ho =  null;
        buildingId = 0;
        latitude = 0;
        longitude = 0;
    }

    // ======== Getter ========
    public int getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserPwd() { return userPwd; }
    public String getPhone() { return phone; }
    public String getUserName() { return userName; }
    public String getDong() { return dong; }
    public String getHo() { return ho; }
    public int getBuildingId() { return buildingId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public boolean isSensorStatus() {
        sensorStatus = sharedPreferences.getBoolean("sensor_status", false);
        return sensorStatus;
    }

    public boolean isTestFlag() {
        testFlag = sharedPreferences.getBoolean("test_flag", false);
        return testFlag;
    }

    public void setTestFlag(boolean testFlag) {
        sharedPreferences.edit().putBoolean("test_flag", testFlag).apply();
        this.testFlag = testFlag;
    }

    public boolean isServiceStatus() {
        serviceStatus = sharedPreferences.getBoolean("service_flag", false);
        return serviceStatus;
    }

    public void setSensorStatus(boolean sensorStatus) {
        sharedPreferences.edit().putBoolean("sensor_status", sensorStatus).apply();
        this.sensorStatus = sensorStatus;
    }

    // ======== Setter (개별 변경 필요 시) ========
    public void setUserName(String name) {
        this.userName = name;
        sharedPreferences.edit().putString("user_name", name).apply();
    }

    public int getBeaconMajor() {
        beaconMajor = sharedPreferences.getInt("beacon_major", 0);
        return beaconMajor;
    }

    public void setBeaconMajor(int beaconMajor) {
        this.beaconMajor = beaconMajor;
        sharedPreferences.edit().putInt("beacon_major", beaconMajor).apply();
    }

    public int getBeaconMinor() {
        beaconMajor = sharedPreferences.getInt("beacon_minor", 0);
        return beaconMajor;
    }

    public void setBeaconMinor(int beaconMinor) {
        this.beaconMinor = beaconMinor;
        sharedPreferences.edit().putInt("beacon_minor", beaconMinor).apply();
    }

    public boolean isServiceFlag() {
        this.serviceFlag = sharedPreferences.getBoolean("service_flag", false);
        return serviceFlag;
    }

    public void setServiceFlag(boolean serviceFlag) {
        Log.d("SERVICE_FLAG", "App setServiceFlag : " + serviceFlag);
        this.serviceFlag = serviceFlag;
        sharedPreferences.edit().putBoolean("service_flag", serviceFlag).apply();
    }

    public String getTime() {
        time = sharedPreferences.getString("time", "0000-00-00 00:00");
        return time;
    }

    public void setTime(String time) {
        this.time = time;
        sharedPreferences.edit().putString("time", time).apply();
    }

    public Queue<Double> getROLL_QUEUE() {
        return ROLL_QUEUE;
    }

    public void setROLL_QUEUE(Queue<Double> ROLL_QUEUE) {
        this.ROLL_QUEUE = ROLL_QUEUE;
    }

    public Queue<Double> getPITCH_QUEUE() {
        return PITCH_QUEUE;
    }

    public void setPITCH_QUEUE(Queue<Double> PITCH_QUEUE) {
        this.PITCH_QUEUE = PITCH_QUEUE;
    }

    public Queue<Double> getYAW_QUEUE() {
        return YAW_QUEUE;
    }

    public void setYAW_QUEUE(Queue<Double> YAW_QUEUE) {
        this.YAW_QUEUE = YAW_QUEUE;
    }

    public int getmSaveCountRoll() {
        return mSaveCountRoll;
    }

    public void setmSaveCountRoll(int mSaveCountRoll) {
        this.mSaveCountRoll = mSaveCountRoll;
    }

    public int getmSaveCountPitch() {
        return mSaveCountPitch;
    }

    public void setmSaveCountPitch(int mSaveCountPitch) {
        this.mSaveCountPitch = mSaveCountPitch;
    }

    public int getmSaveCountYaw() {
        return mSaveCountYaw;
    }

    public void setmSaveCountYaw(int mSaveCountYaw) {
        this.mSaveCountYaw = mSaveCountYaw;
    }

    public Map<String, AccelBeacon> getmAccelBeaconMap() {
        return mAccelBeaconMap;
    }

    public void setmAccelBeaconMap(Map<String, AccelBeacon> mAccelBeaconMap) {
        this.mAccelBeaconMap = mAccelBeaconMap;
    }

    public boolean isPassiveCheck() {
        this.passiveCheck = sharedPreferences.getBoolean("passive_check", false);
        return passiveCheck;
    }

    public void setPassiveCheck(boolean passiveCheck) {
        this.passiveCheck = passiveCheck;
        sharedPreferences.edit().putBoolean("passive_check", passiveCheck).apply();
    }

    public String getSensorTestMode() {
        this.TestSensorMode = sharedPreferences.getString("sensor_test_mode", "테스트");
        return TestSensorMode;
    }


    public void setSensorTestMode(String TestSensorMode) {
        this.TestSensorMode = TestSensorMode;
        sharedPreferences.edit().putString("sensor_test_mode", TestSensorMode).apply();
    }
    //서비스가 시작했다면 타이머 시작
    public void resetDelayList() {
        if (saveDelayList == null) {
            saveDelayList = new LinkedHashSet<>();
        } else {
            saveDelayList.clear();
        }
    }




}