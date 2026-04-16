import React, { useEffect, useState, useCallback } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Platform,
  NativeModules,
  Alert,
  KeyboardAvoidingView,
  Modal,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';
import RestApi, { BASE_UUID } from '../common/RestApi';
import PermissionAlarm from '../common/PermissionAlarm';

const LoginPage = () => {
  const navigation = useNavigation();
  const { AndroidModule } = NativeModules;

  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPermModal, setShowPermModal] = useState(false);
  const [selectedArea, setSelectedArea] = useState(null); // 'dongtan' | 'gwanggyo'

  const canSubmit = userId.trim().length > 0 && password.trim().length > 0 && selectedArea !== null && !loading;

  useEffect(() => {
    const checkInitialPermissions = async () => {
      if (Platform.OS !== 'android' || !AndroidModule) return;

      try {
        const isAllGranted = await AndroidModule.CheckPermissionsStatus();
        if (!isAllGranted) {
          setShowPermModal(true);
        }
      } catch (e) {
        console.warn('[Permission] Status check failed:', e);
        setShowPermModal(true);
      }
    };
    checkInitialPermissions();
  }, [AndroidModule]);

  const handleConfirmPermissionGuide = async () => {
    try {
      if (Platform.OS === 'android' && AndroidModule) {
        const result = await AndroidModule.PermissionCheck();
        console.log('[Permission] 최종 결과:', result);
      }
    } catch (e) {
      console.warn('[Permission] Request process failed:', e);
    } finally {
      setShowPermModal(false);
    }
  };

  const handleLogin = async () => {
    if (!canSubmit) return;
    setLoading(true);

    try {
      let formData;
      let endpoint;

      if (selectedArea === 'dongtan') {
        // @ModelAttribute("loginOld") → id, pass, site 키
        formData = [
          `id=${encodeURIComponent(userId)}`,
          `pass=${encodeURIComponent(password)}`,
          `site=dongtan`,
        ].join('&');
        endpoint = '/app/loginOld';
      } else {
        // 광교
        formData = `id=${encodeURIComponent(userId)}&pass=${encodeURIComponent(password)}`;
        endpoint = '/app/login';
      }

      // 인터셉터가 area를 읽어 baseURL을 결정하므로 요청 전에 먼저 저장
      await EncryptedStorage.setItem('area', selectedArea);

      const response = await RestApi.post(endpoint, formData, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      });

      const userData = response.data;

      if (userData) {
        console.log('Login success:', userData);
        await EncryptedStorage.setItem('user', JSON.stringify(userData));

        if (Platform.OS === 'android' && AndroidModule) {
          // 현장별 UUID + area를 userData에 포함해서 Android로 전달
          const payload = { ...userData, uuid: BASE_UUID[selectedArea], area: selectedArea };
          AndroidModule.startUserIntentService(JSON.stringify(payload));
          AndroidModule.StartApplication();
        }
        navigation.navigate('HomeTabs');
      }
    } catch (e) {
      console.warn('[Login Error]', e);
      const errorMsg =
        e.response?.data?.message || '로그인 중 오류가 발생했습니다.';
      Alert.alert('로그인 실패', errorMsg);
    } finally { 
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <Modal
        visible={showPermModal}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowPermModal(false)}
      >
        <PermissionAlarm
          onConfirm={handleConfirmPermissionGuide}
          onClose={() => setShowPermModal(false)}
        />
      </Modal>

      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.select({ ios: 'padding', android: undefined })}
      >
        <View style={styles.header}>
          <Text style={styles.brand}>스마트 파킹</Text>
          <Text style={styles.title}>로그인</Text>
        </View>

        <View style={styles.form}>
          <View style={styles.inputWrap}>
            <Text style={styles.label}>지역 선택</Text>
            <View style={styles.areaRow}>
              {[
                { key: 'dongtan',  label: '동탄더샵' },
                { key: 'gwanggyo', label: '광교레이크시티' },
              ].map(({ key, label }) => (
                <TouchableOpacity
                  key={key}
                  style={[styles.areaBtn, selectedArea === key && styles.areaBtnSelected]}
                  onPress={() => setSelectedArea(key)}
                  activeOpacity={0.8}
                >
                  <Text style={[styles.areaBtnText, selectedArea === key && styles.areaBtnTextSelected]}>
                    {label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          <View style={styles.inputWrap}>
            <Text style={styles.label}>아이디</Text>
            <TextInput
              style={styles.input}
              placeholder="아이디를 입력하세요"
              placeholderTextColor="#9AA3AF"
              value={userId}
              onChangeText={setUserId}
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          <View style={styles.inputWrap}>
            <Text style={styles.label}>비밀번호</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호를 입력하세요"
              placeholderTextColor="#9AA3AF"
              value={password}
              onChangeText={setPassword}
              secureTextEntry={true}
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          <View style={styles.subActions}>
            <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
              <Text style={styles.linkText}>회원가입</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.ctaWrap}>
          <TouchableOpacity
            style={[
              styles.loginButton,
              !canSubmit && styles.loginButtonDisabled,
            ]}
            onPress={handleLogin}
            disabled={!canSubmit}
            activeOpacity={0.8}
          >
            <Text style={styles.loginButtonText}>
              {loading ? '로그인 중…' : '로그인'}
            </Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

// ... 스타일은 동일 (rowInputs 등 안 쓰는 스타일은 남겨둬도 무방합니다)

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#FFFFFF' },
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  header: {
    paddingHorizontal: wp('6%'),
    paddingTop: hp('5%'),
    paddingBottom: hp('1%'),
  },
  brand: {
    fontSize: hp('2%'),
    color: '#6B7280',
    fontWeight: '600',
    marginBottom: hp('0.8%'),
  },
  title: { fontSize: hp('4%'), fontWeight: '800', color: '#111827' },
  form: { paddingHorizontal: wp('6%'), paddingTop: hp('3%'), gap: hp('1.5%') },
  inputWrap: { width: '100%' },
  // ✅ 동/호 나란히 배치를 위한 스타일
  rowInputs: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
  },
  label: { fontSize: hp('1.8%'), color: '#6B7280', marginBottom: hp('0.8%') },
  input: {
    fontSize: hp('2%'),
    height: hp('6.5%'),
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: wp('3%'),
    paddingHorizontal: wp('4%'),
    backgroundColor: '#F9FAFB',
    color: '#111827',
  },
  subActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: hp('0.5%'),
  },
  linkText: { fontSize: hp('1.8%'), color: '#2563EB', fontWeight: '600' },
  ctaWrap: {
    marginTop: 'auto',
    paddingHorizontal: wp('6%'),
    paddingVertical: hp('3%'),
  },
  loginButton: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#2563EB',
    paddingVertical: hp('2.2%'),
    borderRadius: wp('3%'),
  },
  loginButtonDisabled: { backgroundColor: '#93C5FD' },
  loginButtonText: {
    fontSize: hp('2.2%'),
    color: '#FFFFFF',
    fontWeight: '800',
  },
  areaRow: {
    flexDirection: 'row',
    gap: wp('3%'),
  },
  areaBtn: {
    flex: 1,
    height: hp('6.5%'),
    borderWidth: 1.5,
    borderColor: '#E5E7EB',
    borderRadius: wp('3%'),
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F9FAFB',
  },
  areaBtnSelected: {
    borderColor: '#2563EB',
    backgroundColor: '#EFF6FF',
  },
  areaBtnText: {
    fontSize: hp('1.9%'),
    fontWeight: '600',
    color: '#6B7280',
  },
  areaBtnTextSelected: {
    color: '#2563EB',
    fontWeight: '800',
  },
});

export default LoginPage;
