import React from 'react';
import {NavLink} from 'react-router-dom';
import './BottomNav.css';

const NAV_ITEMS = [
  {to: '/', emoji: '🏠', label: '홈'},
  {to: '/diet', emoji: '🥗', label: '식단'},
  {to: '/exercise', emoji: '💪', label: '운동'},
  {to: '/profile', emoji: '👤', label: '마이'},
];

export function BottomNav() {
  return (
    <nav className="bottom-nav">
      {NAV_ITEMS.map(item => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          className={({isActive}) =>
            `bottom-nav__item ${isActive ? 'bottom-nav__item--active' : ''}`
          }>
          <span className="bottom-nav__emoji">{item.emoji}</span>
          <span className="bottom-nav__label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
