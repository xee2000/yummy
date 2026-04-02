import axios from 'axios';
const apiBaseUrl = `211.240.121.123:3389/pms-dongtan`;
// const apiBaseUrl = `192.168.0.35:3389/pms-dongtan`;
console.log('url ' + apiBaseUrl);
// Axios 인스턴스 생성
const RestApi = axios.create({
  baseURL: `http://${apiBaseUrl}`, // 기본 API URL 설정
});

export default RestApi;
