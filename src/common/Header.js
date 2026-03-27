import React, { useEffect, useState } from 'react';
import { Text, View, StyleSheet, Platform, NativeModules } from 'react-native';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';
import EncryptedStorage from 'react-native-encrypted-storage';

const { AndroidModule } = NativeModules;

const Header = () => {
  const [userInfo, setUserInfo] = useState({
    dong: '',
    ho: '',
    alias: '',
  });

  // ✅ 두 가지 상태 관리
  const [bleRunning, setBleRunning] = useState(false); // 실제 서비스 프로세스 동작 여부 (Ble)
  const [lobbyEnabled, setLobbyEnabled] = useState(false); // 서비스 활성화 설정 여부 (Lobby)

  useEffect(() => {
    // 1. 유저 데이터 로드
    const loadUserData = async () => {
      try {
        const userData = await EncryptedStorage.getItem('user');
        if (userData) {
          const parsed = JSON.parse(userData);
          setUserInfo({
            dong: parsed.dong || '',
            ho: parsed.ho || '',
            alias: parsed.name || '',
          });
        }
      } catch (e) {
        console.warn('[Header] Failed to load user data', e);
      }
    };

    // 2. 서비스 상태 체크 함수 (Ble와 Lobby 개별 확인)
    const checkServiceStatus = async () => {
      if (Platform.OS === 'android' && AndroidModule?.ServiceCheck) {
        try {
          // Native에서 WritableMap { Ble: boolean, Lobby: boolean } 반환
          const res = await AndroidModule.ServiceCheck();

          setBleRunning(!!res?.Ble);
          setLobbyEnabled(!!res?.Lobby);
        } catch (err) {
          console.warn('[Header] ServiceCheck Error:', err);
        }
      }
    };

    loadUserData();
    checkServiceStatus();

    // 3초마다 상태 갱신
    const interval = setInterval(() => {
      checkServiceStatus();
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  return (
    <View style={[styles.header, styles.shadow]}>
      <View style={styles.contentRow}>
        {/* 왼쪽: 입주민 정보 */}
        <View style={styles.leftCol}>
          <Text style={styles.unitInfoText}>
            {userInfo.dong}동 {userInfo.ho}호
          </Text>
          <Text style={styles.welcomeText}>
            <Text style={styles.userName}>{userInfo.alias}</Text> 입주민님
          </Text>
          <Text style={styles.welcomeSubText}>환영합니다</Text>
        </View>

        {/* 오른쪽: 상태 배너 리스트 */}
        <View style={styles.statusColumn}>
          {/* 1. 백그라운드 스캔 상태 (Ble) */}
          {/* <View
            style={[
              styles.statusBadge,
              bleRunning ? styles.badgeOk : styles.badgeError,
            ]}
          >
            <View
              style={[styles.dot, bleRunning ? styles.dotOk : styles.dotError]}
            />
            <Text
              style={[
                styles.statusLabel,
                bleRunning ? styles.textOk : styles.textError,
              ]}
            >
              주차위치 {bleRunning ? '정상' : '중지'}
            </Text>
          </View> */}

          {/* 2. 주차/로비 서비스 설정 상태 (Lobby) */}
          <View
            style={[
              styles.statusBadge,
              lobbyEnabled ? styles.badgePrimary : styles.badgeDisabled,
              { marginTop: hp('0.8%') },
            ]}
          >
            <View
              style={[
                styles.dot,
                lobbyEnabled ? styles.dotPrimary : styles.dotDisabled,
              ]}
            />
            <Text
              style={[
                styles.statusLabel,
                lobbyEnabled ? styles.textPrimary : styles.textDisabled,
              ]}
            >
              자동문열림 {lobbyEnabled ? 'ON' : 'OFF'}
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  header: {
    backgroundColor: '#FFFFFF',
    paddingTop: Platform.OS === 'ios' ? hp('7%') : hp('5%'),
    paddingBottom: hp('2.5%'),
    paddingHorizontal: wp('6%'),
    zIndex: 10,
  },
  shadow: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 3,
  },
  contentRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  leftCol: {
    flex: 1,
  },
  statusColumn: {
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
  unitInfoText: {
    fontSize: hp('1.8%'),
    color: '#6B7280',
    fontWeight: '600',
    marginBottom: hp('0.3%'),
  },
  welcomeText: {
    fontSize: hp('2.2%'),
    fontWeight: '500',
    color: '#111827',
  },
  welcomeSubText: {
    fontSize: hp('2.2%'),
    fontWeight: '500',
    color: '#111827',
    marginTop: -hp('0.3%'),
  },
  userName: {
    fontSize: hp('2.6%'),
    fontWeight: '800',
    color: '#2563EB',
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: wp('2.5%'),
    paddingVertical: hp('0.4%'),
    borderRadius: wp('4%'),
    borderWidth: 1,
    minWidth: wp('22%'),
    justifyContent: 'flex-start',
  },
  // 배지 상태별 스타일
  badgeOk: { backgroundColor: '#ECFDF5', borderColor: '#10B981' },
  badgeError: { backgroundColor: '#FEF2F2', borderColor: '#EF4444' },
  badgePrimary: { backgroundColor: '#EFF6FF', borderColor: '#3B82F6' },
  badgeDisabled: { backgroundColor: '#F3F4F6', borderColor: '#9CA3AF' },

  dot: {
    width: wp('1.8%'),
    height: wp('1.8%'),
    borderRadius: wp('0.9%'),
    marginRight: wp('1.2%'),
  },
  dotOk: { backgroundColor: '#10B981' },
  dotError: { backgroundColor: '#EF4444' },
  dotPrimary: { backgroundColor: '#3B82F6' },
  dotDisabled: { backgroundColor: '#9CA3AF' },

  statusLabel: {
    fontSize: hp('1.3%'),
    fontWeight: '800',
  },
  textOk: { color: '#065F46' },
  textError: { color: '#991B1B' },
  textPrimary: { color: '#1E40AF' },
  textDisabled: { color: '#4B5563' },
});

export default Header;
