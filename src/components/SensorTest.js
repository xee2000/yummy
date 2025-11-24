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
  const [count, setCount] = useState(3);
  const timerRef = useRef(null);

  // 초기: 네이티브 결과 1회 조회 → true면 곧바로 완료 상태로
  useEffect(() => {
    const checkInitial = async () => {
      try {
        const r = await AndroidModule?.SensorTestResult?.(); // boolean
        if (r === true) {
          setResult(true);
          setPhase('done'); // 바로 완료화면 + 재실행 버튼 노출
        } else {
          setPhase('idle');
        }
      } catch (e) {
        // 네이티브 부재/에러여도 테스트는 수동 시작 가능
        setPhase('idle');
      }
    };
    checkInitial();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  const progress = useMemo(
    () => ({
      center: phase !== 'idle',
      left: phase === 'running' || phase === 'done',
      right: phase === 'done',
    }),
    [phase],
  );

  const instruction = useMemo(() => {
    if (phase === 'idle') return '테스트를 시작하세요.';
    if (phase === 'running')
      return `휴대폰을 좌우로 흔들어주세요.\n센서 테스트 진행중입니다... (${count})`;
    if (phase === 'done') {
      if (result === true) return '테스트 종료: 센서 정상으로 감지되었습니다.';
      if (result === false)
        return '테스트 종료: 센서 감지에 실패했습니다. 다시 시도해주세요.';
      return '테스트 종료';
    }
    return '';
  }, [phase, count, result]);

  const startTest = async () => {
    try {
      await AndroidModule?.SensorTestStart?.();
    } catch (e) {
      console.warn('SensorTestStart error:', e);
    }

    setResult(null);
    setCount(3);
    setPhase('running');

    // 3초 카운트다운 → 결과 조회
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setCount(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          fetchResult();
          return 0;
        }
        return prev - 1;
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

  const fetchResult = async () => {
    try {
      const r = await AndroidModule?.SensorTestResult?.(); // boolean
      setResult(!!r);
    } catch (e) {
      console.warn('SensorTestResult error:', e);
      setResult(false);
    } finally {
      setPhase('done');
    }
  };

  // 주 버튼 동작
  const onPrimaryPress = async () => {
    if (phase === 'idle') {
      startTest();
    } else if (phase === 'done') {
      // 재실행: Stop 한 번 호출 후 재시작
      await stopFlagOnce();
      setPhase('idle');
      setTimeout(startTest, 80);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.phoneWrap}>
        <Image
          source={require('../assets/phone_stay.png')}
          style={styles.phone}
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

      {/* 주 버튼: idle → 테스트 시작 / running → 비활성 / done → 재실행 */}
      <TouchableOpacity
        style={[
          styles.primaryBtn,
          phase === 'running' ? { opacity: 0.6 } : null,
        ]}
        onPress={onPrimaryPress}
        activeOpacity={0.9}
        disabled={phase === 'running'}
      >
        <Text style={styles.primaryTxt}>
          {phase === 'idle' && '테스트 시작'}
          {phase === 'running' && '테스트 중...'}
          {phase === 'done' && '재실행'}
        </Text>
      </TouchableOpacity>

      {/* 보조 버튼: 완료 시 홈으로 이동 (원하지 않으면 이 블록 삭제 가능) */}
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
