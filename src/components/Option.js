import React, {useState, useEffect} from 'react';
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

const Option = ({navigation}) => {
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

  const handleManagertag = () => navigation.navigate('ManagerTag');
  const handleAppSettings = async () => {
    const canOpen = await Linking.canOpenURL('app-settings:');
    if (canOpen) Linking.openURL('app-settings:');
    else Alert.alert('오류', '앱 설정 화면을 열 수 없습니다.');
  };
  const handleAppRecode = () => navigation.navigate('RecodeManager');
  const handleManagerGuardian = () => navigation.navigate('GuardianManager');
  const handleTimeManager = () => navigation.navigate('TimeManagerTabs');
  const handleTest = () => navigation.navigate('SensorSetting');

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.header}>앱 설정</Text>

      {/* userId === 1: 모든 항목 표시 */}
      {userId === 1 && (
        <>
          <TouchableOpacity
            style={styles.button}
            onPress={handleManagerGuardian}>
            <Text style={styles.buttonText}>보호자</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={handleManagertag}>
            <Text style={styles.buttonText}>스마트 태그</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={handleAppRecode}>
            <Text style={styles.buttonText}>녹음확인</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={handleTest}>
            <Text style={styles.buttonText}>센서테스트</Text>
          </TouchableOpacity>
        </>
      )}

      {/* 모든 사용자 공통: 시간 설정 */}
      {userId && (
        <TouchableOpacity style={styles.button} onPress={handleTimeManager}>
          <Text style={styles.buttonText}>시간설정</Text>
        </TouchableOpacity>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 30,
    backgroundColor: '#f5f5f5',
    paddingTop: '10%',
    marginTop: '5%',
  },
  header: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
    textAlign: 'center',
  },
  button: {
    marginTop: 20,
    padding: 15,
    backgroundColor: '#007BFF',
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#fff',
  },
});

export default Option;
