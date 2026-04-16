import React, {createContext, useContext, useEffect, useState} from 'react';

export interface UserProfile {
  userKey: string;
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
  saveProfile: (data: {height: number; age: number; weight: number}) => void;
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
    // 저장된 프로필 불러오기
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const profile: UserProfile = JSON.parse(saved);
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

      // Toss 앱 환경 감지 (bridge 사용 가능한 경우)
      try {
        const {appLogin} = await import('@apps-in-toss/web-bridge');
        const {authorizationCode} = await appLogin();
        userKey = `toss_${authorizationCode.slice(0, 8)}`;
      } catch {
        // 일반 브라우저(개발 환경) - 임시 게스트 로그인
        userKey = `dev_${Date.now()}`;
      }

      const profile: UserProfile = {userKey, height: null, age: null, weight: null};
      localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
      setState({
        isLoading: false,
        isLoggedIn: true,
        isOnboarded: false,
        profile,
      });
    } catch (e) {
      console.error('로그인 실패', e);
      throw e;
    }
  };

  const saveProfile = (data: {height: number; age: number; weight: number}) => {
    const profile: UserProfile = {...state.profile!, ...data};
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
    setState(prev => ({...prev, isOnboarded: true, profile}));
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
