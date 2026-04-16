import {apiClient} from './client';

export interface FoodItem {
  id: string;
  name: string;
  calories: number;
  carbs: number;
  protein: number;
  fat: number;
  servingSize: number;
  unit: string;
}

export interface MealRecord {
  id: string;
  date: string;
  mealType: 'breakfast' | 'lunch' | 'dinner' | 'snack';
  foods: FoodItem[];
  totalCalories: number;
}

export interface DailyDietSummary {
  date: string;
  totalCalories: number;
  targetCalories: number;
  meals: MealRecord[];
  nutrients: {
    carbs: number;
    protein: number;
    fat: number;
  };
}

export const dietApi = {
  searchFood: (query: string): Promise<FoodItem[]> =>
    apiClient.get('/diet/foods/search', {params: {q: query}}),

  getDailySummary: (date: string): Promise<DailyDietSummary> =>
    apiClient.get(`/diet/daily/${date}`),

  addMealRecord: (
    record: Omit<MealRecord, 'id' | 'totalCalories'>,
  ): Promise<MealRecord> => apiClient.post('/diet/meals', record),

  deleteMealRecord: (id: string): Promise<void> =>
    apiClient.delete(`/diet/meals/${id}`),
};
