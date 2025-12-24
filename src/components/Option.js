// src/components/Option.js
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

  // 센서/서비스 상태
  const [sensorOk, setSensorOk] = useState(false); // sensor_status
  const [serviceEnabled, setServiceEnabled] = useState(false); // service_status

  const { AndroidModule } = NativeModules;

  // 초기 로드: 사용자 / 센서/서비스 상태
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

      try {
        if (Platform.OS === 'android' && AndroidModule?.ServiceCheck) {
          // ServiceCheck() → { sensor_status: boolean, service_status: boolean } 형태라고 가정
          const res = await AndroidModule.ServiceCheck();
          // 방어코드: key casing/타입이 다를 수도 있으니 boolean 변환
          const sOk = typeof res?.sensor_test === 'boolean';

          const svcRaw = res?.service_flag;

          const svc = typeof svcRaw === 'boolean' ? svcRaw : !!svcRaw;

          setSensorOk(sOk);
          setServiceEnabled(svc);
        }
      } catch (err) {
        console.error('ServiceCheck failed:', err);
      }
    };
    bootstrap();
  }, [AndroidModule]);

  // 앱 설정 이동
  const handleAppSettings = async () => {
    try {
      if (Platform.OS === 'ios') {
        await Linking.openURL('app-settings:');
      } else {
        await Linking.openSettings();
      }
    } catch (error) {
      Alert.alert('오류', '앱 설정 화면을 열 수 없습니다.');
      console.error('handleAppSettings error:', error);
    }
  };

  // 로그아웃 (필요하면 저장값 초기화 추가)
  const handleLogout = async () => {
    navigation.navigate('Login');
  };

  // 센서 테스트: sensorOk === false일 때만 이동
  const handleSensorTestRow = () => {
    if (!sensorOk) {
      navigation.navigate('SensorTest');
    }
  };

  // 서비스 토글
  const onToggleService = useCallback(async () => {
    try {
      const next = !serviceEnabled;
      // 네이티브에 우선 전달
      if (Platform.OS === 'android' && AndroidModule?.ServiceFlag) {
        await AndroidModule.ServiceFlag(next);
      }
      setServiceEnabled(next);
    } catch (err) {
      console.error('ServiceFlag failed:', err);
      Alert.alert('오류', '서비스 상태 변경에 실패했습니다.');
    }
  }, [serviceEnabled, AndroidModule]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.header}>설정 메뉴</Text>

      {/* 앱 설정 이동 */}
      <TouchableOpacity
        style={styles.menuButton}
        onPress={handleAppSettings}
        activeOpacity={0.9}
      >
        <Text style={styles.menuText}>앱 설정으로 이동</Text>
      </TouchableOpacity>

      {/* 센서 상태 (체크박스 형태) */}
      <TouchableOpacity
        style={styles.itemRow}
        onPress={handleSensorTestRow}
        activeOpacity={sensorOk ? 1 : 0.8}
      >
        <Text style={styles.itemLabel}>휴대폰 센서 작동 확인</Text>
        <View style={styles.rightWrap}>
          <Text style={[styles.stateText, sensorOk ? styles.on : styles.off]}>
            {sensorOk ? '정상' : '미확인'}
          </Text>
          <Icon
            name={sensorOk ? 'checkbox-outline' : 'square-outline'}
            size={22}
            color={sensorOk ? '#16A34A' : '#9CA3AF'}
            style={{ marginLeft: 8 }}
          />
        </View>
      </TouchableOpacity>

      {/* 서비스 사용여부 (목록 + 스위치) */}
      <View style={styles.itemRow}>
        <Text style={styles.itemLabel}>서비스 사용여부</Text>
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

      {/* 로그아웃 */}
      <TouchableOpacity
        style={styles.menuButton}
        onPress={handleLogout}
        activeOpacity={0.9}
      >
        <Text style={styles.menuText}>로그아웃</Text>
      </TouchableOpacity>

      {/* 유저 표시(옵션) */}
      {userId != null && (
        <Text style={styles.footerInfo}>현재 사용자 ID: {String(userId)}</Text>
      )}
    </ScrollView>
  );
};

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
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 3,
    elevation: 1,
  },
  menuText: {
    fontSize: 17,
    fontWeight: '600',
    color: '#333',
  },
  itemRow: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 20,
    marginBottom: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',

    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 3,
    elevation: 1,
  },
  itemLabel: {
    fontSize: 17,
    fontWeight: '600',
    color: '#333',
  },
  rightWrap: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  stateText: {
    fontSize: 15,
    fontWeight: '700',
  },
  on: { color: '#16A34A' }, // green-600
  off: { color: '#9CA3AF' }, // gray-400
  footerInfo: {
    marginTop: 24,
    textAlign: 'center',
    color: '#6b7280',
  },
});

export default Option;
