import React, { useEffect, useState } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';
import VersionCheck from 'react-native-version-check';
import ForceUpdateModal from './ForceUpdateModal';

export default function AuthGate() {
  const navigation = useNavigation();
  const [needsUpdate, setNeedsUpdate] = useState(false);
  const [storeUrl, setStoreUrl] = useState('');

  useEffect(() => {
    const init = async () => {
      // ── 1단계: 플레이스토어 버전 체크 ─────────────────────────────
      try {
        const currentVersion = VersionCheck.getCurrentVersion();
        const latestVersion  = await VersionCheck.getLatestVersion({ provider: 'playStore' });

        console.log(`[AuthGate] 현재 버전: ${currentVersion}, 스토어 버전: ${latestVersion}`);

        const needUpdate = await VersionCheck.needUpdate({
          currentVersion,
          latestVersion,
        });

        if (needUpdate?.isNeeded) {
          console.log('[AuthGate] 업데이트 필요 → 강제 업데이트 팝업');
          setStoreUrl(needUpdate.storeUrl ?? '');
          setNeedsUpdate(true);
          return;
        }
      } catch (e) {
        // 스토어 조회 실패 시 스킵 (네트워크 없음 등)
        console.warn('[AuthGate] 버전 체크 실패 — 스킵:', e.message);
      }

      // ── 2단계: 인증 체크 ──────────────────────────────────────────
      try {
        const raw = await EncryptedStorage.getItem('user');
        console.log('[AuthGate] Retrieved user data:', raw);

        if (raw) {
          try {
            const user = JSON.parse(raw);
            if (user && user.name && user.name.trim() !== '') {
              console.log('[AuthGate] Valid user found, navigating to Home');
              navigation.reset({ index: 0, routes: [{ name: 'HomeTabs' }] });
              return;
            }
          } catch (parseError) {
            console.error('[AuthGate] JSON Parse Error:', parseError);
          }
        }

        console.log('[AuthGate] No valid user, navigating to Login');
        navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
      } catch (e) {
        console.error('[AuthGate] Storage Error:', e);
        navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
      }
    };

    init();
  }, [navigation]);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#2563EB" />
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
