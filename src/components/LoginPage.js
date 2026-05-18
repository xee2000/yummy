import React, { useEffect, useState } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Platform,
  NativeModules,
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
  const [selectedArea, setSelectedArea] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');

  const canSubmit =
    userId.trim().length > 0 &&
    password.trim().length > 0 &&
    selectedArea !== null &&
    !loading;

  useEffect(() => {
    const checkInitialPermissions = async () => {
      if (Platform.OS !== 'android' || !AndroidModule) return;
      try {
        const isAllGranted = await AndroidModule.CheckPermissionsStatus();
        if (!isAllGranted) setShowPermModal(true);
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
    setErrorMsg('');

    try {
      let formData;
      let endpoint;

      if (selectedArea === 'dongtan') {
        formData = [
          `id=${encodeURIComponent(userId.trim())}`,
          `pass=${encodeURIComponent(password.trim())}`,
          `site=dongtan`,
        ].join('&');
        endpoint = 'loginOld';
      } else {
        formData = `id=${encodeURIComponent(userId.trim())}&pass=${encodeURIComponent(password.trim())}`;
        endpoint = 'login';
      }

      await EncryptedStorage.setItem('area', selectedArea);

      const response = await RestApi.post(endpoint, formData, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      });

      const userData = response.data;

      // result_code가 '000'인 경우만 홈으로 이동
      console.log('data : ' + JSON.stringify(userData));
    if (userData && userData.returnCode === 0) {
    console.log('✅ 조건 통과, 홈으로 이동 시도');  // ← 이게 찍히는지 확인
    
    const userInfo = userData.result; // ← result 안에서 꺼내야 함
    await EncryptedStorage.setItem('user', JSON.stringify(userInfo));

    if (Platform.OS === 'android' && AndroidModule) {
      const payload = {
        ...userInfo,
        uuid: BASE_UUID[selectedArea],
        area: selectedArea,
      };
      AndroidModule.startUserIntentService(JSON.stringify(payload));
      AndroidModule.StartApplication();
    }
  
  console.log('✅ navigate 호출');
  navigation.navigate('HomeTabs');
} else {
        // 실패 시 페이지 유지 + 하단 에러 메시지
        setErrorMsg(userData?.message || '로그인에 실패했습니다.');
      }
    } catch (e) {
      console.warn('[Login Error]', e);
      setErrorMsg(e.response?.data?.message || '로그인 중 오류가 발생했습니다.');
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
          {/* 지역 선택 */}
          <View style={styles.inputWrap}>
            <Text style={styles.label}>지역 선택</Text>
            <View style={styles.areaRow}>
              {[
                { key: 'dongtan', label: '동탄더샵' },
                { key: 'gwanggyo', label: '광교레이크시티' },
              ].map(({ key, label }) => (
                <TouchableOpacity
                  key={key}
                  style={[
                    styles.areaBtn,
                    selectedArea === key && styles.areaBtnSelected,
                  ]}
                  onPress={() => {
                    setSelectedArea(key);
                    setErrorMsg('');
                  }}
                  activeOpacity={0.8}
                >
                  <Text
                    style={[
                      styles.areaBtnText,
                      selectedArea === key && styles.areaBtnTextSelected,
                    ]}
                  >
                    {label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* 아이디 */}
          <View style={styles.inputWrap}>
            <Text style={styles.label}>아이디</Text>
            <TextInput
              style={styles.input}
              placeholder="아이디를 입력하세요"
              placeholderTextColor="#9AA3AF"
              value={userId}
              onChangeText={text => {
                setUserId(text);
                setErrorMsg('');
              }}
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          {/* 비밀번호 */}
          <View style={styles.inputWrap}>
            <Text style={styles.label}>비밀번호</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호를 입력하세요"
              placeholderTextColor="#9AA3AF"
              value={password}
              onChangeText={text => {
                setPassword(text);
                setErrorMsg('');
              }}
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
          {/* 에러 메시지 */}
          {errorMsg !== '' && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>⚠️ {errorMsg}</Text>
            </View>
          )}

          <TouchableOpacity
            style={[styles.loginButton, !canSubmit && styles.loginButtonDisabled]}
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
  errorBox: {
    backgroundColor: '#FEF2F2',
    borderWidth: 1,
    borderColor: '#FECACA',
    borderRadius: wp('3%'),
    paddingVertical: hp('1.5%'),
    paddingHorizontal: wp('4%'),
    marginBottom: hp('1.5%'),
  },
  errorText: {
    fontSize: hp('1.8%'),
    color: '#DC2626',
    fontWeight: '600',
    textAlign: 'center',
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