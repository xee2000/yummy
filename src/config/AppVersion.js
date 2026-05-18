/**
 * 앱 버전 설정
 * 빌드할 때마다 android/app/build.gradle 의 versionName 과 맞춰서 올려주세요.
 *
 * 버전 비교 규칙:
 *   "1.3" < "1.4"  → 업데이트 필요
 *   "1.4" >= "1.4" → 정상
 */
export const APP_VERSION = '1.3';

/**
 * 사이트별 버전 체크 URL (RestApi.js 의 BASE_URLS 와 동일한 서버)
 * 기본값은 dongtan (area 미설정 시)
 */
const VERSION_CHECK_URLS = {
  dongtan:  'http://211.240.121.123:3389/pms-dongtan/app/app-version',
  gwanggyo: 'http://211.52.72.27:4000/pms-server-web/app/app-version',
};

export function getVersionCheckUrl(area) {
  return VERSION_CHECK_URLS[area] ?? VERSION_CHECK_URLS.dongtan;
}
