import React, {useState, useEffect} from 'react';
import {Text, View, StyleSheet, Platform, TouchableOpacity} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import {useNavigation} from '@react-navigation/native';
import RestApi from './RestApi';
import EncryptedStorage from 'react-native-encrypted-storage';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';

const Header = () => {
  const navigation = useNavigation();
  const [userId, setUserId] = useState(null);

  useEffect(() => {
    const getUserId = async () => {
      try {
        const userData = await EncryptedStorage.getItem('user');
        if (userData) {
          const parsed = JSON.parse(userData);
          setUserId(parsed.id);
        }
      } catch (err) {
        console.error('Failed to load user ID from storage:', err);
      }
    };
    getUserId();
  }, []);

  const MainHome = () => {
    console.log('로그인페이지 복귀');
    RestApi.post('/api/user/logout')
      .then(response => {
        console.log('사용자가 로그아웃 하였습니다' + response);
      })
      .catch(error => {
        console.log('error message : ' + error);
      });
    navigation.navigate('Login');
  };

  const navigateToAlarm = () => navigation.navigate('Alarm');
  const navigateToProfile = () => navigation.navigate('UserProfile');
  const navigateToAI = () => navigation.navigate('HealthPrompt');

  const isGuardian = userId === 2;

  return (
    <View
      style={[
        styles.header,
        {backgroundColor: isGuardian ? '#2E8B57' : '#1E90FF'},
      ]}>
      <Text style={styles.Logo_Text}>BALEM</Text>
      {isGuardian && <Text style={styles.roleLabel}>보호자용</Text>}
      {userId === 1 && <Text style={styles.roleLabel}>사용자용</Text>}

      <View style={styles.menuContainer}>
        <TouchableOpacity style={styles.menuItemContainer} onPress={MainHome}>
          <Icon name="log-out-outline" size={hp('3.5%')} color="#FFFFFF" />
          <Text style={styles.menuItem}>로그아웃</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.menuItemContainer}
          onPress={navigateToProfile}>
          <Icon name="person-outline" size={hp('3.5%')} color="#FFFFFF" />
          <Text style={styles.menuItem}>프로필</Text>
        </TouchableOpacity>
        {userId === 1 && (
          <>
            <TouchableOpacity
              style={styles.menuItemContainer}
              onPress={navigateToAI}>
              <Icon
                name="stats-chart-outline"
                size={hp('3.5%')}
                color="#FFFFFF"
              />
              <Text style={styles.menuItem}>AI헬스케어</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.menuItemContainer}
              onPress={navigateToAlarm}>
              <Icon name="alarm-outline" size={hp('3.5%')} color="#FFFFFF" />
              <Text style={styles.menuItem}>알림</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  header: {
    paddingVertical: hp('1.5%'),
    paddingHorizontal: wp('5%'),
    alignItems: 'center',
    position: 'absolute',
    top: Platform.OS === 'ios' ? hp('5%') : 0,
    left: 0,
    right: 0,
    zIndex: 1,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 5,
    borderBottomLeftRadius: wp('5%'),
    borderBottomRightRadius: wp('5%'),
  },
  Logo_Text: {
    fontSize: hp('4.5%'),
    fontWeight: 'bold',
    color: '#FFFFFF',
    textAlign: 'center',
    marginBottom: hp('0.5%'),
    letterSpacing: 2,
  },
  roleLabel: {
    fontSize: hp('2%'),
    color: '#FFFFFF',
    marginBottom: hp('1%'),
  },
  menuContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    width: '100%',
    marginTop: hp('1%'),
  },
  menuItemContainer: {
    alignItems: 'center',
    marginHorizontal: wp('2%'),
  },
  menuItem: {
    fontSize: hp('1.7%'),
    color: '#FFFFFF',
    marginTop: hp('0.5%'),
    fontWeight: '500',
  },
});

export default Header;
