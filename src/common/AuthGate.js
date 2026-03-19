import React, { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';

export default function AuthGate() {
  const navigation = useNavigation();

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const raw = await EncryptedStorage.getItem('user');
        console.log('[AuthGate] Retrieved user data:', raw);

        if (raw) {
          try {
            const user = JSON.parse(raw);

            // ✅ 수정된 비교 로직: user 객체가 존재하고, name 필드에 값이 있는지 확인
            // 로그 데이터 기준: {"name":"김두열", "dong":"2512", ...}
            if (user && user.name && user.name.trim() !== '') {
              console.log('[AuthGate] Valid user found, navigating to Home');
              navigation.reset({
                index: 0,
                routes: [{ name: 'HomeTabs' }],
              });
              return;
            }
          } catch (parseError) {
            console.error('[AuthGate] JSON Parse Error:', parseError);
          }
        }

        // 유저 정보가 없거나 올바르지 않으면 로그인 화면으로
        console.log('[AuthGate] No valid user, navigating to Login');
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      } catch (e) {
        console.error('[AuthGate] Storage Error:', e);
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      }
    };

    checkAuth();
  }, [navigation]);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#2563EB" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
  },
});
