import React, {createContext, useContext, useEffect, useState} from 'react';
import {userApi} from '@yummy/api';

export interface UserProfile {
  userKey: string;
  userId: number | null;   // 서버 DB의 numeric id
  name: string;
  height: number | null;
  age: number | null;
  weight: number | null;
}

interface AuthState {
  isLoading: boolean;
  isLoggedIn: boolean;
  isOnboarded: boolean;
  profile: UserProfile | null;
}

interface AuthContextValue extends AuthState {
  login: () => Promise<void>;
  saveProfile: (data: {name?: string; height: number; age: number; weight: number}) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const STORAGE_KEY = 'yummy_user';

export function AuthProvider({children}: {children: React.ReactNode}) {
  const [state, setState] = useState<AuthState>({
    isLoading: true,
    isLoggedIn: false,
    isOnboarded: false,
    profile: null,
  });

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const profile: UserProfile = JSON.parse(saved) as UserProfile;
      setState({
        isLoading: false,
        isLoggedIn: true,
        isOnboarded: !!(profile.height && profile.age && profile.weight),
        profile,
      });
    } else {
      setState(prev => ({...prev, isLoading: false}));
    }
  }, []);

  const login = async () => {
    try {
      let userKey: string;

      // Toss 앱 환경 감지
      try {
        const {appLogin} = await import('@apps-in-toss/web-bridge');
        const {authorizationCode} = await appLogin();
        userKey = `toss_${authorizationCode}`;
      } catch {
        // 일반 브라우저(개발 환경) - 고정 dev 키 사용
        userKey = 'dev_user';
      }

      // 서버에서 유저 조회 또는 신규 생성
      let serverUser;
      try {
        serverUser = await userApi.findByTossId(userKey);
      } catch {
        serverUser = await userApi.create({tossId: userKey, name: '사용자'});
      }

      const profile: UserProfile = {
        userKey,
        userId: serverUser.id,
        name: serverUser.name,
        height: serverUser.height,
        age: serverUser.age,
        weight: serverUser.weight,
      };

      localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
      setState({
        isLoading: false,
        isLoggedIn: true,
        isOnboarded: !!(profile.height && profile.age && profile.weight),
        profile,
      });
    } catch (e) {
      console.error('로그인 실패', e);
      throw e;
    }
  };

  const saveProfile = async (data: {name?: string; height: number; age: number; weight: number}) => {
    const profile = state.profile!;
    if (profile.userId) {
      await userApi.update(profile.userId, data);
    }
    const updated: UserProfile = {...profile, ...data};
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    setState(prev => ({...prev, isOnboarded: true, profile: updated}));
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setState({isLoading: false, isLoggedIn: false, isOnboarded: false, profile: null});
  };

  return (
    <AuthContext.Provider value={{...state, login, saveProfile, logout}}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
