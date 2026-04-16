import React from 'react';
import './ProgressBar.css';

interface ProgressBarProps {
  value: number;
  max?: number;
  color?: string;
  height?: number;
}

export function ProgressBar({value, max = 100, color = '#3D8EF0', height = 8}: ProgressBarProps) {
  const percent = Math.min(Math.max((value / max) * 100, 0), 100);
  return (
    <div className="progress-track" style={{height}}>
      <div className="progress-fill" style={{width: `${percent}%`, background: color, height}} />
    </div>
  );
}
