// src/screens/AuthGate.js
import React, { useEffect } from 'react';
import { View, ActivityIndicator } from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';

export default function AuthGate() {
  const navigation = useNavigation();

  useEffect(() => {
    (async () => {
      try {
        const raw = await EncryptedStorage.getItem('user');

        if (raw) {
          // 유저 객체면 홈으로 (파싱 실패 방지)
          try {
            const user = JSON.parse(raw);
            if (user && user.id) {
              navigation.reset({
                index: 0,
                routes: [{ name: 'HomeTabs' }],
              });
              return;
            }
          } catch {
            // 파싱 실패시 로그인으로
          }
        }

        // 아무것도 없으면 로그인으로
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      } catch (e) {
        navigation.reset({
          index: 0,
          routes: [{ name: 'Login' }],
        });
      }
    })();
  }, [navigation]);

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <ActivityIndicator />
    </View>
  );
}
