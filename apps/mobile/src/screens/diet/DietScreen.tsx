import React, {useState} from 'react';
import {View, StyleSheet, TouchableOpacity, FlatList, TextInput} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Card, Text, Button, Badge} from '@yummy/ui';

type MealType = 'breakfast' | 'lunch' | 'dinner' | 'snack';
type Step = 'select_meal' | 'search_food' | 'confirm';

interface SelectedFood {
  name: string;
  calories: number;
  amount: number;
}

const MEAL_LABELS: Record<MealType, string> = {
  breakfast: '아침',
  lunch: '점심',
  dinner: '저녁',
  snack: '간식',
};

const MOCK_FOODS = [
  {name: '닭가슴살 100g', calories: 165, carbs: 0, protein: 31, fat: 3.6},
  {name: '현미밥 210g', calories: 320, carbs: 68, protein: 5, fat: 1.5},
  {name: '고구마 100g', calories: 86, carbs: 20, protein: 1.6, fat: 0.1},
  {name: '바나나 1개', calories: 89, carbs: 23, protein: 1.1, fat: 0.3},
  {name: '삶은 달걀 1개', calories: 78, carbs: 0.6, protein: 6.3, fat: 5.3},
  {name: '두부 150g', calories: 120, carbs: 2, protein: 13, fat: 6},
];

export function DietScreen() {
  const [step, setStep] = useState<Step>('select_meal');
  const [selectedMeal, setSelectedMeal] = useState<MealType | null>(null);
  const [query, setQuery] = useState('');
  const [selectedFoods, setSelectedFoods] = useState<SelectedFood[]>([]);

  const filteredFoods = MOCK_FOODS.filter(f => f.name.includes(query));

  const handleMealSelect = (meal: MealType) => {
    setSelectedMeal(meal);
    setStep('search_food');
  };

  const handleFoodSelect = (food: (typeof MOCK_FOODS)[0]) => {
    setSelectedFoods(prev => {
      const existing = prev.find(f => f.name === food.name);
      if (existing) return prev;
      return [...prev, {name: food.name, calories: food.calories, amount: 1}];
    });
  };

  const handleNext = () => setStep('confirm');
  const handleBack = () => {
    if (step === 'search_food') setStep('select_meal');
    if (step === 'confirm') setStep('search_food');
  };
  const handleSubmit = () => {
    setStep('select_meal');
    setSelectedMeal(null);
    setSelectedFoods([]);
    setQuery('');
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.container}>
        {/* 스텝 인디케이터 */}
        <View style={styles.stepIndicator}>
          {(['select_meal', 'search_food', 'confirm'] as Step[]).map((s, i) => (
            <View key={s} style={styles.stepItem}>
              <View style={[styles.stepDot, step === s && styles.stepDotActive,
                (['select_meal', 'search_food', 'confirm'] as Step[]).indexOf(step) > i && styles.stepDotDone]}>
                <Text variant="caption" color={step === s ? 'accent' : 'tertiary'}>
                  {i + 1}
                </Text>
              </View>
              {i < 2 && <View style={styles.stepLine} />}
            </View>
          ))}
        </View>

        {/* Step 1: 식사 유형 선택 */}
        {step === 'select_meal' && (
          <View style={styles.stepContent}>
            <Text variant="heading2" style={styles.stepTitle}>
              어떤 식사를 기록할까요?
            </Text>
            <View style={styles.mealGrid}>
              {(Object.keys(MEAL_LABELS) as MealType[]).map(meal => (
                <TouchableOpacity
                  key={meal}
                  style={[styles.mealCard, selectedMeal === meal && styles.mealCardActive]}
                  onPress={() => handleMealSelect(meal)}
                  activeOpacity={0.8}>
                  <Text variant="heading1">{meal === 'breakfast' ? '🌅' : meal === 'lunch' ? '☀️' : meal === 'dinner' ? '🌙' : '🍎'}</Text>
                  <Text variant="label" color={selectedMeal === meal ? 'accent' : 'primary'}>
                    {MEAL_LABELS[meal]}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        {/* Step 2: 음식 검색 */}
        {step === 'search_food' && (
          <View style={styles.stepContent}>
            <Text variant="heading2" style={styles.stepTitle}>
              {MEAL_LABELS[selectedMeal!]} 음식을 추가하세요
            </Text>
            <TextInput
              style={styles.searchInput}
              placeholder="음식 이름을 검색하세요"
              value={query}
              onChangeText={setQuery}
              placeholderTextColor="#9CA3AF"
            />
            <FlatList
              data={filteredFoods}
              keyExtractor={item => item.name}
              renderItem={({item}) => {
                const isSelected = selectedFoods.some(f => f.name === item.name);
                return (
                  <TouchableOpacity
                    style={[styles.foodItem, isSelected && styles.foodItemSelected]}
                    onPress={() => handleFoodSelect(item)}
                    activeOpacity={0.8}>
                    <View style={styles.foodInfo}>
                      <Text variant="body1">{item.name}</Text>
                      <Text variant="caption" color="secondary">
                        탄 {item.carbs}g · 단 {item.protein}g · 지 {item.fat}g
                      </Text>
                    </View>
                    <Badge label={`${item.calories}kcal`} color={isSelected ? 'green' : 'blue'} />
                  </TouchableOpacity>
                );
              }}
              style={styles.foodList}
            />
            {selectedFoods.length > 0 && (
              <Button label={`다음 (${selectedFoods.length}개 선택)`} onPress={handleNext} />
            )}
          </View>
        )}

        {/* Step 3: 확인 */}
        {step === 'confirm' && (
          <View style={styles.stepContent}>
            <Text variant="heading2" style={styles.stepTitle}>
              기록을 확인하세요
            </Text>
            <Card style={styles.confirmCard}>
              <View style={styles.confirmHeader}>
                <Badge label={MEAL_LABELS[selectedMeal!]} color="blue" />
                <Text variant="body2" color="secondary">
                  총 {selectedFoods.reduce((sum, f) => sum + f.calories, 0)}kcal
                </Text>
              </View>
              {selectedFoods.map((food, i) => (
                <View key={i} style={styles.confirmItem}>
                  <Text variant="body2">{food.name}</Text>
                  <Text variant="body2" color="accent">
                    {food.calories}kcal
                  </Text>
                </View>
              ))}
            </Card>
            <Button label="저장하기" onPress={handleSubmit} style={styles.submitBtn} />
          </View>
        )}

        {step !== 'select_meal' && (
          <TouchableOpacity onPress={handleBack} style={styles.backBtn}>
            <Text variant="body2" color="accent">
              ← 이전
            </Text>
          </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#F8FAFC'},
  container: {flex: 1, paddingHorizontal: 16},
  stepIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 20,
  },
  stepItem: {flexDirection: 'row', alignItems: 'center'},
  stepDot: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#F3F4F6',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#E5E7EB',
  },
  stepDotActive: {borderColor: '#3D8EF0', backgroundColor: '#EFF4FF'},
  stepDotDone: {borderColor: '#22C55E', backgroundColor: '#F0FDF4'},
  stepLine: {width: 40, height: 2, backgroundColor: '#E5E7EB'},
  stepContent: {flex: 1},
  stepTitle: {marginBottom: 20},
  mealGrid: {flexDirection: 'row', flexWrap: 'wrap', gap: 12},
  mealCard: {
    width: '47%',
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 16,
    alignItems: 'center',
    gap: 8,
    borderWidth: 2,
    borderColor: '#E5E7EB',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.04,
    elevation: 2,
  },
  mealCardActive: {borderColor: '#3D8EF0', backgroundColor: '#EFF4FF'},
  searchInput: {
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#E5E7EB',
    marginBottom: 12,
    color: '#1A1A2E',
  },
  foodList: {flex: 1, marginBottom: 12},
  foodItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#F3F4F6',
  },
  foodItemSelected: {borderColor: '#22C55E', backgroundColor: '#F0FDF4'},
  foodInfo: {gap: 2, flex: 1},
  confirmCard: {marginBottom: 16},
  confirmHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  confirmItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#F3F4F6',
  },
  submitBtn: {marginTop: 8},
  backBtn: {paddingVertical: 16, alignItems: 'center'},
});
