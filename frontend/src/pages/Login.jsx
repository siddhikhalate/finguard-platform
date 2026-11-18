import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Lock, User, AlertCircle } from 'lucide-react';

const Login = () => {
  const { login, loading } = useAuth();
  const [username, setUsername] = useState('analyst');
  const [password, setPassword] = useState('Password@123');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const res = await login(username, password);
    if (!res.success) {
      setError(res.error);
    }
  };

  const fillCredentials = (userRole) => {
    if (userRole === 'admin') {
      setUsername('admin');
      setPassword('Password@123');
    } else if (userRole === 'analyst') {
      setUsername('analyst');
      setPassword('Password@123');
    } else {
      setUsername('user');
      setPassword('Password@123');
    }
  };

  return (
    <div className="min-h-screen bg-[#0b1120] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-8">
        <div className="text-center mb-8">
          <div className="inline-flex p-3 bg-sky-500/10 border border-sky-500/30 rounded-xl text-sky-400 mb-3">
            <ShieldCheck className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold text-slate-100">FinGuard Platform</h1>
          <p className="text-sm text-slate-400 mt-1">Enterprise Financial Crime & Fraud Monitoring</p>
        </div>

        {error && (
          <div className="mb-6 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-center gap-2 text-rose-400 text-sm">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1.5">Username or Email</label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full pl-9 pr-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-100 focus:outline-none focus:border-sky-500"
                placeholder="Enter username"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1.5">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full pl-9 pr-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-100 focus:outline-none focus:border-sky-500"
                placeholder="Enter password"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 bg-sky-600 hover:bg-sky-500 text-white font-medium rounded-lg text-sm shadow-lg shadow-sky-600/20 transition-all disabled:opacity-50"
          >
            {loading ? 'Authenticating...' : 'Sign In to Portal'}
          </button>

          <button
            type="button"
            onClick={() => login('analyst', 'Password@123')}
            className="w-full py-2.5 px-4 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-sky-400 font-medium rounded-lg text-sm transition-all flex items-center justify-center gap-2"
          >
            <span>⚡ Explore in Demo Mode</span>
            <span className="text-[11px] text-slate-400 font-normal">(Instant Access)</span>
          </button>
        </form>

        <div className="mt-6 pt-4 border-t border-slate-800">
          <p className="text-xs text-slate-400 text-center mb-3">Quick Fill Demo Credentials:</p>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => fillCredentials('analyst')}
              className="py-1.5 px-2 text-xs font-medium bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded text-sky-400 transition-colors"
            >
              Analyst
            </button>
            <button
              type="button"
              onClick={() => fillCredentials('admin')}
              className="py-1.5 px-2 text-xs font-medium bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded text-indigo-400 transition-colors"
            >
              Admin
            </button>
            <button
              type="button"
              onClick={() => fillCredentials('user')}
              className="py-1.5 px-2 text-xs font-medium bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded text-emerald-400 transition-colors"
            >
              User
            </button>
          </div>
          <p className="text-[11px] text-slate-500 text-center mt-3 leading-relaxed">
            Note: If backend services on port 8080 are not running, clicking Sign In or Explore automatically launches offline demo mode.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
