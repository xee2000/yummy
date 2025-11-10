// src/components/Home.js
import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import SafeScreen from '../common/SafeScreen';

const Home = () => {
  const handleManualGateOpen = () => {
    // TODO: 실제 문열림 로직 연동
    // 예: native module 호출 / REST API 호출 등
    Alert.alert('수동 문열림', '수동 문열림을 요청했어요.');
    console.log('[Home] manual gate open pressed');
  };

  const handleManualParkPosition = () => {
    // TODO: 현재 위치 저장 로직 연동
    // 예: GPS → 서버 업로드 or 로컬 저장
    Alert.alert('수동 주차위치', '현재 위치를 주차 위치로 저장했어요.');
    console.log('[Home] manual park position pressed');
  };

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      {/* 경고 배너 */}
      <View style={styles.banner}>
        <Text style={styles.bannerText}>
          현재 자동 주차위치 서비스가 동작 중이므로 자동 방식은 취소될 수
          있습니다.
        </Text>
      </View>

      {/* 본문 */}
      <View style={styles.inner}>
        {/* 버튼 그룹 */}
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
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  banner: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFF5CC', // 연한 노랑
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E5D58A',
  },
  bannerText: {
    color: '#7A5D00',
    fontSize: 13,
    lineHeight: 18,
  },
  inner: {
    flex: 1,
    paddingHorizontal: 20,
    paddingTop: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000000',
    marginBottom: 16,
  },
  buttonsRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  btn: {
    flex: 1,
    height: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnPrimary: {
    backgroundColor: '#0A84FF',
  },
  btnPrimaryText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  btnOutline: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#CCCCCC',
  },
  btnOutlineText: {
    color: '#111111',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default Home;
