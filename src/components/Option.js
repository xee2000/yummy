import React, { useState, useEffect } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  Linking,
  Alert,
  ScrollView,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
const Option = ({ navigation }) => {
  const [userId, setUserId] = useState(null);
  useEffect(() => {
    const fetchUserId = async () => {
      try {
        const userData = await EncryptedStorage.getItem('user');
        if (userData) {
          const parsed = JSON.parse(userData);
          setUserId(parsed.id);
        }
      } catch (err) {
        console.error('Failed to load user ID:', err);
      }
    };
    fetchUserId();
  }, []);

  /** 앱 설정 이동 */
  const handleAppSettings = async () => {
    const canOpen = await Linking.canOpenURL('app-settings:');
    if (canOpen) {
      Linking.openURL('app-settings:');
    } else {
      Alert.alert('오류', '앱 설정 화면을 열 수 없습니다.');
    }
  };

  const handleLogout = async () => {
    navigation.navigate('Login');
  };

  /** 센서 테스트 화면 이동 */
  const handleSensorTest = () => navigation.navigate('SensorSetting');

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.header}>설정 메뉴</Text>

      <TouchableOpacity style={styles.menuButton} onPress={handleAppSettings}>
        <Text style={styles.menuText}>앱 설정으로 이동</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.menuButton} onPress={handleSensorTest}>
        <Text style={styles.menuText}>서비스 사용여부</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.menuButton} onPress={handleSensorTest}>
        <Text style={styles.menuText}>센서 테스트</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.menuButton} onPress={handleLogout}>
        <Text style={styles.menuText}>로그아웃</Text>
      </TouchableOpacity>
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
});

export default Option;
