import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
  ViewStyle,
  TextStyle,
} from 'react-native';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps {
  label: string;
  onPress: () => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  disabled?: boolean;
  loading?: boolean;
  style?: ViewStyle;
}

export function Button({
  label,
  onPress,
  variant = 'primary',
  size = 'md',
  disabled = false,
  loading = false,
  style,
}: ButtonProps) {
  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled || loading}
      style={[styles.base, styles[variant], styles[size], disabled && styles.disabled, style]}
      activeOpacity={0.8}>
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? '#fff' : '#3D8EF0'} size="small" />
      ) : (
        <Text style={[styles.label, styles[`${variant}Label`] as TextStyle]}>{label}</Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  base: {
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 12,
  },
  primary: {
    backgroundColor: '#3D8EF0',
  },
  secondary: {
    backgroundColor: '#EFF4FF',
    borderWidth: 1,
    borderColor: '#3D8EF0',
  },
  ghost: {
    backgroundColor: 'transparent',
  },
  sm: {
    paddingVertical: 8,
    paddingHorizontal: 16,
  },
  md: {
    paddingVertical: 14,
    paddingHorizontal: 24,
  },
  lg: {
    paddingVertical: 18,
    paddingHorizontal: 32,
  },
  disabled: {
    opacity: 0.4,
  },
  label: {
    fontWeight: '600',
    fontSize: 16,
  },
  primaryLabel: {
    color: '#fff',
  },
  secondaryLabel: {
    color: '#3D8EF0',
  },
  ghostLabel: {
    color: '#3D8EF0',
  },
});
