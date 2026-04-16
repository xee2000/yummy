import React from 'react';
import {View, StyleSheet, ScrollView} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Card, Text, ProgressBar, Badge} from '@yummy/ui';
import {formatDate, today} from '@yummy/utils';

const MOCK_DATA = {
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

export function HomeScreen() {
  const {targetCalories, consumedCalories, burnedCalories, nutrients, meals, exercises} =
    MOCK_DATA;
  const remaining = targetCalories - consumedCalories + burnedCalories;
  const progress = Math.min((consumedCalories / targetCalories) * 100, 100);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text variant="caption" color="secondary">
            {formatDate(today())}
          </Text>
          <Text variant="heading2">오늘의 식단</Text>
        </View>

        {/* 칼로리 요약 카드 */}
        <Card style={styles.card}>
          <View style={styles.calorieRow}>
            <View style={styles.calorieItem}>
              <Text variant="heading1" color="accent">
                {remaining}
              </Text>
              <Text variant="caption" color="secondary">
                남은 칼로리
              </Text>
            </View>
            <View style={styles.divider} />
            <View style={styles.calorieItem}>
              <Text variant="body1" color="primary">
                {consumedCalories}
              </Text>
              <Text variant="caption" color="secondary">
                섭취
              </Text>
            </View>
            <View style={styles.divider} />
            <View style={styles.calorieItem}>
              <Text variant="body1" color="success">
                {burnedCalories}
              </Text>
              <Text variant="caption" color="secondary">
                소모
              </Text>
            </View>
          </View>
          <View style={styles.progressWrap}>
            <ProgressBar value={progress} color="#3D8EF0" height={10} />
            <Text variant="caption" color="secondary" style={styles.progressLabel}>
              목표 {targetCalories}kcal 중 {Math.round(progress)}%
            </Text>
          </View>
        </Card>

        {/* 영양소 카드 */}
        <Card style={styles.card}>
          <Text variant="label" color="primary">
            영양소
          </Text>
          <View style={styles.nutrientRow}>
            <View style={styles.nutrientItem}>
              <ProgressBar value={nutrients.carbs} max={250} color="#3D8EF0" />
              <Text variant="caption" color="secondary" style={styles.nutrientLabel}>
                탄수화물 {nutrients.carbs}g
              </Text>
            </View>
            <View style={styles.nutrientItem}>
              <ProgressBar value={nutrients.protein} max={150} color="#22C55E" />
              <Text variant="caption" color="secondary" style={styles.nutrientLabel}>
                단백질 {nutrients.protein}g
              </Text>
            </View>
            <View style={styles.nutrientItem}>
              <ProgressBar value={nutrients.fat} max={80} color="#F97316" />
              <Text variant="caption" color="secondary" style={styles.nutrientLabel}>
                지방 {nutrients.fat}g
              </Text>
            </View>
          </View>
        </Card>

        {/* 식사 기록 */}
        <Card style={styles.card}>
          <Text variant="label" color="primary" style={styles.sectionTitle}>
            식사 기록
          </Text>
          {meals.map((meal, i) => (
            <View key={i} style={styles.mealRow}>
              <Badge label={meal.type} color="blue" />
              <Text variant="body2" color="secondary">
                {meal.time}
              </Text>
              <Text variant="body2" color="primary">
                {meal.calories}kcal
              </Text>
            </View>
          ))}
        </Card>

        {/* 운동 기록 */}
        <Card style={[styles.card, styles.lastCard]}>
          <Text variant="label" color="primary" style={styles.sectionTitle}>
            오늘의 운동
          </Text>
          {exercises.map((ex, i) => (
            <View key={i} style={styles.mealRow}>
              <Badge label={ex.name} color="green" />
              <Text variant="body2" color="secondary">
                {ex.duration}
              </Text>
              <Text variant="body2" color="success">
                -{ex.calories}kcal
              </Text>
            </View>
          ))}
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#F8FAFC'},
  container: {flex: 1, paddingHorizontal: 16},
  header: {paddingVertical: 20},
  card: {marginBottom: 12},
  lastCard: {marginBottom: 32},
  calorieRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    marginBottom: 16,
  },
  calorieItem: {alignItems: 'center', gap: 4},
  divider: {width: 1, height: 40, backgroundColor: '#E5E7EB'},
  progressWrap: {gap: 6},
  progressLabel: {textAlign: 'right'},
  nutrientRow: {gap: 10, marginTop: 10},
  nutrientItem: {gap: 4},
  nutrientLabel: {marginTop: 2},
  sectionTitle: {marginBottom: 12},
  mealRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
});
