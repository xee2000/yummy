import React, {useState} from 'react';
import {View, StyleSheet, ScrollView, TouchableOpacity} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Card, Text, Button, ProgressBar} from '@yummy/ui';
import {calculateBMR, calculateTDEE} from '@yummy/utils';

const MOCK_PROFILE = {
  name: '이정호',
  age: 30,
  gender: 'male' as const,
  height: 175,
  weight: 72,
  targetWeight: 68,
  activityLevel: 'moderate' as const,
};

const ACTIVITY_LABELS = {
  sedentary: '거의 활동 없음',
  light: '가벼운 활동 (주 1-3회)',
  moderate: '보통 활동 (주 3-5회)',
  active: '활발한 활동 (주 6-7회)',
  very_active: '매우 활발 (하루 2회)',
};

const WEEKLY_DATA = [
  {day: '월', calories: 1800},
  {day: '화', calories: 2100},
  {day: '수', calories: 1950},
  {day: '목', calories: 2200},
  {day: '금', calories: 1750},
  {day: '토', calories: 2400},
  {day: '일', calories: 1200},
];

export function ProfileScreen() {
  const [profile] = useState(MOCK_PROFILE);
  const bmr = calculateBMR(profile.gender, profile.weight, profile.height, profile.age);
  const tdee = calculateTDEE(bmr, profile.activityLevel);
  const weightProgress =
    ((profile.weight - profile.targetWeight) /
      (MOCK_PROFILE.weight - profile.targetWeight)) *
    100;
  const maxCal = Math.max(...WEEKLY_DATA.map(d => d.calories));

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
        {/* 프로필 헤더 */}
        <Card style={styles.profileCard}>
          <View style={styles.avatar}>
            <Text variant="heading1">👤</Text>
          </View>
          <Text variant="heading2" style={styles.name}>
            {profile.name}
          </Text>
          <Text variant="body2" color="secondary">
            {profile.age}세 · {profile.height}cm · {profile.weight}kg
          </Text>
          <Text variant="caption" color="accent" style={styles.activityLabel}>
            {ACTIVITY_LABELS[profile.activityLevel]}
          </Text>
        </Card>

        {/* 목표 체중 */}
        <Card style={styles.card}>
          <View style={styles.cardHeader}>
            <Text variant="label">목표 체중</Text>
            <Text variant="body2" color="accent">
              {profile.targetWeight}kg 목표
            </Text>
          </View>
          <View style={styles.weightRow}>
            <Text variant="heading2">{profile.weight}kg</Text>
            <Text variant="body2" color="secondary">
              → {profile.targetWeight}kg
            </Text>
          </View>
          <ProgressBar
            value={Math.max(0, 100 - weightProgress)}
            max={100}
            color="#3D8EF0"
            height={8}
          />
          <Text variant="caption" color="secondary" style={styles.caption}>
            목표까지 {profile.weight - profile.targetWeight}kg 남음
          </Text>
        </Card>

        {/* 칼로리 정보 */}
        <Card style={styles.card}>
          <Text variant="label" style={styles.cardTitle}>
            나의 칼로리 정보
          </Text>
          <View style={styles.calRow}>
            <View style={styles.calItem}>
              <Text variant="heading2" color="secondary">
                {bmr}
              </Text>
              <Text variant="caption" color="secondary">
                기초대사량
              </Text>
            </View>
            <View style={styles.calDivider} />
            <View style={styles.calItem}>
              <Text variant="heading2" color="accent">
                {tdee}
              </Text>
              <Text variant="caption" color="secondary">
                권장 칼로리
              </Text>
            </View>
          </View>
          <Text variant="caption" color="tertiary" style={styles.caption}>
            Harris-Benedict 공식 기준
          </Text>
        </Card>

        {/* 주간 칼로리 차트 */}
        <Card style={styles.card}>
          <Text variant="label" style={styles.cardTitle}>
            이번 주 섭취 칼로리
          </Text>
          <View style={styles.chart}>
            {WEEKLY_DATA.map((d, i) => (
              <View key={i} style={styles.barWrap}>
                <Text variant="caption" color="secondary">
                  {d.calories}
                </Text>
                <View style={styles.barTrack}>
                  <View
                    style={[
                      styles.bar,
                      {
                        height: `${(d.calories / maxCal) * 100}%`,
                        backgroundColor: d.calories > tdee ? '#EF4444' : '#3D8EF0',
                      },
                    ]}
                  />
                </View>
                <Text variant="caption" color="secondary">
                  {d.day}
                </Text>
              </View>
            ))}
          </View>
        </Card>

        {/* 설정 메뉴 */}
        <Card style={[styles.card, styles.lastCard]}>
          {[
            {label: '프로필 수정', icon: '✏️'},
            {label: '목표 설정', icon: '🎯'},
            {label: '알림 설정', icon: '🔔'},
            {label: '로그아웃', icon: '👋'},
          ].map((item, i) => (
            <TouchableOpacity key={i} style={styles.menuItem} activeOpacity={0.7}>
              <Text variant="body1">
                {item.icon} {item.label}
              </Text>
              <Text variant="body2" color="tertiary">
                →
              </Text>
            </TouchableOpacity>
          ))}
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#F8FAFC'},
  container: {flex: 1, paddingHorizontal: 16},
  profileCard: {alignItems: 'center', marginTop: 16, marginBottom: 12, gap: 4},
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#EFF4FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  name: {marginTop: 4},
  activityLabel: {marginTop: 4},
  card: {marginBottom: 12},
  lastCard: {marginBottom: 32},
  cardHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10},
  cardTitle: {marginBottom: 12},
  weightRow: {flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 10},
  caption: {marginTop: 6, textAlign: 'right'},
  calRow: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around'},
  calItem: {alignItems: 'center', gap: 4},
  calDivider: {width: 1, height: 40, backgroundColor: '#E5E7EB'},
  chart: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', height: 120},
  barWrap: {alignItems: 'center', flex: 1, gap: 4},
  barTrack: {width: 20, height: 70, backgroundColor: '#F3F4F6', borderRadius: 4, justifyContent: 'flex-end', overflow: 'hidden'},
  bar: {width: '100%', borderRadius: 4},
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
});
