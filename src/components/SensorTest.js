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
} from 'react-native';

const { AndroidModule } = NativeModules;

const SensorTest = ({ navigation }) => {
  // 'idle' | 'running' | 'done'
  const [phase, setPhase] = useState('idle');
  const [result, setResult] = useState(null); // true | false | null
  const [count, setCount] = useState(0); // 1..2..3 표시용
  const timerRef = useRef(null);

  // 어떤 단계인지: 'center' 정면 → 'left' 좌측 → 'right' 우측
  const [step, setStep] = useState('center'); // 'center' | 'left' | 'right'

  // 이미지 상태
  // 'stay'      : 정면 기본
  // 'stay_ok'   : 정면 성공
  const [imageVariant, setImageVariant] = useState('stay');

  // 초기: 네이티브 결과 1회 조회 → 이미 정면 통과한 상태면 바로 OK로
  useEffect(() => {
    const checkInitial = async () => {
      try {
        const r = await AndroidModule?.StayResult?.(); // boolean 예상
        if (r === true) {
          setResult(true);
          setImageVariant('stay_ok');
          setPhase('done'); // 정면 완료 상태
          setStep('center'); // 아직 좌측/우측은 안 들어간 상태
        } else {
          setPhase('idle');
        }
      } catch (e) {
        setPhase('idle');
      }
    };
    checkInitial();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  // 상단 뱃지 상태
  const progress = useMemo(() => {
    const centerOn =
      step === 'center' ? phase !== 'idle' || result === true : true;

    const leftOn =
      step === 'center'
        ? false
        : step === 'left'
        ? phase !== 'idle' || result === true
        : true; // step === 'right' 이면 이미 좌측까지 끝난 상태

    const rightOn =
      step === 'right' ? phase !== 'idle' || result === true : false;

    return { center: centerOn, left: leftOn, right: rightOn };
  }, [step, phase, result]);

  // 안내 문구
  const instruction = useMemo(() => {
    // 정면 단계
    if (step === 'center') {
      if (phase === 'idle') {
        return '테스트를 시작하세요.\n휴대폰을 정면으로 들고 가만히 두세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 정면으로 들고 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true) {
          return '정면 테스트 완료!\n다음, 휴대폰을 좌측으로 돌려주세요.';
        }
        if (result === false) {
          return '테스트 종료: 정면 감지에 실패했습니다.\n다시 시도해주세요.';
        }
        return '테스트 종료';
      }
    }

    // 좌측 단계
    if (step === 'left') {
      if (phase === 'idle') {
        return '휴대폰을 좌측 방향으로 돌려 들고\n테스트를 시작하세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 좌측 방향으로 돌려 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true) {
          return '좌측 테스트 완료!\n다음, 휴대폰을 우측으로 돌려주세요.';
        }
        if (result === false) {
          return '테스트 종료: 좌측 감지에 실패했습니다.\n다시 시도해주세요.';
        }
        return '테스트 종료';
      }
    }

    // 우측 단계
    if (step === 'right') {
      if (phase === 'idle') {
        return '휴대폰을 우측 방향으로 돌려 들고\n테스트를 시작하세요.';
      }
      if (phase === 'running') {
        return `휴대폰을 우측 방향으로 돌려 가만히 두세요.\n센서 테스트 진행중입니다... (${count} / 3초)`;
      }
      if (phase === 'done') {
        if (result === true) {
          return '우측 테스트까지 모두 완료되었습니다.\n테스트를 종료해 주세요.';
        }
        if (result === false) {
          return '테스트 종료: 우측 감지에 실패했습니다.\n다시 시도해주세요.';
        }
        return '테스트 종료';
      }
    }

    return '';
  }, [step, phase, count, result]);

  // 현재 보여줄 이미지 (정면 이미지 하나만 쓰고, step에 따라 회전)
  const phoneImageSource = useMemo(() => {
    if (imageVariant === 'stay_ok') {
      return require('../assets/phone_stay_ok.png');
    }
    // 기본 정면
    return require('../assets/phone_stay.png');
  }, [imageVariant]);

  // step에 따라 회전값
  const phoneStyle = useMemo(() => {
    let transform = [];
    if (step === 'left') {
      // 좌측: 정면 이미지를 왼쪽으로 90도 회전
      transform.push({ rotate: '-90deg' });
    } else if (step === 'right') {
      // 우측: 정면 이미지를 오른쪽으로 90도 회전
      transform.push({ rotate: '90deg' });
    }
    return [styles.phone, transform.length ? { transform } : null];
  }, [step]);

  const startTest = async () => {
    try {
      // 모드 지정
      if (step === 'left') {
        await AndroidModule?.LeftSensorTestStart?.();
      } else if (step === 'right') {
        await AndroidModule?.RightSensorTestStart?.();
      } else {
        await AndroidModule?.CenterSensorTestStart?.();
      }

      // 실제 테스트 시작 (flag만 ON)
      await AndroidModule?.SensorTestStart?.();
    } catch (e) {
      console.warn('SensorTestStart error:', e);
    }

    setResult(null);
    setImageVariant('stay');
    setCount(0);
    setPhase('running');

    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setCount(prev => {
        const next = prev + 1;
        if (next >= 3) {
          clearInterval(timerRef.current);
          fetchStayResult();
        }
        return next;
      });
    }, 1000);
  };

  const stopFlagOnce = async () => {
    try {
      await AndroidModule?.SensorTestStop?.();
    } catch (e) {
      console.warn('SensorTestStop error:', e);
    }
  };

  // 3초 이후 각 단계 상태 결과 가져오기
  const fetchStayResult = async () => {
    try {
      let r = false;

      if (step === 'center') {
        // 정면 결과
        r = await AndroidModule?.StayResult?.();
      } else if (step === 'left') {
        // 좌측 결과
        r = await AndroidModule?.LeftResult?.();
      } else if (step === 'right') {
        // 우측 결과
        r = await AndroidModule?.RightResult?.();
      }

      const ok = !!r;
      setResult(ok);

      // 중앙만 OK 이미지로 변경, 좌/우는 회전만 유지
      if (ok && step === 'center') {
        setImageVariant('stay_ok');
      }
    } catch (e) {
      console.warn('StepResult error:', e);
      setResult(false);
    } finally {
      setPhase('done');
    }
  };
  // 주 버튼 동작
  const onPrimaryPress = async () => {
    // 1) 테스트 시작 (center / left / right 공통)
    if (phase === 'idle') {
      startTest();
      return;
    }

    // 2) 테스트 종료 후 버튼 동작
    if (phase === 'done') {
      // (A) 정면 단계에서 성공했을 때 -> 좌측 단계로 넘어가기
      if (step === 'center' && result === true) {
        await stopFlagOnce();

        setStep('left');
        setPhase('idle');
        setResult(null);
        setCount(0);
        // 좌측 테스트 자동 시작
        setTimeout(startTest, 80);
        return;
      }

      // (B) 좌측 단계에서 성공했을 때 -> 우측 단계로 넘어가기
      if (step === 'left' && result === true) {
        await stopFlagOnce();

        setStep('right');
        setPhase('idle');
        setResult(null);
        setCount(0);
        // 우측 테스트 자동 시작
        setTimeout(startTest, 80);
        return;
      }

      // (C) 그 외에는 현재 단계 재실행 (실패했거나 이미 우측까지 끝난 상태)
      await stopFlagOnce();
      setPhase('idle');
      setCount(0);
      setTimeout(startTest, 80);
    }
  };

  // 버튼 텍스트
  const primaryLabel = useMemo(() => {
    if (phase === 'idle') {
      if (step === 'center') return '정면 테스트 시작';
      if (step === 'left') return '좌측 테스트 시작';
      if (step === 'right') return '우측 테스트 시작';
    }
    if (phase === 'running') {
      return '테스트 중...';
    }
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
          style={phoneStyle} // ← 여기서 step에 따라 회전
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

      {/* 주 버튼: idle → 테스트 시작 / running → 비활성 / done → 단계 전환 or 재실행 */}
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

      {/* 보조 버튼: 언제든 홈으로 이동 */}
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
