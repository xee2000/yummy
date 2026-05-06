import React, { useEffect, useState } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';
import { APP_VERSION, VERSION_CHECK_URL } from '../config/AppVersion';
import ForceUpdateModal from './ForceUpdateModal';

// ── 버전 비교 ("1.3" vs "1.4") ───────────────────────────────────
function isVersionLessThan(current, minimum) {
  const toNumbers = v =>
    String(v)
      .split('.')
      .map(n => parseInt(n, 10) || 0);
  const cur = toNumbers(current);
  const min = toNumbers(minimum);
  const len = Math.max(cur.length, min.length);
  for (let i = 0; i < len; i++) {
    const c = cur[i] ?? 0;
    const m = min[i] ?? 0;
    if (c < m) return true;
    if (c > m) return false;
  }
  return false;
}

export default function AuthGate() {
  const navigation = useNavigation();
  const [needsUpdate, setNeedsUpdate] = useState(false);
  const [storeUrl, setStoreUrl] = useState('');

  useEffect(() => {
    const init = async () => {
      // ── 1단계: 버전 체크 ──────────────────────────────────────
      try {
        const res = await fetch(VERSION_CHECK_URL, {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
        });
        if (res.ok) {
          const json = await res.json();
          const minVersion = json.minVersion ?? '1.0';
          const url = json.storeUrl ?? '';
          console.log(
            `[AuthGate] 버전 체크: 현재=${APP_VERSION}, 최소=${minVersion}`,
          );
          if (isVersionLessThan(APP_VERSION, minVersion)) {
            console.log('[AuthGate] 업데이트 필요 → 강제 업데이트 팝업');
            setStoreUrl(url);
            setNeedsUpdate(true);
            return; // 인증 체크 건너뜀
          }
        }
      } catch (e) {
        // 서버 연결 실패 시 업데이트 체크 스킵 (앱 이용 가능)
        console.warn('[AuthGate] 버전 체크 실패 (서버 미응답) — 스킵:', e.message);
      }

      // ── 2단계: 인증 체크 ──────────────────────────────────────
      try {
        const raw = await EncryptedStorage.getItem('user');
        console.log('[AuthGate] Retrieved user data:', raw);

        if (raw) {
          try {
            const user = JSON.parse(raw);
            if (user && user.name && user.name.trim() !== '') {
              console.log('[AuthGate] Valid user found, navigating to Home');
              navigation.reset({
                index: 0,
                routes: [{ name: 'HomeTabs' }],
              });
              return;
            }
          } catch (parseError) {
            console.error('[AuthGate] JSON Parse Error:', parseError);
          }
        }

        console.log('[AuthGate] No valid user, navigating to Login');
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      } catch (e) {
        console.error('[AuthGate] Storage Error:', e);
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      }
    };

    init();
  }, [navigation]);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#2563EB" />

      {/* 강제 업데이트 팝업 (닫기 불가) */}
      <ForceUpdateModal visible={needsUpdate} storeUrl={storeUrl} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
  },
});
