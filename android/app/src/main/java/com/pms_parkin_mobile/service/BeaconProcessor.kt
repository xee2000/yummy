package com.pms_parkin_mobile.service

import android.content.Context
import com.pms_parkin_mobile.dto.AccelBeacon
import com.pms_parkin_mobile.service.ParkingStateManager
import timber.log.Timber
import java.util.TreeSet
import android.util.Log

/**
 * BeaconProcessor
 *
 * 기존 BeaconFunction.java 완전 대체 (Kotlin 재작성)
 *
 * -- Major 번호 매핑 -------------------------------------------------------------
 *   1 → 로비        (Onepass 문열림 + 출차 준비)
 *   2 → 입구        (입차 시작 조건)
 *   3 → 엘리베이터  (출차 확정 / 입차 준비)
 *   4 → 주차면 평시 (주차면 데이터 수집)
 *   5 → 주차면 변화 (변화 데이터 수집)
 *   6 → 주차장 진입 (WholeTimer 시작)
 *
 * -- 문열림 쿨타임 정책 (비콘별 독립 적용) ----------------------------------------
 *   - 기본 쿨타임 : API 호출 직후 3초 (중복 호출 방지)
 *   - 성공 쿨타임 : 문열림 성공 콜백 후 10초 (연속 열림 방지)
 *   - 실패 시     : 쿨타임 즉시 해제 → 재시도 가능
 *
 *   ※ 쿨타임은 OnepassBeacon.setCooldown() / isCooldown 을 통해
 *     각 비콘 객체 내부에서 독립적으로 관리됩니다.
 *     이전의 전역 isLobbyTimerRunning / Timer 방식은 완전히 제거되었습니다.
 *
 * -- 입차 흐름 -------------------------------------------------------------------
 *   Major=2 (RSSI≥-90)         → Start1=true
 *   Major=6 (RSSI≥-80, Start1) → Start2=true, WholeTimer(15분) 시작
 *   WholeTimer 중 Major=4/5    → AccelBeaconMap / ChangeBeaconList 수집
 *
 * -- 출차 흐름 -------------------------------------------------------------------
 *   Major=1 (RSSI≥-80, WholeTimer중)   → End1=true, INOUT=OUT, AfterLobbyElevatorTimer
 *   Major=3 (RSSI≥-75, End1=true, OUT) → End2=true, CollectLobbyTimer(5초)
 *   CollectLobbyTimer 완료              → WholeTimer 강제 종료 → ParkingOut
 *
 * -- 재입차 흐름 ------------------------------------------------------------------
 *   Major=3 (End1/2 모두 false, WholeTimer 미동작) → End2=true, INOUT=IN
 *   Major=1 (End2=true, INOUT=IN, WholeTimer 미동작) → StayRestartTimer(900초)
 *   StayRestartTimer가 열어준 900초 창 안에 Major=6 비콘이 오면 재입차 확정
 */
class BeaconProcessor(private val context: Context) {

    companion object {
        private const val TAG = "BeaconProcessor"

        /**
         * 서버/로비폰이 DetectBeaconSignal을 수신 후 인체감지 대기 중임을 나타내는 오류 코드
         * → 이 경우 쿨다운을 유지한 채 다음 쿨다운 만료 후 재전송
         * → 그 외 오류(네트워크, 기타)는 clearCooldown() 후 즉시 재시도
         */
        private const val ERROR_CODE_LOBBY_PENDING = "55"
    }

    val state = ParkingStateManager()
    val timers = BeaconTimerManager(context, state)

    // -- AccelBeacon 수집 자료구조 -------------------------------------------------
    private val accelBeaconMap = mutableMapOf<String, AccelBeaconEntry>()
    private val accelBeaconDelayMap = mutableMapOf<String, LinkedHashSet<String>>()

    // ------------------------------------------------------------------------
    // Major=1 : 로비 — BLE_PARKING 미포함 (Onepass 전용)
    // 기존: OnlyOpenLobby()
    //
    // 쿨타임 정책:
    //   - beacon.isCooldown == true  → 해당 비콘은 스킵 (다른 비콘은 계속 처리)
    //   - API 호출 직전              → UserDataStore의 cooldownIgnoreSec 쿨타임 설정
    //   - 문열림 성공 콜백           → UserDataStore의 cooldownSendSec 쿨타임 갱신
    //   - 문열림 실패 콜백           → 무문별한 재시도를 방지하기 위해 쿨타임 유지(쿨타임 종료 후 재시도 가능)
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Major=1 : 로비 — BLE_PARKING 포함
    // 기존: LOBBY_BEACON()
    // 쿨타임 정책: processOnepass() 와 동일
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Major=2 : 입구 비콘
    // 기존: ENTRANCE_BEACON()
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // Major=3 : 엘리베이터 비콘
    // 기존: ELEVATOR_BEACON()
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Major=4 : 주차면 평시 비콘
    // 기존: StayBeacon()
    // ------------------------------------------------------------------------

    fun processStayParking(minor: Int, rssi: Double) {
        // 수동 주차 모드 중: 자동 주차 타이머 로직 스킵, 비콘 데이터만 수집
        if (App.instance.isPassiveCheck) {
            if (rssi >= -80) {
                val id = if (minor > 32768) minor - 32768 else minor
                val hexId = "%04X".format(id)
                savePassiveBeacon(hexId, rssi)
            }
            return
        }

        // AfterLobbyElevatorTimer 동작 중이면 비콘 수신 카운터 증가
        // 원본 StayBeacon: isAFTER_LOBBY_ELEVATOR_CHECK() 조건과 동일

        // StartCalcTimer 동작 중이면 Dead-beacon 감시 카운터 증가
        // 원본 StayBeacon: isCOLLECT_START_BEACON_CALC() 조건과 동일 (RSSI 무관)
        if (timers.isStartCalcTimerRunning) state.collectStartCalcBeaconCount++

        Timber.i("${TAG} timers.isWholeTimerRunning " + timers.isWholeTimerRunning)
        Timber.i("${TAG} state.isWorkingState " + state.isWorkingState)

        // WholeTimer 미동작 + 신호 충분 + isWorkingState=false → 비콘 기반 주차 시작 트리거
        if (!timers.isWholeTimerRunning && rssi >= -80 && !state.isWorkingState) {
            Timber.i("${TAG}:processStayParking 3")
            timers.startAfterAccelTimer()
        }

        // AfterAccelTimer 동작 중이면 카운터 증가 (원본 isAFTER_ACCEL_START_CALC 조건)
        // onTick에서 이 카운터가 0이 아니면 WholeTimer 시작 트리거
        if (timers.isAfterAccelRunning) {
            state.afterAccelCount++
            Timber.d("$TAG [StayBeacon] afterAccelCount=${state.afterAccelCount}")
        }

        if (!timers.isWholeTimerRunning) return


        val id = if (minor > 32768) minor - 32768 else minor
        val hexId = "%04X".format(id)

        // 자이로 발생 후 수집 — 운전 중 빠른 주차 완료 시 비콘 미수집 보완
        if (timers.isAfterGyroRunning && rssi >= -80) {
            Log.d("TEST", "isAfterGyroRunning Start")

            saveAccelBeacon(hexId, rssi)
            addAccelDelay(hexId, rssi)
        }

        // CollectAccelBeacon 윈도우 중 수집
        if (timers.isCollectAccelBeaconRunning && rssi >= -80) {
            saveAccelBeacon(hexId, rssi)
            addAccelDelay(hexId, rssi)
        }
    }

    // ------------------------------------------------------------------------
    // Major=4 (RSSI < -90) : 주차장 서비스 유지
    // 기존: stayParkingService()
    // ------------------------------------------------------------------------

    fun keepParkingServiceAlive() {
        state.collectStartCalcBeaconCount++
    }

    // ------------------------------------------------------------------------
    // Major=5 : 주차면 변화 비콘
    // 기존: ChangeBeacon()
    // ------------------------------------------------------------------------

    fun processChangeParking(major: Int, minor: Int, rssi: Double) {
        // 수동 주차 모드 중에는 자동 주차 서비스 로직 전체 스킵
        if (App.instance.isPassiveCheck) return

        // AfterLobbyElevatorTimer 동작 중이면 비콘 수신 카운터 증가
        // 원본 ChangeBeacon: isAFTER_LOBBY_ELEVATOR_CHECK() 조건과 동일
        if (timers.isAfterLobbyElevatorRunning) state.afterLobbyEleCount++

        // StartCalcTimer 동작 중이면 Dead-beacon 감시 카운터 증가
        // 원본 ChangeBeacon: isCOLLECT_START_BEACON_CALC() 조건과 동일
        if (timers.isStartCalcTimerRunning) state.collectStartCalcBeaconCount++

        if (!timers.isWholeTimerRunning) return

        if (minor > 32768) {
            val id = minor - 32768
            val hexId = "%04X".format(id)

            // ChangeBeaconList에 추가 (서버 전송 데이터)
            state.addChangeBeacon(hexId, rssi, major, state.beaconSequence, state.wholeTimerDelay)
            state.beaconSequence++
            Timber.d("$TAG [ChangeBeacon] id=$hexId, rssi=$rssi, major=$major, seq=${state.beaconSequence - 1}")

            // CollectAccelBeacon 윈도우 활성화
            timers.setCollectAccelBeacon(true)
            saveAccelBeacon(hexId, rssi)
            addAccelDelay(hexId, rssi)
        } else {
            val hexId = "%04X".format(minor)
            if (timers.isCollectAccelBeaconRunning) {
                saveAccelBeacon(hexId, rssi)
                addAccelDelay(hexId, rssi)
            }
        }
    }

    // ------------------------------------------------------------------------
    // Major=6 : 주차장 진입 비콘
    // 기존: PARKING_BEACON()
    // ------------------------------------------------------------------------

    fun processParkingEntrance(major: Int, minor: Int, rssi: Double) {
        // 수동 주차 모드 중에는 자동 주차 서비스 로직 전체 스킵
        if (App.instance.isPassiveCheck) return

        // 원본 PARKING_BEACON: isAfterStart / isAFTER_GYRO_START_CALC 카운터
        if (timers.isAfterAccelRunning) {
            state.afterStartCount++
            Timber.d("$TAG [PARKING_BEACON] afterStartCount=${state.afterStartCount}")
        }
        if (timers.isAfterGyroRunning) state.afterGyroCount++

        if (rssi < -80) return

        if (state.isStart1Beacon) {
            // -- 정상 입차: Start1=true + Start2=false + WholeTimer 미동작 -
            if (!state.isStart2Beacon && !timers.isWholeTimerRunning) {
                state.isStart2Beacon = true
                state.isEnd2Beacon = false
                state.wholeTimerDelay = 0
                state.beaconSequence = 0
                state.recordInputTime()
                resetCollectionData()
                timers.cancelAfterAccelTimer()  // Major=4->6 연속 감지 시 루프 정리
                timers.startWholeTimer()
//                ParkingServiceApi.gateInformation(context, "$major", "$minor")
//                ParkingServiceApi.gateInformation(context, "NORMAL_START", "NORMAL_START")
                Timber.d("$TAG [PARKING_BEACON] 정상 입차: Start2=true, WholeTimer 시작")
            }
        } else {
            // -- Start1 없이 주차장 비콘 먼저 수신 (일부 기기 보완) ---------
            if (!state.isStart2Beacon) {
                state.isStart2Beacon = true
//                ParkingServiceApi.gateInformation(context, "$major", "$minor")
                Timber.d("$TAG [PARKING_BEACON] Start1 없이 Start2=true")
            }
        }

        // -- 재입차 확정: StayRestartTimer 동작 중 + isRestartBeacon + WholeTimer 미동작
        // StayRestartTimer가 열어준 900초 창 안에 Major=6 비콘이 오면 재입차 확정
        // 원본: 자이로 기반 WholeTimer 트리거가 주석 처리된 후,
        //       비콘 기반(Major=6 감지)으로 재입차 처리하도록 변경됨
        if (timers.isStayRestartRunning && state.isRestartBeacon && !timers.isWholeTimerRunning) {
            Timber.d("$TAG [PARKING_BEACON] 재입차 확정: StayRestart 창 내 Major=6 감지")
            timers.cancelStayRestartTimer()
            state.isRestartBeacon = false
            state.isEnd1Beacon   = false
            state.isEnd2Beacon   = false
//            ParkingServiceApi.gateInformation(context, "$major", "$minor")
            timers.cancelAfterAccelTimer() // 재진입 중 Major=4 선 수신 시 루프 정리
            timers.startWholeTimer()  // 재입차 WholeTimer 시작
//            ParkingServiceApi.gateInformation(context, "RESTART_START", "RESTART_START")
        }
    }

    // ------------------------------------------------------------------------
    // 수동 주차 모드 전용: App.mAccelBeaconMap 에 직접 저장
    // PassiveParkingService.parkingEnd() 가 이 맵을 읽어 위치 추정함
    // ------------------------------------------------------------------------

    private fun savePassiveBeacon(hexId: String, rssi: Double) {
        val app = App.instance
        synchronized(app.mAccelBeaconMap) {
            val rssiStr = rssi.toInt().toString()
            val existing = app.mAccelBeaconMap[hexId]
            if (existing == null) {
                val beacon = AccelBeacon().apply {
                    beaconId = hexId
                    this.rssi = rssiStr
                    delayList.add("${rssi.toInt()}_0")
                }
                app.mAccelBeaconMap[hexId] = beacon
            } else {
                // RSSI 최댓값 갱신
                if (rssi > (existing.rssi?.toDoubleOrNull() ?: Double.MIN_VALUE)) {
                    existing.rssi = rssiStr
                }
                val idx = existing.delayList.size
                existing.delayList.add("${rssi.toInt()}_$idx")
            }
        }
        Log.d("Passive", "[PassiveBeacon] $hexId RSSI=$rssi")
    }

    // ------------------------------------------------------------------------
    // AccelBeacon 수집 내부 유틸
    // 기존: SaveArrayListValue.SaveAccelBeacon() + BeaconFunction.AddAccelDelay()
    // ------------------------------------------------------------------------

    /**
     * AccelBeaconMap 저장
     * 동일 hexId: RSSI 최댓값 유지, count 누적
     */
    private fun saveAccelBeacon(hexId: String, rssi: Double) {
        val rssiStr = rssi.toInt().toString()
        val delayStr = state.wholeTimerDelay.toString()
        val existing = accelBeaconMap[hexId]

        Log.d("TEST" ,"AccelBeacon Save")
        accelBeaconMap[hexId] = if (existing == null) {
            AccelBeaconEntry(id = hexId, rssi = rssiStr, delay = delayStr, count = 1)
        } else {
            val newCount = existing.count + 1
            if (rssi >= existing.rssi.toFloat()) {
                existing.copy(rssi = rssiStr, delay = delayStr, count = newCount)
            } else {
                existing.copy(count = newCount)
            }
        }
    }

    /**
     * AccelBeaconDelayMap 저장 (기존 AddAccelDelay)
     *
     * 형식: "rssi_delay"
     * 동일 delay에 대해 RSSI가 더 크면 교체
     * delay 기준 오름차순 정렬 후 LinkedHashSet으로 순서 유지
     */
    private fun addAccelDelay(hexId: String, rssi: Double) {
        val delayStr = state.wholeTimerDelay.toString()
        val newEntry = "${rssi.toInt()}_$delayStr"

        val set = LinkedHashSet(accelBeaconDelayMap[hexId] ?: emptySet())

        // 동일 delay 중 RSSI가 더 크면 교체
        var toUpdate: String? = null
        for (entry in set) {
            val parts = entry.split("_", limit = 2)
            if (parts.size == 2 && parts[1] == delayStr) {
                val storedRssi = parts[0].toDoubleOrNull() ?: 0.0
                if (rssi > storedRssi) toUpdate = entry
                break
            }
        }
        if (toUpdate != null) set.remove(toUpdate)
        set.add(newEntry)

        // delay 기준 오름차순 정렬
        val sortedTreeSet = TreeSet<String> { s1, s2 ->
            if (!s1.contains("_") || !s2.contains("_")) return@TreeSet 0
            val d1 = s1.split("_", limit = 2)[1].toIntOrNull() ?: 0
            val d2 = s2.split("_", limit = 2)[1].toIntOrNull() ?: 0
            d1.compareTo(d2)
        }
        try {
            sortedTreeSet.addAll(set)
            accelBeaconDelayMap[hexId] = LinkedHashSet(sortedTreeSet)
            Timber.d("$TAG [AccelDelay] $hexId → ${accelBeaconDelayMap[hexId]}")
        } catch (e: Exception) {
            Timber.e("$TAG [AccelDelay] 예외: ${e.message}")
        }
    }


    // ------------------------------------------------------------------------
    // 수집 데이터 조회 (ParkingComplete API 전송 시 사용)
    // ------------------------------------------------------------------------

    fun getAccelBeaconMap(): Map<String, AccelBeaconEntry> = accelBeaconMap.toMap()
    fun getAccelBeaconDelayMap(): Map<String, LinkedHashSet<String>> = accelBeaconDelayMap.toMap()

    fun resetCollectionData() {
        accelBeaconMap.clear()
        accelBeaconDelayMap.clear()
        Timber.d("$TAG AccelBeacon 수집 데이터 초기화")
    }

    // ------------------------------------------------------------------------

    data class AccelBeaconEntry(
        val id: String,
        val rssi: String,
        val delay: String,
        val count: Int,
    )
}
