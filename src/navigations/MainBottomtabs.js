import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import Icon from 'react-native-vector-icons/Ionicons';
import Home from '../components/Home';
import Option from '../components/Option';
import Parking from '../components/Parking';
const Tab = createBottomTabNavigator();

const MainBottomTabs = () => {
  return (
    <Tab.Navigator
      initialRouteName="Home"
      screenOptions={{
        headerShown: false, // ✅ 상단 자동 헤더 숨김
        tabBarStyle: { backgroundColor: '#f8f8f8', borderTopWidth: 0 },
        tabBarLabelStyle: { fontSize: 13, fontWeight: '500' },
        tabBarActiveTintColor: '#0055FF',
        tabBarInactiveTintColor: '#999',
      }}
    >
      <Tab.Screen
        name="Parking"
        component={Parking}
        options={{
          tabBarLabel: '주차위치',
          tabBarIcon: ({ color, size }) => (
            <Icon name="map-outline" color={color} size={size} />
          ),
        }}
      />
      <Tab.Screen
        name="Home"
        component={Home}
        options={{
          tabBarLabel: '홈',
          tabBarIcon: ({ color, size }) => (
            <Icon name="home-outline" color={color} size={size} />
          ),
        }}
      />
      <Tab.Screen
        name="Option"
        component={Option}
        options={{
          tabBarLabel: '설정',
          tabBarIcon: ({ color, size }) => (
            <Icon name="settings-outline" color={color} size={size} /> // ✅ 아이콘도 설정 아이콘으로 교체
          ),
        }}
      />
    </Tab.Navigator>
  );
};

export default MainBottomTabs;
