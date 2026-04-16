/**
 * Harris-Benedict 공식으로 기초대사량(BMR) 계산
 */
export function calculateBMR(
  gender: 'male' | 'female',
  weight: number,
  height: number,
  age: number,
): number {
  if (gender === 'male') {
    return Math.round(88.362 + 13.397 * weight + 4.799 * height - 5.677 * age);
  }
  return Math.round(447.593 + 9.247 * weight + 3.098 * height - 4.33 * age);
}

const ACTIVITY_MULTIPLIERS = {
  sedentary: 1.2,
  light: 1.375,
  moderate: 1.55,
  active: 1.725,
  very_active: 1.9,
} as const;

type ActivityLevel = keyof typeof ACTIVITY_MULTIPLIERS;

/**
 * 활동량을 고려한 일일 권장 칼로리(TDEE) 계산
 */
export function calculateTDEE(bmr: number, activityLevel: ActivityLevel): number {
  return Math.round(bmr * ACTIVITY_MULTIPLIERS[activityLevel]);
}

/**
 * 남은 칼로리 계산 (목표 - 섭취 + 소모)
 */
export function calculateRemainingCalories(
  target: number,
  consumed: number,
  burned: number,
): number {
  return target - consumed + burned;
}

/**
 * 영양소 비율 계산 (%)
 */
export function calculateNutrientRatio(
  carbs: number,
  protein: number,
  fat: number,
): {carbs: number; protein: number; fat: number} {
  const carbsCal = carbs * 4;
  const proteinCal = protein * 4;
  const fatCal = fat * 9;
  const total = carbsCal + proteinCal + fatCal;

  if (total === 0) return {carbs: 0, protein: 0, fat: 0};

  return {
    carbs: Math.round((carbsCal / total) * 100),
    protein: Math.round((proteinCal / total) * 100),
    fat: Math.round((fatCal / total) * 100),
  };
}
