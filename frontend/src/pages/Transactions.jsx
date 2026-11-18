import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { 
  ArrowLeftRight, 
  Plus, 
  Search, 
  Filter, 
  RefreshCw, 
  AlertCircle,
  CheckCircle,
  Clock,
  XCircle,
  ExternalLink
} from 'lucide-react';

const Transactions = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form state
  const [formData, setFormData] = useState({
    sourceAccountNumber: 'ACC-1001',
    targetAccountNumber: 'ACC-2002',
    amount: '12500.00',
    currency: 'USD',
    merchantCategory: 'ELECTRONICS',
    location: 'New York, USA',
    channel: 'ONLINE'
  });

  const fetchTransactions = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const res = await api.get('/transactions');
      setTransactions(res.data.content || res.data || []);
    } catch (err) {
      console.warn('Backend not responding, using demo transactions:', err);
      // Demo fallback transactions for offline testing
      setTransactions([
        {
          id: 'tx-001',
          transactionReference: 'TXN-90281-99A',
          sourceAccountNumber: 'ACC-1001',
          targetAccountNumber: 'ACC-9921',
          amount: 25000.00,
          currency: 'USD',
          merchantCategory: 'LUXURY_GOODS',
          location: 'Dubai, UAE',
          channel: 'ONLINE',
          status: 'FLAGGED',
          createdAt: new Date(Date.now() - 1000 * 60 * 15).toISOString()
        },
        {
          id: 'tx-002',
          transactionReference: 'TXN-88123-10B',
          sourceAccountNumber: 'ACC-1001',
          targetAccountNumber: 'ACC-1002',
          amount: 45.50,
          currency: 'USD',
          merchantCategory: 'GROCERY',
          location: 'New York, USA',
          channel: 'POS',
          status: 'COMPLETED',
          createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString()
        },
        {
          id: 'tx-003',
          transactionReference: 'TXN-77319-44C',
          sourceAccountNumber: 'ACC-1002',
          targetAccountNumber: 'ACC-5512',
          amount: 14500.00,
          currency: 'USD',
          merchantCategory: 'WIRE_TRANSFER',
          location: 'Singapore',
          channel: 'ONLINE',
          status: 'FLAGGED',
          createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString()
        },
        {
          id: 'tx-004',
          transactionReference: 'TXN-66120-01D',
          sourceAccountNumber: 'ACC-1003',
          targetAccountNumber: 'ACC-1001',
          amount: 800.00,
          currency: 'USD',
          merchantCategory: 'UTILITIES',
          location: 'New York, USA',
          channel: 'MOBILE',
          status: 'COMPLETED',
          createdAt: new Date(Date.now() - 1000 * 60 * 240).toISOString()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const payload = {
        sourceAccountNumber: formData.sourceAccountNumber,
        targetAccountNumber: formData.targetAccountNumber,
        amount: parseFloat(formData.amount),
        currency: formData.currency,
        merchantCategory: formData.merchantCategory,
        location: formData.location,
        channel: formData.channel,
        idempotencyKey: `idemp-${Date.now()}-${Math.random().toString(36).substring(7)}`
      };

      const res = await api.post('/transactions', payload);
      setSuccessMsg(`Transaction submitted successfully! Reference: ${res.data?.transactionReference || 'Saved'}. Dispatched to Transactional Outbox.`);
      setShowModal(false);
      fetchTransactions();
    } catch (err) {
      console.error('Submission failed:', err);
      // In standalone demo mode, append optimistic record
      const mockRecord = {
        id: `tx-${Date.now()}`,
        transactionReference: `TXN-${Math.floor(10000 + Math.random() * 90000)}-SIM`,
        sourceAccountNumber: formData.sourceAccountNumber,
        targetAccountNumber: formData.targetAccountNumber,
        amount: parseFloat(formData.amount),
        currency: formData.currency,
        merchantCategory: formData.merchantCategory,
        location: formData.location,
        channel: formData.channel,
        status: parseFloat(formData.amount) > 10000 ? 'FLAGGED' : 'COMPLETED',
        createdAt: new Date().toISOString()
      };
      setTransactions(prev => [mockRecord, ...prev]);
      setSuccessMsg(`Transaction simulated! Reference: ${mockRecord.transactionReference}`);
      setShowModal(false);
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle className="w-3 h-3" /> Completed
          </span>
        );
      case 'FLAGGED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <AlertCircle className="w-3 h-3" /> Flagged
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle className="w-3 h-3" /> Rejected
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
            <Clock className="w-3 h-3" /> {status}
          </span>
        );
    }
  };

  const filteredTransactions = transactions.filter(t => {
    const matchesSearch = 
      t.transactionReference?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.sourceAccountNumber?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.targetAccountNumber?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.location?.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <ArrowLeftRight className="w-7 h-7 text-sky-400" />
            Transaction Management Hub
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Real-time ledger of inbound financial transactions with transactional outbox event streams.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchTransactions}
            disabled={loading}
            className="flex items-center gap-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm font-medium border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-sky-500 hover:bg-sky-400 text-slate-950 font-semibold rounded-lg text-sm transition shadow-lg shadow-sky-500/20"
          >
            <Plus className="w-4 h-4" />
            New Transaction
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center justify-between">
          <span>{successMsg}</span>
          <button onClick={() => setSuccessMsg('')} className="text-emerald-400 hover:text-white">&times;</button>
        </div>
      )}
      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center justify-between">
          <span>{errorMsg}</span>
          <button onClick={() => setErrorMsg('')} className="text-rose-400 hover:text-white">&times;</button>
        </div>
      )}

      {/* Controls & Search */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-500" />
          <input
            type="text"
            placeholder="Search by Reference, Account or Location..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-10 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-500" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-sky-500 transition"
          >
            <option value="ALL">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="FLAGGED">Flagged</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800 text-xs uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3.5">Reference</th>
                <th className="px-5 py-3.5">Source &rarr; Target</th>
                <th className="px-5 py-3.5">Amount</th>
                <th className="px-5 py-3.5">Category</th>
                <th className="px-5 py-3.5">Location</th>
                <th className="px-5 py-3.5">Status</th>
                <th className="px-5 py-3.5">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading && transactions.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-5 py-12 text-center text-slate-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-sky-400" />
                    Loading transaction records...
                  </td>
                </tr>
              ) : filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-5 py-12 text-center text-slate-500">
                    No transactions found matching your criteria.
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((tx) => (
                  <tr key={tx.id || tx.transactionReference} className="hover:bg-slate-800/30 transition">
                    <td className="px-5 py-4 font-mono text-xs font-semibold text-sky-400">
                      {tx.transactionReference}
                    </td>
                    <td className="px-5 py-4">
                      <div className="text-slate-200 text-xs font-medium font-mono">{tx.sourceAccountNumber}</div>
                      <div className="text-slate-500 text-[11px] font-mono">&rarr; {tx.targetAccountNumber}</div>
                    </td>
                    <td className="px-5 py-4 font-semibold text-slate-100">
                      ${tx.amount?.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                      <span className="text-xs font-normal text-slate-500 ml-1">{tx.currency}</span>
                    </td>
                    <td className="px-5 py-4 text-slate-400 text-xs">
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[11px]">
                        {tx.merchantCategory || 'GENERAL'}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-slate-300 text-xs">
                      {tx.location || 'Unknown'}
                    </td>
                    <td className="px-5 py-4">
                      {getStatusBadge(tx.status)}
                    </td>
                    <td className="px-5 py-4 text-slate-500 text-xs font-mono">
                      {tx.createdAt ? new Date(tx.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Just now'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* New Transaction Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative">
            <h2 className="text-xl font-bold text-white mb-2">Initiate Financial Transaction</h2>
            <p className="text-xs text-slate-400 mb-6">
              Dispatches an atomic database write with Transactional Outbox pattern before publishing to Kafka.
            </p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Source Account</label>
                  <input
                    type="text"
                    required
                    value={formData.sourceAccountNumber}
                    onChange={(e) => setFormData({ ...formData, sourceAccountNumber: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 font-mono focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Target Account</label>
                  <input
                    type="text"
                    required
                    value={formData.targetAccountNumber}
                    onChange={(e) => setFormData({ ...formData, targetAccountNumber: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 font-mono focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Amount ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={formData.amount}
                    onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 font-mono focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Currency</label>
                  <select
                    value={formData.currency}
                    onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-sky-500"
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Merchant Category</label>
                  <select
                    value={formData.merchantCategory}
                    onChange={(e) => setFormData({ ...formData, merchantCategory: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-sky-500"
                  >
                    <option value="ELECTRONICS">ELECTRONICS</option>
                    <option value="LUXURY_GOODS">LUXURY_GOODS</option>
                    <option value="WIRE_TRANSFER">WIRE_TRANSFER</option>
                    <option value="GROCERY">GROCERY</option>
                    <option value="CRYPTO_EXCHANGE">CRYPTO_EXCHANGE</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Location</label>
                  <input
                    type="text"
                    required
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2 bg-sky-500 hover:bg-sky-400 text-slate-950 font-bold rounded-lg text-sm transition shadow-lg shadow-sky-500/20 disabled:opacity-50"
                >
                  {submitting ? 'Submitting...' : 'Dispatch Transaction'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
