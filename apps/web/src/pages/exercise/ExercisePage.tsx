import React, {useEffect, useState} from 'react';
import {Card, Text, Button, Badge} from '@yummy/ui';
import {exerciseApi, ExerciseType} from '@yummy/api';
import {useAuth} from '../../contexts/AuthContext';
import './ExercisePage.css';

const EMOJI_MAP: Record<string, string> = {
  running: '🏃', cycling: '🚴', swimming: '🏊', squat: '🏋️',
  bench_press: '🤸', deadlift: '💀', leg_press: '🦵',
  side_lateral_raise: '💪', overhead_press: '🙌', lat_pulldown: '🔝',
  barbell_row: '🏗️', leg_curl: '🦿', leg_extension: '🦾',
  dumbbell_curl: '💪', tricep_pushdown: '👇', pull_up: '🆙',
  push_up: '💪', plank: '🔥', side_plank: '🔥',
  burpee: '⚡', mountain_climber: '🧗', jump_rope: '⚡', yoga: '🧘',
};

const CATEGORY_COLOR: Record<string, 'blue' | 'orange' | 'green'> = {
  전신: 'blue', 하체: 'orange', 가슴: 'orange', '등/하체': 'orange',
  어깨: 'orange', 등: 'orange', 햄스트링: 'orange', 대퇴사두: 'orange',
  이두: 'orange', 삼두: 'orange', '등/이두': 'orange', '가슴/삼두': 'orange',
  코어: 'orange', 유연성: 'green', '코어/전신': 'blue',
};

// 세트 입력 상태
interface SetInput {
  setNumber: number;
  reps: string;
  weight: string;
  durationSeconds: string;
}

const defaultSet = (n: number): SetInput => ({
  setNumber: n, reps: '', weight: '', durationSeconds: '',
});

export function ExercisePage() {
  const {profile} = useAuth();
  const userId = profile?.userId;

  const [exercises, setExercises] = useState<ExerciseType[]>([]);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<ExerciseType | null>(null);
  const [sets, setSets] = useState<SetInput[]>([defaultSet(1)]);
  const [saving, setSaving] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    exerciseApi.getExerciseTypes().then(setExercises).catch(console.error);
  }, []);

  const filtered = exercises.filter(
    e => e.nameKo.includes(query) || (e.muscleGroup ?? '').includes(query),
  );

  const selectExercise = (ex: ExerciseType) => {
    setSelected(prev => (prev?.id === ex.id ? null : ex));
    setSets([defaultSet(1)]);
    setDone(false);
  };

  const addSet = () => setSets(prev => [...prev, defaultSet(prev.length + 1)]);

  const updateSet = (idx: number, field: keyof SetInput, value: string) => {
    setSets(prev => prev.map((s, i) => (i === idx ? {...s, [field]: value} : s)));
  };

  const handleSave = async () => {
    if (!selected) return;
    if (!userId) {
      setError('로그인이 필요합니다.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const today = new Date().toISOString().slice(0, 10);
      const payload = {
        workoutDate: today,
        sets: sets.map(s => ({
          exerciseTypeId: selected.id,
          setNumber: s.setNumber,
          ...(selected.trackingType !== 'time' && s.reps ? {reps: Number(s.reps)} : {}),
          ...(selected.trackingType !== 'time' && s.weight ? {weight: Number(s.weight)} : {}),
          ...(selected.trackingType !== 'reps' && s.durationSeconds
            ? {durationSeconds: Number(s.durationSeconds)}
            : {}),
        })),
      };
      await exerciseApi.createWorkoutLog(userId, payload);
      setDone(true);
      setSelected(null);
      setSets([defaultSet(1)]);
    } catch (e) {
      console.error(e);
      setError('저장에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="exercise">
      <div className="exercise__header">
        <Text variant="heading2">운동 기록</Text>
      </div>

      {done && (
        <Card style={{marginBottom: 12, background: '#ECFDF5'}}>
          <Text variant="body2" color="success">✅ 기록이 저장되었습니다!</Text>
        </Card>
      )}

      {error && (
        <Card style={{marginBottom: 12, background: '#FEF2F2'}}>
          <Text variant="body2" style={{color: '#EF4444'}}>⚠️ {error}</Text>
        </Card>
      )}

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
            className={`ex-card ${selected?.id === ex.id ? 'ex-card--active' : ''}`}
            onClick={() => selectExercise(ex)}>
            <span className="ex-card__emoji">{EMOJI_MAP[ex.name] ?? '🏅'}</span>
            <Text variant="label" color={selected?.id === ex.id ? 'accent' : 'primary'}>
              {ex.nameKo}
            </Text>
            <Badge
              label={ex.muscleGroup ?? ''}
              color={CATEGORY_COLOR[ex.muscleGroup ?? ''] ?? 'blue'}
            />
          </button>
        ))}
      </div>

      {selected && (
        <Card style={{marginTop: 12}}>
          <Text variant="label" style={{marginBottom: 12, display: 'block'}}>
            {EMOJI_MAP[selected.name] ?? '🏅'} {selected.nameKo}
          </Text>

          {/* 세트 목록 */}
          <div className="sets-header">
            <Text variant="caption" color="secondary">세트</Text>
            {selected.trackingType !== 'time' && (
              <>
                <Text variant="caption" color="secondary">횟수</Text>
                <Text variant="caption" color="secondary">중량(kg)</Text>
              </>
            )}
            {selected.trackingType !== 'reps' && (
              <Text variant="caption" color="secondary">시간(초)</Text>
            )}
          </div>

          {sets.map((s, i) => (
            <div key={i} className="set-row">
              <Text variant="body2" color="secondary" style={{minWidth: 24, textAlign: 'center'}}>
                {s.setNumber}
              </Text>
              {selected.trackingType !== 'time' && (
                <>
                  <input
                    className="set-input"
                    type="number"
                    min="1"
                    placeholder="횟수"
                    value={s.reps}
                    onChange={e => updateSet(i, 'reps', e.target.value)}
                  />
                  <input
                    className="set-input"
                    type="number"
                    min="0"
                    placeholder="중량"
                    value={s.weight}
                    onChange={e => updateSet(i, 'weight', e.target.value)}
                  />
                </>
              )}
              {selected.trackingType !== 'reps' && (
                <input
                  className="set-input"
                  type="number"
                  min="1"
                  placeholder="초"
                  value={s.durationSeconds}
                  onChange={e => updateSet(i, 'durationSeconds', e.target.value)}
                />
              )}
            </div>
          ))}

          <button className="add-set-btn" onClick={addSet}>+ 세트 추가</button>

          <Button
            label={saving ? '저장 중...' : '기록 추가'}
            onClick={handleSave}
            fullWidth
          />
        </Card>
      )}
    </div>
  );
}
