import axios from 'axios';
import EncryptedStorage from 'react-native-encrypted-storage';

const BASE_URLS = {
  dongtan:  'http://211.240.121.123:3389/pms-dongtan',
  gwanggyo: 'http://211.52.72.27:4000/pms-server-web',
};

const RestApi = axios.create();

// 요청마다 저장된 area로 baseURL 자동 전환
RestApi.interceptors.request.use(async config => {
  try {
    const raw = await EncryptedStorage.getItem('area');
    const area = raw ?? 'dongtan';
    config.baseURL = BASE_URLS[area] ?? BASE_URLS.dongtan;
    console.log('[RestApi] baseURL:', config.baseURL);
  } catch {
    config.baseURL = BASE_URLS.dongtan;
  }
  return config;
});

export default RestApi;
