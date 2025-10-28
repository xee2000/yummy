import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import Header from '../../common/Header';
import MainBottomTabs from './MainBottomtabs';

const Stack = createNativeStackNavigator();

const HomeTabs = () => {
  return (
    <Stack.Navigator
      initialRouteName="MainBottomTabs"
      screenOptions={{
        header: () => <Header />, // 공통 헤더 설정
      }}>
      <Stack.Screen
        name="MainBottomTabs"
        component={MainBottomTabs}
        options={{
          headerShown: true, // 헤더를 활성화
        }}
      />
    </Stack.Navigator>
  );
};

export default HomeTabs;
