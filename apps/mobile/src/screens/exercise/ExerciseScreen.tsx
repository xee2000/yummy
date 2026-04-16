import React, {useState} from 'react';
import {View, StyleSheet, TouchableOpacity, FlatList, TextInput} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Card, Text, Button, Badge, ProgressBar} from '@yummy/ui';

const EXERCISES = [
  {id: '1', name: '러닝', category: '유산소', emoji: '🏃', calPerMin: 10},
  {id: '2', name: '사이클', category: '유산소', emoji: '🚴', calPerMin: 8},
  {id: '3', name: '수영', category: '유산소', emoji: '🏊', calPerMin: 9},
  {id: '4', name: '스쿼트', category: '근력', emoji: '🏋️', calPerMin: 7},
  {id: '5', name: '푸시업', category: '근력', emoji: '💪', calPerMin: 6},
  {id: '6', name: '요가', category: '유연성', emoji: '🧘', calPerMin: 4},
  {id: '7', name: '줄넘기', category: '유산소', emoji: '⚡', calPerMin: 12},
  {id: '8', name: '플랭크', category: '근력', emoji: '🔥', calPerMin: 5},
];

const TODAY_RECORDS = [
  {name: '러닝', duration: 30, calories: 300, emoji: '🏃'},
];

export function ExerciseScreen() {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [duration, setDuration] = useState('30');

  const filtered = EXERCISES.filter(
    e => e.name.includes(query) || e.category.includes(query),
  );
  const selected = EXERCISES.find(e => e.id === selectedId);
  const estimatedCalories = selected ? selected.calPerMin * Number(duration || 0) : 0;

  const totalBurned = TODAY_RECORDS.reduce((sum, r) => sum + r.calories, 0);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.container}>
        {/* 오늘 요약 */}
        <Card style={styles.summaryCard}>
          <Text variant="label" color="secondary">
            오늘 소모 칼로리
          </Text>
          <Text variant="heading1" color="success">
            {totalBurned} kcal
          </Text>
          <ProgressBar value={totalBurned} max={500} color="#22C55E" height={6} />
          <Text variant="caption" color="secondary" style={styles.progressLabel}>
            목표 500kcal
          </Text>
          <View style={styles.records}>
            {TODAY_RECORDS.map((r, i) => (
              <View key={i} style={styles.recordItem}>
                <Text variant="body2">
                  {r.emoji} {r.name}
                </Text>
                <Text variant="caption" color="secondary">
                  {r.duration}분 · {r.calories}kcal
                </Text>
              </View>
            ))}
          </View>
        </Card>

        {/* 운동 추가 */}
        <Text variant="label" color="primary" style={styles.sectionTitle}>
          운동 추가
        </Text>
        <TextInput
          style={styles.searchInput}
          placeholder="운동 검색"
          value={query}
          onChangeText={setQuery}
          placeholderTextColor="#9CA3AF"
        />

        <FlatList
          data={filtered}
          keyExtractor={item => item.id}
          numColumns={2}
          columnWrapperStyle={styles.grid}
          renderItem={({item}) => (
            <TouchableOpacity
              style={[styles.exCard, selectedId === item.id && styles.exCardActive]}
              onPress={() => setSelectedId(prev => (prev === item.id ? null : item.id))}
              activeOpacity={0.8}>
              <Text variant="heading2">{item.emoji}</Text>
              <Text variant="label" color={selectedId === item.id ? 'accent' : 'primary'}>
                {item.name}
              </Text>
              <Badge
                label={item.category}
                color={item.category === '유산소' ? 'blue' : item.category === '근력' ? 'orange' : 'green'}
              />
            </TouchableOpacity>
          )}
          style={styles.list}
        />

        {selected && (
          <Card style={styles.addPanel}>
            <View style={styles.addRow}>
              <Text variant="label">
                {selected.emoji} {selected.name}
              </Text>
              <View style={styles.durationRow}>
                <TextInput
                  style={styles.durationInput}
                  value={duration}
                  onChangeText={setDuration}
                  keyboardType="number-pad"
                  maxLength={3}
                />
                <Text variant="body2" color="secondary">
                  분
                </Text>
              </View>
            </View>
            <Text variant="caption" color="secondary" style={styles.estimate}>
              예상 소모: {estimatedCalories}kcal
            </Text>
            <Button label="기록 추가" onPress={() => setSelectedId(null)} size="md" />
          </Card>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#F8FAFC'},
  container: {flex: 1, paddingHorizontal: 16},
  summaryCard: {marginTop: 16, marginBottom: 20},
  progressLabel: {textAlign: 'right', marginTop: 4},
  records: {marginTop: 12, gap: 6},
  recordItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
    borderTopWidth: 1,
    borderTopColor: '#F3F4F6',
  },
  sectionTitle: {marginBottom: 10},
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
  list: {flex: 1},
  grid: {gap: 10, marginBottom: 10},
  exCard: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 14,
    alignItems: 'center',
    gap: 6,
    borderWidth: 2,
    borderColor: '#F3F4F6',
  },
  exCardActive: {borderColor: '#3D8EF0', backgroundColor: '#EFF4FF'},
  addPanel: {marginTop: 8, marginBottom: 16},
  addRow: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8},
  durationRow: {flexDirection: 'row', alignItems: 'center', gap: 6},
  durationInput: {
    width: 60,
    backgroundColor: '#F8FAFC',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 16,
    textAlign: 'center',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    color: '#1A1A2E',
  },
  estimate: {marginBottom: 12},
});
