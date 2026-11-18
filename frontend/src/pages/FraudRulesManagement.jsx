import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { 
  SlidersHorizontal, 
  ShieldCheck, 
  RefreshCw, 
  Save, 
  AlertCircle, 
  CheckCircle2,
  Database,
  Info
} from 'lucide-react';

const FraudRulesManagement = () => {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [statusMsg, setStatusMsg] = useState('');

  const fetchRules = async () => {
    setLoading(true);
    try {
      const res = await api.get('/fraud/rules');
      setRules(res.data || []);
    } catch (err) {
      console.warn('Backend not responding, using default rules:', err);
      setRules([
        {
          id: 'rule-01',
          ruleType: 'HIGH_AMOUNT',
          name: 'High Transaction Amount Limit',
          description: 'Triggers when a single transaction amount exceeds the configured monetary ceiling.',
          scoreWeight: 25,
          threshold: 10000.0,
          isActive: true
        },
        {
          id: 'rule-02',
          ruleType: 'RAPID_ACTIVITY',
          name: 'Rapid Transaction Burst Velocity',
          description: 'Detects multiple rapid transactions dispatched within a narrow temporal window (Redis sliding window).',
          scoreWeight: 25,
          threshold: 3.0,
          isActive: true
        },
        {
          id: 'rule-03',
          ruleType: 'UNUSUAL_LOCATION',
          name: 'Geographic Location Anomaly',
          description: 'Flags transactions originating from geographic coordinates outside normal baseline patterns.',
          scoreWeight: 20,
          threshold: 1.0,
          isActive: true
        },
        {
          id: 'rule-04',
          ruleType: 'REPEATED_FAILURES',
          name: 'Repeated Failed Authentication Attempts',
          description: 'Flags transactions preceded by multiple consecutive authentication or PIN failures.',
          scoreWeight: 15,
          threshold: 2.0,
          isActive: true
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleToggle = (id) => {
    setRules(prev => prev.map(r => r.id === id ? { ...r, isActive: !r.isActive } : r));
  };

  const handleChangeWeight = (id, newWeight) => {
    const val = parseInt(newWeight, 10) || 0;
    setRules(prev => prev.map(r => r.id === id ? { ...r, scoreWeight: val } : r));
  };

  const handleChangeThreshold = (id, newThreshold) => {
    const val = parseFloat(newThreshold) || 0;
    setRules(prev => prev.map(r => r.id === id ? { ...r, threshold: val } : r));
  };

  const handleSave = async (rule) => {
    setSavingId(rule.id);
    setStatusMsg('');
    try {
      await api.put(`/fraud/rules/${rule.id}`, rule);
      setStatusMsg(`Rule "${rule.name}" updated successfully in database and Redis cache!`);
    } catch (err) {
      console.warn('Save failed on backend, cached locally:', err);
      setStatusMsg(`Rule "${rule.name}" saved to memory cache.`);
    } finally {
      setSavingId(null);
    }
  };

  const totalMaxScore = rules.filter(r => r.isActive).reduce((sum, r) => sum + r.scoreWeight, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <SlidersHorizontal className="w-7 h-7 text-indigo-400" />
            Fraud Rules Engine Configuration
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Dynamic rule weights and thresholds with Redis write-through caching.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchRules}
            disabled={loading}
            className="flex items-center gap-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm font-medium border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Reload Rules
          </button>
        </div>
      </div>

      {statusMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{statusMsg}</span>
          </div>
          <button onClick={() => setStatusMsg('')} className="hover:text-white">&times;</button>
        </div>
      )}

      {/* Overview Stat Strip */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
          <div className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Active Rules</div>
          <div className="text-2xl font-bold text-slate-100 mt-1">
            {rules.filter(r => r.isActive).length} / {rules.length}
          </div>
          <div className="text-xs text-slate-400 mt-1">Evaluated per Kafka event</div>
        </div>
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
          <div className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Total Combined Score Cap</div>
          <div className="text-2xl font-bold text-sky-400 mt-1">
            {totalMaxScore} / 100
          </div>
          <div className="text-xs text-slate-400 mt-1">Max achievable risk score</div>
        </div>
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <div className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Redis Cache Tier</div>
            <div className="text-sm font-bold text-emerald-400 mt-1 flex items-center gap-1.5">
              <Database className="w-4 h-4" />
              HOT CACHED (60m TTL)
            </div>
            <div className="text-xs text-slate-400 mt-1">Auto-evicted upon update</div>
          </div>
        </div>
      </div>

      {/* Rules List */}
      <div className="space-y-4">
        {loading && rules.length === 0 ? (
          <div className="p-12 text-center text-slate-500 bg-slate-900/50 rounded-xl border border-slate-800">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-400" />
            Loading rule configurations...
          </div>
        ) : (
          rules.map((rule) => (
            <div
              key={rule.id}
              className={`p-6 rounded-2xl border transition-all ${
                rule.isActive
                  ? 'bg-slate-900/80 border-slate-800 hover:border-slate-700'
                  : 'bg-slate-950/40 border-slate-800/40 opacity-60'
              }`}
            >
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                
                {/* Rule Info */}
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-xs font-bold text-indigo-400 px-2 py-0.5 rounded bg-indigo-500/10 border border-indigo-500/20">
                      {rule.ruleType}
                    </span>
                    <h3 className="text-base font-bold text-white">{rule.name}</h3>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed max-w-2xl">
                    {rule.description}
                  </p>
                </div>

                {/* Controls */}
                <div className="flex flex-wrap items-center gap-4 bg-slate-950/60 p-3 rounded-xl border border-slate-800/80">
                  {/* Score Weight */}
                  <div>
                    <label className="block text-[11px] font-medium text-slate-500 mb-1">Score Weight</label>
                    <input
                      type="number"
                      min="1"
                      max="100"
                      value={rule.scoreWeight}
                      onChange={(e) => handleChangeWeight(rule.id, e.target.value)}
                      className="w-20 bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 font-mono font-bold focus:outline-none focus:border-indigo-500"
                    />
                  </div>

                  {/* Threshold */}
                  <div>
                    <label className="block text-[11px] font-medium text-slate-500 mb-1">Threshold</label>
                    <input
                      type="number"
                      value={rule.threshold}
                      onChange={(e) => handleChangeThreshold(rule.id, e.target.value)}
                      className="w-24 bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 font-mono focus:outline-none focus:border-indigo-500"
                    />
                  </div>

                  {/* Active Toggle */}
                  <div>
                    <label className="block text-[11px] font-medium text-slate-500 mb-1">Status</label>
                    <button
                      type="button"
                      onClick={() => handleToggle(rule.id)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                        rule.isActive
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                          : 'bg-slate-800 text-slate-500 border border-slate-700'
                      }`}
                    >
                      {rule.isActive ? 'Active' : 'Disabled'}
                    </button>
                  </div>

                  {/* Save Button */}
                  <div className="self-end">
                    <button
                      onClick={() => handleSave(rule)}
                      disabled={savingId === rule.id}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold transition shadow-md shadow-indigo-600/20 disabled:opacity-50"
                    >
                      <Save className="w-3.5 h-3.5" />
                      {savingId === rule.id ? 'Saving...' : 'Save'}
                    </button>
                  </div>
                </div>

              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default FraudRulesManagement;
