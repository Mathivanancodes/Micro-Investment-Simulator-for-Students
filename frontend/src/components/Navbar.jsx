import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, TrendingUp, Award, History, GraduationCap, LogOut } from 'lucide-react';
import './Navbar.css';

const Navbar = ({ onLogout }) => {
  const username = localStorage.getItem('username') || 'Student User';
  const userId = localStorage.getItem('userId') || '1';

  return (
    <aside className="sidebar glass-card">
      <div className="sidebar-logo">
        <GraduationCap className="logo-icon" size={32} />
        <div className="logo-text">
          <h2>ApexTrade</h2>
          <span>Simulator</span>
        </div>
      </div>

      <nav className="sidebar-menu">
        <NavLink 
          to="/" 
          className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={20} />
          <span>Dashboard</span>
        </NavLink>

        <NavLink 
          to="/trade" 
          className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}
        >
          <TrendingUp size={20} />
          <span>Trade Stocks</span>
        </NavLink>

        <NavLink 
          to="/leaderboard" 
          className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}
        >
          <Award size={20} />
          <span>Leaderboard</span>
        </NavLink>

        <NavLink 
          to="/transactions" 
          className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}
        >
          <History size={20} />
          <span>History</span>
        </NavLink>
        
        <button className="menu-item btn-logout" onClick={onLogout} style={{ background: 'transparent', border: 'none', width: '100%', textAlign: 'left', cursor: 'pointer' }}>
          <LogOut size={20} />
          <span>Sign Out</span>
        </button>
      </nav>

      <div className="sidebar-footer">
        <div className="user-avatar">
          {username.substring(0, 2).toUpperCase()}
        </div>
        <div className="user-info">
          <h4>{username}</h4>
          <span>ID: #{userId}</span>
        </div>
      </div>
    </aside>
  );
};

export default Navbar;
