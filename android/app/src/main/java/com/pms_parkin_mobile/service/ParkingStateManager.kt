package com.pms_parkin_mobile.service

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ParkingStateManager
 *
 * 기존 DataManagerSingleton 대체
 * 주차 입/출차 시퀀스 상태 변수를 단일 클래스로 관리
 *
 * 상태 흐름:
 *   입차: isStart1Beacon → isStart2Beacon → WholeTimer 시작 → 주차면 수집
 *   출차: isEnd1Beacon  → isEnd2Beacon  → CollectLobby → ParkingOut
 *
 * reset()을 출차 완료 / 서비스 재시작 시 반드시 호출 (기존 누락 버그 수정)
 */
class ParkingStateManager {

    // -- 입차 시퀀스 --------------------------------------------------------------
    /** Major=2(입구) 감지 여부 */
    @Volatile var isStart1Beacon: Boolean = false

    /** Major=6(주차장) 감지 여부 → WholeTimer 시작 조건 */
    @Volatile var isStart2Beacon: Boolean = false

    // -- 출차 시퀀스 --------------------------------------------------------------
    /** Major=1(로비) 감지 여부 → 출차 준비 */
    @Volatile var isEnd1Beacon: Boolean = false

    /** Major=3(엘리베이터) 감지 여부 → 출차 확정 */
    @Volatile var isEnd2Beacon: Boolean = false

    // -- 보조 상태 ---------------------------------------------------------------
    /** 출차 처리 진행 중 */
    @Volatile var isOutParking: Boolean = false

    /** 재입차 시퀀스 진행 중 */
    @Volatile var isRestartBeacon: Boolean = false

    /** 입/출차 방향 - null 대신 UNKNOWN enum 사용 (NPE 방지) */
    @Volatile var inoutState: InOutState = InOutState.UNKNOWN

    /** 입차 시각 문자열 (서버 전송용) */
    @Volatile var inputTime: String = ""

    // -- 주차면 변화 비콘 수집 (Major=5, minor > 32768) --------------------------------
    private val _changeBeaconList = mutableListOf<ChangeBeaconData>()
    val changeBeaconList: List<ChangeBeaconData> get() = _changeBeaconList.toList()

    // -- 카운터 (기존 DataManagerSingleton 수치 변수) ---------------------------------
    /** CollectStartBeaconCalc 중 수신된 비콘 수 (주차장 서비스 유지 판단) */
    @Volatile var collectStartCalcBeaconCount: Int = 0

    /**
     * AfterAccelTimer 동작 중 Major=4 비콘 수신 카운터 (원본 afterAccelCount)
     * 0이 아니면 주차면 비콘이 실제로 잡히고 있다는 증거 → WholeTimer 시작 조건
     */
    @Volatile var afterAccelCount: Int = 0

    // AfterAccelTimer(isAfterAccelRunning) 동작 중 비콘 수신 카운터 (원본 afterStartCount)
    @Volatile var afterStartCount: Int = 0

    // AfterGyroTimer(isAfterGyroRunning) 동작 중 비콘 수신 카운터 (원본 afterGyroCount)
    @Volatile var afterGyroCount: Int = 0

    /** 주차 완료까지 경과 시간 (초) - ParkingComplete 전송 시 SAVE_DELAY 역할 */
    @Volatile var saveDelay: Int = 0

    /** WholeTimer 경과 틱 (delay 값) */
    @Volatile var wholeTimerDelay: Int = 0

    /** 주차면 변화 비콘 시퀀스 번호 */
    @Volatile var beaconSequence: Int = 0

    /** Major 1/3 수집용 (CollectLobby 중 inout 판단) */
    private val _inoutDataMajor = mutableListOf<Int>()
    val inoutDataMajor: List<Int> get() = _inoutDataMajor.toList()

    /**
     * CollectLobbyTimer 재귀 판단용 마지막 수신 비콘 Major 번호
     * 원본 LAST_BEACON. 3(엘리베이터)일 때만 WholeTimer 종료 확정
     */
    @Volatile var lastBeacon: Int = 0

    /** 서비스가 실제 주차 처리 중인지 여부 (WholeTimer 동작 기준) */
    @Volatile var isWorkingState: Boolean = false

    /**
     * 비정상 종료 여부 (비콘 30초 미수신으로 START_CALC_TIMER가 WholeTimer 강제 종료 시 true)
     * ParingState 문자열에 "-end" 접미어 추가용
     */
    @Volatile var isAbnormalEnd: Boolean = false

    /**
     * 로비 비콘만 수신 후 엘리베이터 없이 AfterLobbyElevatorTimer 만료된 경우 true
     * ParingState 문자열에 "-lobby" 접미어 추가용
     */
    @Volatile var isLobbyBeaconEnd: Boolean = false

    /**
     * AfterLobbyElevatorTimer 동작 중 수신된 비콘 카운트 (원본 afterLobbyEleCount)
     * LOBBY_BEACON / StayBeacon / ChangeBeacon 수신 시 증가
     * AfterLobbyElevatorTimer 타임아웃 시 0이면 비콘 없음 판단 → 종료 확정
     *                                       0 아니면 비콘 수신 중 → count 리셋 후 재귀
     */
    @Volatile var afterLobbyEleCount: Int = 0

    /**
     * AfterLobbyElevatorTimer 동작 중 엘리베이터 비콘 수신 여부 (원본 elevatorBeaconGet)
     * processElevator에서 isStartCalcTimerRunning 중일 때 true로 세팅
     * AfterLobbyElevatorTimer에서 WholeTimer 강제 종료 여부 결정에 사용
     *   - true  → 엘리베이터 비콘 수신됨 → 정상 출차 진행 중 → 타이머만 종료
     *   - false → 엘리베이터 비콘 없음   → isLobbyBeaconEnd = true → finishWholeTimer
     */
    @Volatile var elevatorBeaconGet: Boolean = false

    // -------------------------------------------------------------------------

    enum class InOutState { IN, OUT, UNKNOWN }

    data class ChangeBeaconData(
        val beaconId: String,
        val rssi:     String,
        val state:    String,   // Major 값
        val delay:    String,
        val seq:      String,
    )

    // -------------------------------------------------------------------------

    /** 입차 시각 기록 */
    fun recordInputTime() {
        inputTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.KOREA)
            .format(Calendar.getInstance().time)
    }

    /** Major=5 변화 비콘 추가 */
    fun addChangeBeacon(hexId: String, rssi: Double, major: Int, seq: Int, delay: Int) {
        _changeBeaconList.add(
            ChangeBeaconData(
                beaconId = hexId,
                rssi     = rssi.toInt().toString(),
                state    = major.toString(),
                delay    = delay.toString(),
                seq      = seq.toString(),
            )
        )
    }

    /** CollectLobby 중 Major 수집 */
    fun addInoutDataMajor(major: Int) {
        _inoutDataMajor.add(major)
    }

    /** inoutDataMajor 리스트 초기화 */
    fun clearInoutDataMajor() {
        _inoutDataMajor.clear()
    }

    /** inoutDataMajor의 마지막 값을 lastBeacon에 저장 후 리스트 초기화 */
    fun consumeInoutDataMajor() {
        if (_inoutDataMajor.isNotEmpty()) {
            lastBeacon = _inoutDataMajor.last()
        }
        _inoutDataMajor.clear()
    }

    /**
     * 전체 상태 초기화
     * 출차 완료 / 서비스 재시작 시 반드시 호출
     */
    fun reset() {
        isStart1Beacon              = false
        isStart2Beacon              = false
        isEnd1Beacon                = false
        isEnd2Beacon                = false
        isOutParking                = false
        isRestartBeacon             = false
        inoutState                  = InOutState.UNKNOWN
        inputTime                   = ""
        isWorkingState              = false
        collectStartCalcBeaconCount = 0
        saveDelay                   = 0
        wholeTimerDelay             = 0
        beaconSequence              = 0
        _changeBeaconList.clear()
        _inoutDataMajor.clear()
        isAbnormalEnd    = false
        isLobbyBeaconEnd = false
        lastBeacon       = 0
        afterLobbyEleCount  = 0
        elevatorBeaconGet   = false
        afterStartCount = 0
        afterGyroCount = 0
        afterAccelCount = 0
    }
}
