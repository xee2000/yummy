import React from 'react';
import {Card, Text, ProgressBar, Badge} from '@yummy/ui';
import {formatDate, today} from '@yummy/utils';
import './HomePage.css';

const MOCK = {
  targetCalories: 2000,
  consumedCalories: 1200,
  burnedCalories: 300,
  nutrients: {carbs: 150, protein: 80, fat: 40},
  meals: [
    {type: '아침', calories: 400, time: '08:30'},
    {type: '점심', calories: 600, time: '12:15'},
    {type: '간식', calories: 200, time: '15:00'},
  ],
  exercises: [{name: '러닝', duration: '30분', calories: 300}],
};

export function HomePage() {
  const {targetCalories, consumedCalories, burnedCalories, nutrients, meals, exercises} = MOCK;
  const remaining = targetCalories - consumedCalories + burnedCalories;
  const progress = Math.min((consumedCalories / targetCalories) * 100, 100);

  return (
    <div className="home">
      <div className="home__header">
        <Text variant="caption" color="secondary">{formatDate(today())}</Text>
        <Text variant="heading2">오늘의 식단</Text>
      </div>

      <Card className="home__card">
        <div className="calorie-row">
          <div className="calorie-item">
            <Text variant="heading1" color="accent">{remaining}</Text>
            <Text variant="caption" color="secondary">남은 칼로리</Text>
          </div>
          <div className="calorie-divider" />
          <div className="calorie-item">
            <Text variant="body1">{consumedCalories}</Text>
            <Text variant="caption" color="secondary">섭취</Text>
          </div>
          <div className="calorie-divider" />
          <div className="calorie-item">
            <Text variant="body1" color="success">{burnedCalories}</Text>
            <Text variant="caption" color="secondary">소모</Text>
          </div>
        </div>
        <ProgressBar value={progress} color="#3D8EF0" height={10} />
        <Text variant="caption" color="secondary" style={{textAlign: 'right', marginTop: 6}}>
          목표 {targetCalories}kcal 중 {Math.round(progress)}%
        </Text>
      </Card>

      <Card className="home__card">
        <Text variant="label">영양소</Text>
        <div className="nutrient-list">
          <div className="nutrient-item">
            <div className="nutrient-label-row">
              <Text variant="caption" color="secondary">탄수화물</Text>
              <Text variant="caption" color="secondary">{nutrients.carbs}g</Text>
            </div>
            <ProgressBar value={nutrients.carbs} max={250} color="#3D8EF0" height={6} />
          </div>
          <div className="nutrient-item">
            <div className="nutrient-label-row">
              <Text variant="caption" color="secondary">단백질</Text>
              <Text variant="caption" color="secondary">{nutrients.protein}g</Text>
            </div>
            <ProgressBar value={nutrients.protein} max={150} color="#22C55E" height={6} />
          </div>
          <div className="nutrient-item">
            <div className="nutrient-label-row">
              <Text variant="caption" color="secondary">지방</Text>
              <Text variant="caption" color="secondary">{nutrients.fat}g</Text>
            </div>
            <ProgressBar value={nutrients.fat} max={80} color="#F97316" height={6} />
          </div>
        </div>
      </Card>

      <Card className="home__card">
        <Text variant="label" style={{marginBottom: 12}}>식사 기록</Text>
        {meals.map((meal, i) => (
          <div key={i} className="record-row">
            <Badge label={meal.type} color="blue" />
            <Text variant="body2" color="secondary">{meal.time}</Text>
            <Text variant="body2">{meal.calories}kcal</Text>
          </div>
        ))}
      </Card>

      <Card className="home__card">
        <Text variant="label" style={{marginBottom: 12}}>오늘의 운동</Text>
        {exercises.map((ex, i) => (
          <div key={i} className="record-row">
            <Badge label={ex.name} color="green" />
            <Text variant="body2" color="secondary">{ex.duration}</Text>
            <Text variant="body2" color="success">-{ex.calories}kcal</Text>
          </div>
        ))}
      </Card>
    </div>
  );
}
