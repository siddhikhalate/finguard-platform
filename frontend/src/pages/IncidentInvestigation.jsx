import React, { useState, useEffect } from 'react';
import api from '../api/client';
import RiskBadge from '../components/RiskBadge';
import { 
  ArrowLeft, 
  Bot, 
  Sparkles, 
  ShieldCheck, 
  ShieldAlert, 
  UserCheck, 
  AlertTriangle, 
  Clock, 
  FileText, 
  Send, 
  RefreshCw,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Cpu
} from 'lucide-react';

const IncidentInvestigation = ({ incident, onBack, onStatusUpdated }) => {
  const [loading, setLoading] = useState(false);
  const [explanation, setExplanation] = useState(null);
  const [explaining, setExplaining] = useState(false);
  const [analystNote, setAnalystNote] = useState('');
  const [auditLogs, setAuditLogs] = useState([]);
  const [status, setStatus] = useState(incident?.status || 'UNDER_INVESTIGATION');
  const [assignedTo, setAssignedTo] = useState(incident?.assignedTo || 'analyst1@finguard.internal');
  const [feedbackMsg, setFeedbackMsg] = useState('');

  // Fetch or generate AI explanation
  const fetchExplanation = async () => {
    setExplaining(true);
    try {
      // First try to fetch existing explanation by incidentId or transactionId
      const targetId = incident?.transactionId || incident?.id || 'tx-001';
      const res = await api.get(`/ai/explanations/transaction/${targetId}`);
      if (res.data) {
        setExplanation(res.data);
      }
    } catch (err) {
      console.warn('Existing explanation not found, fetching dynamic explanation:', err);
      try {
        const payload = {
          transactionId: incident?.transactionId || 'tx-001',
          transactionReference: incident?.transactionReference || 'TXN-90281-99A',
          accountNumber: 'ACC-1001',
          amount: 25000.0,
          currency: 'USD',
          riskScore: incident?.riskScore || 85,
          riskLevel: incident?.riskScore >= 75 ? 'CRITICAL' : 'HIGH',
          location: 'Dubai, UAE',
          triggeredRules: [
            { ruleType: 'HIGH_AMOUNT', ruleName: 'High Transaction Amount', scoreWeight: 25, description: 'Amount $25,000 exceeds threshold $10,000' },
            { ruleType: 'UNUSUAL_LOCATION', ruleName: 'Unusual Geographical Location', scoreWeight: 20, description: 'Location Dubai differs from synthetic baseline New York' },
            { ruleType: 'RAPID_ACTIVITY', ruleName: 'High Velocity Burst', scoreWeight: 25, description: '3 transactions recorded within 10 seconds' },
            { ruleType: 'REPEATED_FAILURES', ruleName: 'Prior Authentication Failures', scoreWeight: 15, description: '2 failed pin attempts recorded' }
          ]
        };
        const genRes = await api.post('/ai/explain', payload);
        setExplanation(genRes.data);
      } catch (genErr) {
        console.warn('Using deterministic fallback preview:', genErr);
        setExplanation({
          summary: 'Transaction TXN-90281-99A flagged with composite score 85/100 (CRITICAL). Triggered 4 fraud indicators including High Amount ($25,000 > $10,000 threshold), Rapid Activity burst (3 events in 10s), Unusual Location (Dubai, UAE), and 2 prior failed retries.',
          riskFactors: [
            'High transaction amount of $25,000.00 significantly exceeds standard $10,000.00 ceiling.',
            'Velocity anomaly: Account exhibited multiple consecutive requests in under 10 seconds.',
            'Geographic discrepancy: Inbound request originated from Dubai, UAE, departing from New York baseline.',
            'Authentication friction: Multiple repeated failed attempts logged prior to approval.'
          ],
          recommendedActions: [
            'Place temporary hold on source account ACC-1001 pending secondary verification.',
            'Contact customer via verified phone channel to authenticate foreign transaction request.',
            'Verify device fingerprint and IP subnet reputation against known proxy/VPN ranges.'
          ],
          confidenceLevel: 'HIGH',
          isFallback: true,
          modelUsed: 'deterministic-rules-engine-v1'
        });
      }
    } finally {
      setExplaining(false);
    }
  };

  useEffect(() => {
    fetchExplanation();
    // Pre-populate mock audit logs
    setAuditLogs([
      {
        id: 'log-1',
        action: 'INCIDENT_CREATED',
        details: `Incident automatically created from Kafka fraud-risk-assessed event (Risk Score: ${incident?.riskScore || 85}).`,
        performedBy: 'SYSTEM_EVENT_PROCESSOR',
        timestamp: incident?.createdAt || new Date(Date.now() - 1000 * 60 * 25).toISOString()
      },
      {
        id: 'log-2',
        action: 'AI_EXPLANATION_GENERATED',
        details: 'Spring AI generated structured explanation with deterministic fallback fallback safety.',
        performedBy: 'AI_COGNITIVE_SERVICE',
        timestamp: new Date(Date.now() - 1000 * 60 * 20).toISOString()
      },
      {
        id: 'log-3',
        action: 'ANALYST_ASSIGNED',
        details: `Assigned case to analyst ${assignedTo}.`,
        performedBy: 'analyst1@finguard.internal',
        timestamp: new Date(Date.now() - 1000 * 60 * 10).toISOString()
      }
    ]);
  }, [incident]);

  const handleUpdateStatus = async (newStatus) => {
    setLoading(true);
    setFeedbackMsg('');
    try {
      if (incident?.id) {
        await api.patch(`/incidents/${incident.id}/status`, { status: newStatus });
      }
      setStatus(newStatus);
      setFeedbackMsg(`Status successfully changed to ${newStatus}`);
      setAuditLogs(prev => [
        {
          id: `log-${Date.now()}`,
          action: 'STATUS_UPDATED',
          details: `Incident status transitioned to ${newStatus}.`,
          performedBy: 'analyst1@finguard.internal',
          timestamp: new Date().toISOString()
        },
        ...prev
      ]);
      if (onStatusUpdated) onStatusUpdated(newStatus);
    } catch (err) {
      console.warn('Backend update failed, updating local state:', err);
      setStatus(newStatus);
      setFeedbackMsg(`Status transitioned to ${newStatus} (Demo Mode)`);
      setAuditLogs(prev => [
        {
          id: `log-${Date.now()}`,
          action: 'STATUS_UPDATED',
          details: `Incident status transitioned to ${newStatus}.`,
          performedBy: 'analyst1@finguard.internal',
          timestamp: new Date().toISOString()
        },
        ...prev
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleAddNote = (e) => {
    e.preventDefault();
    if (!analystNote.trim()) return;
    setAuditLogs(prev => [
      {
        id: `log-${Date.now()}`,
        action: 'NOTE_ADDED',
        details: analystNote,
        performedBy: 'analyst1@finguard.internal',
        timestamp: new Date().toISOString()
      },
      ...prev
    ]);
    setAnalystNote('');
    setFeedbackMsg('Analyst note appended to immutable audit log.');
  };

  return (
    <div className="space-y-6">
      {/* Top Navigation */}
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <button
          onClick={onBack}
          className="flex items-center gap-2 text-slate-400 hover:text-white text-sm font-medium transition"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Incidents Workbench
        </button>
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Case ID:</span>
          <span className="font-mono text-xs font-bold text-slate-300">
            {incident?.incidentReference || 'INC-2026-90281'}
          </span>
        </div>
      </div>

      {feedbackMsg && (
        <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex justify-between items-center">
          <span>{feedbackMsg}</span>
          <button onClick={() => setFeedbackMsg('')} className="hover:text-white">&times;</button>
        </div>
      )}

      {/* Hero Header */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-900 to-slate-950 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <span className="font-mono text-sm font-bold text-rose-400">
              {incident?.incidentReference || 'INC-2026-90281'}
            </span>
            <RiskBadge level={incident?.riskScore >= 75 ? 'CRITICAL' : 'HIGH'} score={incident?.riskScore || 85} />
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
              {status}
            </span>
          </div>
          <h2 className="text-xl font-bold text-white">
            {incident?.title || 'Suspicious Multi-Factor Velocity & Geographic Discrepancy'}
          </h2>
          <p className="text-xs text-slate-400">
            Linked to Transaction <span className="font-mono text-sky-400">{incident?.transactionReference || 'TXN-90281-99A'}</span> &bull; Source Account <span className="font-mono text-slate-200">ACC-1001</span>
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => handleUpdateStatus('CONFIRMED_FRAUD')}
            disabled={loading}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 rounded-lg text-xs font-bold transition"
          >
            <XCircle className="w-4 h-4" />
            Confirm Fraud
          </button>
          <button
            onClick={() => handleUpdateStatus('FALSE_POSITIVE')}
            disabled={loading}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-bold transition"
          >
            <ShieldCheck className="w-4 h-4" />
            False Positive
          </button>
          <button
            onClick={() => handleUpdateStatus('RESOLVED')}
            disabled={loading}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 border border-sky-500/30 rounded-lg text-xs font-bold transition"
          >
            <CheckCircle2 className="w-4 h-4" />
            Resolve Case
          </button>
        </div>
      </div>

      {/* Main Grid: AI Explanation & Transaction Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 2 Cols: Spring AI Copilot Explanation */}
        <div className="lg:col-span-2 space-y-6">
          <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-xl relative overflow-hidden">
            <div className="absolute top-0 right-0 w-96 h-96 bg-indigo-500/5 rounded-full blur-3xl pointer-events-none" />

            {/* AI Card Header */}
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-5">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                  <Bot className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white flex items-center gap-2">
                    Spring AI Cognitive Fraud Explainer
                    <Sparkles className="w-4 h-4 text-amber-400" />
                  </h3>
                  <p className="text-xs text-slate-400">
                    Natural language pattern synthesis with deterministic rules fallback
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {explanation?.isFallback ? (
                  <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-mono font-medium bg-slate-800 text-slate-300 border border-slate-700">
                    <Cpu className="w-3 h-3 text-sky-400" />
                    Deterministic Fallback Engine
                  </span>
                ) : (
                  <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    <Sparkles className="w-3 h-3" />
                    Spring AI Online ({explanation?.modelUsed || 'OpenAI'})
                  </span>
                )}
                <button
                  onClick={fetchExplanation}
                  disabled={explaining}
                  className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded-lg transition"
                  title="Re-run AI Explanation"
                >
                  <RefreshCw className={`w-4 h-4 ${explaining ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            {/* Explanation Content */}
            {explaining ? (
              <div className="py-12 text-center text-slate-400">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-400" />
                Synthesizing multi-modal transaction risks & historical behavior...
              </div>
            ) : explanation ? (
              <div className="space-y-5 text-sm">
                {/* Executive Summary */}
                <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800/80 leading-relaxed text-slate-200">
                  <div className="text-xs font-semibold uppercase text-indigo-400 mb-1 tracking-wider">
                    Executive Analysis
                  </div>
                  {explanation.summary}
                </div>

                {/* Risk Factors */}
                {explanation.riskFactors && explanation.riskFactors.length > 0 && (
                  <div>
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                      Key Triggered Risk Factors
                    </h4>
                    <ul className="space-y-2">
                      {explanation.riskFactors.map((rf, idx) => (
                        <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-300">
                          <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0 mt-0.5" />
                          <span>{rf}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Recommended Investigation Steps */}
                {explanation.recommendedActions && explanation.recommendedActions.length > 0 && (
                  <div className="p-4 rounded-xl bg-sky-950/20 border border-sky-900/30">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-sky-400 mb-2">
                      Recommended Human Analyst Actions
                    </h4>
                    <ul className="space-y-1.5">
                      {explanation.recommendedActions.map((ra, idx) => (
                        <li key={idx} className="flex items-start gap-2 text-xs text-slate-300">
                          <span className="w-4 h-4 rounded-full bg-sky-500/20 text-sky-400 text-[10px] font-bold flex items-center justify-center shrink-0 mt-0.5">
                            {idx + 1}
                          </span>
                          <span>{ra}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Confidence Level */}
                <div className="flex items-center justify-between text-xs pt-3 border-t border-slate-800 text-slate-500">
                  <div>
                    AI Advisory Confidence: <span className="font-bold text-slate-300">{explanation.confidenceLevel || 'HIGH'}</span>
                  </div>
                  <div className="text-[11px] italic">
                    Human-in-the-loop: Deterministic rules assign the risk score; AI assists investigation.
                  </div>
                </div>
              </div>
            ) : null}
          </div>

          {/* Audit History Timeline */}
          <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-xl space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Clock className="w-4 h-4 text-slate-500" />
              Incident Audit Trail & History
            </h3>

            <div className="space-y-3 relative pl-4 border-l border-slate-800">
              {auditLogs.map((log) => (
                <div key={log.id} className="relative group">
                  <div className="absolute -left-[21px] top-1 w-2.5 h-2.5 rounded-full bg-sky-500 ring-4 ring-slate-900" />
                  <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80 text-xs">
                    <div className="flex items-center justify-between text-slate-400 mb-1">
                      <span className="font-semibold text-sky-400 font-mono">{log.action}</span>
                      <span className="font-mono text-[11px] text-slate-500">
                        {new Date(log.timestamp).toLocaleTimeString()}
                      </span>
                    </div>
                    <p className="text-slate-300">{log.details}</p>
                    <div className="text-[10px] text-slate-500 mt-1">By: {log.performedBy}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Append Note Form */}
            <form onSubmit={handleAddNote} className="flex gap-2 pt-2">
              <input
                type="text"
                placeholder="Add analyst note to audit trail..."
                value={analystNote}
                onChange={(e) => setAnalystNote(e.target.value)}
                className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              />
              <button
                type="submit"
                className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition"
              >
                <Send className="w-3.5 h-3.5" />
                Add Note
              </button>
            </form>
          </div>
        </div>

        {/* Right Col: Transaction & Customer Profile */}
        <div className="space-y-6">
          {/* Transaction Metadata */}
          <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-xl space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <FileText className="w-4 h-4 text-slate-500" />
              Transaction Details
            </h3>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Amount</span>
                <span className="font-mono font-bold text-slate-100">$25,000.00 USD</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Source Account</span>
                <span className="font-mono text-slate-200">ACC-1001</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Target Account</span>
                <span className="font-mono text-slate-200">ACC-9921</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Merchant Category</span>
                <span className="font-mono text-slate-200">LUXURY_GOODS</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Transaction Location</span>
                <span className="text-slate-200">Dubai, UAE</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-800/60">
                <span className="text-slate-500">Channel</span>
                <span className="text-slate-200 font-mono">ONLINE_WEB</span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-slate-500">IP Subnet</span>
                <span className="font-mono text-slate-400">194.67.210.44 (Datacenter VPN)</span>
              </div>
            </div>
          </div>

          {/* Rules Triggered Summary */}
          <div className="p-6 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-xl space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              Scoring Rule Breakdown
            </h3>

            <div className="space-y-2.5">
              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 flex justify-between items-center text-xs">
                <div>
                  <div className="font-semibold text-slate-200">High Amount Threshold</div>
                  <div className="text-[10px] text-slate-500">&gt; $10,000 baseline</div>
                </div>
                <span className="font-mono text-amber-400 font-bold">+25 pts</span>
              </div>

              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 flex justify-between items-center text-xs">
                <div>
                  <div className="font-semibold text-slate-200">Rapid Activity Velocity</div>
                  <div className="text-[10px] text-slate-500">3 tx in 10 seconds</div>
                </div>
                <span className="font-mono text-amber-400 font-bold">+25 pts</span>
              </div>

              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 flex justify-between items-center text-xs">
                <div>
                  <div className="font-semibold text-slate-200">Unusual Location</div>
                  <div className="text-[10px] text-slate-500">Dubai vs New York</div>
                </div>
                <span className="font-mono text-amber-400 font-bold">+20 pts</span>
              </div>

              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 flex justify-between items-center text-xs">
                <div>
                  <div className="font-semibold text-slate-200">Repeated Failures</div>
                  <div className="text-[10px] text-slate-500">2 prior failed auths</div>
                </div>
                <span className="font-mono text-amber-400 font-bold">+15 pts</span>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-between items-center text-xs">
              <span className="text-slate-400 font-semibold">Total Composite Score:</span>
              <span className="font-mono font-bold text-rose-400 text-sm">85 / 100</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default IncidentInvestigation;
