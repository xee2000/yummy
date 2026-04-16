import React, {useState} from 'react';
import {useAuth} from '../../contexts/AuthContext';
import {calculateBMR, calculateTDEE} from '@yummy/utils';
import './OnboardingPage.css';

type Step = 'height' | 'age' | 'weight' | 'done';

export function OnboardingPage() {
  const {saveProfile} = useAuth();
  const [step, setStep] = useState<Step>('height');
  const [values, setValues] = useState({height: '', age: '', weight: ''});

  const steps: Step[] = ['height', 'age', 'weight'];
  const stepIdx = steps.indexOf(step as Exclude<Step, 'done'>);

  const STEP_CONFIG = {
    height: {label: '키', unit: 'cm', emoji: '📏', min: 100, max: 250, placeholder: '예) 175'},
    age: {label: '나이', unit: '세', emoji: '🎂', min: 10, max: 100, placeholder: '예) 28'},
    weight: {label: '몸무게', unit: 'kg', emoji: '⚖️', min: 30, max: 300, placeholder: '예) 68'},
  };

  const currentKey = step as keyof typeof STEP_CONFIG;
  const config = step !== 'done' ? STEP_CONFIG[currentKey] : null;

  const handleNext = () => {
    if (step === 'height') setStep('age');
    else if (step === 'age') setStep('weight');
    else if (step === 'weight') {
      saveProfile({
        height: Number(values.height),
        age: Number(values.age),
        weight: Number(values.weight),
      });
      setStep('done');
    }
  };

  const isValid = step !== 'done' && values[currentKey] !== '' && Number(values[currentKey]) > 0;

  // 완료 화면 - TDEE 계산해서 보여주기
  if (step === 'done') {
    const bmr = calculateBMR('male', Number(values.weight), Number(values.height), Number(values.age));
    const tdee = calculateTDEE(bmr, 'moderate');
    return (
      <div className="onboarding onboarding--done">
        <div className="onboarding__done-emoji">🎉</div>
        <h2 className="onboarding__done-title">설정 완료!</h2>
        <p className="onboarding__done-subtitle">
          {values.height}cm · {values.age}세 · {values.weight}kg
        </p>
        <div className="onboarding__tdee-card">
          <p className="onboarding__tdee-label">일일 권장 칼로리</p>
          <p className="onboarding__tdee-value">{tdee} kcal</p>
          <p className="onboarding__tdee-desc">기초대사량 {bmr}kcal 기준</p>
        </div>
      </div>
    );
  }

  return (
    <div className="onboarding">
      <div className="onboarding__progress">
        {steps.map((s, i) => (
          <div
            key={s}
            className={`onboarding__dot ${i <= stepIdx ? 'onboarding__dot--active' : ''}`}
          />
        ))}
      </div>

      <div className="onboarding__content">
        <div className="onboarding__emoji">{config!.emoji}</div>
        <h2 className="onboarding__title">{config!.label}가 어떻게 되나요?</h2>
        <p className="onboarding__subtitle">
          정확한 칼로리 계산을 위해 필요해요
        </p>

        <div className="onboarding__input-wrap">
          <input
            className="onboarding__input"
            type="number"
            inputMode="numeric"
            placeholder={config!.placeholder}
            value={values[currentKey]}
            onChange={e =>
              setValues(prev => ({...prev, [currentKey]: e.target.value}))
            }
            autoFocus
          />
          <span className="onboarding__unit">{config!.unit}</span>
        </div>
      </div>

      <div className="onboarding__footer">
        <button
          className="onboarding__btn"
          onClick={handleNext}
          disabled={!isValid}>
          {step === 'weight' ? '시작하기' : '다음'}
        </button>
      </div>
    </div>
  );
}
