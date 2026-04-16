import React, {useState} from 'react';
import {Card, Text, Button, Badge, ProgressBar} from '@yummy/ui';
import './ExercisePage.css';

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

const TODAY_RECORDS = [{name: '러닝', duration: 30, calories: 300, emoji: '🏃'}];

export function ExercisePage() {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [duration, setDuration] = useState('30');

  const filtered = EXERCISES.filter(e => e.name.includes(query) || e.category.includes(query));
  const selected = EXERCISES.find(e => e.id === selectedId);
  const estimatedCalories = selected ? selected.calPerMin * Number(duration || 0) : 0;
  const totalBurned = TODAY_RECORDS.reduce((s, r) => s + r.calories, 0);

  return (
    <div className="exercise">
      <div className="exercise__header">
        <Text variant="heading2">운동 기록</Text>
      </div>

      <Card style={{marginBottom: 20}}>
        <Text variant="label" color="secondary">오늘 소모 칼로리</Text>
        <Text variant="heading1" color="success" style={{margin: '6px 0'}}>
          {totalBurned} kcal
        </Text>
        <ProgressBar value={totalBurned} max={500} color="#22C55E" height={6} />
        <Text variant="caption" color="secondary" style={{textAlign: 'right', marginTop: 4}}>
          목표 500kcal
        </Text>
        <div className="today-records">
          {TODAY_RECORDS.map((r, i) => (
            <div key={i} className="today-record-item">
              <Text variant="body2">{r.emoji} {r.name}</Text>
              <Text variant="caption" color="secondary">{r.duration}분 · {r.calories}kcal</Text>
            </div>
          ))}
        </div>
      </Card>

      <Text variant="label" style={{marginBottom: 10, display: 'block'}}>운동 추가</Text>
      <input
        className="ex-search"
        placeholder="운동 검색"
        value={query}
        onChange={e => setQuery(e.target.value)}
      />

      <div className="ex-grid">
        {filtered.map(ex => (
          <button
            key={ex.id}
            className={`ex-card ${selectedId === ex.id ? 'ex-card--active' : ''}`}
            onClick={() => setSelectedId(prev => prev === ex.id ? null : ex.id)}>
            <span className="ex-card__emoji">{ex.emoji}</span>
            <Text variant="label" color={selectedId === ex.id ? 'accent' : 'primary'}>{ex.name}</Text>
            <Badge
              label={ex.category}
              color={ex.category === '유산소' ? 'blue' : ex.category === '근력' ? 'orange' : 'green'}
            />
          </button>
        ))}
      </div>

      {selected && (
        <Card style={{marginTop: 12}}>
          <div className="ex-add-row">
            <Text variant="label">{selected.emoji} {selected.name}</Text>
            <div className="duration-row">
              <input
                className="duration-input"
                value={duration}
                onChange={e => setDuration(e.target.value)}
                type="number"
                min="1"
                max="300"
              />
              <Text variant="body2" color="secondary">분</Text>
            </div>
          </div>
          <Text variant="caption" color="secondary" style={{marginBottom: 12, display: 'block'}}>
            예상 소모: {estimatedCalories}kcal
          </Text>
          <Button label="기록 추가" onClick={() => setSelectedId(null)} fullWidth />
        </Card>
      )}
    </div>
  );
}
