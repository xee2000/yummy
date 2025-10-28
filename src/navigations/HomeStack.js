import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import Login from '../components/Login';
const Stack = createNativeStackNavigator();

const HomeStack = () => (
  <Stack.Navigator>
    <Stack.Screen
      name="Login"
      component={Login}
      options={{headerShown: false}} // Login 화면에서는 헤더 숨기기
    />
  </Stack.Navigator>
);

export default HomeStack;
