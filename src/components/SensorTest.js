// src/components/SensorTestUI.js
import React, { useState, useMemo, useRef, useEffect } from 'react';
import {
  View,
  Image,
  StyleSheet,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  NativeModules,
  BackHandler,
  Alert,
} from 'react-native';

const { AndroidModule } = NativeModules;

const STEP_MODES = {
  center: '중앙',
  left: '좌측',
  right: '우측',
};

const SensorTest = ({ navigation }) => {
  // 'idle' | 'running' | 'done'
  const [phase, setPhase] = useState('idle');
  const [result, setResult] = useState(null); // true | false | null

  // UI 표시용(1..2..3초) - 실제 판정은 네이티브 wait로 한다
  const [count, setCount] = useState(0);
  const timerRef = useRef(null);

  // 어떤 단계인지: 'center' → 'left' → 'right'
  const [step, setStep] = useState('center');

  // 이미지 상태
  const [imageVariant, setImageVariant] = useState('stay');

  // 네이티브 호출 중복 방지 (연타/상태 꼬임 방지)
  const runningRef = useRef(false);

  const clearTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  // ✅ stop + 타이머 정리(뒤로가기/이탈 시 호출)
  const stopAndCleanup = async () => {
    runningRef.current = false;
    clearTimer();
    try {
      await AndroidModule?.SensorTestStop?.();
    } catch (e) {
      console.warn('SensorTestStop error:', e);
    }
  };

  // ✅ 초기 상태 체크 (선택사항: 이미 통과했으면 표시)
  useEffect(() => {
    const checkInitial = async () => {
      try {
        const r = await AndroidModule?.StayResult?.();
        if (r === true) {
          setResult(true);
          setImageVariant('stay_ok');
          setPhase('done');
          setStep('center');
        } else {
          setPhase('idle');
        }
      } catch (e) {
        setPhase('idle');
      }
    };

    checkInitial();

    return () => {
      clearTimer();
    };
  }, []);

  // ✅ 화면 나가기 직전 stop
  useEffect(() => {
    if (!navigation?.addListener) return;

    const unsubscribe = navigation.addListener('beforeRemove', () => {
      if (phase === 'running') stopAndCleanup();
    });

    return unsubscribe;
  }, [navigation, phase]);

  // ✅ 하드웨어 뒤로가기 stop
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (phase === 'running') stopAndCleanup();
      return false;
    });
    return () => sub.remove();
  }, [phase]);

  // ✅ 언마운트에서도 stop
  useEffect(() => {
    return () => {
      stopAndCleanup();
    };
  }, []);

  // 뱃지 상태
  const progress = useMemo(() => {
    const centerOn =
      step === 'center' ? phase !== 'idle' || result === true : true;

    const leftOn =
      step === 'center'
        ? false
        : step === 'left'
        ? phase !== 'idle' || result === true
        : true;

    const rightOn =
      step === 'right' ? phase !== 'idle' || result === true : false;

    return { center: centerOn, left: leftOn, right: rightOn };
  }, [step, phase, result]);

  // 안내 문구
  const instruction = useMemo(() => {
    if (step === 'center') {
      if (phase === 'idle') {
        return '테스트를 시작하세요.\n휴대폰을 정면으로 들고 가만히 두세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 정면으로 들고 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true)
          return '정면 테스트 완료!\n다음, 휴대폰을 좌측으로 돌려주세요.';
        if (result === false)
          return '테스트 종료: 정면 감지에 실패했습니다.\n다시 시도해주세요.';
        return '테스트 종료';
      }
    }

    if (step === 'left') {
      if (phase === 'idle') {
        return '휴대폰을 좌측 방향으로 돌려 들고\n테스트를 시작하세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 좌측 방향으로 돌려 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true)
          return '좌측 테스트 완료!\n다음, 휴대폰을 우측으로 돌려주세요.';
        if (result === false)
          return '테스트 종료: 좌측 감지에 실패했습니다.\n다시 시도해주세요.';
        return '테스트 종료';
      }
    }

    if (step === 'right') {
      if (phase === 'idle') {
        return '휴대폰을 우측 방향으로 돌려 들고\n테스트를 시작하세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 우측 방향으로 돌려 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true)
          return '우측 테스트까지 모두 완료되었습니다.\n테스트를 종료해 주세요.';
        if (result === false)
          return '테스트 종료: 우측 감지에 실패했습니다.\n다시 시도해주세요.';
        return '테스트 종료';
      }
    }

    return '';
  }, [step, phase, count, result]);

  // 이미지
  const phoneImageSource = useMemo(() => {
    if (imageVariant === 'stay_ok')
      return require('../assets/phone_stay_ok.png');
    return require('../assets/phone_stay.png');
  }, [imageVariant]);

  const phoneStyle = useMemo(() => {
    const transform = [];
    if (step === 'left') transform.push({ rotate: '-90deg' });
    if (step === 'right') transform.push({ rotate: '90deg' });
    return [styles.phone, transform.length ? { transform } : null];
  }, [step]);

  // ✅ 실제 테스트 시작 (네이티브 wait 기반)
  const startTest = async (targetStep = step) => {
    if (runningRef.current) return;
    runningRef.current = true;

    const mode = STEP_MODES[targetStep] || STEP_MODES.center;

    setResult(null);
    setImageVariant('stay');
    setCount(0);
    setPhase('running');

    // UI용 3초 카운트
    clearTimer();
    timerRef.current = setInterval(() => {
      setCount(prev => {
        const next = prev + 1;
        if (next >= 3) {
          clearTimer();
        }
        return next;
      });
    }, 1000);

    try {
      // ✅ 모드+시작을 한 번에
      await AndroidModule?.SensorTestStartWithMode?.(mode);

      // ✅ 여기서 “끝날 때까지 기다려서” 결과 받음 (타이밍 꼬임 해결 포인트)
      const ok = await AndroidModule?.SensorTestWaitResult?.(mode, 4500);

      setResult(!!ok);

      if (ok && targetStep === 'center') {
        setImageVariant('stay_ok');
      }

      // ✅ 우측까지 끝난 순간 최종 결과까지 확인
      if (targetStep === 'right') {
        try {
          const finalOk = await AndroidModule?.SensorTestResult?.();
          Alert.alert(
            '센서 테스트 최종 결과',
            finalOk
              ? '✅ 중앙/좌측/우측 테스트 모두 성공!'
              : '❌ 일부 테스트가 실패했습니다.',
          );
        } catch (e) {
          console.warn('SensorTestResult error:', e);
          Alert.alert('센서 테스트', '최종 결과 확인 중 오류가 발생했습니다.');
        }
      }
    } catch (e) {
      console.warn('startTest error:', e);
      setResult(false);
      Alert.alert(
        '센서 테스트',
        '테스트 중 오류가 발생했습니다.\n다시 시도해 주세요.',
      );
    } finally {
      runningRef.current = false;
      clearTimer();
      setPhase('done');
    }
  };

  const stopFlagOnce = async () => {
    try {
      await AndroidModule?.SensorTestStop?.();
    } catch (e) {
      console.warn('SensorTestStop error:', e);
    }
  };

  // 주 버튼
  const onPrimaryPress = async () => {
    if (phase === 'idle') {
      await startTest(step);
      return;
    }

    if (phase === 'done') {
      // center 성공 → left
      if (step === 'center' && result === true) {
        await stopFlagOnce();

        setStep('left');
        setPhase('idle');
        setResult(null);
        setCount(0);

        // ✅ setState 반영 이후 targetStep 명시로 시작 (step stale 방지)
        setTimeout(() => startTest('left'), 80);
        return;
      }

      // left 성공 → right
      if (step === 'left' && result === true) {
        await stopFlagOnce();

        setStep('right');
        setPhase('idle');
        setResult(null);
        setCount(0);

        setTimeout(() => startTest('right'), 80);
        return;
      }

      // 실패/재시도
      await stopFlagOnce();
      setPhase('idle');
      setCount(0);
      setTimeout(() => startTest(step), 80);
    }
  };

  const primaryLabel = useMemo(() => {
    if (phase === 'idle') {
      if (step === 'center') return '정면 테스트 시작';
      if (step === 'left') return '좌측 테스트 시작';
      if (step === 'right') return '우측 테스트 시작';
    }
    if (phase === 'running') return '테스트 중...';
    if (phase === 'done') {
      if (step === 'center' && result === true) return '좌측 테스트 시작';
      if (step === 'left' && result === true) return '우측 테스트 시작';
      return '재실행';
    }
    return '테스트';
  }, [phase, step, result]);

  return (
    <View style={styles.container}>
      <View style={styles.phoneWrap}>
        <Image
          source={phoneImageSource}
          style={phoneStyle}
          resizeMode="contain"
        />
      </View>

      <View style={styles.badges}>
        <Badge label="중앙" on={progress.center} />
        <Badge label="좌측" on={progress.left} />
        <Badge label="우측" on={progress.right} />
      </View>

      <Text style={styles.instruction}>{instruction}</Text>

      {phase === 'running' && (
        <View style={{ marginTop: 10, alignItems: 'center' }}>
          <ActivityIndicator />
        </View>
      )}

      <TouchableOpacity
        style={[
          styles.primaryBtn,
          phase === 'running' ? { opacity: 0.6 } : null,
        ]}
        onPress={onPrimaryPress}
        activeOpacity={0.9}
        disabled={phase === 'running'}
      >
        <Text style={styles.primaryTxt}>{primaryLabel}</Text>
      </TouchableOpacity>

      {phase === 'done' && (
        <TouchableOpacity
          style={styles.secondaryBtn}
          onPress={() => navigation?.navigate?.('HomeTabs')}
          activeOpacity={0.9}
        >
          <Text style={styles.secondaryTxt}>확인</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

const Badge = ({ label, on }) => (
  <View style={[styles.badge, on ? styles.badgeOn : styles.badgeOff]}>
    <Text
      style={[styles.badgeText, on ? styles.badgeTextOn : styles.badgeTextOff]}
    >
      {label}
    </Text>
  </View>
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: '10%',
    paddingHorizontal: 20,
    backgroundColor: '#FFFFFF',
  },
  phoneWrap: {
    alignSelf: 'center',
    width: '70%',
    aspectRatio: 3 / 5,
    justifyContent: 'center',
    alignItems: 'center',
  },
  phone: { width: '100%', height: '100%' },
  badges: {
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'center',
    marginTop: 12,
    marginBottom: 8,
  },
  badge: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 999,
    borderWidth: 1,
  },
  badgeOn: {
    backgroundColor: '#DCFCE7',
    borderColor: '#16A34A',
  },
  badgeOff: {
    backgroundColor: '#F1F5F9',
    borderColor: '#CBD5E1',
  },
  badgeText: { fontSize: 13, fontWeight: '700' },
  badgeTextOn: { color: '#166534' },
  badgeTextOff: { color: '#334155' },
  instruction: {
    marginTop: 6,
    textAlign: 'center',
    fontSize: 15,
    color: '#111827',
    lineHeight: 22,
  },
  primaryBtn: {
    marginTop: 18,
    alignSelf: 'center',
    backgroundColor: '#111827',
    paddingVertical: 14,
    paddingHorizontal: 22,
    borderRadius: 12,
  },
  primaryTxt: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 16,
  },
  secondaryBtn: {
    marginTop: 10,
    alignSelf: 'center',
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  secondaryTxt: {
    color: '#374151',
    fontWeight: '700',
    fontSize: 15,
  },
});

export default SensorTest;
