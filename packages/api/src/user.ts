import {apiClient} from './client';

export interface UserProfile {
  id: string;
  name: string;
  age: number;
  gender: 'male' | 'female';
  height: number;
  weight: number;
  targetWeight?: number;
  targetCalories: number;
  activityLevel: 'sedentary' | 'light' | 'moderate' | 'active' | 'very_active';
}

export interface WeeklyReport {
  weekStart: string;
  weekEnd: string;
  averageCaloriesIn: number;
  averageCaloriesOut: number;
  weightChange: number;
  exerciseDays: number;
}

export const userApi = {
  getProfile: (): Promise<UserProfile> => apiClient.get('/user/profile'),

  updateProfile: (data: Partial<UserProfile>): Promise<UserProfile> =>
    apiClient.put('/user/profile', data),

  getWeeklyReport: (weekStart: string): Promise<WeeklyReport> =>
    apiClient.get('/user/report/weekly', {params: {weekStart}}),
};
