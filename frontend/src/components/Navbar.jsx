import { NavLink } from 'react-router-dom';
import { LayoutDashboard, TrendingUp, Award, History, GraduationCap, LogOut, Wallet } from 'lucide-react';
import './Navbar.css';

export default function Navbar({ onLogout }) {
  const name = localStorage.getItem('name') || 'Student';
  const links = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/wallet', label: 'Wallet', icon: Wallet },
    { to: '/trade', label: 'Trade Stocks', icon: TrendingUp },
    { to: '/leaderboard', label: 'Leaderboard', icon: Award },
    { to: '/transactions', label: 'History', icon: History },
  ];
  return <aside className="sidebar glass-card">
    <div className="sidebar-logo"><GraduationCap className="logo-icon" size={32} aria-hidden="true" /><div className="logo-text"><h2>ApexTrade</h2><span>Virtual INR Simulator</span></div></div>
    <nav className="sidebar-menu" aria-label="Main navigation">
      {links.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}><Icon size={20} aria-hidden="true" /><span>{label}</span></NavLink>)}
      <button className="menu-item btn-logout" onClick={onLogout}><LogOut size={20} aria-hidden="true" /><span>Sign Out</span></button>
    </nav>
    <div className="sidebar-footer"><div className="user-avatar" aria-hidden="true">{name.substring(0, 2).toUpperCase()}</div><div className="user-info"><h3>{name}</h3><span>Practice. Learn. Invest.</span></div></div>
  </aside>;
}
