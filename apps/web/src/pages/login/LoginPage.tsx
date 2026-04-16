import React, {useState} from 'react';
import {useAuth} from '../../contexts/AuthContext';
import './LoginPage.css';

export function LoginPage() {
  const {login} = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    setLoading(true);
    setError('');
    try {
      await login();
    } catch {
      setError('로그인에 실패했어요. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login">
      <div className="login__top">
        <div className="login__logo">🥗</div>
        <h1 className="login__title">잘먹겠습니다</h1>
        <p className="login__subtitle">
          식단과 운동을 기록하고<br />
          건강한 습관을 만들어보세요
        </p>
      </div>

      <div className="login__features">
        {[
          {icon: '🍱', label: '식단 기록'},
          {icon: '💪', label: '운동 관리'},
          {icon: '📊', label: '칼로리 분석'},
        ].map(f => (
          <div key={f.label} className="login__feature">
            <span className="login__feature-icon">{f.icon}</span>
            <span className="login__feature-label">{f.label}</span>
          </div>
        ))}
      </div>

      <div className="login__bottom">
        {error && <p className="login__error">{error}</p>}
        <button
          className="login__toss-btn"
          onClick={handleLogin}
          disabled={loading}>
          {loading ? (
            <span className="login__spinner" />
          ) : (
            <>
              <span className="login__toss-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" fill="#fff"/>
                </svg>
              </span>
              토스로 시작하기
            </>
          )}
        </button>
        <p className="login__notice">
          로그인 시 서비스 이용약관 및 개인정보 처리방침에 동의합니다
        </p>
      </div>
    </div>
  );
}
