import React from 'react';
import {Routes, Route, Navigate} from 'react-router-dom';
import {BottomNav} from '@yummy/ui';
import {AuthProvider, useAuth} from './contexts/AuthContext';
import {LoginPage} from './pages/login/LoginPage';
import {OnboardingPage} from './pages/onboarding/OnboardingPage';
import {HomePage} from './pages/home/HomePage';
import {DietPage} from './pages/diet/DietPage';
import {ExercisePage} from './pages/exercise/ExercisePage';
import {ProfilePage} from './pages/profile/ProfilePage';
import './App.css';

function AppRoutes() {
  const {isLoading, isLoggedIn, isOnboarded} = useAuth();

  if (isLoading) {
    return (
      <div className="app__loading">
        <span className="app__loading-spinner" />
      </div>
    );
  }

  // 비로그인 → 로그인 화면
  if (!isLoggedIn) {
    return <LoginPage />;
  }

  // 로그인은 됐지만 프로필 미입력 → 온보딩
  if (!isOnboarded) {
    return <OnboardingPage />;
  }

  // 정상 화면
  return (
    <div className="app">
      <main className="app__content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/diet" element={<DietPage />} />
          <Route path="/exercise" element={<ExercisePage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <BottomNav />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
