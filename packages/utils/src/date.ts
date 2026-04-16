/**
 * Date를 'YYYY-MM-DD' 형식으로 변환
 */
export function toDateString(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * 오늘 날짜 문자열 반환
 */
export function today(): string {
  return toDateString(new Date());
}

/**
 * 날짜 포맷 (예: '4월 16일 수요일')
 */
export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
  const weekday = weekdays[date.getDay()];
  return `${month}월 ${day}일 ${weekday}요일`;
}

/**
 * N일 전/후 날짜 계산
 */
export function addDays(dateString: string, days: number): string {
  const date = new Date(dateString);
  date.setDate(date.getDate() + days);
  return toDateString(date);
}

/**
 * 해당 주의 시작(월요일) 날짜 반환
 */
export function getWeekStart(dateString: string): string {
  const date = new Date(dateString);
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + diff);
  return toDateString(date);
}
