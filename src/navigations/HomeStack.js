import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import Login from '../components/Login';
import HomeTabs from '../navigations/HomeTabs';
import Home from '../components/Home';
import Option from '../components/Option';
import ParkingLocation from '../components/ParkingLocation';
const Stack = createNativeStackNavigator();

const HomeStack = () => (
  <Stack.Navigator>
    <Stack.Screen
      name="Login"
      component={Login}
      options={{ headerShown: false }} // Login 화면에서는 헤더 숨기기
    />
    <Stack.Screen
      name="Home"
      component={Home}
      options={{ headerShown: false }} // Login 화면에서는 헤더 숨기기
    />
    <Stack.Screen
      name="Option"
      component={Option}
      options={{ headerShown: false }} // Login 화면에서는 헤더 숨기기
    />
    <Stack.Screen
      name="ParkingLocation"
      component={ParkingLocation}
      options={{ headerShown: false }} // Login 화면에서는 헤더 숨기기
    />
    <Stack.Screen
      name="HomeTabs"
      component={HomeTabs}
      options={{ headerShown: false }} // Login 화면에서는 헤더 숨기기
    />
  </Stack.Navigator>
);

export default HomeStack;
