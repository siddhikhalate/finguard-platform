import React, { useState, useEffect } from 'react';
import api from '../api/client';
import RiskBadge from '../components/RiskBadge';
import { 
  AlertTriangle, 
  Search, 
  Filter, 
  RefreshCw, 
  UserCheck, 
  Clock, 
  CheckCircle2, 
  XCircle, 
  ShieldAlert,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';

const Incidents = ({ onInvestigate }) => {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchIncidents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/incidents?size=25');
      setIncidents(res.data.content || res.data || []);
    } catch (err) {
      console.warn('Backend not responding, using fallback incidents:', err);
      // Realistic incidents for testing
      setIncidents([
        {
          id: 'inc-001',
          incidentReference: 'INC-2026-90281',
          transactionReference: 'TXN-90281-99A',
          transactionId: 'tx-001',
          status: 'UNDER_INVESTIGATION',
          priority: 'CRITICAL',
          riskScore: 85,
          assignedTo: 'analyst1@finguard.internal',
          title: 'Multiple rapid velocity anomalies & foreign terminal',
          summary: 'Transaction amount $25,000 flagged in Dubai with 3 rapid bursts within 10 seconds.',
          createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
          updatedAt: new Date(Date.now() - 1000 * 60 * 5).toISOString()
        },
        {
          id: 'inc-002',
          incidentReference: 'INC-2026-77319',
          transactionReference: 'TXN-77319-44C',
          transactionId: 'tx-003',
          status: 'NEW',
          priority: 'HIGH',
          riskScore: 65,
          assignedTo: null,
          title: 'High amount wire transfer from new region',
          summary: 'Transaction amount $14,500 routed via foreign wire terminal.',
          createdAt: new Date(Date.now() - 1000 * 60 * 60).toISOString(),
          updatedAt: new Date(Date.now() - 1000 * 60 * 60).toISOString()
        },
        {
          id: 'inc-003',
          incidentReference: 'INC-2026-66120',
          transactionReference: 'TXN-66120-01D',
          transactionId: 'tx-004',
          status: 'CONFIRMED_FRAUD',
          priority: 'CRITICAL',
          riskScore: 90,
          assignedTo: 'analyst2@finguard.internal',
          title: 'Stolen card credential stuffing attack confirmed',
          summary: 'Cardholder confirmed account compromise via phone verification.',
          createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
          updatedAt: new Date(Date.now() - 1000 * 60 * 30).toISOString()
        },
        {
          id: 'inc-004',
          incidentReference: 'INC-2026-55410',
          transactionReference: 'TXN-55410-88F',
          transactionId: 'tx-006',
          status: 'RESOLVED',
          priority: 'HIGH',
          riskScore: 55,
          assignedTo: 'analyst1@finguard.internal',
          title: 'Legitimate international travel purchase verified',
          summary: 'Customer verified travel notice on mobile banking app.',
          createdAt: new Date(Date.now() - 1000 * 60 * 360).toISOString(),
          updatedAt: new Date(Date.now() - 1000 * 60 * 120).toISOString()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'NEW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-500/10 text-sky-400 border border-sky-500/20">
            <Clock className="w-3 h-3" /> New
          </span>
        );
      case 'UNDER_INVESTIGATION':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <UserCheck className="w-3 h-3" /> Investigating
          </span>
        );
      case 'CONFIRMED_FRAUD':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle className="w-3 h-3" /> Confirmed Fraud
          </span>
        );
      case 'RESOLVED':
      case 'FALSE_POSITIVE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <ShieldCheck className="w-3 h-3" /> {status === 'FALSE_POSITIVE' ? 'False Positive' : 'Resolved'}
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
            {status}
          </span>
        );
    }
  };

  const getPriorityBadge = (priority) => {
    switch (priority) {
      case 'CRITICAL':
        return <span className="font-bold text-rose-400 text-xs">CRITICAL</span>;
      case 'HIGH':
        return <span className="font-semibold text-amber-400 text-xs">HIGH</span>;
      case 'MEDIUM':
        return <span className="font-medium text-sky-400 text-xs">MEDIUM</span>;
      default:
        return <span className="text-slate-400 text-xs">LOW</span>;
    }
  };

  const filtered = incidents.filter(i => {
    const matchesStatus = statusFilter === 'ALL' || i.status === statusFilter;
    const matchesPriority = priorityFilter === 'ALL' || i.priority === priorityFilter;
    const matchesSearch = 
      i.incidentReference?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.transactionReference?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.title?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesPriority && matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <AlertTriangle className="w-7 h-7 text-rose-400" />
            Fraud Incident Workbench
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Triage, investigate, and resolve high-risk fraud cases with automated audit logging.
          </p>
        </div>
        <button
          onClick={fetchIncidents}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm font-medium border border-slate-700 transition"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-500" />
          <input
            type="text"
            placeholder="Search by Incident Ref, Tx Ref or Summary..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-10 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-rose-500 transition"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-500" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-rose-500 transition"
          >
            <option value="ALL">All Statuses</option>
            <option value="NEW">New</option>
            <option value="UNDER_INVESTIGATION">Under Investigation</option>
            <option value="CONFIRMED_FRAUD">Confirmed Fraud</option>
            <option value="FALSE_POSITIVE">False Positive</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-rose-500 transition"
          >
            <option value="ALL">All Priorities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
          </select>
        </div>
      </div>

      {/* Incidents Table */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-xs uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3.5">Incident Reference</th>
                <th className="px-5 py-3.5">Tx Reference</th>
                <th className="px-5 py-3.5">Priority & Score</th>
                <th className="px-5 py-3.5">Status</th>
                <th className="px-5 py-3.5">Assigned Analyst</th>
                <th className="px-5 py-3.5">Created</th>
                <th className="px-5 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading && incidents.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-5 py-12 text-center text-slate-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-rose-400" />
                    Loading incidents queue...
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-5 py-12 text-center text-slate-500">
                    No incidents match your filter criteria.
                  </td>
                </tr>
              ) : (
                filtered.map((inc) => (
                  <tr key={inc.id || inc.incidentReference} className="hover:bg-slate-800/30 transition">
                    <td className="px-5 py-4">
                      <div className="font-mono text-xs font-bold text-rose-400">
                        {inc.incidentReference}
                      </div>
                      <div className="text-slate-400 text-xs mt-0.5 line-clamp-1 max-w-xs">
                        {inc.title}
                      </div>
                    </td>
                    <td className="px-5 py-4 font-mono text-xs text-sky-400 font-medium">
                      {inc.transactionReference}
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        {getPriorityBadge(inc.priority)}
                        <span className="text-slate-600">&bull;</span>
                        <span className="font-mono text-xs font-bold text-slate-200">{inc.riskScore} pts</span>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      {getStatusBadge(inc.status)}
                    </td>
                    <td className="px-5 py-4 text-xs">
                      {inc.assignedTo ? (
                        <span className="text-slate-300 font-medium">{inc.assignedTo}</span>
                      ) : (
                        <span className="text-slate-500 italic">Unassigned</span>
                      )}
                    </td>
                    <td className="px-5 py-4 text-slate-500 text-xs font-mono">
                      {inc.createdAt ? new Date(inc.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recently'}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <button
                        onClick={() => onInvestigate && onInvestigate(inc)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 border border-sky-500/30 rounded-lg text-xs font-semibold transition"
                      >
                        Investigate <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Incidents;
