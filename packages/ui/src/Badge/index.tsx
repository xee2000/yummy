import React from 'react';
import './Badge.css';

type BadgeColor = 'blue' | 'green' | 'red' | 'orange' | 'gray';

interface BadgeProps {
  label: string;
  color?: BadgeColor;
  style?: React.CSSProperties;
}

export function Badge({label, color = 'blue', style}: BadgeProps) {
  return (
    <span className={`badge badge--${color}`} style={style}>
      {label}
    </span>
  );
}
