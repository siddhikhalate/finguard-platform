import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, UserCircle, LogOut } from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 border-b border-slate-800 bg-slate-900/95 sticky top-0 z-30 px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="p-2 bg-sky-500/10 border border-sky-500/30 rounded-lg text-sky-400">
          <ShieldCheck className="w-6 h-6" />
        </div>
        <div>
          <span className="text-lg font-bold bg-gradient-to-r from-sky-400 to-indigo-400 bg-clip-text text-transparent">
            FinGuard
          </span>
          <span className="ml-2 text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
            Enterprise Monitoring
          </span>
        </div>
      </div>

      {user && (
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-sm text-slate-300">
            <UserCircle className="w-5 h-5 text-sky-400" />
            <span className="font-medium">{user.username}</span>
            <div className="flex gap-1 ml-1">
              {user.roles?.map((role) => (
                <span
                  key={role}
                  className="text-[10px] px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20"
                >
                  {role.replace('ROLE_', '')}
                </span>
              ))}
            </div>
          </div>
          <button
            onClick={logout}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-rose-400 px-3 py-1.5 rounded-lg border border-slate-700 hover:border-rose-500/30 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sign Out
          </button>
        </div>
      )}
    </header>
  );
};

export default Navbar;
