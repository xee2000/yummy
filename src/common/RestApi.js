import axios from 'axios';
import EncryptedStorage from 'react-native-encrypted-storage';

const BASE_URLS = {
  dongtan:  'http://211.240.121.123:3389/pms-dongtan/app/',
  gwanggyo: 'http://211.52.72.27:4000/pms-server-web/app/',
};

const BASE_UUID = {
  dongtan:  '20151005-8864-5654-4159-013500201901',
  gwanggyo: '20151005-8864-5654-4111-710200210801',
};

const RestApi = axios.create();

// 요청 인터셉터
RestApi.interceptors.request.use(async config => {
  try {
    const raw = await EncryptedStorage.getItem('area');
    const area = raw ?? 'dongtan';
    config.baseURL = BASE_URLS[area] ?? BASE_URLS.dongtan;

    const fullUrl = `${config.baseURL}${config.url || ''}`;
    console.log('[RestApi] ▶ URL    :', fullUrl);
    console.log('[RestApi] ▶ Method :', config.method?.toUpperCase());
    if (config.params) console.log('[RestApi] ▶ Params :', JSON.stringify(config.params));
    if (config.data)   console.log('[RestApi] ▶ Data   :', config.data);
  } catch {
    config.baseURL = BASE_URLS.dongtan;
  }
  return config;
});

// 응답 인터셉터
RestApi.interceptors.response.use(
  response => {
    console.log('[RestApi] ◀ Status :', response.status);
    console.log('[RestApi] ◀ Data   :', JSON.stringify(response.data));
    return response;
  },
  error => {
    const url = (error.config?.baseURL ?? '') + (error.config?.url ?? '');
    console.error('[RestApi] ✕ URL    :', url);
    console.error('[RestApi] ✕ Status :', error.response?.status);
    console.error('[RestApi] ✕ Data   :', JSON.stringify(error.response?.data));
    return Promise.reject(error);
  }
);

export { BASE_UUID };
export default RestApi;