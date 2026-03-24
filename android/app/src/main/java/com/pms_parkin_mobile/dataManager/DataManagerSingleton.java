package com.pms_parkin_mobile.dataManager;

import com.pms_parkin_mobile.dto.AccelBeacon;
import com.pms_parkin_mobile.dto.AccelSensor;
import com.pms_parkin_mobile.dto.AccelSensor2;
import com.pms_parkin_mobile.dto.Beacon;
import com.pms_parkin_mobile.dto.GyroSensor;
import com.pms_parkin_mobile.dto.GyroSensor2;
import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.service.TimerSingleton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import timber.log.Timber;

public class DataManagerSingleton {
    private final Queue<Double> ROLL_QUEUE;
    private final Queue<Double> PITCH_QUEUE;
    private final Queue<Double> YAW_QUEUE;

    private String mInputTime = "";
    private int mSaveCountRoll = 0, mSaveCountPitch = 0, mSaveCountYaw = 0;
    private String mPreState = null;
    private boolean mStart1Beacon = false;     //  시작
    private boolean mStart2Beacon = false;     //  시작2
    private boolean mEnd1Beacon = false;       //  끝
    private boolean mEnd2Beacon = false;       //  끝2
    private boolean mOutParking = false;       //  타이머 종료시 출차 확인
    private boolean mParingState = false;      //  실시간 ParingStateCheck
    private boolean mParingStateSave = false;  //  한번이라도 Paring 되면 true 상태 유지
    private boolean ABNORMAL_END = false;
    private ArrayList<Beacon> mBeaconArrayList;
    private ArrayList<GyroSensor> mGyroSensorArrayList;
    private ArrayList<GyroSensor2> mGyroSensorArrayList2;
    private ArrayList<AccelSensor> mAccelSensorArrayList;
    // jhlee 0421 AccelSensor2클래스 추가
    private ArrayList<AccelSensor2> mAccelSensorArrayList2;
    // jhlee 0421 AccelSensor2클래스 추가끝
    private ArrayList<Total> mTotalArrayList;
    private ArrayList<AccelBeacon> mAccelBeaconArrayList;
    private ArrayList<Integer> INOUT_DATA_MAJOR;
    private ArrayList<Integer> NO_START_BEACON;       //  시작 비컨을 못받았을 경우 -시작이 가능한지 일반 비컨이 많이 들어오는 경우를 측정
    private int mCollectStartCalcBeacon = 0;           //  시작 이후 비컨을 수집하여 일정시간동안 비컨이 안들어오는 상태일경우 종료 시켜버린다.
    private ArrayList<Integer> mAccelBeaconDelayArray;
    private Map<String, AccelBeacon> mAccelBeaconMap;
    //0110 jhlee Inter에서 String으로 타입변환 수정
    private Map<String, LinkedHashSet<String>> mAccelBeaconDelayMap;
    //0110 jhlee Inter에서 String으로 타입변환 수정끝
    private int mWholeTimerDelay = 0;
    private int mBeaconSequence = 0;
    private int mAccelSequence = 0;
    private int mAccelCount = 0;
    private int SAVE_DELAY = 0;
    /**
     * Timeout 시 시도횟수 Check
     **/
    private int mTimeoutCount = 0;
    private Total CAN_NOT_SEND_TOTAL_SAVE;
    //region Beacon Lobby
    private boolean mLobbyGet = false;
    private boolean mElevatorGet = false;
    private String INOUT_STATE = null;
    private boolean RESTART_BEACON = false;
    private int LAST_BEACON = 0;
    private int PARING_COUNT = 0;
    private int mAfterStartCount = 0;
    private int mAfterGyroCount = 0;
    private int mAfterLobbyEleCount = 0;
    private boolean mLobbyBeaconEnd = false;
    private boolean mElevatorBeaconGet = false;
    private String mParingStateValue = "non-paring";
    // Gyro Send Count
    private int mGyroSendCount = 0;
    // jhlee 추가 시작
    private int mAfterAccelCount = 0;
    private int mAccelSendCount = 0;
    //jhlee 추가 끝 */

    //jhlee 0708 추가시작 네트워크 재전송시 임시 저장값 초기화
    private Total pendingTotal;
    private Boolean workingApiCheck = false;

    //jhlee 0214 delayList 수정
    private LinkedHashSet<String> saveDelayList;

    //변경버전 jhlee
    //해당 flag를 false로 할경우 자이로시점인 경우에서만 수집해서 처리하는 방식을 적용한다 수정중
    // 해당 Flag는 UserDataSingleton에서 관리함.
    // private Boolean bigDataFlag = false;
    //해당 flag를 false로 할경우 자이로시점인 경우에서만 수집해서 처리하는 방식을 적용한다 수정끝

    private DataManagerSingleton() {
        mBeaconArrayList = new ArrayList<>();
        mGyroSensorArrayList = new ArrayList<>();
        // jhlee 0311 mGyroSensorArrayList2 초기화 추가
        mGyroSensorArrayList2 = new ArrayList<>();
        mAccelSensorArrayList = new ArrayList<>();
        // jhlee 0421 mAccelSensorArrayList2 초기화 추가
        mAccelSensorArrayList2 = new ArrayList<>();
        // jhlee 0421 mAccelSensorArrayList2 초기화 추가끝
        mTotalArrayList = new ArrayList<>();
        mAccelBeaconArrayList = new ArrayList<>();
        INOUT_DATA_MAJOR = new ArrayList<>();
        NO_START_BEACON = new ArrayList<>();
        mAccelBeaconDelayArray = new ArrayList<>();

        pendingTotal = null;

        mAccelBeaconMap = new HashMap<>();
        mAccelBeaconDelayMap = new HashMap<>();

        CAN_NOT_SEND_TOTAL_SAVE = new Total();

        ROLL_QUEUE = new LinkedList<>();
        PITCH_QUEUE = new LinkedList<>();
        YAW_QUEUE = new LinkedList<>();

        saveDelayList = new LinkedHashSet<String>();

        ROLL_QUEUE.clear();
        PITCH_QUEUE.clear();
        YAW_QUEUE.clear();
    }

    public static DataManagerSingleton getInstance() {
        return DataManagerSingletonHolder.instance;
    }

    public boolean isStart2Beacon() {
        return mStart2Beacon;
    }

    public void setStart2Beacon(boolean start2Beacon) {
        mStart2Beacon = start2Beacon;
    }

    public boolean isEnd1Beacon() {
        return mEnd1Beacon;
    }

    public void setEnd1Beacon(boolean end1Beacon) {
        mEnd1Beacon = end1Beacon;
    }

    public boolean isEnd2Beacon() {
        return mEnd2Beacon;
    }

    public void setEnd2Beacon(boolean end2Beacon) {
        mEnd2Beacon = end2Beacon;
    }

    public boolean isOutParking() {
        return mOutParking;
    }

    public void setOutParking(boolean outParking) {
        mOutParking = outParking;
    }

    public boolean isParingState() {
        return mParingState;
    }

    public void setParingState(boolean paringState) {
        mParingState = paringState;
    }

    public boolean isParingStateSave() {
        return mParingStateSave;
    }

    public void setParingStateSave(boolean paringStateSave) {
        mParingStateSave = paringStateSave;
    }

    public boolean isABNORMAL_END() {
        return ABNORMAL_END;
    }

    public void setABNORMAL_END(boolean ABNORMAL_END) {
        this.ABNORMAL_END = ABNORMAL_END;
    }

    public String getInputTime() {
        return mInputTime;
    }

    public void setInputTime(String inputTime) {
        this.mInputTime = inputTime;
    }

    public String getPreState() {
        return mPreState;
    }

    public void setPreState(String preState) {
        mPreState = preState;
    }

    public boolean isStart1Beacon() {
        return mStart1Beacon;
    }

    public void setStart1Beacon(boolean start1Beacon) {
        mStart1Beacon = start1Beacon;
    }

    public List<Beacon> getBeaconArrayList() {
        return mBeaconArrayList;
    }

    public void setBeaconArrayList(ArrayList<Beacon> beaconArrayList) {
        this.mBeaconArrayList = beaconArrayList;
    }

    public ArrayList<GyroSensor> getGyroSensorArrayList() {
        return mGyroSensorArrayList;
    }

    public void setGyroSensorArrayList(ArrayList<GyroSensor> gyroSensorArrayList) {
        this.mGyroSensorArrayList = gyroSensorArrayList;
    }

    //jhlee 0311 gyrosensor2 get추가
    public ArrayList<GyroSensor2> getGyroSensorArrayList2() {
        return mGyroSensorArrayList2;
    }

    public ArrayList<AccelSensor> getAccelSensorArrayList() {
        return mAccelSensorArrayList;
    }

    public void setAccelSensorArrayList(ArrayList<AccelSensor> accelSensorArrayList) {
        this.mAccelSensorArrayList = accelSensorArrayList;
    }

    public ArrayList<AccelSensor2> getAccelSensorArrayList2() {
        return mAccelSensorArrayList2;
    }

    // 0421 jhlee AccelSensor2 적재하는 ArrayList추가 추가
    public void setAccelSensorArrayList2(ArrayList<AccelSensor2> accelSensorArrayList2) {
        this.mAccelSensorArrayList2 = accelSensorArrayList2;
    }
    // 0421 jhlee AccelSensor2 적재하는 ArrayList추가 추가끝

    public ArrayList<Total> getTotalArrayList() {
        return mTotalArrayList;
    }

    public void setTotalArrayList(ArrayList<Total> totalArrayList) {
        this.mTotalArrayList = totalArrayList;
    }

    public ArrayList<AccelBeacon> getAccelBeaconArrayList() {
        return mAccelBeaconArrayList;
    }

    public void setAccelBeaconArrayList(ArrayList<AccelBeacon> accelBeaconArrayList) {
        this.mAccelBeaconArrayList = accelBeaconArrayList;
    }

    public ArrayList<Integer> getINOUT_DATA_MAJOR() {
        return INOUT_DATA_MAJOR;
    }

    public void setINOUT_DATA_MAJOR(ArrayList<Integer> INOUT_DATA_MAJOR) {
        this.INOUT_DATA_MAJOR = INOUT_DATA_MAJOR;
    }

    public ArrayList<Integer> getNO_START_BEACON() {
        return NO_START_BEACON;
    }

    public void setNO_START_BEACON(ArrayList<Integer> NO_START_BEACON) {
        this.NO_START_BEACON = NO_START_BEACON;
    }

    public int getCollectStartCalcBeacon() {
        return mCollectStartCalcBeacon;
    }

    public void setCollectStartCalcBeacon(int collectStartCalcBeacon) {
        mCollectStartCalcBeacon = collectStartCalcBeacon;
    }

    public ArrayList<Integer> getAccelBeaconDelayArray() {
        return mAccelBeaconDelayArray;
    }

    public void setAccelBeaconDelayArray(ArrayList<Integer> accelBeaconDelayArray) {
        mAccelBeaconDelayArray = accelBeaconDelayArray;
    }

    public Map<String, AccelBeacon> getAccelBeaconMap() {
        return mAccelBeaconMap;
    }

    public void setAccelBeaconMap(Map<String, AccelBeacon> accelBeaconMap) {
        this.mAccelBeaconMap = accelBeaconMap;
    }

    //0110 jhlee String으로 타입변환 수정
    public Map<String, LinkedHashSet<String>> getAccelBeaconDelayMap() {
        return mAccelBeaconDelayMap;
    }

    public void setAccelBeaconDelayMap(Map<String, LinkedHashSet<String>> accelBeaconDelayMap) {
        mAccelBeaconDelayMap = accelBeaconDelayMap;
    }
    //0110 jhlee String으로 타입변환 수정끝

    public Queue<Double> getROLL_QUEUE() {
        return ROLL_QUEUE;
    }

    public Queue<Double> getPITCH_QUEUE() {
        return PITCH_QUEUE;
    }

    public Queue<Double> getYAW_QUEUE() {
        return YAW_QUEUE;
    }

    public int getSaveCountRoll() {
        return mSaveCountRoll;
    }

    public void setSaveCountRoll(int saveCountRoll) {
        this.mSaveCountRoll = saveCountRoll;
    }

    public int getSaveCountPitch() {
        return mSaveCountPitch;
    }

    public void setSaveCountPitch(int saveCountPitch) {
        this.mSaveCountPitch = saveCountPitch;
    }

    public int getSaveCountYaw() {
        return mSaveCountYaw;
    }

    public void setSaveCountYaw(int saveCountYaw) {
        this.mSaveCountYaw = saveCountYaw;
    }

    public int getWholeTimerDelay() {
        return mWholeTimerDelay;
    }

    public void setWholeTimerDelay(int wholeTimerDelay) {
        mWholeTimerDelay = wholeTimerDelay;
    }

    public int getBeaconSequence() {
        return mBeaconSequence;
    }

    public void setBeaconSequence(int beaconSequence) {
        mBeaconSequence = beaconSequence;
    }

    public int getAccelSequence() {
        return mAccelSequence;
    }

    public void setAccelSequence(int accelSequence) {
        mAccelSequence = accelSequence;
    }

    public int getAccelCount() {
        return mAccelCount;
    }

    public void setAccelCount(int accelCount) {
        mAccelCount = accelCount;
    }

    public int getSAVE_DELAY() {
        return SAVE_DELAY;
    }

    public void setSAVE_DELAY(int SAVE_DELAY) {
        this.SAVE_DELAY = SAVE_DELAY;
    }

    public int getTimeoutCount() {
        return mTimeoutCount;
    }

    public void setTimeoutCount(int timeoutCount) {
        mTimeoutCount = timeoutCount;
    }

    public Total getCAN_NOT_SEND_TOTAL_SAVE() {
        return CAN_NOT_SEND_TOTAL_SAVE;
    }

    public void setCAN_NOT_SEND_TOTAL_SAVE(Total CAN_NOT_SEND_TOTAL_SAVE) {
        this.CAN_NOT_SEND_TOTAL_SAVE = CAN_NOT_SEND_TOTAL_SAVE;
    }

    public boolean isLobbyGet() {
        return mLobbyGet;
    }

    public void setLobbyGet(boolean lobbyGet) {
        mLobbyGet = lobbyGet;
    }

    public boolean isElevatorGet() {
        return mElevatorGet;
    }

    public void setElevatorGet(boolean elevatorGet) {
        mElevatorGet = elevatorGet;
    }

    public String getINOUT_STATE() {
        return INOUT_STATE;
    }

    public void setINOUT_STATE(String INOUT_STATE) {
        this.INOUT_STATE = INOUT_STATE;
    }

    public boolean isRESTART_BEACON() {
        return RESTART_BEACON;
    }

    public void setRESTART_BEACON(boolean RESTART_BEACON) {
        this.RESTART_BEACON = RESTART_BEACON;
    }

    public int getLAST_BEACON() {
        return LAST_BEACON;
    }

    public void setLAST_BEACON(int LAST_BEACON) {
        this.LAST_BEACON = LAST_BEACON;
    }

    public int getPARING_COUNT() {
        return PARING_COUNT;
    }

    public void setPARING_COUNT(int PARING_COUNT) {
        this.PARING_COUNT = PARING_COUNT;
    }

    public int getAfterStartCount() {
        return mAfterStartCount;
    }

    public void setAfterStartCount(int afterStartCount) {
        mAfterStartCount = afterStartCount;
    }
    //endregion

    public int getAfterGyroCount() {
        return mAfterGyroCount;
    }

    public void setAfterGyroCount(int afterGyroCount) {
        mAfterGyroCount = afterGyroCount;
    }

    // jhlee 추가 시작
    public int getAfterAccelCount() {
        return mAfterAccelCount;
    }

    public void setAfterAccelCount(int afterAccelCount) {
        mAfterAccelCount = afterAccelCount;
    }
    //jhlee 추가 끝 */

    public int getAfterLobbyEleCount() {
        return mAfterLobbyEleCount;
    }

    public void setAfterLobbyEleCount(int afterLobbyEleCount) {
        mAfterLobbyEleCount = afterLobbyEleCount;
    }

    public boolean isLobbyBeaconEnd() {
        return mLobbyBeaconEnd;
    }

    public void setLobbyBeaconEnd(boolean lobbyBeaconEnd) {
        mLobbyBeaconEnd = lobbyBeaconEnd;
    }

    public boolean isElevatorBeaconGet() {
        return mElevatorBeaconGet;
    }

    public void setElevatorBeaconGet(boolean elevatorBeaconGet) {
        mElevatorBeaconGet = elevatorBeaconGet;
    }

    public String getParingStateValue() {
        return mParingStateValue;
    }

    public void setParingStateValue(String paringStateValue) {
        mParingStateValue = paringStateValue;
    }

    public boolean increaseGyroSendCount() {
        if (++this.mGyroSendCount >= 10) {
            this.mGyroSendCount = 0;
            return true;
        }
        return false;
    }

    public void setGyroSendCount(int count) {
        this.mGyroSendCount = count;
    }

    // jhlee 추가 시작
    public boolean increaseAccelSendCount() {
        if (++this.mAccelSendCount >= 10) {
            this.mAccelSendCount = 0;
            return true;
        }
        return false;
    }

    //jhlee 0214 delayList 수정
    public LinkedHashSet<String> GetDelayList() {
        return saveDelayList;
    }

    public LinkedHashSet<String> SetDelayList(String value) {
        saveDelayList.add(value);
        return saveDelayList;
    }
    //jhlee 0214 delayList 수정끝

    public void setAccelSendCount(int count) {
        this.mAccelSendCount = count;
    }
    //jhlee 추가 끝 */

    public void setCurrentInputTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        this.mInputTime = simpleDateFormat.format(Calendar.getInstance().getTime());
    }

    public Total getPendingTotal() {
        return pendingTotal;
    }

    public void setPendingTotal(Total total) {
        this.pendingTotal = total;
    }

    public void clearPendingTotal() {
        this.pendingTotal = null;
    }

    public boolean isWorkingState() {
        return workingApiCheck;
    }

    public void setWorkingApiCheck(boolean workingApiCheck) {
        this.workingApiCheck = workingApiCheck;
    }

    //변경버전 1220 jhlee 자이로 빅데이터 버전 GET, SET추가 변경중
    //gyrosensor를 그대로 List에 넣어주는 함수 추가
    // public Boolean getBigDataFlag() {
    //    return UserDataSingleton.getInstance().getBigDataSend();
    // }

    //변경버전 1220 jhlee 자이로 빅데이터 버전 GET, SET추가 변경끝
    public void Reset() {
        Timber.d("Try reset()");
        mInputTime = "";

        mPreState = null;

        mStart1Beacon = false;     //  시작
        mStart2Beacon = false;     //  시작2
        mEnd1Beacon = false;       //  끝
        mEnd2Beacon = false;       //  끝2
        mOutParking = false;       //  타이머 종료시 출차 확인

        ABNORMAL_END = false;
        mParingStateSave = false;

        mBeaconArrayList.clear();
        mGyroSensorArrayList.clear();
        //jhlee 0311 gyroSensor2초기화 추가
        mGyroSensorArrayList2.clear();
        mAccelSensorArrayList.clear();
        mAccelSensorArrayList2.clear();
        mTotalArrayList.clear();
        mAccelBeaconArrayList.clear();

        INOUT_DATA_MAJOR.clear();
        NO_START_BEACON.clear();

        mAccelBeaconDelayArray.clear();

        mAccelBeaconDelayMap.clear();
        mAccelBeaconMap.clear();
        //0717 jhlee 추가초기화
        DataManagerSingleton.getInstance().setWorkingApiCheck(false);
        //jhlee 0806 total임사저장값 초기화
        clearPendingTotal();
        //jhlee 0806 total임사저장값 초기화끝
        PARING_COUNT = 0;
        mWholeTimerDelay = 0;
        mBeaconSequence = 0;
        //jhlee 0305 Accel Sensor seq가 초기화 안되는 증상 수정
        mAccelSequence = 0;
        mAccelCount = 0;

        SAVE_DELAY = 0;

        ROLL_QUEUE.clear();
        PITCH_QUEUE.clear();
        YAW_QUEUE.clear();

        mSaveCountRoll = 0;
        mSaveCountPitch = 0;
        mSaveCountYaw = 0;

        //region Beacon Lobby
        mLobbyGet = false;
        mElevatorGet = false;
        INOUT_STATE = null;
        RESTART_BEACON = false;
        LAST_BEACON = 0;

        mTimeoutCount = 0;
        TimerSingleton.getInstance().setAfterStart(false);
        mAfterStartCount = 0;
        mAfterGyroCount = 0;

        mAfterLobbyEleCount = 0;
        mLobbyBeaconEnd = false;
        mElevatorBeaconGet = false;

        mGyroSendCount = 0;
        // jhlee 추가 시작
        mAfterAccelCount = 0;
        mAccelSendCount = 0;

        saveDelayList.clear();
        //jhlee 추가 끝 */
    }

    private static class DataManagerSingletonHolder {
        private static final DataManagerSingleton instance = new DataManagerSingleton();
    }
}
