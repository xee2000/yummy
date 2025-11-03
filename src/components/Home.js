// src/components/Home.js
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import SafeScreen from '../common/SafeScreen';

const Home = () => {
  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <View style={styles.inner}>
        <Text style={styles.text}>홈</Text>
      </View>
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  inner: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000000',
  },
});

export default Home;
