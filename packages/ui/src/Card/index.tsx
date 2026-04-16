import React from 'react';
import './Card.css';

interface CardProps {
  children: React.ReactNode;
  style?: React.CSSProperties;
  padding?: number;
  className?: string;
}

export function Card({children, style, padding = 16, className}: CardProps) {
  return (
    <div className={`card ${className ?? ''}`} style={{padding, ...style}}>
      {children}
    </div>
  );
}
