import {apiClient} from './client';

export type TrackingType = 'reps' | 'time' | 'both';

export interface ExerciseType {
  id: number;
  name: string;
  nameKo: string;
  muscleGroup: string | null;
  trackingType: TrackingType;
}

export interface WorkoutSetPayload {
  exerciseTypeId: number;
  setNumber: number;
  reps?: number;
  weight?: number;
  durationSeconds?: number;
}

export interface CreateWorkoutLogPayload {
  workoutDate: string; // 'YYYY-MM-DD'
  notes?: string;
  sets: WorkoutSetPayload[];
}

export const exerciseApi = {
  /** 운동 종목 전체 조회 */
  getExerciseTypes: (): Promise<ExerciseType[]> =>
    apiClient.get('/exercise-types'),

  /** 운동 기록 저장 */
  createWorkoutLog: (userId: number, payload: CreateWorkoutLogPayload) =>
    apiClient.post(`/users/${userId}/workout-logs`, payload),

  /** 날짜별 운동 기록 조회 */
  getWorkoutLogByDate: (userId: number, date: string) =>
    apiClient.get(`/users/${userId}/workout-logs/by-date`, {params: {date}}),

  /** 종목별 오늘 추천 */
  getRecommendation: (userId: number, exerciseTypeId: number) =>
    apiClient.get(`/users/${userId}/recommendations/exercise/${exerciseTypeId}`),

  /** 영양 추천 */
  getNutritionRecommendation: (userId: number, date: string) =>
    apiClient.get(`/users/${userId}/recommendations/nutrition`, {params: {date}}),
};
