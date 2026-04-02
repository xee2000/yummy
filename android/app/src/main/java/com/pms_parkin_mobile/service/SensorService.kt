package com.pms_parkin_mobile.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.os.IBinder
import com.pms_parkin_mobile.api.RestController
import com.pms_parkin_mobile.data.SensorDataStore
import com.woorisystem.domain.AccelSensorData
import com.woorisystem.domain.AccelSensorData2
import com.woorisystem.domain.GyroSensorData
import com.woorisystem.domain.GyroSensorData2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SensorService
 *
 * 기존 SensorService.java Kotlin 재작성
 *
 * ── 역할 ──────────────────────────────────────────────────────────────────
 *   1. 가속도 센서 (TYPE_ACCELEROMETER)
 *      - CVA(Combined Vector Acceleration) 계산 → 변화량이 임계값 이상이면 accelCount 증가
 *      - AccelTimer (1초 반복) → accelCount 기반 T/S/W 상태 판별 → AccelSensorData 수집
 *      - 운전 중 판별: 중력 제거 후 0.1초 단위 평균 → 범위 내 진동이면 드라이브로 판단
 *        (현재 운전 판별 결과는 수집하지 않음. BeaconProcessor의 AfterAccelTimer로 대체됨)
 *
 *   2. 자이로 센서 (TYPE_GYROSCOPE)
 *      - WholeTimer 동작 중에만 수집
 *      - bigDataSend == false (기본): 칼만 필터 적용 → Roll/Pitch/Yaw 큐 기반 회전 감지
 *        → 회전 종료 순간(IMatchValue 발생) SaveGyro() 호출
 *      - bigDataSend == true : 필터 없이 원시값 즉시 수집 + GyroSensorData2 추가 수집
 *
 * ── 원본 대비 개선 사항 ───────────────────────────────────────────────────
 *   제거: NetworkStateReceiver 등록 → SensorService는 센서만 담당. 네트워크 감시는
 *        BluetoothService 또는 별도 레이어에서 처리하는 것이 책임 분리 원칙에 맞음
 *   제거: drive_con_count 활용 로직 → 원본에서 이미 전부 주석 처리된 Dead Code.
 *        비콘 기반 AfterAccelTimer로 완전히 대체되었으므로 제거
 *   제거: AFTER_GYRO_START_TIMER / StartWholeTimer 자이로 트리거 → 원본에서 주석 처리.
 *        현재 WholeTimer 시작은 비콘(Major=2,6)으로만 동작
 *   개선: CountDownTimer 반복 패턴 → 코루틴 기반 AccelTimer로 교체
 *        (cancel 레이스컨디션, 메모리 누수 해소)
 *   개선: DataManagerSingleton 직접 접근 → ParkingStateManager / SensorDataStore 경유
 *   개선: 자이로 Queue 로직의 toArray() 캐스팅 → ArrayDeque<Double>로 타입 안전하게 교체
 *   개선: filter() 함수의 ALPHA 상수 오용 버그 수정
 *        → 원본은 KeyCharacterMap.ALPHA(= 'A' = 65)를 import해서 사용하는 버그 존재.
 *          실제 의도는 0.8f 로우패스 필터 계수이므로 명시적 상수로 수정
 *   개선: KalmanFilter를 inner class로 통합 (별도 파일 불필요)
 *   개선: static boolean mSensorServiceRunning → companion object + @Volatile
 */
@SuppressLint("MissingPermission")
class SensorService : Service(), SensorEventListener {

    // =========================================================================
    // Companion / 상수
    // =========================================================================
    companion object {
        private const val TAG = "SensorService"

        /** 로우패스 필터 계수 (중력 성분 분리용). 원본의 alpha = 0.8f */
        private const val LOW_PASS_ALPHA = 0.8f

        /** CVA 변화량 임계값. 이 값 이상이면 진동(움직임)으로 판단 */
        private const val CVA_THRESHOLD = 4.0f

        private val rollAxis = GyroAxisState()
        private val pitchAxis = GyroAxisState()
        private val yawAxis = GyroAxisState()

        /** 운전 판별: 0.1초(10 샘플) 주기 */
        private const val DRIVE_SAMPLE_WINDOW = 10

        /** 운전 판별: 중력 제거 후 축별 진동이 이 범위 안에 있으면 드라이빙 진동으로 판단 */
        private const val DRIVE_ACCEL_MIN = 0.02f
        private const val DRIVE_ACCEL_MAX = 0.4f

        /** 자이로 큐 윈도우 크기 */
        private const val GYRO_QUEUE_SIZE = 4

        /** 자이로 정지 판단 임계값 (이 범위 안이면 미세 진동 → 정지로 간주) */
        private const val GYRO_STILL_THRESHOLD = 0.025f

        /** 자이로 회전 카운트가 이 값 이상이어야 유효한 회전으로 인정 */
        private const val GYRO_ROTATION_LIMIT = 100

        /** 자이로 급격한 변화 한계 (이 범위 밖이면 칼만 필터 초기화) */
        private const val GYRO_SPIKE_LIMIT = 0.5f

        /** AccelTimer 주기 (ms) */
        private val parkingState get() = BluetoothService.getInstance()?.parkingState

        private const val ACCEL_TIMER_INTERVAL_MS = 1_000L

        /**
         * CollectAccelBeacon 트리거에 필요한 연속 S/W 횟수
         *
         * SENSOR_DELAY_NORMAL(200ms)에서 1초에 최대 5샘플
         * → accelCount 12 이상인 "W"는 사실상 불가. T/S만 구분됨
         * → T→S 1회 전환으로 즉시 트리거 시 오탐 원인:
         *     보행(주차장 내 이동), 폰 꺼내는 동작, 버스 탑승
         *
         * 3초 연속 S/W → 순간 충격/보행 필터링, 지속 진동(운전)만 통과
         * 예) 폰 꺼냄(1초 S) → 차단 / 차량 운전(3초+ S) → 통과
         */
        private const val ACCEL_MOTION_CONFIRM_COUNT = 3

        @Volatile
        private var instance: SensorService? = null

        fun getInstance(): SensorService? = instance
        fun isRunning(): Boolean = instance != null
    }

    // =========================================================================
    // 센서
    // =========================================================================
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    val sensorDataStore = SensorDataStore()
    // =========================================================================
    // 가속도 — CVA 계산용
    // =========================================================================
    /** 로우패스 필터 통과 후 값 (이전 샘플) */
    private var prevCva: Float = 0f

    /** 중력 성분 누적 (로우패스) */
    private val gravity = FloatArray(3)

    /** 중력 제거 후 선형 가속도 누적 (운전 판별용) */
    private val linearAccelAcc = FloatArray(3)   // x,y,z 누적 합

    /** 운전 판별 샘플 인덱스 (0~9) */
    private var driveAccelIndex = 0

    // =========================================================================
    // 자이로 — 칼만 필터
    // =========================================================================
    private val kalmanX = KalmanFilter()
    private val kalmanY = KalmanFilter()
    private val kalmanZ = KalmanFilter()

    // =========================================================================
    // 자이로 — 회전 감지용 슬라이딩 큐
    // =========================================================================
    /**
     * 자이로 축별 상태를 하나의 객체로 묶어 관리
     * 람다 파라미터 과다 문제 해소 + 축별 상태가 한눈에 보이도록 개선
     */
    private data class GyroAxisState(
        val queue: ArrayDeque<Double> = ArrayDeque(GYRO_QUEUE_SIZE),
        var count: Int = 0,
        var preCount: Int = 0,
        var flag: Boolean = false,
    )



    /**
     * 회전 이벤트 발생 횟수 누적
     * IMatchValue != 0 일 때 회전 종료를 감지하고 SaveGyro 호출
     */
    private var iMatchValue = 0

    // =========================================================================
    // AccelTimer (코루틴 기반, 1초 반복)
    // =========================================================================
    private val scope = CoroutineScope(Dispatchers.Main)
    private var accelTimerJob: Job? = null

    /** AccelTimer 동작 중 여부 */
    @Volatile
    private var isAccelTimerRunning = false

    // =========================================================================
    // 외부 주입: ParkingStateManager (BluetoothService 공유 인스턴스)
    // =========================================================================
    /**
     * SensorService는 BluetoothService와 ParkingStateManager를 공유해야 합니다.
     * BluetoothService.getInstance()?.beaconProcessor?.state 를 통해 접근합니다.
     *
     * 직접 의존성을 줄이기 위해 getter 형태로 접근합니다.
     */
    private val parkingState get() = BluetoothService.getInstance()?.parkingState

    // =========================================================================
    // 센서 수집 데이터 저장소 (SensorDataStore로 분리)
    // WholeTimer onFinish 시 여기서 데이터를 꺼내 Total을 구성합니다.
    // =========================================================================
    // =========================================================================
    // Service Lifecycle
    // =========================================================================
    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.i("$TAG: onCreate")
        Log.d("TEST", "Sennsor Start")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // SENSOR_DELAY_NORMAL ≈ 200ms 간격. 원본은 SENSOR_STATUS_ACCURACY_LOW를
        // 잘못된 위치(registerListener delay 파라미터)에 사용했는데, 이는 API 오용.
        // delay 파라미터에는 SensorManager.SENSOR_DELAY_* 상수를 사용해야 함.
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }


    override fun onDestroy() {
        sensorManager.unregisterListener(this, gyroSensor)
        sensorManager.unregisterListener(this, accelSensor)
        accelTimerJob?.cancel()
        accelTimerJob = null
        isAccelTimerRunning = false
        instance = null
        Timber.i("$TAG: onDestroy")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================================================================
    // SensorEventListener
    // =========================================================================
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* 사용 안 함 */
    }

    override fun onSensorChanged(event: SensorEvent) {
        // WholeTimer 미동작 시 센서 처리 불필요
        val state = parkingState ?: return
        if (!state.isWorkingState) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> onAccelerometerChanged(event)
            Sensor.TYPE_GYROSCOPE -> onGyroscopeChanged(event)
        }
    }

    // =========================================================================
    // 가속도 처리
    // =========================================================================
    private fun onAccelerometerChanged(event: SensorEvent) {
        // ── CVA 계산 (주차 상태 T/S/W 분류용) ──────────────────────────────────
        val filtered = lowPassFilter(event.values)
        val cva = sqrt(filtered[0] * filtered[0] + filtered[1] * filtered[1] + filtered[2] * filtered[2])

        if (prevCva != 0f && abs(prevCva - cva) >= CVA_THRESHOLD) {
            sensorDataStore.incrementAccelCount()
        }
        prevCva = cva

        // AccelTimer 시작 (미동작 시에만)
        if (!isAccelTimerRunning) startAccelTimer()

        // ── 운전 중 판별 (진동 범위 감지) ─────────────────────────────────────
        // 결과는 현재 사용되지 않음 (비콘 기반 AfterAccelTimer로 대체)
        // 향후 활용을 위해 로직은 유지하되 부작용 없이 보존
        updateDriveDetection(event.values)
    }

    /**
     * 로우패스 필터 (중력 성분 제거)
     * 원본의 filter() 함수. 원본에서 ALPHA(=KeyCharacterMap.ALPHA='A'=65)를 잘못
     * import하여 실제로는 output[i] = output[i] + 65 * (input[i] - output[i]) 로
     * 동작하는 버그가 있었음. 올바른 계수 LOW_PASS_ALPHA(0.8f)로 수정.
     */
    private val lpfBuffer = FloatArray(3)
    private fun lowPassFilter(input: FloatArray): FloatArray {
        for (i in lpfBuffer.indices) {
            lpfBuffer[i] = lpfBuffer[i] + LOW_PASS_ALPHA * (input[i] - lpfBuffer[i])
        }
        return lpfBuffer
    }

    /**
     * 운전 중 판별 로직
     * 10 샘플(≈0.1초) 단위로 평균 내어 진동 범위 확인
     * drive_con_count 활용 코드는 원본에서 전부 주석 처리된 Dead Code이므로 제거.
     * 판별 결과(isDriving)는 향후 활용 시점에 연결할 수 있도록 계산만 보존.
     */
    private fun updateDriveDetection(rawValues: FloatArray) {
        if (driveAccelIndex >= DRIVE_SAMPLE_WINDOW - 1) {
            val avgX = linearAccelAcc[0] / DRIVE_SAMPLE_WINDOW
            val avgY = linearAccelAcc[1] / DRIVE_SAMPLE_WINDOW
            val avgZ = linearAccelAcc[2] / DRIVE_SAMPLE_WINDOW

            @Suppress("UNUSED_VARIABLE")
            val isDriving =
                (avgX in DRIVE_ACCEL_MIN..DRIVE_ACCEL_MAX) || (avgY in DRIVE_ACCEL_MIN..DRIVE_ACCEL_MAX) || (avgZ in DRIVE_ACCEL_MIN..DRIVE_ACCEL_MAX)

            // 리셋
            linearAccelAcc[0] = 0f
            linearAccelAcc[1] = 0f
            linearAccelAcc[2] = 0f
            driveAccelIndex = 0
        } else {
            // 중력 제거
            gravity[0] = LOW_PASS_ALPHA * gravity[0] + (1 - LOW_PASS_ALPHA) * rawValues[0]
            gravity[1] = LOW_PASS_ALPHA * gravity[1] + (1 - LOW_PASS_ALPHA) * rawValues[1]
            gravity[2] = LOW_PASS_ALPHA * gravity[2] + (1 - LOW_PASS_ALPHA) * rawValues[2]

            linearAccelAcc[0] += abs(rawValues[0] - gravity[0])
            linearAccelAcc[1] += abs(rawValues[1] - gravity[1])
            linearAccelAcc[2] += abs(rawValues[2] - gravity[2])
            driveAccelIndex++
        }
    }

    // =========================================================================
    // AccelTimer — 1초마다 T/S/W 상태 판별 및 수집
    // =========================================================================
    /**
     * AccelTimer (코루틴 기반 1초 반복)
     *
     * 원본의 CountDownTimer 1초 반복 → 완료 후 자기 자신 재호출 패턴을
     * 코루틴 while 루프로 교체. cancel 레이스컨디션 해소.
     *
     * T/S/W 분류 기준 (원본 AccelSensorResult() 동일):
     *   accelCount < 3            → "T" (정지, Trivial)
     *   3 ≤ accelCount < 12      → "S" (보행, Slow)
     *   accelCount ≥ 12          → "W" (운전, Walking/Wheeling)
     */
    private fun startAccelTimer() {
        if (isAccelTimerRunning) return
        isAccelTimerRunning = true
        Timber.d("$TAG: AccelTimer 시작")

        accelTimerJob = scope.launch {
            var prevState: String? = null
            var motionConsecutiveCount = 0  // S/W 연속 횟수 카운터
            var motionStartedFromT = false   // T→S/W 전환이 시작된 상태인지
            var seq = sensorDataStore.accelSequence

            while (parkingState?.isWorkingState == true) {
                delay(ACCEL_TIMER_INTERVAL_MS)

                val count = sensorDataStore.accelCount
                val state = accelCountToState(count)
                val delay = parkingState?.wholeTimerDelay ?: 0

                // AccelSensorData (T/S/W 상태)
                sensorDataStore.addAccelSensorData(
                    AccelSensorData(
                        state = state,
                        delay = delay.toString(),
                        seq = seq.toString(),
                    )
                )

                // AccelSensorData2 (실측 카운트값)
                sensorDataStore.addAccelSensorData2(
                    AccelSensorData2(
                        state = count.toString(),
                        delay = delay.toString(),
                        seq = seq.toString(),
                    )
                )

                Timber.v("$TAG: AccelTimer seq=$seq state=$state count=$count delay=$delay")

                // S/W 연속 횟수 추적
                if (state == "S" || state == "W") {
                    // T에서 막 전환됐으면 플래그 세팅
                    if (motionConsecutiveCount == 0 && prevState == "T") {
                        motionStartedFromT = true
                    }
                    motionConsecutiveCount++
                } else {
                    // T로 돌아오면 리셋
                    motionConsecutiveCount = 0
                    motionStartedFromT = false
                }
                // T에서 출발해서 N초 연속 S/W가 유지된 경우에만 트리거
                if (motionStartedFromT && motionConsecutiveCount == ACCEL_MOTION_CONFIRM_COUNT) {
                    BluetoothService.getInstance()?.beaconTimers?.setCollectAccelBeacon(true)
                    Timber.d("$TAG: T→S/W ${ACCEL_MOTION_CONFIRM_COUNT}초 지속 확인 → CollectAccelBeacon 시작")
                    motionStartedFromT = false  // 한 번만 트리거
                }

                prevState = state
                seq++
                sensorDataStore.accelSequence = seq
                sensorDataStore.resetAccelCount()
            }

            isAccelTimerRunning = false
            Timber.d("$TAG: AccelTimer 종료 (WholeTimer 미동작)")
        }
    }

    /** accelCount → T/S/W 문자열 변환 (원본 AccelSensorResult() 동일 기준) */
    private fun accelCountToState(count: Int): String = when {
        count < 3 -> "T"
        count < 12 -> "S"
        else -> "W"
    }

    // =========================================================================
    // 자이로 처리
    // =========================================================================
    private fun onGyroscopeChanged(event: SensorEvent) {
        val roll = event.values[0].toDouble()
        val pitch = event.values[1].toDouble()
        val yaw = event.values[2].toDouble()


            // bigDataSend: 원시값을 즉시 GyroSensorData로 수집 + GyroSensorData2도 수집
            collectGyroRaw(roll, pitch, yaw)

    }

    /**
     * bigDataSend 모드: 원시값 즉시 수집
     * GyroSensorData(소수 2자리 절삭) + GyroSensorData2(동일 절삭값) 모두 추가
     * 원본의 noFormatVersionGyroSensorResult()에 해당
     */
    private fun collectGyroRaw(roll: Double, pitch: Double, yaw: Double) {
        val delay = Companion.parkingState?.wholeTimerDelay?.toString() ?: "0"

        sensorDataStore.addGyroSensorData(
            GyroSensorData(
                x = cutTo2(roll).toString(),
                y = cutTo2(pitch).toString(),
                z = cutTo2(yaw).toString(),
                delay = delay,
            )
        )
        // bigData 모드에서는 같은 값을 GyroSensorData2에도 수집 (원본 동일)
        collectGyroFiltered(roll, pitch, yaw)

    }

    /**
     * 표준 모드: 슬라이딩 큐 기반 회전 감지 후 SaveGyro
     * 원본의 formatVersionGyroSensorResult()에 해당
     *
     * 큐 로직 원본 버그 수정:
     *   원본은 Queue.poll()을 4번 호출해서 head를 포함한 값들을 꺼낸 후
     *   head를 제외한 나머지 3개만 다시 넣는 패턴인데,
     *   이는 LinkedList 기반 Queue에서 poll이 head부터 제거함을 이용한 것임.
     *   실제 동작: [A,B,C,D] → poll×4 → [] → offer(B,C,D,new) → [B,C,D,new]
     *   즉, head(A)를 버리고 new를 뒤에 추가하는 슬라이딩 윈도우.
     *   Kotlin ArrayDeque로 명확하게 재구현: removeFirst() + addLast()
     */
    private fun collectGyroFiltered(roll: Double, pitch: Double, yaw: Double) {
        updateGyroAxis(rollAxis, roll)
        updateGyroAxis(pitchAxis, pitch)
        updateGyroAxis(yawAxis, yaw)

        // 2. 조건 분석용 변수 추출
        val isLimitReached = (rollAxis.preCount >= GYRO_ROTATION_LIMIT ||
                pitchAxis.preCount >= GYRO_ROTATION_LIMIT ||
                yawAxis.preCount >= GYRO_ROTATION_LIMIT)

        val isStoppedNow = ((rollAxis.count == 0 && rollAxis.preCount != 0) ||
                (pitchAxis.count == 0 && pitchAxis.preCount != 0) ||
                (yawAxis.count == 0 && yawAxis.preCount != 0))

        val rotationEnded = isLimitReached && isStoppedNow

        // 상세 분석 로그
        if (rollAxis.count > 0 || pitchAxis.count > 0 || yawAxis.count > 0 || isStoppedNow) {
            Log.v("GYRO_DEBUG", "--- [회전 분석 중] ---")
            Log.v("GYRO_DEBUG", "Roll  | Count: ${rollAxis.count}, Pre: ${rollAxis.preCount}")
            Log.v("GYRO_DEBUG", "Pitch | Count: ${pitchAxis.count}, Pre: ${pitchAxis.preCount}")
            Log.v("GYRO_DEBUG", "Yaw   | Count: ${yawAxis.count}, Pre: ${yawAxis.preCount}")
            Log.v("GYRO_DEBUG", "상태  | LimitReached: $isLimitReached, StoppedNow: $isStoppedNow -> Result: $rotationEnded")
        }

        if (rotationEnded) {
            Log.d("GYRO_DEBUG", "✅ [회전 종료 감지] iMatchValue 증가 시도")
            iMatchValue++
            sensorDataStore.saveCountRoll = rollAxis.preCount
            sensorDataStore.saveCountPitch = pitchAxis.preCount
            sensorDataStore.saveCountYaw = yawAxis.preCount

            // 리셋 전 로그 보존
            Log.i("GYRO_DEBUG", "저장될 카운트 - R:${rollAxis.preCount}, P:${pitchAxis.preCount}, Y:${yawAxis.preCount}")

            rollAxis.flag = false; rollAxis.preCount = 0
            pitchAxis.flag = false; pitchAxis.preCount = 0
            yawAxis.flag = false; yawAxis.preCount = 0
        } else {
            if (iMatchValue != 0) {
                Log.d("GYRO_DEBUG", "🚀 [saveGyro 호출] iMatchValue: $iMatchValue")
                saveGyro()
                RestController.instance.Message("자이로반응확인")

                val btService = BluetoothService.getInstance()
                if (btService?.parkingState?.isWorkingState == true) {
                    btService.beaconTimers.startAfterGyroTimer()
                    Timber.d("$TAG: 자이로 회전 종료 -> AfterGyroTimer 시작")
                }
                iMatchValue = 0
                // 아래 count 리셋은 이미 rotationEnded에서 0이 되었겠지만 명시적으로 처리
                rollAxis.count = 0
                pitchAxis.count = 0
                yawAxis.count = 0
            }
        }
    }

    /**
     * 축별 자이로 큐 업데이트 공통 로직
     * 큐가 꽉 차면 head 제거 후 새 값 추가 (슬라이딩 윈도우)
     */
    private fun updateGyroAxis(axis: GyroAxisState, newValue: Double) {
        if (axis.queue.size == GYRO_QUEUE_SIZE) {
            val stillThreshold = GYRO_STILL_THRESHOLD.toDouble()
            val allMoving = axis.queue.none { it in -stillThreshold..stillThreshold }
            if (allMoving) {
                axis.count++
            } else {
                if (axis.count >= GYRO_ROTATION_LIMIT) {
                    axis.flag = true
                    axis.preCount = axis.count
                }
                axis.count = 0
            }
            axis.queue.removeFirst()
        }
        axis.queue.addLast(newValue)
    }

    /**
     * 회전 종료 시 자이로 데이터 저장
     * 원본 SaveArrayListValue.SaveGyro() / SaveGyro2() 역할
     *
     * bigDataSend 여부:
     *   - false: GyroSensorData만 추가 (Roll/Pitch/Yaw 카운트값)
     *   - true : GyroSensorData2에 추가 (bigData 모드에서는 원시값을 real-time으로
     *            이미 GyroSensorData에 추가했으므로 GyroSensorData2에는 카운트값 추가)
     */
    private fun saveGyro() {
        val delay = parkingState?.wholeTimerDelay?.toString() ?: "0"
        val gyro = GyroSensorData(
            x = sensorDataStore.saveCountRoll.toString(),
            y = sensorDataStore.saveCountPitch.toString(),
            z = sensorDataStore.saveCountYaw.toString(),
            delay = delay,
        )

        Log.d("TEST", "자이로 확인")
            sensorDataStore.addGyroSensorData2(
                GyroSensorData2(
                    x = gyro.x,
                    y = gyro.y,
                    z = gyro.z,
                    delay = gyro.delay,
                )
            )

    }

    // =========================================================================
    // 소수 2자리 절삭 (원본 cutTo2())
    // =========================================================================
    private fun cutTo2(value: Double): Double {
        val factor = 100.0
        return (value * factor).toLong() / factor
    }

    // =========================================================================
    // 상태 초기화 (WholeTimer 시작 시 호출)
    // =========================================================================
    fun resetSensorState() {
        prevCva = 0f
        gravity.fill(0f)
        linearAccelAcc.fill(0f)
        driveAccelIndex = 0
        rollAxis.queue.clear(); rollAxis.count = 0; rollAxis.preCount = 0; rollAxis.flag = false
        pitchAxis.queue.clear(); pitchAxis.count = 0; pitchAxis.preCount = 0; pitchAxis.flag = false
        yawAxis.queue.clear(); yawAxis.count = 0; yawAxis.preCount = 0; yawAxis.flag = false
        iMatchValue = 0
        kalmanX.init(); kalmanY.init(); kalmanZ.init()
        accelTimerJob?.cancel()
        accelTimerJob = null
        isAccelTimerRunning = false
        sensorDataStore.reset()
        Timber.d("$TAG: 센서 상태 초기화")
    }

    // =========================================================================
    // KalmanFilter (내부 클래스)
    // 원본 KalmanFilter.java와 동일 알고리즘, 별도 파일 불필요하므로 통합
    // =========================================================================
    private class KalmanFilter {
        private val q = 0.00001   // 프로세스 잡음
        private val r = 0.001    // 측정 잡음
        private var x = 0.0      // 상태 추정값
        private var p = 1.0      // 오차 공분산
        private var k = 0.0      // 칼만 게인

        fun update(value: Double): Double {
            // 측정 업데이트
            k = (p + q) / (p + q + r)
            p = r * (p + q) / (r + p + q)
            x += (value - x) * k
            return x
        }

        fun init() {
            x = 0.0; p = 0.0; k = 0.0
            update(0.0)
        }
    }
}
