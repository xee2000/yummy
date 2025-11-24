// src/components/Parking.js
import React from 'react';
import { View, Text, StyleSheet, FlatList, Dimensions } from 'react-native';
import SafeScreen from '../common/SafeScreen';
import ParkingLocation from './ParkingLocation';

const VEHICLES = ['test1234', 'test5678', 'test9012'];

const Parking = () => {
  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <Text style={styles.title}>차량별 주차 위치</Text>

      {/* 차량별 도면 리스트 (세로 스크롤) */}
      <FlatList
        data={VEHICLES}
        keyExtractor={item => item}
        showsVerticalScrollIndicator={false}
        renderItem={({ item }) => (
          <View style={styles.itemWrapper}>
            <Text style={styles.carName}>{item}</Text>
            <ParkingLocation selectedCar={item} />
          </View>
        )}
        contentContainerStyle={styles.listContent}
      />
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFF' },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#111',
    textAlign: 'center',
    marginVertical: 20,
  },
  listContent: {
    paddingBottom: 50,
  },
  itemWrapper: {
    marginBottom: 40, // 도면 간격
    alignItems: 'center',
  },
  carName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 10,
  },
});

export default Parking;
