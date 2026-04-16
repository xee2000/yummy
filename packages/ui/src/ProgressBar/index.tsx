import React from 'react';
import {View, StyleSheet} from 'react-native';

interface ProgressBarProps {
  value: number;
  max?: number;
  color?: string;
  height?: number;
}

export function ProgressBar({value, max = 100, color = '#3D8EF0', height = 8}: ProgressBarProps) {
  const percent = Math.min(Math.max((value / max) * 100, 0), 100);

  return (
    <View style={[styles.track, {height}]}>
      <View style={[styles.fill, {width: `${percent}%`, backgroundColor: color, height}]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    width: '100%',
    backgroundColor: '#F3F4F6',
    borderRadius: 100,
    overflow: 'hidden',
  },
  fill: {
    borderRadius: 100,
  },
});
