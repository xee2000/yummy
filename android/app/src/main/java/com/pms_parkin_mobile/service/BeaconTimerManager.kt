package com.pms_parkin_mobile.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.gson.GsonBuilder
import com.pms_parkin_mobile.api.RestController
import android.util.Log
import com.pms_parkin_mobile.util.TotalGzipService
import com.woorisystem.domain.AccelBeaconData
import com.woorisystem.domain.ParkingBeaconData
import com.woorisystem.domain.ParkingTotal
import timber.log.Timber
import java.util.LinkedHashSet

/**
 * BeaconTimerManager
 *
 * 기존 TimerSingleton (CountDownTimer 기반) 완전 대체
 * 코루틴 기반으로 교체해 cancel 레이스컨디션 및 메모리 누수 해소
 *
 * 타이머 목록:
 *   WholeTimer              : 15분, 주차 데이터 수집 전체 타임아웃
 *   LobbyTimer              : 3초,  로비 비콘 중복 요청 방지 쿨다운
 *   CollectLobbyTimer       : 5초,  출차 시 로비+엘리베이터 비콘 재확인
 *   StayRestartTimer        : 900초, 재입차 가드 타이머 (비콘 기반 재입차 기회 창)
 *   AfterLobbyElevatorTimer : 출차 준비 대기 (로비 후 엘리베이터 대기)
 *   AfterAccelTimer         : WholeTimer 외부에서 주차면 비콘 수집 시작 트리거
 *   AfterGyroTimer          : 자이로 발생 후 비콘 수집 윈도우
 *   CollectAccelBeaconTimer : Major=5 감지 후 accel 비콘 수집 윈도우
 */
class BeaconTimerManager(
    private val context: Context,
    private val state:   ParkingStateManager,
) {
    companion object {
        private const val TAG = "BeaconTimerManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    // -- Job -----------------------------------------------------------------
    private var wholeTimerJob:              Job? = null
    private var startCalcTimerJob:          Job? = null
    private var lobbyTimerJob:              Job? = null
    private var collectLobbyTimerJob:       Job? = null
    private var stayRestartTimerJob:        Job? = null
    private var afterLobbyElevatorJob:      Job? = null
    private var afterAccelTimerJob:         Job? = null
    private var afterGyroTimerJob:          Job? = null
    private var collectAccelBeaconTimerJob: Job? = null

    // -- 상태 플래그 --------------------------------------------------------------
    var isWholeTimerRunning:          Boolean = false; private set
    var isStartCalcTimerRunning:      Boolean = false; private set
    var isLobbyTimerRunning:          Boolean = false; private set
    var isCollectLobbyRunning:        Boolean = false; private set
    var isStayRestartRunning:         Boolean = false; private set
    var isAfterLobbyElevatorRunning:  Boolean = false; private set
    var isAfterAccelRunning:          Boolean = false; private set
    var isAfterGyroRunning:           Boolean = false; private set
    var isCollectAccelBeaconRunning:  Boolean = false; private set

    // AfterAccelTimer 만료 횟수 (원본 mAccelSendCount: 10회마다 로그 전송)
    private var accelSendCount: Int = 0

    // 수동 주차(passive) 진입 시 WholeTimer 실행 여부 보존
    private var wasWholeTimerRunningBeforePassive = false

    // ------------------------------------------------------------------------
    // WholeTimer (15분)
    // 주차 데이터 수집 전체 타임아웃 — 완료 시 ParkingOut
    // ------------------------------------------------------------------------

    fun startWholeTimer() {
        if (isWholeTimerRunning) return
        isWholeTimerRunning    = true
        state.isWorkingState   = true
        state.wholeTimerDelay  = 0
        Timber.d("BeaconTimerManager: ")
        // SensorService 센서 상태 초기화 (죽어있으면 재시작)
        val svc = SensorService.getInstance()
        Log.d("TEST", "▶ startWholeTimer: SensorService=${if (svc != null) "실행중" else "null → 재시작"}")
        if (svc != null) {
            svc.resetSensorState()
        } else {
            context.startService(Intent(context, SensorService::class.java))
        }

        // WholeTimer 시작과 동시에 Dead-beacon 감시 타이머 시작
        startCalcTimer()

        wholeTimerJob = scope.launch {
            // 1초마다 delay 카운트 증가
            repeat(15 * 60) {
                delay(1_000L)
                state.wholeTimerDelay++
                Log.d("TEST", "delay :" + state.wholeTimerDelay)
            }
            Timber.d("BeaconTimerManager: WholeTimer 완료 → ParkingOut")
            onWholeTimerFinish()
        }
    }

    /** 강제 완료 (입구 재감지, CollectLobby 완료 등) */
    fun finishWholeTimer() {
        Timber.d("BeaconTimerManager: WholeTimer 강제 완료 → ParkingOut")
        wholeTimerJob?.cancel()
        wholeTimerJob = null
        onWholeTimerFinish()
    }

    /**
     * WholeTimer 완료 처리 (자연 만료 / 강제 완료 공통)
     *
     * -- 처리 순서 ----------------------------------------------------------
     *  1. 플래그 해제
     *  2. AccelTimer 중지 (SensorService)
     *  3. saveDelay 기록 (1분 미만이면 ParkingComplete 전송 생략)
     *  4. ParingState 문자열 조합
     *  5. AccelBeaconMap → AccelBeaconList 변환 (SaveAccelBeacon)
     *  6. ParkingTotal 조립
     *  7. bigDataSend == true → Total을 Gzip 압축 → file 필드 첨부
     *  8. ParkingComplete 전송
     *  9. isOutParking == true → 추가로 ParkingOut 전송
     * 10. 상태 초기화
     */
    private fun onWholeTimerFinish() {
        isWholeTimerRunning  = false
        state.isWorkingState = false

        // -- 0. StartCalcTimer 취소 (WholeTimer 종료되면 감시 불필요) -------
        cancelCalcTimer()

        // -- 1. AccelTimer 중지 --------------------------------------------
        // SensorService의 AccelTimer는 isWorkingState 변경으로 자동 종료되지만
        // SensorService가 살아있는지 확인 후 명시적으로 상태도 초기화
        val sensorService = SensorService.getInstance()

        // -- 2. saveDelay 기록 ---------------------------------------------
        state.saveDelay = state.wholeTimerDelay
        Timber.d("$TAG: WholeTimer 완료 saveDelay=${state.saveDelay}초")

        // -- 3. 1분(60초) 미만 데이터는 전송 의미 없음 → 초기화만 수행 ------
        if (state.saveDelay < 30) {
            Log.d("TEST" ,"주차데이터부족")
            Timber.w("$TAG: 주차 데이터 부족 (${state.saveDelay}초 < 60초) → ParkingComplete 생략")
            state.reset()
            sensorService?.resetSensorState()
            return
        }

        // -- 4. ParingState 문자열 조합 ------------------------------------
        // 원본: paringStateValue("ANDROID") + 접미어(-end, -lobby, -out)
        // "ANDROID"는 AfterStartTimer(10초) 완료 후 설정되던 값.
        // AfterStartTimer는 현재 미구현이므로 기본값 "ANDROID"로 고정.
        var paringState = "ANDROID"
        if (state.isAbnormalEnd)    paringState += "-end"
        if (state.isLobbyBeaconEnd) paringState += "-lobby"
        if (state.isOutParking && !state.isAbnormalEnd) paringState += "-out"
        Timber.d("$TAG: ParingState=$paringState")

        // -- 5. AccelBeaconMap → AccelBeaconList 변환 (SaveAccelBeacon) ----
        // 원본 SaveArrayListValue.SaveAccelBeacon() 역할
        // BeaconProcessor의 accelBeaconMap을 AccelBeaconData 리스트로 변환
        val btService = BluetoothService.getInstance()
        Log.d("TEST", "엑셀비컨 주입시작")
        val accelBeaconList = buildAccelBeaconList(btService)

        // -- 6. 주차면 변화 비콘 목록 (ChangeBeaconList → ParkingBeaconData) -
        val beaconList = state.changeBeaconList.map { c ->
            ParkingBeaconData(
                beaconId = c.beaconId,
                state    = c.state,
                rssi     = c.rssi,
                delay    = c.delay,
                seq      = c.seq,
            )
        }

        // -- 7. ParkingTotal 조립 ------------------------------------------
        val sensorData  = sensorService?.sensorDataStore
//        val bigDataSend = UserDataStore.userData.bigDataSend
        Log.d("TEST" ,"최종 accelList2 확인 : " + sensorData?.accelList2)
        val total = ParkingTotal(
            inputDate      = state.inputTime,
            sensorList     = sensorData?.accelList  ?: emptyList(),
            sensorList2    = sensorData?.accelList2 ?: emptyList(),
            beaconList     = beaconList,
            gyroList       = sensorData?.gyroList   ?: emptyList(),
            gyroList2      = sensorData?.gyroList2,
            accelBeaconList = accelBeaconList,
            paringState    = paringState,
        )
        Timber.d("$TAG: total : " + total.sensorList2)

        Log.d("TEST" ,"최종 데이터 확인 : " + total)


        // -- 8. bigDataSend → Gzip 압축 → file 첨부 -----------------------
        // 원본: gyroList2를 잠시 null로 비우고 압축 후 다시 채우는 방식
        // Kotlin data class는 copy()로 깔끔하게 처리
        val finalTotal = {
            val totalWithoutGyro2 = total.copy(gyroList2 = null)
            val gzipBytes = TotalGzipService.toGzipBytes(totalWithoutGyro2)
            total.copy(file = gzipBytes)
        }

        // -- 9. ParkingComplete 전송 ---------------------------------------
        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        RestController.instance.Parking(context,total)
        Timber.i("$TAG: ParkingComplete 전송 beacons=${beaconList.size} accelBeacons=${accelBeaconList.size}")


        // -- 11. 상태 초기화 -----------------------------------------------
        state.reset()
        sensorService?.resetSensorState()
        Timber.d("$TAG: WholeTimer 완료 처리 종료")
    }

    /**
     * AccelBeaconMap → AccelBeaconData 리스트 변환
     * 원본 SaveArrayListValue.SaveAccelBeacon() 역할
     *
     * accelBeaconDelayMap의 delay 기준 중복 제거 + 오름차순 정렬 후 AccelBeaconData로 변환
     */
    private fun buildAccelBeaconList(btService: BluetoothService?): List<AccelBeaconData> {
        Log.d("TEST", "엑셀비컨 주입확인")
        // BeaconProcessor의 accelBeaconMap / accelBeaconDelayMap getter 활용
        val processor = btService?.beaconProcessorForTimer ?: return emptyList()
        val accelMap      = processor.getAccelBeaconMap()
        val accelDelayMap = processor.getAccelBeaconDelayMap()
        Log.d("TEST", "accelMap : " + accelMap.toString())
        return accelMap.values.map { entry ->
            val rawDelaySet = accelDelayMap[entry.id]

            // delay 기준 중복 제거: 동일 delay 중 RSSI 최댓값만 유지
            val deduped = LinkedHashMap<String, String>() // delay → "rssi_delay"
            rawDelaySet?.forEach { item ->
                val parts = item.split("_", limit = 2)
                if (parts.size == 2) {
                    val (rssiStr, delayStr) = parts
                    val existing = deduped[delayStr]
                    if (existing == null ||
                        (rssiStr.toDoubleOrNull() ?: 0.0) >
                        (existing.split("_")[0].toDoubleOrNull() ?: 0.0)
                    ) {
                        deduped[delayStr] = item
                    }
                }
            }

            // delay 기준 오름차순 정렬
            val sortedList = deduped.values
                .sortedBy { it.split("_", limit = 2).getOrNull(1)?.toIntOrNull() ?: 0 }

            Log.d("TEST", "accelbeacon : " + sortedList.toString())
            AccelBeaconData(
                beaconId  = entry.id,
                rssi      = entry.rssi,
                delay     = entry.delay,
                count     = entry.count.toString(),
                delayList = LinkedHashSet(sortedList),
            )
        }
    }

    fun cancelWholeTimer() {
        cancelCalcTimer()
        wholeTimerJob?.cancel()
        wholeTimerJob       = null
        isWholeTimerRunning = false
        state.isWorkingState = false
    }

    // ------------------------------------------------------------------------
    // LobbyTimer (3초)
    // 로비 비콘 중복 문열림 요청 방지 쿨다운
    // ------------------------------------------------------------------------

    fun startLobbyTimer() {
        if (isLobbyTimerRunning) return
        isLobbyTimerRunning = true
        Timber.d("BeaconTimerManager: LobbyTimer 시작 (3초)")

        lobbyTimerJob = scope.launch {
            delay(3_000L)
            isLobbyTimerRunning = false
            Timber.d("BeaconTimerManager: LobbyTimer 완료")
        }
    }

    fun cancelLobbyTimer() {
        lobbyTimerJob?.cancel()
        lobbyTimerJob       = null
        isLobbyTimerRunning = false
    }

    // ------------------------------------------------------------------------
    // StartCalcTimer (2분 반복)
    // Dead-beacon 감시: WholeTimer 동작 중 2분마다 비콘 수신 여부 확인
    // 2분간 비콘 없으면 → isAbnormalEnd 마킹 후 WholeTimer 강제 종료
    // ------------------------------------------------------------------------

    /**
     * StartCalcTimer (원본 START_CALC_TIMER)
     *
     * WholeTimer 시작 시 함께 시작되며 2분마다 반복 실행.
     *
     * 동작:
     *   collectStartCalcBeaconCount > 0  → 비콘 수신 중 → 카운터 리셋 후 계속 감시
     *   collectStartCalcBeaconCount == 0 → 2분간 비콘 전혀 없음
     *                                      → isOutParking = false (로비 비콘 없이 끊긴 상태)
     *                                      → isAbnormalEnd 는 마킹하지 않음
     *                                        (원본의 isABNORMAL_END 블록은 주석 처리된 Dead Code)
     *                                      → finishWholeTimer() 강제 종료
     *
     * 원본과의 차이:
     *   - Java Timer/TimerTask → 코루틴 while 루프로 교체
     *   - isABNORMAL_END 세팅 블록은 원본에서 주석 처리된 Dead Code → 미구현
     */
    fun startCalcTimer() {
        if (isStartCalcTimerRunning) return
        isStartCalcTimerRunning = true
        Timber.d("BeaconTimerManager: StartCalcTimer 시작 (2분 반복)")

        Log.d("TEST", "StartCalcTimer 시작 (2분 반복)")
        startCalcTimerJob = scope.launch {
            while (isWholeTimerRunning) {
                delay(10_000L)  // 2분 대기

                if (!isWholeTimerRunning) break  // WholeTimer가 먼저 끝난 경우

                val count = state.collectStartCalcBeaconCount
                if (count != 0) {
                    // 비콘 수신 중 → 카운터 리셋 후 계속 감시
                    Timber.d("BeaconTimerManager: StartCalcTimer 비콘 수신 확인 (count=$count) → 리셋 후 계속")
                    Log.d("TEST", "StartCalcTimer 비콘 수신 확인 : " + count)

                    state.collectStartCalcBeaconCount = 0
                } else {
                    // 2분간 비콘 없음 → WholeTimer 강제 종료
                    Timber.w("BeaconTimerManager: StartCalcTimer 2분간 비콘 없음 → WholeTimer 강제 종료")
                    Log.d("TEST", "2분간 비콘 없음 → WholeTimer 강제 종료")

//                    ParkingServiceApi.gyroInformation(context, "2분 검사시 비콘 없음 확인")

                    // 로비 비콘으로 종료된 것이 아니므로 isOutParking 해제
                    state.isOutParking = false
                    state.collectStartCalcBeaconCount = 0

                    isStartCalcTimerRunning = false
                    finishWholeTimer()
                    break
                }
            }
            isStartCalcTimerRunning = false
            Timber.d("BeaconTimerManager: StartCalcTimer 종료")
        }
    }

    fun cancelCalcTimer() {
        startCalcTimerJob?.cancel()
        startCalcTimerJob       = null
        isStartCalcTimerRunning = false
        Timber.d("BeaconTimerManager: StartCalcTimer 취소")
    }

    // ------------------------------------------------------------------------
    // CollectLobbyTimer (3초 반복 재귀)
    // 출차 확정 후 로비/엘리베이터 비콘 수신이 완전히 끊길 때까지 대기
    // ------------------------------------------------------------------------

    /**
     * startCollectLobbyTimer (원본 StartCollectLobby)
     *
     * 단순 5초→종료가 아닌 재귀 구조:
     *   3초마다 inoutDataMajor 리스트 확인
     *
     *   [리스트 비어 있음] → 비콘 수신이 완전히 끊김
     *     lastBeacon == 3(엘리베이터) → WholeTimer 강제 종료 (출차 확정)
     *     lastBeacon != 3            → 아직 불확실 → 리스트 초기화 후 재귀 대기
     *
     *   [리스트 비어 있지 않음] → 아직 비콘 수신 중
     *     마지막 수신 비콘을 lastBeacon에 저장 → 리스트 초기화 → 재귀 대기
     *
     * cancelCollectLobbyTimer() 호출 시 (입구 재감지 등 강제 취소):
     *   재귀 루프 즉시 중단, WholeTimer는 유지
     */
    fun startCollectLobbyTimer() {
        if (isCollectLobbyRunning) return
        isCollectLobbyRunning = true
        Timber.d("BeaconTimerManager: CollectLobbyTimer 시작")
        collectLobbyIteration()
    }

    private fun collectLobbyIteration() {
        collectLobbyTimerJob = scope.launch {
            delay(3_000L)

            // 취소된 경우 (cancelCollectLobbyTimer 호출) → 즉시 종료
            if (!isCollectLobbyRunning) return@launch

            isCollectLobbyRunning = false

            if (state.inoutDataMajor.isEmpty()) {
                // 3초간 비콘 없음 → lastBeacon 기준으로 종료 여부 판단
                Timber.d("BeaconTimerManager: CollectLobby inoutDataMajor 비어있음 lastBeacon=${state.lastBeacon}")
                if (state.lastBeacon == 3) {
                    // 마지막이 엘리베이터(3) → 출차 확정 → WholeTimer 종료
                    Timber.d("BeaconTimerManager: CollectLobby lastBeacon=3 → WholeTimer 강제 종료")
                    finishWholeTimer()
                } else {
                    // 아직 불확실 → 재귀 대기
                    Timber.d("BeaconTimerManager: CollectLobby lastBeacon=${state.lastBeacon} → 재귀 대기")
                    state.clearInoutDataMajor()
                    isCollectLobbyRunning = true
                    collectLobbyIteration()
                }
            } else {
                // 3초 내 비콘 수신 있음 → 마지막 값 기록 후 재귀 대기
                Timber.d("BeaconTimerManager: CollectLobby 비콘 수신 중 (last=${state.inoutDataMajor.last()}) → 재귀")
                state.consumeInoutDataMajor()   // lastBeacon 갱신 + 리스트 초기화
                isCollectLobbyRunning = true
                collectLobbyIteration()
            }
        }
    }

    /** 강제 취소: 입구 재감지 등으로 CollectLobby를 중단하되 WholeTimer는 유지 */
    fun cancelCollectLobbyTimer() {
        collectLobbyTimerJob?.cancel()
        collectLobbyTimerJob  = null
        isCollectLobbyRunning = false
        state.clearInoutDataMajor()
        Timber.d("BeaconTimerManager: CollectLobbyTimer 강제 취소")
    }

    // ------------------------------------------------------------------------
    // StayRestartTimer (900초, 15분)
    // 재입차 가드 타이머: 로비 비콘 감지 후 열리는 재입차 기회 창(window)
    // 이 시간 안에 Major=6 비콘이 오면 processParkingEntrance에서 WholeTimer 시작
    // 900초가 지나도록 재입차가 없으면 onFinish에서 상태만 리셋 (API 호출 없음)
    // 원본: TimerSingleton.StartStayRestart() 900*1000ms, onFinish = 상태 리셋만
    // ------------------------------------------------------------------------
    fun startStayRestartTimer() {
        if (isStayRestartRunning) return
        isStayRestartRunning = true
        Timber.d("BeaconTimerManager: StayRestartTimer 시작 (900초 — 재입차 기회 창 오픈)")

        stayRestartTimerJob = scope.launch {
            delay(900_000L) // 원본 TimerSingleton.StartStayRestart: 900 * 1000ms
            Timber.d("BeaconTimerManager: StayRestartTimer 만료 → 재입차 없음, 상태 리셋")
            isStayRestartRunning = false
            // 900초 내 재입차(Major=6)가 없었으므로 상태만 초기화
            // API 호출 없음 — 원본 onFinish와 동일
            state.isRestartBeacon = false
            state.isEnd1Beacon   = false
            state.isEnd2Beacon   = false

        }
    }

    fun cancelStayRestartTimer() {
        stayRestartTimerJob?.cancel()
        stayRestartTimerJob  = null
        isStayRestartRunning = false
    }

    // ------------------------------------------------------------------------
    // AfterLobbyElevatorTimer (10초 반복 재귀)
    // 출차 준비 후 엘리베이터 비콘 수신이 완전히 끊길 때까지 대기
    // ------------------------------------------------------------------------

    /**
     * startAfterLobbyElevatorTimer (원본 AFTER_LOBBY_ELEVATOR_TIMER)
     *
     * CollectLobbyTimer와 동일한 재귀 구조:
     *   10초마다 afterLobbyEleCount 확인
     *
     *   [count == 0] → 10초간 비콘 수신 없음
     *     WholeTimer 동작 중 && elevatorBeaconGet == false
     *       → isLobbyBeaconEnd = true → finishWholeTimer() (엘리베이터 없이 로비만 감지)
     *     그 외 (elevatorBeaconGet == true)
     *       → 정상 출차 진행 중 → 타이머만 종료 (End1 리셋 없음)
     *
     *   [count != 0] → 비콘 수신 중
     *     → afterLobbyEleCount 리셋 → 재귀 (10초 다시 대기)
     */
    fun startAfterLobbyElevatorTimer() {
        if (isAfterLobbyElevatorRunning) return
        isAfterLobbyElevatorRunning = true
        state.afterLobbyEleCount = 0
        Timber.d("BeaconTimerManager: AfterLobbyElevatorTimer 시작")
        afterLobbyElevatorIteration()
    }

    private fun afterLobbyElevatorIteration() {
        afterLobbyElevatorJob = scope.launch {
            delay(10_000L)

            if (!isAfterLobbyElevatorRunning) return@launch

            isAfterLobbyElevatorRunning = false

            if (state.afterLobbyEleCount == 0) {
                // 10초간 비콘 없음
                if (isWholeTimerRunning && !state.elevatorBeaconGet) {
                    // 엘리베이터 비콘 미수신 → isLobbyBeaconEnd 마킹 후 WholeTimer 강제 종료
                    Timber.w("BeaconTimerManager: AfterLobbyElevator - 엘리베이터 미수신 → isLobbyBeaconEnd=true, WholeTimer 종료")
                    state.isLobbyBeaconEnd = true
                    finishWholeTimer()
                } else {
                    // elevatorBeaconGet==true (정상 출차 진행) 또는 WholeTimer 미동작 → 그냥 종료
                    Timber.d("BeaconTimerManager: AfterLobbyElevator 종료 (elevatorBeaconGet=${state.elevatorBeaconGet})")
                }
            } else {
                // 비콘 수신 중 → count 리셋 후 재귀 대기
                Timber.d("BeaconTimerManager: AfterLobbyElevator 비콘 수신 중 (count=${state.afterLobbyEleCount}) → 재귀")
                state.afterLobbyEleCount = 0
                isAfterLobbyElevatorRunning = true
                afterLobbyElevatorIteration()
            }
        }
    }

    fun cancelAfterLobbyElevatorTimer() {
        afterLobbyElevatorJob?.cancel()
        afterLobbyElevatorJob       = null
        isAfterLobbyElevatorRunning = false
        state.afterLobbyEleCount    = 0
    }

    // ------------------------------------------------------------------------
    // AfterAccelTimer (원본 AFTER_ACCEL_START_TIMER)
    // 10초 동안 1초마다 afterAccelCount 확인
    // Major=4 비콘이 1개라도 잡히면(count != 0) → WholeTimer 시작 + GateInformation
    // 10초 내 비콘 없으면 만료 → 10회마다 GyroInformation 로그 전송
    // ------------------------------------------------------------------------
    fun startAfterAccelTimer() {
        if (isAfterAccelRunning) return
        isAfterAccelRunning = true
        state.afterAccelCount = 0  // 타이머 시작 시 카운터 초기화
        Timber.d("BeaconTimerManager: AfterAccelTimer 시작 (10초)")

        Log.d("TEST", "AfterAccelTimer 시작 (10초)")
        afterAccelTimerJob = scope.launch {
            // 1초마다 afterAccelCount 확인 (원본 onTick 1000ms)
            repeat(10) { tick ->
                delay(1_000L)
                if (!isAfterAccelRunning) return@launch  // 취소된 경우
                Log.d("TEST", "AfterAccelTimer tick=${tick+1}/10, afterAccelCount=${state.afterAccelCount}")

                if (state.afterAccelCount != 0 && !isWholeTimerRunning) {
                    Timber.d("BeaconTimerManager: AfterAccelTimer — 비콘 감지(${state.afterAccelCount}) → WholeTimer 시작")
                    state.recordInputTime()
                    RestController.instance.Message("주차서비스 시작")
                    isAfterAccelRunning = false
                    
                    startWholeTimer()
                    return@launch  // WholeTimer 시작 후 즉시 종료
                }
            }
            // 10초 내 비콘 없이 만료 (원본 onFinish)
            isAfterAccelRunning = false
            accelSendCount++
            if (accelSendCount >= 10) {  // 원본 increaseAccelSendCount(): 10회마다 true
                accelSendCount = 0
                Timber.d("BeaconTimerManager: AfterAccelTimer 10회 만료 — afterAccelCount=${state.afterAccelCount}")
                // 원본은 GyroInformation 주석 처리됨 (2024.08.05) → 로그만 남김
            } else {
                Timber.d("BeaconTimerManager: AfterAccelTimer 만료 (비콘 없음) sendCount=$accelSendCount")
            }
        }
    }

    fun cancelAfterAccelTimer() {
        afterAccelTimerJob?.cancel()
        afterAccelTimerJob  = null
        isAfterAccelRunning = false
    }

    // ------------------------------------------------------------------------
    // AfterGyroTimer — AccelBeacon 수집 윈도우 활성화용
    // 자이로 회전 감지(collectGyroFiltered) 후 isAfterGyroRunning=true 구간 동안
    // processStayParking에서 AccelBeacon 수집 활성화 (WholeTimer 시작과 무관)
    // 원본 AFTER_GYRO_START_TIMER는 현재 RN에서도 주석 처리된 Dead Code
    // → WholeTimer 시작 용도로 사용하지 않음
    // ------------------------------------------------------------------------

    fun startAfterGyroTimer() {
        afterGyroTimerJob?.cancel()
        isAfterGyroRunning = true
        Timber.d("BeaconTimerManager: AfterGyroTimer 시작")
        Log.d("TEST", "gyro 시작 isAfterGyroRunning true")

        afterGyroTimerJob = scope.launch {
            delay(5_000L)
            isAfterGyroRunning = false
            Timber.d("BeaconTimerManager: AfterGyroTimer 완료")
        }
    }

    fun cancelAfterGyroTimer() {
        afterGyroTimerJob?.cancel()
        afterGyroTimerJob  = null
        isAfterGyroRunning = false
    }

    // ------------------------------------------------------------------------
    // CollectAccelBeaconTimer
    // Major=5(주차면 변화) 감지 후 accel 비콘 수집 윈도우 활성화
    // ------------------------------------------------------------------------

    fun setCollectAccelBeacon(active: Boolean) {
        if (active) {
            if (isCollectAccelBeaconRunning) return
            isCollectAccelBeaconRunning = true
            collectAccelBeaconTimerJob?.cancel()
            collectAccelBeaconTimerJob = scope.launch {
                delay(10_000L)
                isCollectAccelBeaconRunning = false
                Timber.d("BeaconTimerManager: CollectAccelBeaconTimer 완료")
            }
        } else {
            collectAccelBeaconTimerJob?.cancel()
            collectAccelBeaconTimerJob  = null
            isCollectAccelBeaconRunning = false
        }
    }

    // ------------------------------------------------------------------------
    // 수동 주차 (Passive) 일시정지 / 재가동
    // ------------------------------------------------------------------------

    /** passiveParking 호출 시: WholeTimer 실행 중이면 멈추고 상태 기억 */
    fun pauseForPassive() {
        wasWholeTimerRunningBeforePassive = isWholeTimerRunning
        if (isWholeTimerRunning) {
            cancelWholeTimer()
            Timber.d("$TAG: [Passive] WholeTimer 일시정지 (수동주차 시작)")
        }
    }

    /** passiveParkingEnd 호출 시: 이전에 실행 중이었으면 처음부터 재시작 */
    fun resumeAfterPassive() {
        if (wasWholeTimerRunningBeforePassive) {
            wasWholeTimerRunningBeforePassive = false
            startWholeTimer()
            Timber.d("$TAG: [Passive] WholeTimer 재가동 (수동주차 종료)")
        } else {
            wasWholeTimerRunningBeforePassive = false
            Timber.d("$TAG: [Passive] WholeTimer 비실행 상태였으므로 재가동 생략")
        }
    }

    // ------------------------------------------------------------------------
    // 전체 취소
    // ------------------------------------------------------------------------

    fun cancelAll() {
        cancelWholeTimer()
        cancelCalcTimer()
        cancelLobbyTimer()
        cancelCollectLobbyTimer()
        cancelStayRestartTimer()
        cancelAfterLobbyElevatorTimer()
        cancelAfterAccelTimer()
        cancelAfterGyroTimer()
        setCollectAccelBeacon(false)
        Timber.d("BeaconTimerManager: 모든 타이머 취소")
    }
}
