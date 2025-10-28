import React, {useState, useEffect} from 'react';
import {
  Text,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Platform,
  Alert,
  Dimensions,
  NativeModules,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import {useNavigation} from '@react-navigation/native';
import Icon from 'react-native-vector-icons/Ionicons';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';
import RestApi from '../common/RestApi';

const LoginPage = () => {
  const navigation = useNavigation();
  const [buildingOptions, setBuildingOptions] = useState([]);
  const [selectedValue, setSelectedValue] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');


  const handleLogin = async () => {
    const login = {
      building_id: selectedValue,
      id: username,
      pwd: password,
    };
    try {
      const response = await RestApi.post('/api/user/login', login);
      if (response.status === 200) {
        await EncryptedStorage.setItem('user', JSON.stringify(response.data));
        const {UserModule, SmartTagConnect} = NativeModules;
        UserModule.startUserIntentService(JSON.stringify(response.data));
        UserModule.StartApplication(JSON.stringify(response.data));
        SmartTagConnect.StartBeaconService();
        navigation.navigate('HomeTabs');
      }
    } catch (error) {
      console.error('Login error:', error);
      Alert.alert('로그인 실패', '로그인 중 오류가 발생했습니다.');
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.headerContainer}>
        <View style={styles.logoContainer}>
          <Text style={styles.Logo_Text}>BALEM</Text>
          <Text style={styles.Logo_Subtitle}>Smart Wallness Solution</Text>
        </View>
      </View>

      <View style={styles.bodyContainer}>
        <View style={styles.inputContainer}>
          <Text style={styles.label}>아파트 단지 선택</Text>
          <View style={styles.pickerContainer}>
            <Text style={styles.selectedText}>
              {selectedValue
                ? buildingOptions.find(b => b.value === selectedValue)?.label
                : '단지를 선택해주세요'}
            </Text>
          </View>

          <Text style={styles.label}>아이디</Text>
          <TextInput
            style={styles.input}
            placeholder="아이디를 입력해주세요"
            placeholderTextColor="#aaa"
            value={username}
            onChangeText={setUsername}
          />
          <Text style={styles.label}>패스워드 입력</Text>
          <TextInput
            style={styles.input}
            placeholder="패스워드"
            placeholderTextColor="#aaa"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />
        </View>

        <TouchableOpacity style={styles.loginButton} onPress={handleLogin}>
          <Text style={styles.loginButtonText}>로그인</Text>
        </TouchableOpacity>

        {/* 카드형 링크 */}
        <View style={styles.cardLinkContainer}>
          <TouchableOpacity
            style={styles.card}
            onPress={() => Alert.alert('아이디/비밀번호 찾기')}>
            <Icon
              name="key-outline"
              size={hp('3%')}
              color="#1E90FF"
              style={styles.cardIcon}
            />
            <Text style={styles.cardText}>아이디 / 비밀번호 찾기</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Signup')}>
            <Icon
              name="person-add-outline"
              size={hp('3%')}
              color="#1E90FF"
              style={styles.cardIcon}
            />
            <Text style={styles.cardText}>회원가입</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  headerContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#1E90FF',
    height: hp('30%'),
  },
  logoContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
  },
  Logo_Text: {
    fontSize: hp('5%'),
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  Logo_Subtitle: {
    fontSize: hp('2%'),
    color: '#FFFFFF',
    textAlign: 'center',
    marginTop: hp('1%'),
  },
  bodyContainer: {
    flex: 1,
    paddingHorizontal: wp('5%'),
    backgroundColor: '#F5F5F5',
    justifyContent: 'flex-start',
    paddingTop: hp('3%'),
  },
  inputContainer: {
    marginBottom: hp('2%'),
  },
  label: {
    fontSize: hp('2%'),
    color: '#333',
    marginBottom: hp('1%'),
  },
  input: {
    fontSize: hp('2%'),
    height: hp('6%'),
    borderColor: '#CCC',
    borderWidth: 1,
    borderRadius: wp('2%'),
    marginBottom: hp('2%'),
    paddingHorizontal: wp('4%'),
    color: '#000',
    backgroundColor: '#FFF',
  },
  pickerContainer: {
    borderColor: '#CCC',
    borderWidth: 1,
    borderRadius: wp('2%'),
    padding: wp('3%'),
    marginBottom: hp('2%'),
    backgroundColor: '#FFF',
  },
  selectedText: {
    color: '#000',
    fontSize: hp('2%'),
  },
  loginButton: {
    alignItems: 'center',
    backgroundColor: '#1E90FF',
    paddingVertical: hp('2%'),
    borderRadius: wp('3%'),
    marginTop: hp('2%'),
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.2,
    shadowRadius: 3,
    elevation: 5,
  },
  loginButtonText: {
    fontSize: hp('2.2%'),
    color: 'white',
    fontWeight: 'bold',
  },
  cardLinkContainer: {
    marginTop: hp('3%'),
    paddingHorizontal: wp('5%'),
    gap: hp('1.5%'),
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    paddingVertical: hp('1.8%'),
    paddingHorizontal: wp('4%'),
    borderRadius: wp('3%'),
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardIcon: {
    marginRight: wp('3%'),
  },
  cardText: {
    fontSize: hp('2%'),
    color: '#1E90FF',
    fontWeight: '600',
  },
});

export default LoginPage;
