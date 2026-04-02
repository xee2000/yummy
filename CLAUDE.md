# 자동 크론 등록 (세션 시작 시 실행)

세션 시작 직후 아래 크론을 CronCreate로 등록할 것:

- cron: `*/20 * * * *`
- recurring: true
- prompt:
  프로젝트 메모리 갱신 작업 (토큰 절약 모드):
  1. `git status --short`로 변경된 파일 목록만 확인
  2. 변경된 파일만 Read로 읽기 (전체 스캔 금지)
  3. `C:\Users\leejeongho\.claude\projects\C--Users-leejeongho-project-pms-parking-mobile\memory\project_file_map.md` 읽고 변경된 항목만 업데이트
  4. 변경사항 없으면 아무것도 하지 않음

  규칙:
  - 이미 memory에 기록된 파일은 다시 전체 스캔하지 않음
  - git M/A/D 파일만 처리
  - 삭제 파일은 memory 제거, 추가/수정 파일은 역할 설명 갱신
  - 완료 후 한 줄 요약만 출력
