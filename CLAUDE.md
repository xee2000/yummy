# Yummy - 헬스 트래커 (잘먹겠습니다)

## 프로젝트 개요
Apps in Toss(토스 미니앱) 플랫폼용 헬스 트래킹 웹앱. Toss WebView 내에서 동작하는 React SPA.

## 모노레포 구조
```
yummy/
├── apps/
│   └── web/          # Vite + React 18 + TypeScript 웹앱 (메인)
├── packages/
│   ├── ui/           # 공통 UI 컴포넌트 (웹 전용, HTML/CSS)
│   ├── utils/        # 유틸 함수 (BMR/TDEE 계산, 날짜)
│   └── api/          # API 타입/함수
└── package.json      # yarn workspaces + turborepo
```

## 실행
```bash
yarn install
yarn workspace @yummy/web dev   # http://localhost:3000
```

## 핵심 기술
- **패키지 매니저**: yarn (v4)
- **번들러**: Vite v6
- **라우터**: react-router-dom v6
- **토스 브릿지**: @apps-in-toss/web-bridge, @apps-in-toss/bridge-core
- **스타일**: 일반 CSS (CSS Modules 아님, className 방식)

## 앱 흐름 (App.tsx)
```
로딩중 → 스피너
비로그인 → LoginPage (토스 로그인)
로그인O + 프로필 미입력 → OnboardingPage (키/나이/몸무게)
로그인O + 프로필 입력O → 메인 탭 (홈/식단/운동/마이)
```

## 인증 (AuthContext.tsx)
- `localStorage` 키: `yummy_user`
- 토스 앱 환경: `appLogin()` from `@apps-in-toss/web-bridge` → authorizationCode 발급
- 일반 브라우저(개발): 자동으로 dev_타임스탬프로 게스트 로그인 처리
- 상태: `isLoading`, `isLoggedIn`, `isOnboarded`, `profile`

## 페이지 목록
| 경로 | 파일 | 설명 |
|------|------|------|
| (로그인 전) | `pages/login/LoginPage.tsx` | 토스로 시작하기 버튼 |
| (온보딩) | `pages/onboarding/OnboardingPage.tsx` | 키→나이→몸무게 3단계 입력 |
| `/` | `pages/home/HomePage.tsx` | 오늘 칼로리 현황 |
| `/diet` | `pages/diet/DietPage.tsx` | 식단 기록 |
| `/exercise` | `pages/exercise/ExercisePage.tsx` | 운동 기록 |
| `/profile` | `pages/profile/ProfilePage.tsx` | 프로필/설정 |

## 공통 컴포넌트 (packages/ui/src)
- `Button`, `Text`, `Card`, `ProgressBar`, `Badge`, `BottomNav`
- `BottomNav`: react-router-dom `NavLink` 사용
- import: `import {Button} from '@yummy/ui'` (vite alias로 src 직접 참조)

## 유틸 (packages/utils/src)
- `calculateBMR(gender, weight, height, age)` → 기초대사량
- `calculateTDEE(bmr, activityLevel)` → 일일 권장 칼로리
- `formatDate(date)`, `today()`, `addDays(date, days)`

## 주의사항
- `packages/` 안에 컴파일된 `.js` 파일 생기면 `.tsx`와 충돌하므로 삭제할 것
- `apps/web/src` 안에도 `.js` 파일 있으면 삭제할 것
- `@apps-in-toss/web-bridge`는 동적 import로 사용 (일반 브라우저 호환)
- vite.config.ts는 tsconfig.node.json으로 별도 관리 (tsconfig.json에서 제외)
- 네트워크 접속용 IP: `192.168.0.35` (토스 샌드박스 앱 테스트용)
