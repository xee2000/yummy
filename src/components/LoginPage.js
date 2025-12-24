import React, { useEffect, useMemo, useState } from 'react';
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
  Pressable,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';
import Icon from 'react-native-vector-icons/Ionicons';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';
import RestApi from '../common/RestApi';

// ✅ 네 Manifest 기준으로 정리한 권한 항목
const PERMISSION_ITEMS = [
  {
    code: 'POST_NOTIFICATIONS',
    icon: 'notifications-outline',
    title: '알림 권한',
    desc: '앱의 주요 안내 및 상태 알림을 받을 수 있어요.',
  },
  {
    code: 'LOCATION',
    icon: 'location-outline',
    title: '위치 권한',
    desc: '주차 위치 확인 및 위치 기반 서비스를 위해 필요해요.',
  },
  {
    code: 'BLUETOOTH',
    icon: 'bluetooth-outline',
    title: '블루투스 권한',
    desc: '주변 기기 감지 및 스마트 연동 기능에 필요해요.',
  },
];

// ---------------------------
// ✅ 권한 모달 컴포넌트
// ---------------------------
const PermissionModal = ({ visible, onClose, onAgree }) => {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <Pressable style={pmStyles.backdrop} onPress={onClose}>
        <Pressable style={pmStyles.sheet} onPress={() => {}}>
          <Text style={pmStyles.title}>앱 사용을 위해 권한이 필요해요</Text>
          <Text style={pmStyles.subtitle}>
            아래 권한을 허용하면 모든 기능을 정상적으로 사용할 수 있어요.
          </Text>

          <View style={pmStyles.list}>
            {PERMISSION_ITEMS.map(item => (
              <View key={item.code} style={pmStyles.itemRow}>
                <View style={pmStyles.iconWrap}>
                  <Icon name={item.icon} size={hp('3%')} color="#2563EB" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={pmStyles.itemTitle}>{item.title}</Text>
                  <Text style={pmStyles.itemDesc}>{item.desc}</Text>
                </View>
              </View>
            ))}
          </View>

          <View style={pmStyles.btnRow}>
            <TouchableOpacity style={pmStyles.cancelBtn} onPress={onClose}>
              <Text style={pmStyles.cancelText}>나중에</Text>
            </TouchableOpacity>
            <TouchableOpacity style={pmStyles.okBtn} onPress={onAgree}>
              <Text style={pmStyles.okText}>허용하기</Text>
            </TouchableOpacity>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
};

const LoginPage = () => {
  const navigation = useNavigation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [secure, setSecure] = useState(true);
  const [loading, setLoading] = useState(false);
  const { AndroidModule, SmartTagConnect } = NativeModules;

  // ✅ 권한 모달 상태
  const [showPermModal, setShowPermModal] = useState(false);

  const canSubmit =
    username.trim().length > 0 && password.length > 0 && !loading;

  // ✅ 화면 진입 시 권한 먼저 체크 → 없으면 모달 띄움
  useEffect(() => {
    const checkPermissions = async () => {
      if (Platform.OS !== 'android') return;

      try {
        // ✅ 네이티브: CheckPermissionsStatus() 먼저 확인
        const hasAll = await AndroidModule.CheckPermissionsStatus();
        setShowPermModal(!hasAll); // 권한 없으면 true, 있으면 false
      } catch (e) {
        // 체크 실패 시 안전하게 모달 띄움
        setShowPermModal(true);
      }
    };

    checkPermissions();
  }, []); // ✅ AndroidModule을 deps에 넣지 말고 1회만

  // ✅ “허용하기” → (1) 다시 체크 (2) 없으면 PermissionCheck()로 요청
  const handleAgreePermissions = async () => {
    if (Platform.OS !== 'android') {
      Alert.alert('권한 안내', 'iOS 권한은 설정에서 허용할 수 있어요.');
      return;
    }

    try {
      // ✅ 이미 허용 상태면 요청/팝업 닫기
      const hasAll = await AndroidModule.CheckPermissionsStatus();
      if (hasAll) {
        setShowPermModal(false);
        return;
      }

      // ✅ 시스템 권한 다이얼로그 띄우기
      const granted = await AndroidModule.PermissionCheck();

      // ✅ 결과 반영
      setShowPermModal(!granted);
      if (!granted) {
        Alert.alert(
          '권한 필요',
          '권한을 허용해야 앱 기능을 정상적으로 사용할 수 있어요.',
        );
      }
    } catch (e) {
      Alert.alert('권한 요청 실패', String(e?.message ?? e));
      setShowPermModal(true);
    }
  };

  const handleLogin = async () => {
    if (!canSubmit) return;
    setLoading(true);

    try {
      // ✅ (예시) 로그인 성공했다고 가정한 user
      const user = {
        id: 1234,
        dong: '101',
        ho: '1001',
        name: '홍길동',
        phone: '01012345678',
        email: 'honggildong@example.com',
        user_name: '홍길동',
        user_id: 'test',
        user_pwd: 'test',
        building_id: 1,
      };

      // ✅ 1) 스토리지 저장
      await EncryptedStorage.setItem('user', JSON.stringify(user));

      // ✅ 2) 네이티브 서비스로도 전달(기존 유지)
      AndroidModule.startUserIntentService(JSON.stringify(user));
      AndroidModule.StartApplication();

      // ✅ 3) 이동
      navigation.navigate('HomeTabs');
    } catch (e) {
      console.warn('[Login] store/login error:', e);
      Alert.alert('로그인 실패', '로그인 처리 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      {/* ✅ 권한 모달 */}
      <PermissionModal
        visible={showPermModal}
        onClose={() => setShowPermModal(false)}
        onAgree={handleAgreePermissions}
      />

      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.select({ ios: 'padding', android: undefined })}
      >
        {/* 헤더 (브랜드/페이지 타이틀) */}
        <View style={styles.header}>
          <Text style={styles.brand}>스마트 파킹</Text>
          <Text style={styles.title}>로그인</Text>
        </View>

        {/* 폼 */}
        <View style={styles.form}>
          <View style={styles.inputWrap}>
            <Text style={styles.label}>아이디</Text>
            <TextInput
              style={styles.input}
              placeholder="아이디를 입력하세요"
              placeholderTextColor="#9AA3AF"
              value={username}
              onChangeText={setUsername}
              autoCapitalize="none"
              autoCorrect={false}
              returnKeyType="next"
            />
          </View>

          <View style={styles.inputWrap}>
            <Text style={styles.label}>비밀번호</Text>
            <View style={styles.passwordRow}>
              <TextInput
                style={[styles.input, styles.passwordInput]}
                placeholder="비밀번호를 입력하세요"
                placeholderTextColor="#9AA3AF"
                secureTextEntry={secure}
                value={password}
                onChangeText={setPassword}
                autoCapitalize="none"
                returnKeyType="done"
              />
              <TouchableOpacity
                onPress={() => setSecure(!secure)}
                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                style={styles.eyeBtn}
              >
                <Icon
                  name={secure ? 'eye-off-outline' : 'eye-outline'}
                  size={hp('2.6%')}
                  color="#6B7280"
                />
              </TouchableOpacity>
            </View>
          </View>

          {/* 보조 액션 */}
          <View style={styles.subActions}>
            <TouchableOpacity
              onPress={() =>
                Alert.alert(
                  '안내',
                  '아이디/비밀번호 찾기 화면으로 이동해주세요.',
                )
              }
            >
              <Text style={styles.linkText}>아이디/비밀번호 찾기</Text>
            </TouchableOpacity>
            <View style={styles.dot} />
            <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
              <Text style={styles.linkText}>회원가입</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 고정 CTA 버튼 */}
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

// ---------------------------
// 기존 스타일 유지
// ---------------------------
const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  header: {
    paddingHorizontal: wp('6%'),
    paddingTop: hp('2%'),
    paddingBottom: hp('1%'),
  },
  brand: {
    fontSize: hp('2%'),
    color: '#6B7280',
    fontWeight: '600',
    marginBottom: hp('0.8%'),
  },
  title: {
    fontSize: hp('4%'),
    fontWeight: '800',
    color: '#111827',
  },
  form: {
    paddingHorizontal: wp('6%'),
    paddingTop: hp('3%'),
    gap: hp('1.8%'),
  },
  inputWrap: {
    width: '100%',
  },
  label: {
    fontSize: hp('1.8%'),
    color: '#6B7280',
    marginBottom: hp('1%'),
  },
  input: {
    fontSize: hp('2.1%'),
    height: hp('6%'),
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: wp('3%'),
    paddingHorizontal: wp('4%'),
    backgroundColor: '#F9FAFB',
    color: '#111827',
  },
  passwordRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  passwordInput: {
    flex: 1,
    paddingRight: wp('12%'),
  },
  eyeBtn: {
    position: 'absolute',
    right: wp('4%'),
    height: '100%',
    justifyContent: 'center',
  },
  subActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: wp('3%'),
    marginTop: hp('1%'),
  },
  linkText: {
    fontSize: hp('1.9%'),
    color: '#2563EB',
    fontWeight: '600',
  },
  dot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#E5E7EB',
  },
  ctaWrap: {
    marginTop: 'auto',
    paddingHorizontal: wp('6%'),
    paddingVertical: hp('2%'),
  },
  loginButton: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#2563EB',
    paddingVertical: hp('2%'),
    borderRadius: wp('3%'),
    shadowColor: '#2563EB',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: Platform.OS === 'ios' ? 0.25 : 0,
    shadowRadius: 10,
    elevation: 0,
  },
  loginButtonDisabled: {
    backgroundColor: '#93C5FD',
  },
  loginButtonText: {
    fontSize: hp('2.2%'),
    color: '#FFFFFF',
    fontWeight: '800',
    letterSpacing: 0.2,
  },
});

// ---------------------------
// ✅ 모달 스타일
// ---------------------------
const pmStyles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    paddingHorizontal: wp('7%'),
  },
  sheet: {
    backgroundColor: '#fff',
    borderRadius: wp('4%'),
    paddingHorizontal: wp('6%'),
    paddingVertical: hp('2.5%'),
  },
  title: {
    fontSize: hp('2.6%'),
    fontWeight: '800',
    color: '#111827',
    marginBottom: hp('0.8%'),
  },
  subtitle: {
    fontSize: hp('1.8%'),
    color: '#6B7280',
    marginBottom: hp('2%'),
  },
  list: {
    gap: hp('1.6%'),
    marginBottom: hp('2.2%'),
  },
  itemRow: {
    flexDirection: 'row',
    gap: wp('3%'),
    alignItems: 'flex-start',
  },
  iconWrap: {
    width: wp('9%'),
    height: wp('9%'),
    borderRadius: wp('4.5%'),
    backgroundColor: '#EFF6FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 2,
  },
  itemTitle: {
    fontSize: hp('2.0%'),
    fontWeight: '700',
    color: '#111827',
    marginBottom: 2,
  },
  itemDesc: {
    fontSize: hp('1.7%'),
    color: '#6B7280',
    lineHeight: hp('2.2%'),
  },
  btnRow: {
    flexDirection: 'row',
    gap: wp('3%'),
  },
  cancelBtn: {
    flex: 1,
    paddingVertical: hp('1.6%'),
    borderRadius: wp('3%'),
    backgroundColor: '#F3F4F6',
    alignItems: 'center',
  },
  okBtn: {
    flex: 1,
    paddingVertical: hp('1.6%'),
    borderRadius: wp('3%'),
    backgroundColor: '#2563EB',
    alignItems: 'center',
  },
  cancelText: {
    fontSize: hp('2.0%'),
    fontWeight: '700',
    color: '#111827',
  },
  okText: {
    fontSize: hp('2.0%'),
    fontWeight: '800',
    color: '#fff',
  },
});

export default LoginPage;
