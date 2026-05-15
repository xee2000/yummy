import React, { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { useNavigation } from '@react-navigation/native';

export default function AuthGate() {
  const navigation = useNavigation();

  useEffect(() => {
    const init = async () => {
      // 버전 체크는 App.js의 AppState 리스너에서 전담
      // 여기서는 인증 체크만 수행
      try {
        const raw = await EncryptedStorage.getItem('user');
        if (raw) {
          try {
            const user = JSON.parse(raw);
            if (user && user.name && user.name.trim() !== '') {
              navigation.reset({ index: 0, routes: [{ name: 'HomeTabs' }] });
              return;
            }
          } catch (parseError) {
            console.error('[AuthGate] JSON Parse Error:', parseError);
          }
        }
        navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
      } catch (e) {
        console.error('[AuthGate] Storage Error:', e);
        navigation.reset({ index: 0, routes: [{ name: 'Login' }] });
      }
    };

    init();
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
