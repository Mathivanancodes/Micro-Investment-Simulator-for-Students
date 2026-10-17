import { useCallback } from 'react';
import { Trophy, Medal, RefreshCw } from 'lucide-react';
import { api } from '../services/api';
import useResource from '../hooks/useResource';
import { formatCurrency, formatPercent } from '../utils/format';
import './Leaderboard.css';

function RankBadge({ rank }) {
  if (rank === 1) return <Trophy className="rank-badge gold" size={20} aria-label="First place" />;
  if (rank <= 3) return <Medal className={`rank-badge ${rank === 2 ? 'silver' : 'bronze'}`} size={20} aria-label={`Rank ${rank}`} />;
  return <span className="rank-number">{rank}</span>;
}

export default function Leaderboard() {
  const currentUserId = Number(localStorage.getItem('userId'));
  const load = useCallback((config) => api.getLeaderboard(config), []);
  const { data: standings, loading, refreshing, error, refresh } = useResource(load, 30000);

  if (loading) return <div className="center-state" role="status"><RefreshCw className="spinner" size={40} /><p>Loading student rankings…</p></div>;
  if (!standings) return <div className="center-state error-state glass-card"><h1>Unable to load the leaderboard</h1><p role="alert">{error}</p><button className="btn btn-primary" onClick={refresh}>Try again</button></div>;

  return <div className="leaderboard-page animate-fade-in">
    <header className="page-header"><div><h1>Classroom Leaderboard</h1><p className="subtitle">Compare virtual portfolio performance with fellow students.</p></div><button className="btn btn-refresh" onClick={refresh} disabled={refreshing}><RefreshCw size={16} className={refreshing ? 'spinner' : ''} aria-hidden="true" /> Refresh</button></header>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    {standings.length > 0 && <section className="podium-section" aria-label="Top students">{standings.slice(0, 3).map((student) => <article key={student.userId} className={`podium-spot spot-${student.rank} glass-card`}><div className="podium-rank"><RankBadge rank={student.rank} /><span>#{student.rank}</span></div><div className="podium-avatar" aria-hidden="true">{student.name.slice(0, 2).toUpperCase()}</div><h2>{student.name}</h2><p className="podium-value">{formatCurrency(student.totalValue)}</p><span className={`podium-return ${Number(student.returnRate) >= 0 ? 'positive' : 'negative'}`}>{formatPercent(student.returnRate)}</span></article>)}</section>}
    <section className="standings-section glass-card"><h2>Current Standings</h2><div className="table-wrapper" tabIndex={0} role="region" aria-label="Student rankings table"><table className="standings-table"><thead><tr><th scope="col" className="rank-col">Rank</th><th scope="col">Student</th><th scope="col">Portfolio Value</th><th scope="col">Return Rate</th><th scope="col">Trades</th></tr></thead><tbody>{!standings.length ? <tr><td colSpan={5} className="empty-row">No student rankings yet.</td></tr> : standings.map((student) => <tr key={student.userId} className={student.userId === currentUserId ? 'current-user-row' : ''}><td className="rank-col"><RankBadge rank={student.rank} /></td><td className="student-name-col">{student.name}{student.userId === currentUserId && <span className="you-tag">YOU</span>}</td><td className="value-col">{formatCurrency(student.totalValue)}</td><td className={`return-col ${Number(student.returnRate) >= 0 ? 'positive' : 'negative'}`}>{formatPercent(student.returnRate)}</td><td>{student.tradesCount}</td></tr>)}</tbody></table></div></section>
    <p className="simulation-note">Return rates account for added virtual funds. Simulated results are for learning and do not predict real investment performance.</p>
  </div>;
}
