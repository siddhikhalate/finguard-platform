import React from 'react';

const RiskBadge = ({ level, score }) => {
  const styles = {
    LOW: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    MEDIUM: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
    HIGH: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
    CRITICAL: 'bg-rose-500/10 text-rose-400 border-rose-500/30 animate-pulse',
  };

  const badgeStyle = styles[level] || 'bg-slate-500/10 text-slate-400 border-slate-500/30';

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeStyle}`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
      {level} {score !== undefined && `(${score})`}
    </span>
  );
};

export default RiskBadge;
