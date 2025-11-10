package com.pms_parkin_mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pms_parkin_mobile.dto.User;

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
    private String phone;
    private String userName;
    private int dong;
    private int ho;
    private int buildingId;
    private double latitude;
    private double longitude;
    private boolean sensorStatus;
    private boolean testFlag;
    private boolean serviceStatus;
    private int beaconMajor;
    private int beaconMinor;
    // ======== private 생성자 ========
    private App(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadUserInfo(); // 초기화 시 기존 저장된 값 불러오기
    }

    // ======== 전역 싱글톤 접근자 (context 불필요) ========
    public static synchronized App getInstance() {
        if (instance == null) {
            // MainApplication이 초기화되어 있어야 함
            Context context = MainApplication.getAppContext();
            instance = new App(context);
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
        editor.putInt("dong", user.getDong());
        editor.putInt("ho", user.getHo());
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
        dong = sharedPreferences.getInt("dong", 0);
        ho = sharedPreferences.getInt("ho", 0);
        buildingId = sharedPreferences.getInt("building_id", 0);
        latitude = sharedPreferences.getFloat("latitude", 0f);
        longitude = sharedPreferences.getFloat("longitude", 0f);
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
        dong = 0;
        ho = 0;
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
    public int getDong() { return dong; }
    public int getHo() { return ho; }
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

    public void setServiceStatus(boolean serviceStatus) {
        sharedPreferences.edit().putBoolean("service_flag", serviceStatus).apply();
        this.serviceStatus = serviceStatus;
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
}