// src/common/SafeScreen.js
import React from 'react';
import { View, Platform, StatusBar, StyleSheet } from 'react-native';

// 1) 기본 폴백 훅(항상 존재)
let useInsets = function useInsetsFallback() {
  return {
    top: Platform.OS === 'android' ? StatusBar.currentHeight || 0 : 20,
    bottom: 0,
    left: 0,
    right: 0,
  };
};

// 2) 라이브러리가 있으면 실제 훅으로 교체 (모듈 로드 시 1회)
try {
  const sac = require('react-native-safe-area-context');
  if (sac?.useSafeAreaInsets) {
    useInsets = sac.useSafeAreaInsets;
  }
} catch {
  /* no-op */
}

export default function SafeScreen({
  children,
  edges = 'vertical',
  style,
  backgroundColor = '#FFFFFF',
  androidTranslucentStatusBar = true,
}) {
  // ✅ 항상 한 번 호출 (조건부 호출 금지)
  const insets = useInsets();

  const wanted = normalizeEdges(edges);

  const needTopPad =
    Platform.OS === 'android'
      ? androidTranslucentStatusBar && wanted.top
      : wanted.top;

  const paddingTop = needTopPad ? insets.top : 0;
  const paddingBottom = wanted.bottom ? insets.bottom : 0;
  const paddingLeft = wanted.left ? insets.left : 0;
  const paddingRight = wanted.right ? insets.right : 0;

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor,
          paddingTop,
          paddingBottom,
          paddingLeft,
          paddingRight,
        },
        style,
      ]}
    >
      {Platform.OS === 'android' && (
        <StatusBar
          translucent={androidTranslucentStatusBar}
          backgroundColor={
            androidTranslucentStatusBar ? 'transparent' : backgroundColor
          }
          barStyle={getBarStyle(backgroundColor)}
        />
      )}
      {children}
    </View>
  );
}

function normalizeEdges(edges) {
  const all = { top: true, bottom: true, left: true, right: true };
  const none = { top: false, bottom: false, left: false, right: false };

  if (edges === 'all') return all;
  if (edges === 'vertical') return { ...none, top: true, bottom: true };
  if (edges === 'top') return { ...none, top: true };
  if (edges === 'bottom') return { ...none, bottom: true };
  if (Array.isArray(edges)) {
    return {
      top: edges.includes('top'),
      bottom: edges.includes('bottom'),
      left: edges.includes('left'),
      right: edges.includes('right'),
    };
  }
  return { ...none, top: true, bottom: true };
}

function getBarStyle(bg) {
  try {
    const hex = bg.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    const luminance = 0.299 * r + 0.587 * g + 0.114 * b;
    return luminance > 160 ? 'dark-content' : 'light-content';
  } catch {
    return 'dark-content';
  }
}

const styles = StyleSheet.create({
  container: { flex: 1, minHeight: '100%' },
});
