import React from 'react';
import {Card, Text, ProgressBar} from '@yummy/ui';
import {calculateBMR, calculateTDEE} from '@yummy/utils';
import './ProfilePage.css';

const PROFILE = {
  name: '이정호', age: 30, gender: 'male' as const,
  height: 175, weight: 72, targetWeight: 68,
  activityLevel: 'moderate' as const,
};

const ACTIVITY_LABELS = {
  sedentary: '거의 활동 없음', light: '가벼운 활동', moderate: '보통 활동',
  active: '활발한 활동', very_active: '매우 활발',
};

const WEEKLY_DATA = [
  {day: '월', cal: 1800}, {day: '화', cal: 2100}, {day: '수', cal: 1950},
  {day: '목', cal: 2200}, {day: '금', cal: 1750}, {day: '토', cal: 2400}, {day: '일', cal: 1200},
];

const MENUS = [
  {label: '프로필 수정', icon: '✏️'},
  {label: '목표 설정', icon: '🎯'},
  {label: '알림 설정', icon: '🔔'},
  {label: '로그아웃', icon: '👋'},
];

export function ProfilePage() {
  const bmr = calculateBMR(PROFILE.gender, PROFILE.weight, PROFILE.height, PROFILE.age);
  const tdee = calculateTDEE(bmr, PROFILE.activityLevel);
  const maxCal = Math.max(...WEEKLY_DATA.map(d => d.cal));
  const weightProgress = ((PROFILE.weight - PROFILE.targetWeight) / (72 - PROFILE.targetWeight)) * 100;

  return (
    <div className="profile">
      <Card className="profile__card">
        <div className="avatar">👤</div>
        <Text variant="heading2">{PROFILE.name}</Text>
        <Text variant="body2" color="secondary">
          {PROFILE.age}세 · {PROFILE.height}cm · {PROFILE.weight}kg
        </Text>
        <Text variant="caption" color="accent" style={{marginTop: 4}}>
          {ACTIVITY_LABELS[PROFILE.activityLevel]}
        </Text>
      </Card>

      <Card style={{marginBottom: 12}}>
        <div className="card-header">
          <Text variant="label">목표 체중</Text>
          <Text variant="body2" color="accent">{PROFILE.targetWeight}kg 목표</Text>
        </div>
        <div className="weight-row">
          <Text variant="heading2">{PROFILE.weight}kg</Text>
          <Text variant="body2" color="secondary">→ {PROFILE.targetWeight}kg</Text>
        </div>
        <ProgressBar value={Math.max(0, 100 - weightProgress)} max={100} color="#3D8EF0" height={8} />
        <Text variant="caption" color="secondary" style={{textAlign: 'right', marginTop: 6}}>
          목표까지 {PROFILE.weight - PROFILE.targetWeight}kg 남음
        </Text>
      </Card>

      <Card style={{marginBottom: 12}}>
        <Text variant="label" style={{marginBottom: 12, display: 'block'}}>나의 칼로리 정보</Text>
        <div className="cal-row">
          <div className="cal-item">
            <Text variant="heading2" color="secondary">{bmr}</Text>
            <Text variant="caption" color="secondary">기초대사량</Text>
          </div>
          <div className="cal-divider" />
          <div className="cal-item">
            <Text variant="heading2" color="accent">{tdee}</Text>
            <Text variant="caption" color="secondary">권장 칼로리</Text>
          </div>
        </div>
        <Text variant="caption" color="tertiary" style={{textAlign: 'center', marginTop: 8, display: 'block'}}>
          Harris-Benedict 공식 기준
        </Text>
      </Card>

      <Card style={{marginBottom: 12}}>
        <Text variant="label" style={{marginBottom: 16, display: 'block'}}>이번 주 섭취 칼로리</Text>
        <div className="chart">
          {WEEKLY_DATA.map((d, i) => (
            <div key={i} className="bar-wrap">
              <Text variant="caption" color="secondary" style={{fontSize: 10}}>{d.cal}</Text>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    height: `${(d.cal / maxCal) * 100}%`,
                    background: d.cal > tdee ? '#EF4444' : '#3D8EF0',
                  }}
                />
              </div>
              <Text variant="caption" color="secondary">{d.day}</Text>
            </div>
          ))}
        </div>
      </Card>

      <Card style={{marginBottom: 32}}>
        {MENUS.map((item, i) => (
          <button key={i} className="menu-item">
            <Text variant="body1">{item.icon} {item.label}</Text>
            <Text variant="body2" color="tertiary">→</Text>
          </button>
        ))}
      </Card>
    </div>
  );
}
