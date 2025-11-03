import React from 'react';
import { View, StyleSheet } from 'react-native';
import LoginPage from './LoginPage';
import SafeScreen from '../common/SafeScreen';
const Login = () => {
  return (
    <SafeScreen style={{ flex: 1 }} backgroundColor="#FFFFFF">
      <LoginPage />
    </SafeScreen>
  );
};

export default Login;
