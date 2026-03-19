import React, { useEffect, useState, useCallback } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Linking,
  Alert,
  ScrollView,
  Switch,
  NativeModules,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import Icon from 'react-native-vector-icons/Ionicons';

const STORAGE_KEYS = {
  USER: 'user',
};

const Option = ({ navigation }) => {
  const [userId, setUserId] = useState(null);

  // sensorOk: 실제 안드로이드 백그라운드 서비스(BleScanner) 실행 여부 (Ble 플래그)
  const [sensorOk, setSensorOk] = useState(false);
  // serviceEnabled: 자동 문열림 서비스 로직 활성화 여부 (Lobby 플래그)
  const [serviceEnabled, setServiceEnabled] = useState(false);

  const { AndroidModule } = NativeModules;

  // -----------------------------
  // ✅ 서비스 상태 체크 함수
  // -----------------------------
  const checkServiceStatus = useCallback(async () => {
    try {
      if (Platform.OS === 'android' && AndroidModule?.ServiceCheck) {
        // 네이티브에서 WritableMap { Ble: boolean, Lobby: boolean } 반환
        const res = await AndroidModule.ServiceCheck();
        console.log('[Option] ServiceCheck Result:', res);

        setSensorOk(!!res?.Ble);
        setServiceEnabled(!!res?.Lobby);
      }
    } catch (err) {
      console.error('[Option] ServiceCheck failed:', err);
    }
  }, [AndroidModule]);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const userData = await EncryptedStorage.getItem(STORAGE_KEYS.USER);
        if (userData) {
          const parsed = JSON.parse(userData);
          setUserId(parsed?.id ?? null);
        }
      } catch (err) {
        console.error('Failed to load user ID:', err);
      }
      await checkServiceStatus();
    };
    bootstrap();
  }, [checkServiceStatus]);

  const handleAppSettings = async () => {
    try {
      if (Platform.OS === 'ios') {
        await Linking.openURL('app-settings:');
      } else {
        await Linking.openSettings();
      }
    } catch (error) {
      Alert.alert('오류', '앱 설정 화면을 열 수 없습니다.');
    }
  };

  const handleLogout = async () => {
    navigation.navigate('Login');
  };

  // -----------------------------
  // ✅ 자동 문열림 서비스 토글 핸들러
  // -----------------------------
  const onToggleService = useCallback(async () => {
    try {
      const nextStatus = !serviceEnabled;

      // ✅ 수정된 부분: AndroidModule.passOpenLobbyFlag 호출
      if (Platform.OS === 'android' && AndroidModule?.passOpenLobbyFlag) {
        await AndroidModule.passOpenLobbyFlag(nextStatus);

        // 네이티브 플래그 변경 후 UI 업데이트를 위해 상태 재확인
        await checkServiceStatus();
      } else {
        setServiceEnabled(nextStatus);
      }
    } catch (err) {
      console.error('passOpenLobbyFlag failed:', err);
      Alert.alert('오류', '서비스 상태 변경에 실패했습니다.');
    }
  }, [serviceEnabled, AndroidModule, checkServiceStatus]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.header}>설정 메뉴</Text>

      <TouchableOpacity
        style={styles.menuButton}
        onPress={handleAppSettings}
        activeOpacity={0.8}
      >
        <Text style={styles.menuText}>앱 설정으로 이동</Text>
      </TouchableOpacity>

      {/* ✅ 주차위치 서비스 가동 상태 (Ble 플래그) */}
      <View style={styles.itemRow}>
        <View>
          <Text style={styles.itemLabel}>주차위치 서비스 상태</Text>
        </View>
        <View style={styles.rightWrap}>
          <Text style={[styles.stateText, sensorOk ? styles.on : styles.off]}>
            {sensorOk ? '작동중' : '중지됨'}
          </Text>
          <Icon
            name={sensorOk ? 'checkmark-circle' : 'alert-circle'}
            size={22}
            color={sensorOk ? '#16A34A' : '#EF4444'}
            style={{ marginLeft: 8 }}
          />
        </View>
      </View>

      {/* ✅ 자동 문열림 서비스 사용여부 (Lobby 플래그) */}
      <View style={styles.itemRow}>
        <View>
          <Text style={styles.itemLabel}>자동 문열림 서비스</Text>
        </View>
        <View style={styles.rightWrap}>
          <Text
            style={[styles.stateText, serviceEnabled ? styles.on : styles.off]}
          >
            {serviceEnabled ? '켜짐' : '꺼짐'}
          </Text>
          <Switch
            value={serviceEnabled}
            onValueChange={onToggleService}
            trackColor={{ false: '#d1d5db', true: '#10b981' }}
            thumbColor={Platform.OS === 'android' ? '#ffffff' : undefined}
            ios_backgroundColor="#d1d5db"
            style={{ marginLeft: 8 }}
          />
        </View>
      </View>

      <TouchableOpacity
        style={styles.menuButton}
        onPress={handleLogout}
        activeOpacity={0.8}
      >
        <Text style={[styles.menuText, { color: '#EF4444' }]}>로그아웃</Text>
      </TouchableOpacity>

      {userId != null && (
        <Text style={styles.footerInfo}>현재 사용자 ID: {String(userId)}</Text>
      )}
    </ScrollView>
  );
};

// ... 스타일 정의는 동일
const styles = StyleSheet.create({
  container: {
    padding: 24,
    backgroundColor: '#f7f7f7',
    flexGrow: 1,
    paddingTop: '15%',
  },
  header: {
    fontSize: 26,
    fontWeight: '700',
    color: '#111',
    textAlign: 'center',
    marginBottom: 40,
  },
  menuButton: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    paddingVertical: 18,
    paddingHorizontal: 20,
    marginBottom: 18,
    elevation: 1,
  },
  menuText: { fontSize: 17, fontWeight: '600', color: '#333' },
  itemRow: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 20,
    marginBottom: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    elevation: 1,
  },
  itemLabel: { fontSize: 17, fontWeight: '600', color: '#333' },
  rightWrap: { flexDirection: 'row', alignItems: 'center' },
  stateText: { fontSize: 15, fontWeight: '700' },
  on: { color: '#16A34A' },
  off: { color: '#9CA3AF' },
  footerInfo: {
    marginTop: 24,
    textAlign: 'center',
    color: '#6b7280',
    fontSize: 13,
  },
});

export default Option;
