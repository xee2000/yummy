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
 * 버전 체크 서버 URL (인증 불필요 퍼블릭 엔드포인트)
 * pms_total_server 의 /app-version 엔드포인트
 */
export const VERSION_CHECK_URL = 'http://192.168.0.75:3000/app-version';
