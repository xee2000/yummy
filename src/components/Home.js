// src/components/Home.js
import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  NativeModules,
} from 'react-native';
import SafeScreen from '../common/SafeScreen';

const { AndroidModule } = NativeModules; // 👈 네이티브 모듈

const Home = () => {
  // 앱 진입 시 서비스 상태 확인 → 꺼져 있으면 시작
  useEffect(() => {
    const ensureService = async () => {
      try {
        const running = await AndroidModule.ServiceRunningCheck(); // boolean
        console.log('runnung : ' + running);
        if (!running) {
          await AndroidModule.startApplication(); // 포그라운드 서비스 시작
          // 필요하면 Alert/Toast 추가 가능
          // Alert.alert('알림', '백그라운드 서비스가 시작되었습니다.');
        }
      } catch (e) {
        console.warn('[Home] ensureService error:', e);
      }
    };
    ensureService();
  }, []);

  const handleManualGateOpen = () => {
    Alert.alert('수동 문열림', '수동 문열림을 요청했어요.');
    console.log('[Home] manual gate open pressed');
  };

  const handleManualParkPosition = () => {
    Alert.alert('수동 주차위치', '현재 위치를 주차 위치로 저장했어요.');
    AndroidModule.passiveParking();
    console.log('[Home] manual park position pressed');
  };

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <View style={styles.banner}>
        <Text style={styles.bannerText}>
          현재 자동 주차위치 서비스가 동작 중이므로 자동 방식은 취소될 수
          있습니다.
        </Text>
      </View>
      <View style={styles.inner}>
        <View style={styles.buttonsRow}>
          <TouchableOpacity
            style={[styles.btn, styles.btnPrimary]}
            onPress={handleManualGateOpen}
            activeOpacity={0.85}
            accessibilityRole="button"
            accessibilityLabel="수동 문열림"
          >
            <Text style={styles.btnPrimaryText}>수동 문열림</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.btn, styles.btnOutline]}
            onPress={handleManualParkPosition}
            activeOpacity={0.85}
            accessibilityRole="button"
            accessibilityLabel="수동 주차위치"
          >
            <Text style={styles.btnOutlineText}>수동 주차위치</Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  banner: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFF5CC',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E5D58A',
  },
  bannerText: { color: '#7A5D00', fontSize: 13, lineHeight: 18 },
  inner: { flex: 1, paddingHorizontal: 20, paddingTop: 24 },
  buttonsRow: { flexDirection: 'row', gap: 12, marginTop: 8 },
  btn: {
    flex: 1,
    height: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnPrimary: { backgroundColor: '#0A84FF' },
  btnPrimaryText: { color: '#FFFFFF', fontSize: 16, fontWeight: '600' },
  btnOutline: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#CCCCCC',
  },
  btnOutlineText: { color: '#111111', fontSize: 16, fontWeight: '600' },
});

export default Home;
