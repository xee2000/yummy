import React, { useEffect, useRef, useState } from 'react';
import { AppState, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import VersionCheck from 'react-native-version-check';
import HomeStack from './src/navigations/HomeStack';
import ForceUpdateModal from './src/common/ForceUpdateModal';

const App = () => {
  const appState = useRef(AppState.currentState);
  const [needsUpdate, setNeedsUpdate] = useState(false);
  const [storeUrl, setStoreUrl] = useState('');

  const checkVersion = async () => {
    try {
      const currentVersion = VersionCheck.getCurrentVersion();
      const latestVersion  = await VersionCheck.getLatestVersion({ provider: 'playStore' });
      const result = await VersionCheck.needUpdate({ currentVersion, latestVersion });
      if (result?.isNeeded) {
        setStoreUrl(result.storeUrl ?? '');
        setNeedsUpdate(true);
      }
    } catch (e) {
      // 네트워크 없음 등 — 스킵
    }
  };

  useEffect(() => {
    // 앱 최초 실행 시 체크
    checkVersion();

    // 백그라운드 → 포그라운드 전환 시마다 체크
    const subscription = AppState.addEventListener('change', nextState => {
      if (appState.current.match(/inactive|background/) && nextState === 'active') {
        checkVersion();
      }
      appState.current = nextState;
    });

    return () => subscription.remove();
  }, []);

  return (
    <NavigationContainer>
      <HomeStack />
      <ForceUpdateModal visible={needsUpdate} storeUrl={storeUrl} />
    </NavigationContainer>
  );
};

export default App;
