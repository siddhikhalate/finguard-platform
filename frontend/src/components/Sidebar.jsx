import React from 'react';
import { 
  LayoutDashboard, 
  ArrowLeftRight, 
  ShieldAlert, 
  AlertTriangle, 
  SlidersHorizontal,
  Bot
} from 'lucide-react';

const Sidebar = ({ activeTab, setActiveTab }) => {
  const menuItems = [
    { id: 'overview', label: 'Overview Dashboard', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transaction Hub', icon: ArrowLeftRight },
    { id: 'fraud', label: 'Fraud Risk Feed', icon: ShieldAlert },
    { id: 'incidents', label: 'Incident Workbench', icon: AlertTriangle },
    { id: 'rules', label: 'Fraud Rules Engine', icon: SlidersHorizontal },
  ];

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-900/60 p-4 flex flex-col gap-1 min-h-[calc(100vh-4rem)]">
      <div className="px-3 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">
        Operations Console
      </div>
      {menuItems.map((item) => {
        const Icon = item.icon;
        const isActive = activeTab === item.id;
        return (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
              isActive
                ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Icon className={`w-4 h-4 ${isActive ? 'text-sky-400' : 'text-slate-500'}`} />
            {item.label}
          </button>
        );
      })}

      <div className="mt-auto p-4 rounded-xl bg-gradient-to-br from-indigo-950/40 to-slate-900 border border-indigo-900/30">
        <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold">
          <Bot className="w-4 h-4" />
          Spring AI Copilot
        </div>
        <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
          Assists fraud analysts with natural language summaries and deterministic fallback resilience.
        </p>
      </div>
    </aside>
  );
};

export default Sidebar;
