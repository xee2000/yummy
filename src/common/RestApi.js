import axios from 'axios';
const apiBaseUrl = `192.168.0.38:7071/balem_web`;
console.log('url ' + apiBaseUrl);
// Axios 인스턴스 생성
const RestApi = axios.create({
  baseURL: `http://${apiBaseUrl}`, // 기본 API URL 설정
});

export default RestApi;
