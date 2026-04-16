import React from 'react';
import {Text as RNText, StyleSheet, TextProps as RNTextProps} from 'react-native';

type TextVariant = 'heading1' | 'heading2' | 'body1' | 'body2' | 'caption' | 'label';
type TextColor = 'primary' | 'secondary' | 'tertiary' | 'accent' | 'danger' | 'success';

interface TextProps extends RNTextProps {
  variant?: TextVariant;
  color?: TextColor;
  children: React.ReactNode;
}

export function Text({variant = 'body1', color = 'primary', style, children, ...props}: TextProps) {
  return (
    <RNText style={[styles[variant], styles[`color_${color}`], style]} {...props}>
      {children}
    </RNText>
  );
}

const styles = StyleSheet.create({
  heading1: {fontSize: 24, fontWeight: '700', lineHeight: 32},
  heading2: {fontSize: 20, fontWeight: '700', lineHeight: 28},
  body1: {fontSize: 16, fontWeight: '400', lineHeight: 24},
  body2: {fontSize: 14, fontWeight: '400', lineHeight: 20},
  caption: {fontSize: 12, fontWeight: '400', lineHeight: 16},
  label: {fontSize: 14, fontWeight: '600', lineHeight: 20},
  color_primary: {color: '#1A1A2E'},
  color_secondary: {color: '#6B7280'},
  color_tertiary: {color: '#9CA3AF'},
  color_accent: {color: '#3D8EF0'},
  color_danger: {color: '#EF4444'},
  color_success: {color: '#22C55E'},
});
