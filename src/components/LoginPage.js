import React, { useState } from 'react';
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

const LoginPage = () => {
  const navigation = useNavigation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [secure, setSecure] = useState(true);
  const [loading, setLoading] = useState(false);
  const { AndroidModule, SmartTagConnect } = NativeModules;
  const canSubmit =
    username.trim().length > 0 && password.length > 0 && !loading;

  const handleLogin = async () => {
    if (!canSubmit) return;
    setLoading(true);

    // const login = {
    //   id: username,
    //   pwd: password,
    // };

    try {
      const User = {
        id: 'test',
        pwd: 'test',
      };
      AndroidModule.StartApplication();
      navigation.navigate('HomeTabs');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
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
    color: '#6B7280', // 토스 느낌의 차분한 보조 텍스트
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
    color: '#2563EB', // 토스 계열 블루
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
    shadowOpacity: Platform.OS === 'ios' ? 0.25 : 0, // iOS 살짝 그림자
    shadowRadius: 10,
    elevation: 0, // Android는 평평하게
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

export default LoginPage;
