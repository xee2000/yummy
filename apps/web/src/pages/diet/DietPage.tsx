import React, {useState} from 'react';
import {Card, Text, Button, Badge} from '@yummy/ui';
import './DietPage.css';

type MealType = 'breakfast' | 'lunch' | 'dinner' | 'snack';
type Step = 'select_meal' | 'search_food' | 'confirm';

interface SelectedFood {name: string; calories: number;}

const MEAL_LABELS: Record<MealType, {label: string; emoji: string}> = {
  breakfast: {label: '아침', emoji: '🌅'},
  lunch: {label: '점심', emoji: '☀️'},
  dinner: {label: '저녁', emoji: '🌙'},
  snack: {label: '간식', emoji: '🍎'},
};

const MOCK_FOODS = [
  {name: '닭가슴살 100g', calories: 165, carbs: 0, protein: 31, fat: 3.6},
  {name: '현미밥 210g', calories: 320, carbs: 68, protein: 5, fat: 1.5},
  {name: '고구마 100g', calories: 86, carbs: 20, protein: 1.6, fat: 0.1},
  {name: '바나나 1개', calories: 89, carbs: 23, protein: 1.1, fat: 0.3},
  {name: '삶은 달걀 1개', calories: 78, carbs: 0.6, protein: 6.3, fat: 5.3},
  {name: '두부 150g', calories: 120, carbs: 2, protein: 13, fat: 6},
];

const STEPS: Step[] = ['select_meal', 'search_food', 'confirm'];
const STEP_LABELS = ['식사 선택', '음식 추가', '확인'];

export function DietPage() {
  const [step, setStep] = useState<Step>('select_meal');
  const [selectedMeal, setSelectedMeal] = useState<MealType | null>(null);
  const [query, setQuery] = useState('');
  const [selectedFoods, setSelectedFoods] = useState<SelectedFood[]>([]);

  const filtered = MOCK_FOODS.filter(f => f.name.includes(query));
  const stepIdx = STEPS.indexOf(step);

  const handleMealSelect = (meal: MealType) => {
    setSelectedMeal(meal);
    setStep('search_food');
  };

  const handleFoodToggle = (food: (typeof MOCK_FOODS)[0]) => {
    setSelectedFoods(prev =>
      prev.find(f => f.name === food.name)
        ? prev.filter(f => f.name !== food.name)
        : [...prev, {name: food.name, calories: food.calories}],
    );
  };

  const handleSubmit = () => {
    setStep('select_meal');
    setSelectedMeal(null);
    setSelectedFoods([]);
    setQuery('');
  };

  const handleBack = () => {
    if (step === 'search_food') setStep('select_meal');
    else if (step === 'confirm') setStep('search_food');
  };

  return (
    <div className="diet">
      <div className="diet__header">
        <Text variant="heading2">식단 기록</Text>
      </div>

      <div className="step-indicator">
        {STEPS.map((s, i) => (
          <React.Fragment key={s}>
            <div
              className={`step-dot ${step === s ? 'step-dot--active' : ''} ${stepIdx > i ? 'step-dot--done' : ''}`}>
              {stepIdx > i ? '✓' : i + 1}
            </div>
            {i < 2 && <div className={`step-line ${stepIdx > i ? 'step-line--done' : ''}`} />}
          </React.Fragment>
        ))}
      </div>
      <Text
        variant="label"
        style={{textAlign: 'center', display: 'block', marginBottom: 24}}>
        {STEP_LABELS[stepIdx]}
      </Text>

      {step === 'select_meal' && (
        <div className="meal-grid">
          {(Object.entries(MEAL_LABELS) as [MealType, {label: string; emoji: string}][]).map(
            ([meal, {label, emoji}]) => (
              <button
                key={meal}
                className={`meal-card ${selectedMeal === meal ? 'meal-card--active' : ''}`}
                onClick={() => handleMealSelect(meal)}>
                <span className="meal-card__emoji">{emoji}</span>
                <Text variant="label" color={selectedMeal === meal ? 'accent' : 'primary'}>
                  {label}
                </Text>
              </button>
            ),
          )}
        </div>
      )}

      {step === 'search_food' && (
        <div className="food-search">
          <Text variant="heading2" style={{marginBottom: 16}}>
            {MEAL_LABELS[selectedMeal!].emoji} {MEAL_LABELS[selectedMeal!].label} 음식 추가
          </Text>
          <input
            className="search-input"
            placeholder="음식 이름 검색"
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
          <div className="food-list">
            {filtered.map(food => {
              const isSelected = selectedFoods.some(f => f.name === food.name);
              return (
                <button
                  key={food.name}
                  className={`food-item ${isSelected ? 'food-item--selected' : ''}`}
                  onClick={() => handleFoodToggle(food)}>
                  <div className="food-item__info">
                    <Text variant="body1">{food.name}</Text>
                    <Text variant="caption" color="secondary">
                      탄 {food.carbs}g · 단 {food.protein}g · 지 {food.fat}g
                    </Text>
                  </div>
                  <Badge label={`${food.calories}kcal`} color={isSelected ? 'green' : 'blue'} />
                </button>
              );
            })}
          </div>
          {selectedFoods.length > 0 && (
            <div className="diet__footer">
              <Button
                label={`다음 (${selectedFoods.length}개)`}
                onClick={() => setStep('confirm')}
                fullWidth
              />
            </div>
          )}
        </div>
      )}

      {step === 'confirm' && (
        <div>
          <Text variant="heading2" style={{marginBottom: 16}}>
            기록 확인
          </Text>
          <Card style={{marginBottom: 16}}>
            <div className="confirm-header">
              <Badge label={MEAL_LABELS[selectedMeal!].label} color="blue" />
              <Text variant="body2" color="secondary">
                총 {selectedFoods.reduce((s, f) => s + f.calories, 0)}kcal
              </Text>
            </div>
            {selectedFoods.map((food, i) => (
              <div key={i} className="confirm-item">
                <Text variant="body2">{food.name}</Text>
                <Text variant="body2" color="accent">
                  {food.calories}kcal
                </Text>
              </div>
            ))}
          </Card>
          <Button label="저장하기" onClick={handleSubmit} fullWidth />
        </div>
      )}

      {step !== 'select_meal' && (
        <button className="back-btn" onClick={handleBack}>
          ← 이전
        </button>
      )}
    </div>
  );
}
