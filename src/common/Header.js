import React from 'react';
import { Text, View, StyleSheet, Platform } from 'react-native';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';

const Header = () => {
  return (
    <View style={[styles.header, styles.shadow]}>
      {/* 상단: 아파트 단지명 + 세대 정보 */}
      <View style={styles.topRow}>
        <Text style={styles.apartmentName}>테스트 단지</Text>
        <Text style={styles.apartmentDetail}>999동 999호 (usertest)</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  header: {
    backgroundColor: '#FFFFFF',
    paddingTop: Platform.OS === 'ios' ? hp('6%') : hp('4%'),
    paddingBottom: hp('2.5%'),
    paddingHorizontal: wp('6%'),
    borderBottomWidth: 0,
    borderBottomColor: '#eee',
  },
  shadow: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 3,
    elevation: 2,
  },
  topRow: {
    alignItems: 'center',
    marginBottom: hp('1%'),
  },
  apartmentName: {
    fontSize: hp('3.4%'),
    fontWeight: '700',
    color: '#111',
    letterSpacing: 1,
  },
  apartmentDetail: {
    fontSize: hp('1.9%'),
    color: '#666',
    marginTop: hp('0.3%'),
  },
});

export default Header;
