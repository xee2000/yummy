import {apiClient} from './client';

export interface ExerciseItem {
  id: string;
  name: string;
  category: 'cardio' | 'strength' | 'flexibility' | 'sports';
  caloriesPerMinute: number;
  description?: string;
}

export interface ExerciseRecord {
  id: string;
  date: string;
  exerciseId: string;
  exerciseName: string;
  durationMinutes: number;
  caloriesBurned: number;
  intensity: 'low' | 'medium' | 'high';
}

export interface DailyExerciseSummary {
  date: string;
  totalCaloriesBurned: number;
  totalDurationMinutes: number;
  records: ExerciseRecord[];
}

export const exerciseApi = {
  searchExercise: (query: string): Promise<ExerciseItem[]> =>
    apiClient.get('/exercise/search', {params: {q: query}}),

  getDailySummary: (date: string): Promise<DailyExerciseSummary> =>
    apiClient.get(`/exercise/daily/${date}`),

  addRecord: (
    record: Omit<ExerciseRecord, 'id' | 'caloriesBurned'>,
  ): Promise<ExerciseRecord> => apiClient.post('/exercise/records', record),

  deleteRecord: (id: string): Promise<void> =>
    apiClient.delete(`/exercise/records/${id}`),
};
