import React from 'react';
import {View, Text, StyleSheet, ViewStyle} from 'react-native';

type BadgeColor = 'blue' | 'green' | 'red' | 'orange' | 'gray';

interface BadgeProps {
  label: string;
  color?: BadgeColor;
  style?: ViewStyle;
}

const colorMap: Record<BadgeColor, {bg: string; text: string}> = {
  blue: {bg: '#EFF4FF', text: '#3D8EF0'},
  green: {bg: '#F0FDF4', text: '#22C55E'},
  red: {bg: '#FEF2F2', text: '#EF4444'},
  orange: {bg: '#FFF7ED', text: '#F97316'},
  gray: {bg: '#F9FAFB', text: '#6B7280'},
};

export function Badge({label, color = 'blue', style}: BadgeProps) {
  const colors = colorMap[color];
  return (
    <View style={[styles.badge, {backgroundColor: colors.bg}, style]}>
      <Text style={[styles.label, {color: colors.text}]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 100,
    alignSelf: 'flex-start',
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
});
