import React from 'react';

type TextVariant = 'heading1' | 'heading2' | 'body1' | 'body2' | 'caption' | 'label';
type TextColor = 'primary' | 'secondary' | 'tertiary' | 'accent' | 'danger' | 'success';

const variantStyles: Record<TextVariant, React.CSSProperties> = {
  heading1: {fontSize: 24, fontWeight: 700, lineHeight: '32px'},
  heading2: {fontSize: 20, fontWeight: 700, lineHeight: '28px'},
  body1: {fontSize: 16, fontWeight: 400, lineHeight: '24px'},
  body2: {fontSize: 14, fontWeight: 400, lineHeight: '20px'},
  caption: {fontSize: 12, fontWeight: 400, lineHeight: '16px'},
  label: {fontSize: 14, fontWeight: 600, lineHeight: '20px'},
};

const colorStyles: Record<TextColor, string> = {
  primary: '#1A1A2E',
  secondary: '#6B7280',
  tertiary: '#9CA3AF',
  accent: '#3D8EF0',
  danger: '#EF4444',
  success: '#22C55E',
};

interface TextProps {
  variant?: TextVariant;
  color?: TextColor;
  children: React.ReactNode;
  style?: React.CSSProperties;
  className?: string;
}

export function Text({variant = 'body1', color = 'primary', children, style, className}: TextProps) {
  return (
    <span
      className={className}
      style={{...variantStyles[variant], color: colorStyles[color], ...style}}>
      {children}
    </span>
  );
}
